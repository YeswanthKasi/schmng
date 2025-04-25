package com.ecorvi.schmng.ui.data.repository

import com.ecorvi.schmng.ui.data.model.Person
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore

class PersonRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun addPerson(person: Person): Task<Void> {
        return when {
            person.type.contains("student", ignoreCase = true) -> {
                db.collection("students").document(person.id).set(person)
            }
            person.type.contains("teacher", ignoreCase = true) -> {
                db.collection("teachers").document(person.id).set(person)
            }
            else -> {
                throw IllegalArgumentException("Invalid person type")
            }
        }
    }

    fun updatePerson(person: Person): Task<Void> {
        return when {
            person.type.contains("student", ignoreCase = true) -> {
                db.collection("students").document(person.id).set(person)
            }
            person.type.contains("teacher", ignoreCase = true) -> {
                db.collection("teachers").document(person.id).set(person)
            }
            else -> {
                throw IllegalArgumentException("Invalid person type")
            }
        }
    }
} 