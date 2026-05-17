package com.agrosphere.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "Guest farmer",
    val email: String = "",
    val photoUrl: String? = null,
    val isAnonymous: Boolean = true,
    val isVerified: Boolean = false,
    // Toggles (local — persist later via DataStore)
    val notificationsOn: Boolean = true,
    val useMetric: Boolean = true,
    val darkTheme: Boolean = true,
)

class ProfileViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.userFlow.collect { user ->
                _state.value = _state.value.copy(
                    displayName = user?.displayName?.takeIf { it.isNotBlank() }
                        ?: user?.email?.substringBefore('@')?.replaceFirstChar { it.uppercase() }
                        ?: "Guest farmer",
                    email = user?.email ?: if (user?.isAnonymous == true) "guest session" else "",
                    photoUrl = user?.photoUrl?.toString(),
                    isAnonymous = user?.isAnonymous ?: true,
                    isVerified = user?.isEmailVerified == true,
                )
            }
        }
    }

    fun toggleNotifications(value: Boolean) { _state.value = _state.value.copy(notificationsOn = value) }
    fun toggleUnits(metric: Boolean) { _state.value = _state.value.copy(useMetric = metric) }
    fun toggleTheme(dark: Boolean) { _state.value = _state.value.copy(darkTheme = dark) }

    fun signOut() = authRepo.signOut()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileViewModel() as T
        }
    }
}
