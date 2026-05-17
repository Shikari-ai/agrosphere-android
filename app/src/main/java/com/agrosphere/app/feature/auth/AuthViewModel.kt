package com.agrosphere.app.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sealed UI state for the auth screen — busy, idle, error, success. */
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object Success : AuthUiState()
}

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Latest non-null transient error message, suitable for showing in a snackbar. */
    fun dismissError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }

    fun signIn(email: String, password: String) = launchAuth {
        repo.signInWithEmail(email, password)
    }

    fun signUp(name: String, email: String, password: String) = launchAuth {
        repo.signUpWithEmail(email, password, name)
    }

    fun signInGoogle(activityContext: Context, webClientId: String) = launchAuth {
        if (webClientId.isBlank() || webClientId.startsWith("REPLACE_")) {
            error(
                "Google sign-in is not configured.\n" +
                "Drop your OAuth Web Client ID into res/values/strings.xml (default_web_client_id)."
            )
        }
        repo.signInWithGoogle(activityContext, webClientId)
    }

    fun signInAsGuest() = launchAuth { repo.signInAnonymously() }

    private inline fun launchAuth(crossinline block: suspend () -> Any) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                block()
                _state.value = AuthUiState.Success
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(t.toFriendlyMessage())
            }
        }
    }

    private fun Throwable.toFriendlyMessage(): String = when (this) {
        is FirebaseAuthInvalidCredentialsException ->
            "That email or password isn't right. Try again."
        is FirebaseAuthInvalidUserException ->
            "No account with that email. Tap Sign up to create one."
        is FirebaseAuthUserCollisionException ->
            "An account with that email already exists. Try signing in."
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak — use at least 6 characters."
        is FirebaseAuthException -> message ?: "Sign-in failed. Try again."
        is FirebaseException ->
            "Network problem reaching Firebase. Check your connection."
        else -> message ?: "Something went wrong. Try again."
    }

    /** Default factory — no DI framework, just a hand-rolled provider. */
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel() as T
        }
    }
}
