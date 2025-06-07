package com.ecorvi.schmng.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.LeaveApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import java.util.*

class TeacherLeaveViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _leaveList = MutableStateFlow<List<LeaveApplication>>(emptyList())
    val leaveList: StateFlow<List<LeaveApplication>> = _leaveList
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess
    private val _leaveDetails = MutableStateFlow<LeaveApplication?>(null)
    val leaveDetails: StateFlow<LeaveApplication?> = _leaveDetails

    fun loadMyLeaves() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        FirestoreDatabase.listenToTeacherLeaves(
            teacherId = userId,
            onUpdate = {
                _loading.value = false
                _leaveList.value = it
            },
            onError = {
                _loading.value = false
                _error.value = it.message
            }
        )
    }

    fun submitLeaveApplication(
        userId: String,
        userName: String,
        userType: String,
        fromDate: Long,
        toDate: Long,
        reason: String,
        leaveType: String,
        department: String
    ) {
        viewModelScope.launch {
            val leaveApplication = LeaveApplication(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                userType = userType,
                fromDate = fromDate,
                toDate = toDate,
                reason = reason,
                leaveType = leaveType,
                department = department,
                appliedAt = System.currentTimeMillis()
            )
            // ... rest of the code ...
        }
    }

    fun submitLeave(fromDate: Long, toDate: Long, reason: String, userName: String) {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        _submitSuccess.value = false

        // Get teacher details from Firestore
        db.collection("teachers").document(userId).get()
            .addOnSuccessListener { doc ->
                val department = doc.getString("department") ?: "Teaching"
                val leave = LeaveApplication(
                    userId = userId,
                    userName = userName,
                    userType = LeaveApplication.TYPE_TEACHER,
                    fromDate = fromDate,
                    toDate = toDate,
                    reason = reason,
                    status = LeaveApplication.STATUS_PENDING,
                    appliedAt = System.currentTimeMillis(),
                    leaveType = "Regular Leave",
                    department = department
                )

                FirestoreDatabase.submitLeave(
                    leave = leave,
                    onSuccess = {
                        _loading.value = false
                        _submitSuccess.value = true
                        loadMyLeaves()
                    },
                    onFailure = { e ->
                        _loading.value = false
                        _error.value = e.message
                    }
                )
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _error.value = e.message
            }
    }

    fun loadLeaveDetails(leaveId: String) {
        _loading.value = true
        FirestoreDatabase.getLeaveDetails(
            leaveId = leaveId,
            onSuccess = {
                _loading.value = false
                _leaveDetails.value = it
            },
            onFailure = {
                _loading.value = false
                _error.value = it.message
            }
        )
    }

    fun resetError() {
        _error.value = null
    }

    fun resetSubmitSuccess() {
        _submitSuccess.value = false
    }
} 