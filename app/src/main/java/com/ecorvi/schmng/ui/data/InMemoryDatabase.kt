package com.ecorvi.schmng.ui.data

import com.ecorvi.schmng.ui.data.model.Person

object InMemoryDatabase {
    val schedulesList: MutableList<String> = mutableListOf()
    val pendingFeesList: MutableList<String> = mutableListOf()
    val studentsList: MutableList<Person> = mutableListOf()
    val teachersList: MutableList<Person> = mutableListOf()
}