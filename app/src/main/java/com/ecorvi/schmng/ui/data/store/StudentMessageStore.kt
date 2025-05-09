package com.ecorvi.schmng.ui.data.store

import com.ecorvi.schmng.ui.data.model.MessageStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object StudentMessageStore {
    private val messagesByChat = mutableMapOf<String, List<TempChatMessage>>()
    private var onMessagesUpdatedListener: ((List<ChatPreview>) -> Unit)? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    fun setOnMessagesUpdatedListener(listener: ((List<ChatPreview>) -> Unit)?) {
        onMessagesUpdatedListener = listener
    }
    
    private fun notifyMessagesUpdated() {
        onMessagesUpdatedListener?.invoke(getAllChatPreviews())
    }
    
    fun getMessages(chatId: String): List<TempChatMessage> {
        return messagesByChat[chatId] ?: emptyList()
    }
    
    fun getAllChatPreviews(): List<ChatPreview> {
        val currentUser = auth.currentUser ?: return emptyList()
        
        // Only return chats that involve the current student
        return messagesByChat
            .map { (chatId, messages) ->
                val lastMessage = messages.lastOrNull()
                ChatPreview(
                    id = chatId,
                    name = getUserName(chatId), // This should be the teacher's name
                    lastMessage = lastMessage?.message ?: "",
                    timestamp = lastMessage?.timestamp ?: "",
                    status = lastMessage?.status ?: MessageStatus.SENT,
                    unread = lastMessage?.let { !it.isCurrentUser && it.status != MessageStatus.READ } ?: false,
                    lastSeen = getUserLastSeen(chatId)
                )
            }
            .sortedByDescending { getTimestampValue(it.timestamp) }
    }
    
    fun addMessage(chatId: String, message: TempChatMessage) {
        val currentUser = auth.currentUser ?: return
        // Only add messages if the student is part of the conversation
        if (isStudentAllowedInChat(chatId)) {
            val currentMessages = messagesByChat[chatId] ?: emptyList()
            messagesByChat[chatId] = currentMessages + message
            notifyMessagesUpdated()
        }
    }
    
    private fun isStudentAllowedInChat(chatId: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        // Add logic to check if the student is allowed to participate in this chat
        // For example, only allow chats with their teachers or class-wide announcements
        return true // Implement proper security check
    }
    
    fun updateMessageStatus(chatId: String, messageId: String, newStatus: MessageStatus) {
        if (isStudentAllowedInChat(chatId)) {
            messagesByChat[chatId]?.let { messages ->
                messagesByChat[chatId] = messages.map { msg ->
                    if (msg.id == messageId) msg.copy(status = newStatus)
                    else msg
                }
                notifyMessagesUpdated()
            }
        }
    }
    
    private fun getUserName(userId: String): String {
        // Implement logic to get teacher/staff name from Firestore
        return "Teacher Name" // Replace with actual implementation
    }
    
    private fun getUserLastSeen(userId: String): String {
        // Implement logic to get last seen status from Firestore
        return "Last seen recently" // Replace with actual implementation
    }
    
    private fun getTimestampValue(timestamp: String): Long {
        return when {
            timestamp.contains("Just now") -> System.currentTimeMillis()
            timestamp.contains("AM") || timestamp.contains("PM") -> {
                System.currentTimeMillis() - 3600000
            }
            timestamp.contains("Yesterday") -> System.currentTimeMillis() - 86400000
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
} 