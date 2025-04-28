package com.ecorvi.schmng.ui.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Task

interface IAuthRepository {
    fun login(email: String, password: String): Task<com.google.firebase.auth.AuthResult>
    fun register(email: String, password: String): Task<com.google.firebase.auth.AuthResult>
    fun logout()
    fun sendPasswordResetEmail(email: String): Task<Void>
    fun getCurrentUser(): FirebaseUser?
}

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : IAuthRepository {
    override fun login(email: String, password: String) = auth.signInWithEmailAndPassword(email, password)
    override fun register(email: String, password: String) = auth.createUserWithEmailAndPassword(email, password)
    override fun logout() = auth.signOut()
    override fun sendPasswordResetEmail(email: String) = auth.sendPasswordResetEmail(email)
    override fun getCurrentUser() = auth.currentUser
} 