package com.ecorvi.schmng.ui.data

import android.util.Log
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.AttendanceOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttendanceStorage {

    // Local copy of attendance data
    private val _attendanceList = mutableListOf<Pair<Person, AttendanceOption>>()


    // Public access
    val attendanceList: List<Pair<Person, AttendanceOption>>
        get() = _attendanceList

    // Save a person's attendance
    fun markAttendance(person: Person, option: AttendanceOption) {
        // Remove any existing record for the person
        _attendanceList.removeAll { it.first.id == person.id }

        // Add the updated record
        _attendanceList.add(person to option)

        // Save attendance to Firestore
        FirestoreDatabase.addAttendanceRecord(person.id, option.name, getCurrentDate(),
            onSuccess = { Log.d("AttendanceStorage", "Attendance recorded successfully in Firestore.") },
            onFailure = { e -> Log.e("AttendanceStorage", "Error saving attendance to Firestore: ${e.message}") }
        )

    }

    // Get attendance option by person
    fun getAttendanceFor(person: Person): AttendanceOption? {
        return _attendanceList.find { it.first.id == person.id }?.second
    }

    // Reset local data (useful before reloading)
    fun clearAttendance() {
        _attendanceList.clear()
    }

    // Upload to Firebase or return for processing
    fun getAttendanceMap(): Map<String, String> {
        return _attendanceList.associate { it.first.id to it.second.name }
    }

    // Helper function to get current date
    private fun getCurrentDate(): String {
        // You can format the current date however you need
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}