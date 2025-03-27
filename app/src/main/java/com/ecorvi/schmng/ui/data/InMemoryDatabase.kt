package com.ecorvi.schmng.ui.data

import com.ecorvi.schmng.ui.data.model.Person

object InMemoryDatabase {
    val studentsList = mutableListOf<Person>()
    val teachersList = mutableListOf<Person>()
    val schedulesList = mutableListOf<String>()
    val pendingFeesList = mutableListOf<String>()
}