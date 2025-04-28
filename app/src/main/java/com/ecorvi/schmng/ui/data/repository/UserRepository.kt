package com.ecorvi.schmng.ui.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

interface IUserRepository {
    fun getUserRole(userId: String): Task<DocumentSnapshot>
    fun addUser(userId: String, userData: Map<String, Any>): Task<Void>
}

class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : IUserRepository {
    override fun getUserRole(userId: String): Task<DocumentSnapshot> =
        db.collection("users").document(userId).get()

    override fun addUser(userId: String, userData: Map<String, Any>): Task<Void> =
        db.collection("users").document(userId).set(userData, SetOptions.merge())
} 