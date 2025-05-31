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

class AdminLeaveViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _leaveList = MutableStateFlow<List<LeaveApplication>>(emptyList())
    val leaveList: StateFlow<List<LeaveApplication>> = _leaveList
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _leaveDetails = MutableStateFlow<LeaveApplication?>(null)
    val leaveDetails: StateFlow<LeaveApplication?> = _leaveDetails

    fun loadLeaves(status: String? = null) {
        _loading.value = true
        FirestoreDatabase.listenToAllLeaves(
            status = status,
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

    fun updateLeaveStatus(leaveId: String, status: String, adminRemarks: String) {
        val adminId = auth.currentUser?.uid ?: return
        _loading.value = true
        FirestoreDatabase.updateLeaveStatus(
            leaveId = leaveId,
            status = status,
            adminRemarks = adminRemarks,
            adminId = adminId,
            onSuccess = {
                _loading.value = false
                loadLeaveDetails(leaveId)
                loadLeaves()
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
} 