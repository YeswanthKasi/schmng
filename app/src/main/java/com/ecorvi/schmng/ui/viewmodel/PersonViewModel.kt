package com.ecorvi.schmng.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.repository.PersonRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PersonUiState {
    object Initial : PersonUiState()
    object Loading : PersonUiState()
    object Success : PersonUiState()
    data class Error(val message: String) : PersonUiState()
}

class PersonViewModel(
    private val personRepository: PersonRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonUiState>(PersonUiState.Initial)
    val uiState: StateFlow<PersonUiState> = _uiState

    fun savePerson(person: Person) {
        viewModelScope.launch {
            try {
                _uiState.value = PersonUiState.Loading
                
                // Create Firebase Authentication account
                auth.createUserWithEmailAndPassword(person.email, person.password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Get the UID from the newly created user
                            val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                            
                            // Create a new Person object with the UID as the id
                            val newPerson = person.copy(id = uid)
                            
                            // Save the person data to Firestore
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