package com.ecorvi.schmng.models

data class ClassEvent(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val eventDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val targetClass: String = "",
    val createdBy: String = "", // Teacher's ID
    val priority: String = "normal", // normal, important, urgent
    val type: String = "general", // general, exam, activity, etc.
    val status: String = "active", // active, cancelled, completed
    val attachments: List<String> = emptyList() // URLs to any attached files
) 