# School Management App (schmng) - Comprehensive Documentation

Welcome to the **School Management App (schmng)**, a state-of-the-art, AI-ready mobile solution designed to streamline school operations, enhance communication, and provide a seamless experience for students, teachers, parents, and administrators.

---

## 🌟 1. Non-Technical Overview

### What is this app?
Think of this app as a "Digital School Office" that fits in your pocket. It replaces paper-based registers, manual fee tracking, and physical notice boards with a fast, secure, and easy-to-use mobile application.

### Who is it for?
- **Administrators**: To manage the entire school, from staff hiring to fee collection.
- **Teachers**: To take attendance, manage schedules, and communicate with students.
- **Students**: To check their timetables, view marks, and stay updated with school news.
- **Parents**: To monitor their child's progress, attendance, and pay fees.
- **Staff**: To manage their leaves and daily tasks.

### Why use it?
- **Efficiency**: No more manual data entry.
- **Transparency**: Parents know exactly when their child is in school.
- **Communication**: Instant notifications for important announcements.
- **Security**: Data is safely stored in the cloud using Google's Firebase.

---

## 🚀 2. Key Features by User Role

### 🔑 Administrator
- **Dashboard**: Real-time overview of school statistics.
- **User Management**: Add, edit, or remove students, teachers, and staff.
- **Fee Management**: Track pending fees and send reminders.
- **Timetable & Scheduling**: Create and manage school-wide schedules.
- **Notice Board**: Post official announcements for the entire school.
- **Leave Management**: Approve or reject leave applications from staff and teachers.

### 👨‍🏫 Teacher
- **Attendance**: Digital attendance marking for assigned classes.
- **My Schedule**: View daily teaching timetable.
- **Student Management**: View details of students in their class.
- **Leave Application**: Apply for leaves and track status.
- **Class Events**: Create and manage events specific to their class.

### 🎓 Student
- **My Profile**: View personal and academic details.
- **Timetable**: Access daily class schedules.
- **Attendance Tracking**: Monitor personal attendance percentage.
- **Marks & Grades**: View results of exams and assignments.
- **Fees**: Check fee status and payment history.

### 👪 Parent
- **Child Monitoring**: View attendance and academic performance of their children.
- **Communication**: Receive messages from teachers and school admin.
- **Fee Payment**: View and manage school fee payments.

### 🛠️ Staff (Non-Teaching)
- **Daily Tasks**: View assigned duties.
- **Attendance**: Mark daily check-in/out.
- **Leave Management**: Apply for and track leaves.

---

## 🏗️ 3. Technical Architecture

The app is built using modern Android development practices:

- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose (Declarative UI)
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Backend**: Firebase (Firestore, Auth, Storage, Cloud Functions, Remote Config)
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Manual (using Singletons and ViewModels)
- **Image Loading**: Coil
- **Data Persistence**: SharedPreferences & Firestore

---

## 📂 4. Detailed Code Documentation (Pin-to-Pin)

### 4.1 Core Application Entry
- **`MainActivity.kt`**: The heart of the app. It initializes Firebase, checks for app updates via Google Play, determines the user's login state, and sets up the main navigation host.
- **`SplashActivity.kt`**: Handles the initial branding screen and pre-loads essential data.

### 4.2 Navigation System
- **`AppNavigation.kt`**: Defines every single "route" (screen) in the app. It uses a `NavHost` to switch between screens like Login, Dashboard, and Profile based on user interaction.
- **`BottomNavItem.kt`**: Defines the icons and labels for the bottom navigation bar used by different roles.

### 4.3 Data Layer (The "Backend" in the Frontend)
- **`FirestoreDatabase.kt`**: A massive utility class (2900+ lines) that handles all communication with the Google Firebase database.
    - *Functions*: `addStudent()`, `markAttendance()`, `fetchFees()`, `validateAdmissionNumber()`, etc.
    - *Logic*: Includes complex validation for Aadhar numbers, Admission numbers, and automated ID generation.
- **Models (`/models`)**: Plain Kotlin classes that define the structure of data.
    - `Student.kt`: Name, Roll No, Class, Parent details.
    - `AttendanceRecord.kt`: Date, Status (Present/Absent), Student ID.
    - `Notice.kt`: Title, Content, Date, Target Audience.

### 4.4 UI Layer (The "Frontend")
- **Screens (`/ui/screens`)**: Each file represents a full screen.
    - `AdminDashboardScreen.kt`: Uses grids and cards to show school stats.
    - `AttendanceScreen.kt`: A list with toggle switches for marking attendance.
    - `LoginScreen.kt`: Handles user authentication with error feedback.
- **Components (`/ui/components`)**: Reusable UI elements like `CustomButton`, `LoadingSpinner`, and `InfoCard`.
- **Theme (`/ui/theme`)**: Defines the colors (Material 3), typography, and shapes to ensure a consistent look.

### 4.5 Business Logic (The "Brain")
- **ViewModels (`/viewmodels`)**: These classes hold the data for the UI and handle user actions.
    - `AttendanceViewModel.kt`: Calculates attendance percentages and filters records by date.
    - `NoticeViewModel.kt`: Handles the logic for posting and fetching announcements.

### 4.6 Background Services
- **`MessagingService.kt`**: Handles Firebase Cloud Messaging (FCM) for push notifications.
- **`RemoteConfigService.kt`**: Allows changing app behavior (like maintenance mode) without a new app release.

---

## 🗄️ 5. Database Structure (Firestore)

The app uses a NoSQL structure in Firebase Firestore:
- **`users`**: Stores authentication UID and user role (Admin/Teacher/etc).
- **`students`**: Detailed profiles of all students.
- **`teachers`**: Profiles and assigned classes for teachers.
- **`attendance`**: Daily records indexed by date and class.
- **`fees`**: Financial records for each student.
- **`meta`**: Stores counters for generating unique IDs (like Admission Numbers).

---

## 🐍 6. Automation & Helper Scripts

- **`generate_dummy_students.py`**: A Python script that uses the Firebase Admin SDK to populate the database with hundreds of fake students for testing.
- **`generate_dummy_teachers.py`**: Similar script for creating teacher accounts.
- **`fastlane/`**: Contains automation scripts for building the app and uploading it to the Google Play Store.

---

## 🐳 7. Development Environment

### Docker Setup
We provide a Dockerized environment to ensure "it works on my machine" for every developer.
- **`Dockerfile`**: Sets up the Android SDK and Gradle environment.
- **`docker-compose.yml`**: Orchestrates the build process.

### Manual Setup
1. Install **Android Studio (Ladybug or newer)**.
2. Clone the repo.
3. Add your `google-services.json` to the `app/` folder.
4. Sync Gradle and Run.

---

## 🛠️ 8. Project Folder Structure

```text
root/
├── app/                        # Main Android Module
│   ├── src/main/java/...       # Kotlin Source Code
│   │   ├── models/             # Data Structures
│   │   ├── ui/                 # UI Screens & Components
│   │   ├── viewmodels/         # Business Logic
│   │   └── services/           # Background Services
│   └── build.gradle.kts        # App-level dependencies
├── fastlane/                   # Deployment Automation
├── scripts/                    # Python Data Generators
├── Dockerfile                  # Container Config
└── README.md                   # This Documentation
```

---

## 🔮 9. Future Roadmap
- **AI Analytics**: Predicting student performance based on attendance and marks.
- **Bus Tracking**: Real-time GPS tracking for school buses.
- **Online Exams**: Integrated quiz and testing module.
- **Multi-language Support**: Support for regional languages.

---

**Built with ❤️ by Yeswanth Kasi**
