package com.ecorvi.schmng.ui.data.model

data class Notice(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Long = System.currentTimeMillis(),
    val author: String = "",
    val status: String = "approved" // pending, approved, rejected
) 