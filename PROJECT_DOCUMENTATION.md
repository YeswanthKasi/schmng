# Detailed Project Documentation

This document provides an in-depth analysis of the source code, breaking down each file, class, and method to explain the inner workings of the School Management App.

## 1. Core Application Files

### 1.1 MainActivity.kt 
**Path**: pp/src/main/java/com/ecorvi/schmng/MainActivity.kt 
**Description**: The entry point of the Android application. It hosts the Jetpack Compose UI, manages app updates, initializes Firebase services, and handles navigation based on authentication state.

#### Class: MainActivity 
Inherits from ComponentActivity.

**Key Properties:**
*   ppUpdateManager: Manages in-app updates via Google Play.
*   irebaseAnalytics: Logs app usage data.
*   prefs: SharedPreferences for storing local flags (e.g., is_first_launch, user_role).
*   
avController: The NavHostController for Compose navigation.
*   isUserLoggedIn, isFirstLaunch, isLoading: State variables controlling the initial UI.

**Methods:**
*   onCreate(savedInstanceState: Bundle?): 
    *   **Description**: Lifecycle method called when the activity is starting. It initializes components, sets up the content view, and checks for updates.
    *   **Logic**: Calls 
egisterNetworkCallback, initializeComponents, setupContent, and checkForAppUpdate.

*   initializeComponents(): 
    *   **Description**: Initializes Firebase, SharedPreferences, Analytics, and Remote Config.
    *   **Logic**: Fetches remote config values (maintenance mode, min version) and sets up the initial auth state.

*   setupContent(): 
    *   **Description**: Sets the Jetpack Compose content.
    *   **Logic**: Uses SchmngTheme and AppNavigation. Displays a loading screen while isLoading is true.

*   setupInitialState(): 
    *   **Description**: Determines if the user is logged in and what the start destination should be.
    *   **Logic**: Checks FirebaseAuth current user and stay_signed_in preference. If valid, calls determineInitialRoute; otherwise, resets to login.

*   determineInitialRoute(auth: FirebaseAuth): 
    *   **Description**: Decides where to navigate based on user role.
    *   **Logic**: Checks if the user has seen the welcome screen. If logged in, fetches the role from Firestore or SharedPreferences to route to the correct dashboard (e.g., 	eacher_dashboard, student_dashboard).

*   checkForAppUpdate(): 
    *   **Description**: Checks Google Play for app updates.
    *   **Logic**: If an update is available, it starts a flexible update flow.

---

### 1.2 AppNavigation.kt 
**Path**: pp/src/main/java/com/ecorvi/schmng/ui/navigation/AppNavigation.kt 
**Description**: Defines the navigation graph for the application using Jetpack Navigation Compose. It maps string routes to Composable screens.

#### Function: AppNavigation 
**Parameters:**
*   
avController: The navigation controller.
*   isUserLoggedIn: Boolean state.
*   isFirstLaunch: Boolean state.
*   initialRoute: The starting route string.

**Key Routes Defined:**
*   **Auth**: welcome, login, 
egister, orgot_password.
*   **Admin**: dmin_dashboard (Home), dmin_notices, grade_entry, manage_teacher_absences.
*   **Student**: student_dashboard, student_timetable, student_profile/{studentId}, student_class_events.
*   **Teacher**: 	eacher_dashboard, 	eacher_attendance, 	eacher_notice_create, 	eacher_leave_apply.
*   **Parent**: parent_dashboard, parent_student_info/{childId}, parent_attendance/{childId}.
*   **Staff**: staff_dashboard, staff_attendance, staff_leave_application.

**Logic**: 
*   Uses NavHost to define the graph.
*   Uses composable to define screens.
*   Passes arguments (like studentId, childId) using 
avArgument.
*   Manages bottom navigation state using currentRoute and onRouteSelected.

---

## 2. Data Layer

### 2.1 FirestoreDatabase.kt 
**Path**: pp/src/main/java/com/ecorvi/schmng/ui/data/FirestoreDatabase.kt 
**Description**: A Singleton object that acts as the central repository for all Firestore and Firebase Storage operations.

#### Object: FirestoreDatabase 
**Key Collections:**
*   students, 	eachers, 
on_teaching_staff, parents, users (for roles).
*   ttendance, 	imetables, schedules, ees, class_events.

**Key Methods:**
*   alidateAdmissionNumber(admissionNumber: String): ValidationResult: 
    *   **Description**: Validates the format (ADM-YYYY-NNNNNN) and uniqueness of an admission number.
    *   **Logic**: Regex check, year validation, and Firestore query to check existence.

*   ddStudent(student: Student, onSuccess: () -> Unit, onFailure: (Exception) -> Unit): 
    *   **Description**: Adds a new student document to the students collection.
    *   **Logic**: Validates required fields and sets the document.

*   getStudent(studentId: String): Person?: 
    *   **Description**: Fetches a student's details, including parent information.
    *   **Logic**: Fetches the student doc. Tries to find parent info via parentId field, parent_student_relationships collection, or embedded fields.

*   etchAttendanceByDate(date: String, userType: UserType, onComplete: (List<AttendanceRecord>) -> Unit): 
    *   **Description**: Retrieves attendance records for a specific date and user type.
    *   **Logic**: Queries ttendance/{date}/{userType} collection.

*   uploadProfilePhoto(userId: String, photoUri: Uri, ...): 
    *   **Description**: Uploads a profile image to Firebase Storage and updates the user's document URL.
    *   **Logic**: Compresses the image, uploads it, gets the download URL, and updates the profilePhoto field in the relevant Firestore collection.

*   
otifyClassAboutEvent(classId: String, eventTitle: String, ...): 
    *   **Description**: Sends FCM notifications to all students in a class.
    *   **Logic**: Fetches all students in the class, retrieves their FCM tokens, and calls the Cloud Function or messaging service.

---

### 2.2 Data Models

The application uses several data classes to represent the core entities. These models are primarily used for Firestore serialization/deserialization and UI state representation.

### `Person.kt` (`com.ecorvi.schmng.ui.data.model`)
Represents a generic person in the system, which can be a student, teacher, staff, or parent. It contains a superset of fields to accommodate various roles.
- **Fields**:
  - `id`: Unique identifier.
  - `firstName`, `lastName`: Name details.
  - `email`, `phone`, `mobileNo`: Contact information.
  - `type`: User role (student, teacher, staff, parent).
  - `className`, `section`, `rollNumber`: Academic details (mostly for students).
  - `gender`, `dateOfBirth`, `address`: Personal details.
  - `designation`, `department`: Staff/Teacher specific fields.
  - `admissionNumber`, `admissionDate`, `academicYear`: Student admission details.
  - `feeStructure`, `feePaid`, `feeRemaining`: Fee management fields.
  - `parentInfo`: Nested `ParentInfo` object.
  - `childInfo`: Nested `ChildInfo` object.

### `Student.kt` (`com.ecorvi.schmng.models`)
A more specific model for Student entities, often used in contexts where only student-specific data is needed.
- **Fields**: `id`, `userId`, `firstName`, `lastName`, `email`, `className`, `rollNumber`, `phoneNumber`, `address`, `parentName`, `parentPhone`, `dateOfBirth`, `gender`, `admissionNumber`, `admissionDate`, `isActive`.
- **Methods**:
  - `toStudentInfo(attendancePercentage: Float)`: Converts to a lightweight `StudentInfo` object.
  - `companion object fromStudentInfo(...)`: Creates a `Student` object from `StudentInfo`.

### `AttendanceRecord.kt` (`com.ecorvi.schmng.models`)
Represents a single attendance entry for a user on a specific date.
- **Fields**:
  - `id`: Unique record ID.
  - `userId`: ID of the user (student/teacher/staff).
  - `userType`: Enum `UserType` (STUDENT, TEACHER, STAFF).
  - `date`: Timestamp of the attendance.
  - `status`: Enum `AttendanceStatus` (PRESENT, ABSENT, PERMISSION).
  - `markedBy`: ID of the user who marked the attendance.
  - `remarks`: Optional notes.
  - `lastModified`: Timestamp of last update.
  - `classId`, `className`: Context for the attendance.

### `Timetable.kt` (`com.ecorvi.schmng.ui.data.model`)
Represents a single slot in a class schedule.
- **Fields**:
  - `id`: Unique ID.
  - `classGrade`: The class this timetable belongs to (e.g., "10th").
  - `dayOfWeek`: Day (e.g., "Monday").
  - `timeSlot`: Time range (e.g., "09:00 AM - 10:00 AM").
  - `subject`: Subject name.
  - `teacher`: Teacher's name.
  - `roomNumber`: Location.
  - `isActive`: Soft delete flag.

### `Notice.kt` (`com.ecorvi.schmng.ui.data.model`)
Represents a notification or announcement.
- **Fields**: `id`, `title`, `content`, `date` (timestamp), `author`, `status` (pending, approved, rejected).

### `TeacherData.kt` & `TeacherSchedule.kt` (`com.ecorvi.schmng.models`)
(Inferred from usage in `TeacherDashboardViewModel`)
- `TeacherData`: Holds teacher profile info (`firstName`, `lastName`, `email`, `subjects`, `classes`).
- `TeacherSchedule`: Likely aggregates `Timetable` items for a teacher.

---

# 3. ViewModels and State Management

The application uses the MVVM (Model-View-ViewModel) architecture. ViewModels are responsible for managing UI state and interacting with the Data Layer (Firestore).

## 3.1 TeacherDashboardViewModel
**File:** `app/src/main/java/com/ecorvi/schmng/viewmodels/TeacherDashboardViewModel.kt`

Manages the state for the Teacher Dashboard screen.
- **StateFlows**:
  - `teacherData`: Holds the current teacher's profile information.
  - `loading`, `attendanceLoading`: Boolean flags for UI loading states.
  - `error`: Holds error messages.
  - `timetables`: List of `Timetable` objects for the teacher.
  - `attendanceStats`: Aggregated attendance statistics (present/absent counts, rate).
  - `attendanceRecords`: List of `AttendanceRecord`s for the selected view (monthly/weekly).
  - `selectedMonth`, `selectedYear`: Controls the date range for attendance analytics.
- **Key Methods**:
  - `loadTeacherData()`: Fetches teacher profile and assigned classes from Firestore.
  - `fetchMonthlyAttendance(year, month)`: Retrieves attendance records for a specific month to calculate stats.
  - `fetchWeeklyAttendance()`: Retrieves attendance for the current week.
  - `fetchAttendanceForDateRange(start, end)`: Core logic to query Firestore for attendance within a time window.
  - `signOut()`: Clears local state and signs out via Firebase Auth.

## 3.2 AttendanceViewModel
**File:** `app/src/main/java/com/ecorvi/schmng/viewmodels/AttendanceViewModel.kt`

Handles the logic for marking and viewing attendance for Students, Teachers, and Staff.
- **StateFlows**:
  - `users`: List of `User` objects (students/staff) to be displayed in the attendance list.
  - `attendanceRecords`: Map of `userId` to `AttendanceRecord` for the current date.
  - `pendingChanges`: Map of `userId` to `AttendanceStatus` tracking unsaved local changes.
  - `hasUnsavedChanges`: Boolean flag to prompt user to save.
  - `presentCount`, `absentCount`, `permissionCount`: Live counters for the current session.
- **Key Methods**:
  - `loadUsers(userType)`: Fetches the list of users (e.g., students of a class) to mark attendance for.
  - `loadAttendance(date, userType, className)`: Listens to real-time updates for attendance records on a specific date.
  - `updateAttendance(userId, userType, status)`: Updates the local `pendingChanges` map.
  - `submitAttendance(userType, className)`: Batches all pending changes and commits them to Firestore.
  - `markAllAttendance(userType, status)`: Bulk updates all loaded users to a specific status locally.

## 3.3 Other State Management
- **Login Logic**: `LoginScreen.kt` uses a helper function `LoginUser` instead of a dedicated ViewModel. It handles Firebase Auth sign-in, FCM token updates, and role-based navigation.
- **Admin Dashboard**: `AdminDashboardScreen.kt` manages its state internally using `remember` and `LaunchedEffect` to fetch counts (students, teachers, staff) and today's attendance summary directly from `FirestoreDatabase`.
- **Student Dashboard**: `StudentDashboardScreen.kt` similarly manages state internally, fetching student profile, timetable, and time slots upon composition.

# 4. UI Screens

The UI is built using Jetpack Compose. Screens are composable functions that observe state (often from ViewModels) and render the UI.

## 4.1 LoginScreen
**File:** `app/src/main/java/com/ecorvi/schmng/ui/screens/LoginScreen.kt`
- **Purpose**: Entry point for unauthenticated users.
- **Features**:
  - Email/Password authentication.
  - "Stay signed in" functionality using SharedPreferences.
  - Credential Manager integration (Smart Lock) for saving/retrieving passwords.
  - Role-based redirection (Admin, Student, Teacher, Parent, Staff) upon successful login.
  - Social login placeholders (Google, Facebook, Apple).

## 4.2 AdminDashboardScreen
**File:** `app/src/main/java/com/ecorvi/schmng/ui/screens/AdminDashboardScreen.kt`
- **Purpose**: Central hub for Administrators.
- **Features**:
  - **Head Count**: Pie chart showing the distribution of Students, Teachers, and Staff.
  - **Attendance Overview**: Summary of today's attendance for all roles.
  - **Fee Analytics**: Bar chart showing monthly fee collection trends.
  - **Quick Actions**: Navigation to Manage Students, Teachers, Staff, Timetable, Marks, etc.
  - **AI Search Bar**: A visual component (currently cosmetic/placeholder) for AI features.
  - **Navigation Drawer**: Provides access to all admin modules.

## 4.3 StudentDashboardScreen
**File:** `app/src/main/java/com/ecorvi/schmng/ui/screens/StudentDashboardScreen.kt`
- **Purpose**: Main interface for Students.
- **Features**:
  - **School Schedule**: Displays today's timetable. Special "Happy Sunday" animation on Sundays.
  - **Timetable View**: Detailed list of classes with time, subject, teacher, and room number.
  - **Quick Access**: Cards for Timetable, Attendance, Notices, Profile.
  - **AI Search**: "Ask AI anything about your studies" search bar.
  - **Navigation Drawer**: Access to Profile, Schedule, Attendance, Notices, Class Events, Messages, etc.

## 4.4 TeacherDashboardScreen
**File:** `app/src/main/java/com/ecorvi/schmng/ui/screens/teacher/TeacherDashboardScreen.kt`
- **Purpose**: Workspace for Teachers.
- **Features**:
  - **Today's Schedule**: A grid view showing the teacher's classes for the day.
  - **Attendance Insights**: Card showing monthly attendance rate and breakdown (Present/Absent/Leave).
  - **Quick Actions**: Navigation to Timetable, Students list, Attendance marking, Profile, Leave management.
  - **Notice Board**: Access to create and view notices.
  - **Live Clock**: Displays current date and time in the top bar.


