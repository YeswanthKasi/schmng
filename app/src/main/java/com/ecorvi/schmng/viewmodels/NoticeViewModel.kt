package com.ecorvi.schmng.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.Notice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoticeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val noticesCollection = firestore.collection("notices")

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _userClass = MutableStateFlow<String?>(null)
    val userClass: StateFlow<String?> = _userClass

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                _userRole.value = userDoc.getString("role")?.lowercase()

                // If student, get their class
                if (_userRole.value == "student") {
                    val studentDoc = firestore.collection("students").document(currentUser.uid).get().await()
                    _userClass.value = studentDoc.getString("className")
                }

                // Load notices after getting user info
                loadNotices()
            } catch (e: Exception) {
                _error.value = "Failed to load user info: ${e.message}"
            }
        }
    }

    suspend fun getNotice(noticeId: String): Notice? {
        return try {
            val doc = noticesCollection.document(noticeId).get().await()
            doc.toObject(Notice::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            _error.value = "Failed to load notice: ${e.message}"
            null
        }
    }

    fun loadNotices(status: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                when (_userRole.value) {
                    "admin" -> {
                        // Admins see notices based on status filter
                        val query = if (status != null && status != "all") {
                            noticesCollection
                                .whereEqualTo("status", status)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                        } else {
                            // By default, show pending notices for admin
                            noticesCollection
                                .whereEqualTo("status", Notice.STATUS_PENDING)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                        }
                        
                        val snapshot = query.get().await()
                        _notices.value = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Notice::class.java)?.copy(id = doc.id)
                        }
                    }
                    "teacher" -> {
                        // For teachers: Show filtered notices or all relevant notices
                        when (status) {
                            null, "all" -> {
                                // Show all approved notices and teacher's own notices
                                val approvedQuery = noticesCollection
                                    .whereEqualTo("status", Notice.STATUS_APPROVED)
                                    .orderBy("createdAt", Query.Direction.DESCENDING)
                                
                                val ownQuery = noticesCollection
                                    .whereEqualTo("authorId", currentUser.uid)
                                    .orderBy("createdAt", Query.Direction.DESCENDING)

                                // Execute both queries
                                val approvedSnapshot = approvedQuery.get().await()
                                val ownSnapshot = ownQuery.get().await()

                                // Combine results
                                val allNotices = (
                                    approvedSnapshot.documents.mapNotNull { doc ->
                                        doc.toObject(Notice::class.java)?.copy(id = doc.id)
                                    } +
                                    ownSnapshot.documents.mapNotNull { doc ->
                                        doc.toObject(Notice::class.java)?.copy(id = doc.id)
                                    }
                                ).distinctBy { it.id }
                                 .sortedByDescending { it.createdAt }

                                _notices.value = allNotices
                            }
                            else -> {
                                // Show only teacher's notices with specific status
                                val query = noticesCollection
                                    .whereEqualTo("authorId", currentUser.uid)
                                    .whereEqualTo("status", status)
                                    .orderBy("createdAt", Query.Direction.DESCENDING)

                                val snapshot = query.get().await()
                                _notices.value = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(Notice::class.java)?.copy(id = doc.id)
                                }
                            }
                        }
                    }
                    else -> {
                        // Students and others see only approved notices
                        val query = noticesCollection
                            .whereEqualTo("status", Notice.STATUS_APPROVED)
                            .whereIn("targetClass", listOf("all", _userClass.value))
                            .orderBy("createdAt", Query.Direction.DESCENDING)

                        val snapshot = query.get().await()
                        _notices.value = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Notice::class.java)?.copy(id = doc.id)
                        }
                    }
                }
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("NoticeViewModel", "Error loading notices", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNoticesByStatus(vararg statuses: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val baseQuery: Query = noticesCollection
                    .whereIn("status", statuses.toList())
                    .orderBy("createdAt", Query.Direction.DESCENDING)

                val snapshot = baseQuery.get().await()
                _notices.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Notice::class.java)?.copy(id = doc.id)
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNotice(
        title: String,
        content: String,
        targetClass: String,
        priority: String,
        attachments: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                val notice = Notice(
                    title = title,
                    content = content,
                    authorId = currentUser.uid,
                    authorName = currentUser.displayName ?: "",
                    targetClass = targetClass,
                    priority = priority,
                    status = Notice.STATUS_DRAFT,
                    attachments = attachments
                )

                noticesCollection.add(notice).await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateNotice(notice: Notice) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                noticesCollection.document(notice.id)
                    .set(notice.copy(updatedAt = System.currentTimeMillis()))
                    .await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitForApproval(noticeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                noticesCollection.document(noticeId)
                    .update(
                        mapOf(
                            "status" to Notice.STATUS_PENDING,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveNotice(noticeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                noticesCollection.document(noticeId)
                    .update(
                        mapOf(
                            "status" to Notice.STATUS_APPROVED,
                            "approvedBy" to currentUser.uid,
                            "approvedAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectNotice(noticeId: String, reason: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                noticesCollection.document(noticeId)
                    .update(
                        mapOf(
                            "status" to Notice.STATUS_REJECTED,
                            "rejectionReason" to reason,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                noticesCollection.document(noticeId).delete().await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateNoticeStatus(noticeId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                noticesCollection.document(noticeId)
                    .update(
                        mapOf(
                            "status" to newStatus,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                loadNotices()
            } catch (e: Exception) {
                _error.value = "Failed to update notice status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 