package com.ecorvi.schmng.ui.data.model

data class Fee(
    var id: String = "",
    var studentName: String = "",
    var studentId: String = "",
    var amount: Double = 0.0,
    var dueDate: String = "",
    var className: String = "",
    var status: String = "Pending",
    var description: String = ""
) {
    constructor() : this("", "", "", 0.0, "", "", "Pending", "")
} 