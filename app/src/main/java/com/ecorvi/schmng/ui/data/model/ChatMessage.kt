package com.ecorvi.schmng.ui.data.model

enum class MessageStatus {
    SENT,      // Single tick
    DELIVERED, // Double tick
    READ       // Blue double tick
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT" // SENT, DELIVERED, READ
) 