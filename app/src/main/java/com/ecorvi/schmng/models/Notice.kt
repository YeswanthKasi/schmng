package com.ecorvi.schmng.models

data class Notice(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val targetClass: String = "all",
    val priority: String = PRIORITY_NORMAL,
    val status: String = STATUS_DRAFT,
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList()
) {
    companion object {
        const val STATUS_DRAFT = "draft"
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"

        const val PRIORITY_LOW = "low"
        const val PRIORITY_NORMAL = "normal"
        const val PRIORITY_HIGH = "high"
    }
} 