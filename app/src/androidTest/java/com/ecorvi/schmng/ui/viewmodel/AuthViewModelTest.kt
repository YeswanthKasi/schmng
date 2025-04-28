package com.ecorvi.schmng.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ecorvi.schmng.ui.data.repository.IAuthRepository
import com.ecorvi.schmng.ui.data.repository.IUserRepository
import com.ecorvi.schmng.ui.data.repository.PersonRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AdditionalUserInfo
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import android.os.Parcel

class InstantTask<T>(private val result: T? = null, private val exception: Exception? = null) : Task<T>() {
    override fun isComplete() = true
    override fun isSuccessful() = exception == null
    override fun isCanceled() = false
    override fun getResult(): T = result ?: throw IllegalStateException()
    override fun <X : Throwable?> getResult(exceptionType: Class<X>): T {
        if (exception != null) {
            if (exceptionType.isInstance(exception)) throw exceptionType.cast(exception)!!
            throw com.google.android.gms.tasks.RuntimeExecutionException(exception)
        }
        return result ?: throw IllegalStateException()
    }
    override fun getException(): Exception? = exception
    override fun addOnCompleteListener(listener: OnCompleteListener<T>): Task<T> {
        listener.onComplete(this)
        return this
    }
    override fun addOnCompleteListener(executor: java.util.concurrent.Executor, listener: OnCompleteListener<T>): Task<T> {
        executor.execute { listener.onComplete(this) }
        return this
    }
    override fun addOnCompleteListener(activity: android.app.Activity, listener: OnCompleteListener<T>): Task<T> {
        listener.onComplete(this)
        return this
    }
    override fun addOnSuccessListener(listener: com.google.android.gms.tasks.OnSuccessListener<in T>): Task<T> {
        if (isSuccessful) listener.onSuccess(result)
        return this
    }
    override fun addOnSuccessListener(activity: android.app.Activity, listener: com.google.android.gms.tasks.OnSuccessListener<in T>): Task<T> {
        if (isSuccessful) listener.onSuccess(result)
        return this
    }
    override fun addOnSuccessListener(executor: java.util.concurrent.Executor, listener: com.google.android.gms.tasks.OnSuccessListener<in T>): Task<T> {
        if (isSuccessful) executor.execute { listener.onSuccess(result) }
        return this
    }
    override fun addOnFailureListener(listener: com.google.android.gms.tasks.OnFailureListener): Task<T> {
        if (!isSuccessful) listener.onFailure(exception!!)
        return this
    }
    override fun addOnFailureListener(activity: android.app.Activity, listener: com.google.android.gms.tasks.OnFailureListener): Task<T> {
        if (!isSuccessful) listener.onFailure(exception!!)
        return this
    }
    override fun addOnFailureListener(executor: java.util.concurrent.Executor, listener: com.google.android.gms.tasks.OnFailureListener): Task<T> {
        if (!isSuccessful) executor.execute { listener.onFailure(exception!!) }
        return this
    }
    override fun addOnCanceledListener(listener: com.google.android.gms.tasks.OnCanceledListener): Task<T> {
        return this
    }
    override fun addOnCanceledListener(activity: android.app.Activity, listener: com.google.android.gms.tasks.OnCanceledListener): Task<T> {
        return this
    }
    override fun addOnCanceledListener(executor: java.util.concurrent.Executor, listener: com.google.android.gms.tasks.OnCanceledListener): Task<T> {
        return this
    }
}

class FakeAuthResult : AuthResult {
    override fun getUser() = null
    override fun getAdditionalUserInfo(): AdditionalUserInfo? = null
    override fun getCredential(): AuthCredential? = null
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) { /* no-op for test */ }
    companion object {
        @JvmField
        val CREATOR: android.os.Parcelable.Creator<FakeAuthResult> = object : android.os.Parcelable.Creator<FakeAuthResult> {
            override fun createFromParcel(source: Parcel): FakeAuthResult = FakeAuthResult()
            override fun newArray(size: Int): Array<FakeAuthResult?> = arrayOfNulls(size)
        }
    }
}

class FakeAuthRepository : IAuthRepository {
    var loginResult: Task<AuthResult>? = null
    var registerResult: Task<AuthResult>? = null
    var resetResult: Task<Void>? = null
    var logoutCalled = false

    private var fakeUser: FirebaseUser? = null
    override fun getCurrentUser(): FirebaseUser? = fakeUser

    override fun login(email: String, password: String): Task<AuthResult> {
        return loginResult ?: InstantTask(FakeAuthResult())
    }
    override fun register(email: String, password: String): Task<AuthResult> {
        return registerResult ?: InstantTask(FakeAuthResult())
    }
    override fun logout() {
        logoutCalled = true
    }
    override fun sendPasswordResetEmail(email: String): Task<Void> {
        return resetResult ?: InstantTask<Void>()
    }
}

class AuthViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: IAuthRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var viewModel: PersonViewModel
    private lateinit var app: Application

    @Before
    fun setup() {
        authRepository = FakeAuthRepository()
        userRepository = mock()
        personRepository = PersonRepository(mock())
        app = mock()
        val sharedPrefs = mock<android.content.SharedPreferences>()
        val editor = mock<android.content.SharedPreferences.Editor>()
        whenever(app.getSharedPreferences(any(), any())).thenReturn(sharedPrefs)
        whenever(sharedPrefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)
        whenever(editor.apply()).then {}
        viewModel = PersonViewModel(app, personRepository, authRepository, userRepository)
    }

    private suspend fun waitForAuthState(expected: AuthState, timeoutMs: Long = 2000) {
        val start = System.currentTimeMillis()
        while (viewModel.authState.value != expected && System.currentTimeMillis() - start < timeoutMs) {
            delay(50)
        }
        assertEquals(expected, viewModel.authState.value)
    }

    private suspend fun waitForUiState(expected: PersonUiState, timeoutMs: Long = 2000) {
        val start = System.currentTimeMillis()
        while (viewModel.uiState.value != expected && System.currentTimeMillis() - start < timeoutMs) {
            delay(50)
        }
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun login_success_updates_state() = runBlocking {
        (authRepository as FakeAuthRepository).loginResult = InstantTask(FakeAuthResult())
        viewModel.login("test@email.com", "password", true)
        waitForAuthState(AuthState.LoggedIn)
        waitForUiState(PersonUiState.Success)
    }

    @Test
    fun login_failure_updates_state() = runBlocking {
        (authRepository as FakeAuthRepository).loginResult = InstantTask<AuthResult>(exception = Exception("Login failed"))
        viewModel.login("test@email.com", "wrongpass", true)
        waitForAuthState(AuthState.Error("Login failed"))
        waitForUiState(PersonUiState.Error("Login failed"))
    }

    @Test
    fun logout_updates_state() = runBlocking {
        viewModel.logout()
        waitForAuthState(AuthState.LoggedOut)
    }

    @Test
    fun password_reset_success() {
        (authRepository as FakeAuthRepository).resetResult = InstantTask<Void>()
        val result = authRepository.sendPasswordResetEmail("test@email.com")
        assertTrue(result.isSuccessful)
    }

    @Test
    fun password_reset_failure() {
        (authRepository as FakeAuthRepository).resetResult = InstantTask<Void>(exception = Exception("No user found"))
        val result = authRepository.sendPasswordResetEmail("notfound@email.com")
        assertFalse(result.isSuccessful)
    }
} 