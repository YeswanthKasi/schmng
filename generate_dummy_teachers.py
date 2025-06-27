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

# List of subjects for random assignment
SUBJECTS = [
    "Mathematics",
    "Science",
    "English",
    "Social Studies",
    "Physical Education",
    "Art",
    "Music",
    "Computer Science",
    "Environmental Science",
    "Language Arts"
]

# List of classes
CLASSES = ["Class 1", "Class 2", "Class 3", "Class 4", "Class 5"]

# List of departments
DEPARTMENTS = [
    "Science Department",
    "Mathematics Department",
    "Languages Department",
    "Arts Department",
    "Physical Education Department"
]

FIRST_NAMES = [
    "John", "Emma", "Michael", "Sarah", "David",
    "Lisa", "James", "Emily", "Robert", "Anna"
]

LAST_NAMES = [
    "Smith", "Johnson", "Williams", "Brown", "Jones",
    "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
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

def generate_teacher_id():
    return firestore.client().collection('teachers').document().id

def generate_teacher(teacher_num):
    teacher_id = generate_teacher_id()
    gender = random.choice(["Male", "Female"])
    first_name = random.choice(FIRST_NAMES)
    last_name = random.choice(LAST_NAMES)
    dob = generate_date_of_birth()
    
    # Randomly select 2-3 subjects for each teacher
    num_subjects = random.randint(2, 3)
    assigned_subjects = random.sample(SUBJECTS, num_subjects)
    
    # Randomly assign 1-2 classes
    num_classes = random.randint(1, 2)
    assigned_classes = random.sample(CLASSES, num_classes)
    
    teacher = {
        "id": teacher_id,
        "firstName": first_name,
        "lastName": last_name,
        "email": f"{first_name.lower()}.{last_name.lower()}@school.com",
        "phone": generate_phone(),
        "mobileNo": generate_phone(),
        "type": "teacher",
        "subjects": assigned_subjects,
        "classes": assigned_classes,
        "gender": gender,
        "dateOfBirth": dob,
        "address": f"{random.randint(100, 999)} School Street, City",
        "age": datetime.now().year - int(dob.split('-')[0]),
        "department": random.choice(DEPARTMENTS),
        "designation": "Teacher",
        "password": "password123"  # Default password
    }
    return teacher

def main():
    teachers_ref = db.collection('teachers')
    users_ref = db.collection('users')
    
    # Generate and upload 10 teachers
    for teacher_num in range(1, 11):
        teacher_data = generate_teacher(teacher_num)
        
        # Upload to Firestore teachers collection
        doc_ref = teachers_ref.document(teacher_data['id'])
        doc_ref.set(teacher_data)
        
        # Add to users collection
        user_data = {
            "role": "teacher",
            "email": teacher_data["email"],
            "name": f"{teacher_data['firstName']} {teacher_data['lastName']}",
            "createdAt": firestore.SERVER_TIMESTAMP,
            "userId": teacher_data["id"],
            "type": "teacher",
            "department": teacher_data["department"]
        }
        users_ref.document(teacher_data['id']).set(user_data)
        
        print(f"Created teacher: {teacher_data['firstName']} {teacher_data['lastName']}")
        print(f"Email: {teacher_data['email']}")
        print(f"Department: {teacher_data['department']}")
        print(f"Subjects: {', '.join(teacher_data['subjects'])}")
        print(f"Classes: {', '.join(teacher_data['classes'])}")
        print("-" * 50)

if __name__ == "__main__":
    main()
    print("\nSuccessfully generated and uploaded 10 dummy teachers!") 