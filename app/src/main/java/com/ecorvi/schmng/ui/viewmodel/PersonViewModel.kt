package com.ecorvi.schmng.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.repository.IAuthRepository
import com.ecorvi.schmng.ui.data.repository.IUserRepository
import com.ecorvi.schmng.ui.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PersonUiState {
    object Initial : PersonUiState()
    object Loading : PersonUiState()
    object Success : PersonUiState()
    data class Error(val message: String) : PersonUiState()
}

sealed class AuthState {
    object LoggedOut : AuthState()
    object LoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

class PersonViewModel(
    application: Application,
    private val personRepository: PersonRepository,
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PersonUiState>(PersonUiState.Initial)
    val uiState: StateFlow<PersonUiState> = _uiState

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    init {
        checkLoginState()
    }

    fun login(email: String, password: String, staySignedIn: Boolean) {
        _uiState.value = PersonUiState.Loading
        authRepository.login(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.LoggedIn
                    prefs.edit().putBoolean("stay_signed_in", staySignedIn).apply()
                    _uiState.value = PersonUiState.Success
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                    _uiState.value = PersonUiState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun logout() {
        authRepository.logout()
        prefs.edit().remove("user_role").remove("stay_signed_in").apply()
        _authState.value = AuthState.LoggedOut
    }

    fun checkLoginState() {
        val user = authRepository.getCurrentUser()
        val staySignedIn = prefs.getBoolean("stay_signed_in", false)
        _authState.value = if (user != null && staySignedIn) AuthState.LoggedIn else AuthState.LoggedOut
    }

    fun savePerson(person: Person) {
        viewModelScope.launch {
            try {
                _uiState.value = PersonUiState.Loading
                authRepository.register(person.email, person.password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                            val newPerson = person.copy(id = uid)
                            personRepository.addPerson(newPerson)
                                .addOnSuccessListener {
                                    _uiState.value = PersonUiState.Success
                                }
                                .addOnFailureListener { e ->
                                    _uiState.value = PersonUiState.Error(e.message ?: "Failed to save person data")
                                }
                        } else {
                            _uiState.value = PersonUiState.Error(task.exception?.message ?: "Failed to create authentication")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = PersonUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
} 