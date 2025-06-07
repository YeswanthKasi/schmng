package com.ecorvi.schmng.ui.data.store

import com.ecorvi.schmng.ui.data.model.MessageStatus

object StaffMessageStore {
    private val messagesByChat = mutableMapOf<String, List<TempChatMessage>>()
    private var onMessagesUpdatedListener: ((List<ChatPreview>) -> Unit)? = null

    // User data with consistent information across screens
    private val userData = mapOf(
        "1" to Triple("John Doe", "Student", "Last seen today at 10:30 AM"),
        "2" to Triple("Jane Smith", "Teacher", "Online"),
        "3" to Triple("Mike Johnson", "Staff", "Last seen yesterday"),
        "4" to Triple("Sarah Wilson", "Student", "Last seen 2 days ago"),
        "5" to Triple("David Brown", "Teacher", "Last seen 30 minutes ago")
    )

    fun setOnMessagesUpdatedListener(listener: ((List<ChatPreview>) -> Unit)?) {
        onMessagesUpdatedListener = listener
    }

    private fun notifyMessagesUpdated() {
        onMessagesUpdatedListener?.invoke(getAllChatPreviews())
    }

    fun getMessages(chatId: String): List<TempChatMessage> {
        return messagesByChat[chatId] ?: getInitialMessages(chatId)
    }

    fun getUserName(userId: String): String {
        return userData[userId]?.first ?: "Unknown User"
    }

    fun getUserRole(userId: String): String {
        return userData[userId]?.second ?: "Unknown Role"
    }

    fun getUserLastSeen(userId: String): String {
        return userData[userId]?.third ?: "Last seen recently"
    }

    fun addMessage(chatId: String, message: TempChatMessage) {
        val currentMessages = messagesByChat[chatId] ?: getInitialMessages(chatId)
        messagesByChat[chatId] = currentMessages + message
        notifyMessagesUpdated()
    }

    fun updateMessageStatus(chatId: String, messageId: String, newStatus: MessageStatus) {
        messagesByChat[chatId]?.let { messages ->
            messagesByChat[chatId] = messages.map { msg ->
                if (msg.id == messageId) msg.copy(status = newStatus)
                else msg
            }
            notifyMessagesUpdated()
        }
    }

    fun getAllChatPreviews(): List<ChatPreview> {
        return userData.map { (userId, userInfo) ->
            val messages = messagesByChat[userId] ?: getInitialMessages(userId)
            val lastMessage = messages.lastOrNull()
            ChatPreview(
                id = userId,
                name = userInfo.first,
                lastMessage = lastMessage?.message ?: "",
                timestamp = lastMessage?.timestamp ?: "",
                status = lastMessage?.status ?: MessageStatus.SENT,
                unread = lastMessage?.let { !it.isCurrentUser && it.status != MessageStatus.READ } ?: false,
                lastSeen = userInfo.third
            )
        }.sortedByDescending { getTimestampValue(it.timestamp) }
    }

    private fun getTimestampValue(timestamp: String): Long {
        return when {
            timestamp.contains("Just now") -> System.currentTimeMillis()
            timestamp.contains("AM") || timestamp.contains("PM") -> {
                // For simplicity, just use the current day
                System.currentTimeMillis() - 3600000 // 1 hour ago
            }
            timestamp.contains("Yesterday") -> System.currentTimeMillis() - 86400000 // 24 hours ago
            timestamp.contains("days ago") -> {
                val days = timestamp.split(" ")[0].toIntOrNull() ?: 0
                System.currentTimeMillis() - (days * 86400000L)
            }
            timestamp.contains("week") -> System.currentTimeMillis() - (7 * 86400000L)
            else -> 0L
        }
    }

    fun clearAllMessages() {
        messagesByChat.clear()
        notifyMessagesUpdated()
    }

    private fun getInitialMessages(chatId: String): List<TempChatMessage> {
        val userName = getUserName(chatId)
        // Initial dummy messages customized for each user
        return listOf(
            TempChatMessage("1", "Hello!", "10:30 AM", true, MessageStatus.READ),
            TempChatMessage("2", "Hi there! How can I help you?", "10:31 AM", false),
            TempChatMessage("3", "I have a question about the new staff policy.", "10:32 AM", true, MessageStatus.READ),
            TempChatMessage("4", "Sure, what is it?", "10:33 AM", false),
            TempChatMessage("5", "Where can I find the document?", "10:34 AM", true, MessageStatus.DELIVERED)
        ).also { messagesByChat[chatId] = it }
    }
} 