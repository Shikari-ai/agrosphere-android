package com.agrosphere.app.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.model.AlertItem
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.data.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TimeOfDay(
    val greeting: String,
    val emoji: String,
    val isDay: Boolean,
)

data class HomeUiState(
    val displayName: String = "there",
    val photoUrl: String? = null,
    val timeOfDay: TimeOfDay = TimeOfDay("Hello", "👋", true),
    val systemHealthy: Boolean = true,
    val notificationCount: Int = 3,
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = true,
    val alerts: List<AlertItem> = emptyList(),
    val cropHealth: Int = 0,                  // 0..100 — 0 means "no data"
    val cropHealthVerdict: String = "—",
    val pestRiskLevel: String = "—",
    val pestRiskBlip: Float = 0f,             // 0..1 — radius fraction
    val fieldsCount: Int = 0,
    val totalAreaHa: Double = 0.0,
    val cropsCount: Int = 0,
    val avgMoisture: Int = 0,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val authRepo = AuthRepository()
    private val _state = MutableStateFlow(HomeUiState(timeOfDay = computeTimeOfDay()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.userFlow.collect { user ->
                _state.update {
                    it.copy(
                        displayName = user?.displayName?.takeIf { n -> n.isNotBlank() }
                            ?: user?.email?.substringBefore('@')?.replaceFirstChar { c -> c.uppercase() }
                            ?: "farmer",
                        photoUrl = user?.photoUrl?.toString(),
                    )
                }
            }
        }
        viewModelScope.launch {
            FieldRepository.fields.collect { fields ->
                val hasFields = fields.isNotEmpty()
                val avgHealth = if (hasFields) fields.map { f -> f.healthScore }.average().toInt() else 0
                _state.update {
                    it.copy(
                        fieldsCount = fields.size,
                        totalAreaHa = fields.sumOf { f -> f.areaHa },
                        cropsCount = fields.map { f -> f.crop }.distinct().size,
                        avgMoisture = if (hasFields) fields.map { f -> f.moisturePct }.average().toInt() else 0,
                        // Crop health is derived from your fields, not a fake number.
                        cropHealth = avgHealth,
                        cropHealthVerdict = when {
                            !hasFields -> "—"
                            avgHealth >= 85 -> "Excellent"
                            avgHealth >= 70 -> "Strong"
                            avgHealth >= 55 -> "Watch"
                            else -> "At risk"
                        },
                        // Pest risk is a placeholder until we wire real scans;
                        // stays "—" until at least one field exists.
                        pestRiskLevel = if (hasFields) "Low" else "—",
                        pestRiskBlip = if (hasFields) 0.25f else 0f,
                        // Real alerts will come from Firestore later;
                        // for now stay empty unless we have fields.
                        alerts = if (hasFields) MockRepository.homeAlerts else emptyList(),
                    )
                }
            }
        }
        refreshWeather()
    }

    fun refresh() {
        _state.update { it.copy(timeOfDay = computeTimeOfDay()) }
        refreshWeather()
    }

    private fun refreshWeather() {
        _state.update { it.copy(weatherLoading = true) }
        viewModelScope.launch {
            try {
                val bundle = WeatherRepository.load(getApplication())
                _state.update { it.copy(weather = bundle.snapshot, weatherLoading = false) }
            } catch (_: Throwable) {
                _state.update { it.copy(weatherLoading = false) }
            }
        }
    }

    private fun computeTimeOfDay(now: LocalTime = LocalTime.now()): TimeOfDay {
        val h = now.hour
        return when (h) {
            in 5..10 -> TimeOfDay("Good morning", "☀️", isDay = true)
            in 11..15 -> TimeOfDay("Good afternoon", "🌤️", isDay = true)
            in 16..19 -> TimeOfDay("Good evening", "🌇", isDay = true)
            else -> TimeOfDay("Good night", "🌙", isDay = false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application) }
        }
    }
}
