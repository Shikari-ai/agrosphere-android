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
    val alerts: List<AlertItem> = MockRepository.homeAlerts,
    val cropHealth: Int = 76,                 // 0..100
    val cropHealthVerdict: String = "Strong",
    val pestRiskLevel: String = "Low",
    val pestRiskBlip: Float = 0.25f,          // 0..1 — radius fraction
    val fieldsCount: Int = MockRepository.fields.size,
    val totalAreaHa: Double = MockRepository.fields.sumOf { it.areaHa },
    val cropsCount: Int = MockRepository.fields.map { it.crop }.distinct().size,
    val avgMoisture: Int = MockRepository.fields.map { it.moisturePct }.average().toInt(),
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
