package com.agrosphere.app.feature.profile

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.repo.AppPreferences
import com.agrosphere.app.data.repo.FieldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore("profile_prefs")

private object PrefKeys {
    val NOTIF  = booleanPreferencesKey("notif")
    val METRIC = booleanPreferencesKey("metric")
    val DARK   = booleanPreferencesKey("dark")
    val PHOTO  = stringPreferencesKey("local_photo_path")
}

data class ProfileUiState(
    val displayName: String = "Guest farmer",
    val email: String = "",
    val photoUrl: String? = null,
    val isAnonymous: Boolean = true,
    val isVerified: Boolean = false,
    // Persisted via DataStore
    val notificationsOn: Boolean = true,
    val useMetric: Boolean = true,
    val darkTheme: Boolean = true,
    // Computed from FieldRepository
    val farmIntelScore: Int = 0,
    val scoreLabel: String = "",
    val cropHealthFraction: Float = 0f,
    val fieldDiversityFraction: Float = 0f,
)

class ProfileViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        // Auth user
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

        // Persisted toggles + local photo — read once on start, also seed AppPreferences so the
        // whole app reflects the user's saved preference immediately on launch.
        viewModelScope.launch {
            val prefs = appContext.profileDataStore.data.first()
            val dark   = prefs[PrefKeys.DARK]   ?: true
            val metric = prefs[PrefKeys.METRIC] ?: true
            AppPreferences.setDarkTheme(dark)
            AppPreferences.setUseMetric(metric)
            val localPhoto = prefs[PrefKeys.PHOTO]?.let { path ->
                val f = File(path); if (f.exists()) f.absolutePath else null
            }
            _state.value = _state.value.copy(
                notificationsOn = prefs[PrefKeys.NOTIF] ?: true,
                useMetric       = metric,
                darkTheme       = dark,
                photoUrl        = localPhoto ?: _state.value.photoUrl,
            )
        }

        // Farm intelligence score — live from fields
        viewModelScope.launch {
            FieldRepository.fields.collect { fields ->
                val score = if (fields.isEmpty()) 0
                    else fields.map { it.healthScore }.average().toInt().coerceIn(0, 100)
                val diversity = if (fields.isEmpty()) 0f
                    else (fields.map { it.crop }.distinct().size / 5f).coerceIn(0f, 1f)
                _state.value = _state.value.copy(
                    farmIntelScore         = score,
                    scoreLabel             = scoreLabel(score),
                    cropHealthFraction     = score / 100f,
                    fieldDiversityFraction = diversity,
                )
            }
        }
    }

    fun toggleNotifications(value: Boolean) {
        _state.value = _state.value.copy(notificationsOn = value)
        viewModelScope.launch { appContext.profileDataStore.edit { it[PrefKeys.NOTIF] = value } }
    }

    fun toggleUnits(metric: Boolean) {
        _state.value = _state.value.copy(useMetric = metric)
        AppPreferences.setUseMetric(metric)
        viewModelScope.launch { appContext.profileDataStore.edit { it[PrefKeys.METRIC] = metric } }
    }

    fun toggleTheme(dark: Boolean) {
        _state.value = _state.value.copy(darkTheme = dark)
        AppPreferences.setDarkTheme(dark)
        viewModelScope.launch { appContext.profileDataStore.edit { it[PrefKeys.DARK] = dark } }
    }

    fun updatePhoto(uri: Uri) {
        viewModelScope.launch {
            val dest = withContext(Dispatchers.IO) {
                val dir = File(appContext.filesDir, "profile").also { it.mkdirs() }
                val out = File(dir, "avatar.jpg")
                try {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    }
                    out
                } catch (_: Exception) { null }
            } ?: return@launch
            appContext.profileDataStore.edit { it[PrefKeys.PHOTO] = dest.absolutePath }
            _state.value = _state.value.copy(photoUrl = dest.absolutePath)
        }
    }

    fun signOut() = authRepo.signOut()

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(appContext = context.applicationContext) as T
        }
    }
}

private fun scoreLabel(score: Int): String = when {
    score == 0 -> ""
    score < 50 -> "Needs attention"
    score < 70 -> "Developing"
    score < 85 -> "Good standing"
    score < 95 -> "Excellent — top 15%"
    else       -> "Outstanding"
}
