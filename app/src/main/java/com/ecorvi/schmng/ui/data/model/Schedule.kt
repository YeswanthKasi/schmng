package com.ecorvi.schmng.ui.data.model

data class Schedule(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var date: String = "",
    var time: String = "",
    var className: String = "",
    var recipientType: String = "Students",
    var status: String = "Pending"
) {
    constructor() : this("", "", "", "", "", "", "Students", "Pending")
} 