package com.ecorvi.schmng.ui.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val type: String = "text",  // text, image, file
    val mediaUrl: String = "",   // URL for image or file if applicable
    val senderName: String = "",
    val receiverName: String = "",
    val participants: List<String> = listOf() // Array containing both senderId and receiverId
) 