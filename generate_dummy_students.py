import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timedelta
import random

# Initialize Firebase Admin SDK
# You'll need to replace 'path/to/serviceAccountKey.json' with your actual service account key path
cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred)

db = firestore.client()

def generate_student_id():
    return firestore.client().collection('students').document().id

def generate_dummy_phone():
    return f"+91{random.randint(7000000000, 9999999999)}"

def generate_student(class_num, student_num):
    student_id = generate_student_id()
    
    # Generate admission date within last 2 years
    admission_date = (datetime.now() - timedelta(days=random.randint(0, 730))).strftime("%Y-%m-%d")
    
    # Generate birth date for appropriate age (between 5-7 years old for class 1, 6-8 for class 2)
    base_year = datetime.now().year - (5 + class_num)
    birth_date = f"{base_year}-{random.randint(1, 12):02d}-{random.randint(1, 28):02d}"
    
    student = {
        "id": student_id,
        "userId": student_id,  # Using same ID for both fields
        "firstName": f"Clss{class_num}_Std{student_num:02d}",
        "lastName": "Student",
        "email": f"class{class_num}.student{student_num:02d}@example.com",
        "className": f"Class {class_num}",
        "rollNumber": f"{class_num}{student_num:02d}",
        "phoneNumber": generate_dummy_phone(),
        "address": f"Address {student_num}, Street {class_num}, City",
        "parentName": f"Parent of Clss{class_num}_Std{student_num:02d}",
        "parentPhone": generate_dummy_phone(),
        "dateOfBirth": birth_date,
        "gender": random.choice(["Male", "Female"]),
        "admissionNumber": f"ADM{class_num}{student_num:02d}2023",
        "admissionDate": admission_date,
        "isActive": True
    }
    return student

def main():
    students_ref = db.collection('students')
    
    # Generate and upload students for Class 1 and 2
    for class_num in range(1, 3):  # Class 1 and 2
        for student_num in range(1, 16):  # 15 students per class
            student_data = generate_student(class_num, student_num)
            
            # Upload to Firestore
            doc_ref = students_ref.document(student_data['id'])
            doc_ref.set(student_data)
            print(f"Created student: {student_data['firstName']}")

if __name__ == "__main__":
    main()
    print("Successfully generated and uploaded 30 dummy students!") 