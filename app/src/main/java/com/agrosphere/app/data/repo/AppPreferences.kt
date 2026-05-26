package com.agrosphere.app.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide live preferences. Seeded from DataStore on first ProfileViewModel init.
 * Screens observe these flows directly so changes apply immediately without
 * recomposing from a parent.
 */
object AppPreferences {
    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _useMetric = MutableStateFlow(true)
    val useMetric: StateFlow<Boolean> = _useMetric.asStateFlow()

    /** "farmer" | "plant" | "both" — seeded from SharedPreferences at app start. */
    private val _userMode = MutableStateFlow("both")
    val userMode: StateFlow<String> = _userMode.asStateFlow()

    fun setDarkTheme(v: Boolean) { _darkTheme.value = v }
    fun setUseMetric(v: Boolean) { _useMetric.value = v }
    fun setUserMode(v: String) { _userMode.value = v }
}
