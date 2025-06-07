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

class StaffLeaveViewModel : ViewModel() {
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
        FirestoreDatabase.listenToStaffLeaves(
            staffId = userId,
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
        fromDate: Long,
        toDate: Long,
        reason: String,
        leaveType: String,
        department: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true

        // Get user details from Firestore
        db.collection("non_teaching_staff").document(userId).get()
            .addOnSuccessListener { doc ->
                val userName = "${doc.getString("firstName")} ${doc.getString("lastName")}"
                val leave = LeaveApplication(
                    userId = userId,
                    userName = userName,
                    userType = LeaveApplication.TYPE_STAFF,
                    fromDate = fromDate,
                    toDate = toDate,
                    reason = reason,
                    status = LeaveApplication.STATUS_PENDING,
                    appliedAt = System.currentTimeMillis(),
                    leaveType = leaveType,
                    department = department
                )

                FirestoreDatabase.submitLeave(
                    leave = leave,
                    onSuccess = {
                        _loading.value = false
                        _submitSuccess.value = true
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
            onFailure = { e ->
                _loading.value = false
                _error.value = e.message
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