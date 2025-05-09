package com.ecorvi.schmng.ui.data.model

enum class MessageStatus {
    SENT,      // Single tick
    DELIVERED, // Double tick
    READ       // Blue double tick
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "This is a dummy message",
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "Dummy Sender",
    val receiverName: String = "Dummy Receiver",
    val status: MessageStatus = MessageStatus.SENT
) 