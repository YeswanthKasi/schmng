package com.ecorvi.schmng.ui.data.model

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val location: String = "",
    val className: String = ""
) 