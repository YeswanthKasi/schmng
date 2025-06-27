import firebase_admin
from firebase_admin import credentials, firestore
import random
from datetime import datetime, timedelta

# Initialize Firebase Admin SDK (if not already initialized)
try:
    app = firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate('serviceAccountKey.json')
    firebase_admin.initialize_app(cred)

db = firestore.client()

# List of departments and their corresponding designations
DEPARTMENT_DESIGNATIONS = {
    "Administration": ["Administrative Assistant", "Office Manager", "Receptionist"],
    "Maintenance": ["Maintenance Supervisor", "Janitor", "Groundskeeper"],
    "Library": ["Librarian", "Library Assistant", "Media Specialist"],
    "IT Support": ["IT Manager", "System Administrator", "Technical Support"],
    "Accounts": ["Accountant", "Finance Manager", "Accounts Clerk"],
    "Security": ["Security Supervisor", "Security Guard", "Gate Officer"],
    "Others": ["Nurse", "Counselor", "Career Advisor"]
}

FIRST_NAMES = [
    "James", "Mary", "Robert", "Patricia", "John",
    "Jennifer", "Michael", "Linda", "William", "Elizabeth",
    "David", "Barbara", "Richard", "Susan", "Joseph"
]

LAST_NAMES = [
    "Smith", "Johnson", "Williams", "Brown", "Jones",
    "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
    "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson"
]

def generate_phone():
    return f"+1{random.randint(100, 999)}{random.randint(100, 999)}{random.randint(1000, 9999)}"

def generate_date_of_birth():
    # Generate date of birth for someone between 25 and 60 years old
    today = datetime.now()
    days_in_year = 365.25
    max_age_days = int(60 * days_in_year)
    min_age_days = int(25 * days_in_year)
    days_ago = random.randint(min_age_days, max_age_days)
    birth_date = today - timedelta(days=days_ago)
    return birth_date.strftime("%Y-%m-%d")

def generate_staff_id():
    return firestore.client().collection('non_teaching_staff').document().id

def generate_staff_member():
    staff_id = generate_staff_id()
    gender = random.choice(["Male", "Female"])
    first_name = random.choice(FIRST_NAMES)
    last_name = random.choice(LAST_NAMES)
    department = random.choice(list(DEPARTMENT_DESIGNATIONS.keys()))
    designation = random.choice(DEPARTMENT_DESIGNATIONS[department])
    dob = generate_date_of_birth()
    
    staff = {
        "id": staff_id,
        "firstName": first_name,
        "lastName": last_name,
        "email": f"{first_name.lower()}.{last_name.lower()}@school.com",
        "phone": generate_phone(),
        "mobileNo": generate_phone(),
        "type": "staff",
        "gender": gender,
        "dateOfBirth": dob,
        "address": f"{random.randint(100, 999)} School Street, City",
        "age": datetime.now().year - int(dob.split('-')[0]),
        "department": department,
        "designation": designation,
        "password": "password123"  # Default password
    }
    return staff

def main():
    staff_ref = db.collection('non_teaching_staff')
    users_ref = db.collection('users')
    
    # Generate and upload 10 staff members
    for i in range(10):
        staff_data = generate_staff_member()
        
        # Upload to Firestore staff collection
        doc_ref = staff_ref.document(staff_data['id'])
        doc_ref.set(staff_data)
        
        # Add to users collection
        user_data = {
            "role": "staff",
            "email": staff_data["email"],
            "name": f"{staff_data['firstName']} {staff_data['lastName']}",
            "createdAt": firestore.SERVER_TIMESTAMP,
            "userId": staff_data["id"],
            "type": "staff",
            "department": staff_data["department"]
        }
        users_ref.document(staff_data['id']).set(user_data)
        
        print(f"Created staff member: {staff_data['firstName']} {staff_data['lastName']}")
        print(f"Email: {staff_data['email']}")
        print(f"Department: {staff_data['department']}")
        print(f"Designation: {staff_data['designation']}")
        print("-" * 50)

if __name__ == "__main__":
    main()
    print("\nSuccessfully generated and uploaded 10 dummy staff members!") 