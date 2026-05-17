package com.agrosphere.app.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.model.AlertItem
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.weather.WeatherRepository
import com.agrosphere.app.ui.theme.AgroPalette
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
    val notificationCount: Int = 0,
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = true,
    val alerts: List<AlertItem> = emptyList(),
    val cropHealth: Int = 0,
    val cropHealthVerdict: String = "—",
    val pestRiskLevel: String = "—",
    val pestRiskBlip: Float = 0f,
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
                recompute(fields = fields, weather = _state.value.weather)
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
                recompute(fields = FieldRepository.current(), weather = bundle.snapshot)
            } catch (_: Throwable) {
                _state.update { it.copy(weatherLoading = false) }
            }
        }
    }

    /** Recomputes everything that depends on (fields, weather). */
    private fun recompute(fields: List<Field>, weather: WeatherSnapshot?) {
        val hasFields = fields.isNotEmpty()
        val avgHealth = if (hasFields) fields.map { it.healthScore }.average().toInt() else 0
        val pest = derivePestRisk(weather, hasFields)
        val alerts = if (hasFields) deriveAlerts(fields, weather) else emptyList()

        _state.update {
            it.copy(
                fieldsCount = fields.size,
                totalAreaHa = fields.sumOf { f -> f.areaHa },
                cropsCount = fields.map { f -> f.crop }.distinct().size,
                avgMoisture = if (hasFields) fields.map { f -> f.moisturePct }.average().toInt() else 0,
                cropHealth = avgHealth,
                cropHealthVerdict = when {
                    !hasFields -> "—"
                    avgHealth >= 85 -> "Excellent"
                    avgHealth >= 70 -> "Strong"
                    avgHealth >= 55 -> "Watch"
                    else -> "At risk"
                },
                pestRiskLevel = pest.first,
                pestRiskBlip = pest.second,
                alerts = alerts,
                notificationCount = alerts.size,
            )
        }
    }

    /** Derives pest pressure from current humidity + temperature (warm + humid = higher). */
    private fun derivePestRisk(weather: WeatherSnapshot?, hasFields: Boolean): Pair<String, Float> {
        if (!hasFields || weather == null) return "—" to 0f
        val score = (weather.humidityPct / 2 + (weather.tempC - 20).coerceAtLeast(0)).coerceIn(0, 100)
        return when {
            score < 30 -> "Low" to 0.20f
            score < 55 -> "Moderate" to 0.45f
            score < 75 -> "High" to 0.70f
            else -> "Severe" to 0.90f
        }
    }

    /** Builds alerts from real signals — no mock content. Returns empty if nothing actionable. */
    private fun deriveAlerts(fields: List<Field>, weather: WeatherSnapshot?): List<AlertItem> {
        val out = mutableListOf<AlertItem>()
        if (weather != null) {
            if (weather.kind == ConditionKind.Storm) {
                out += AlertItem("Storm conditions now", "Postpone spraying and protect machinery.", AgroPalette.Rose)
            } else if (weather.rainMm >= 10) {
                out += AlertItem("Heavy rain — ${weather.rainMm} mm", "Soils will saturate; defer fertigation by 24 h.", AgroPalette.Sky)
            }
            if (weather.tempC >= 35 && weather.humidityPct < 45) {
                out += AlertItem("Heat stress risk", "${weather.tempC}°C with ${weather.humidityPct}% humidity. Irrigate before 9 AM.", AgroPalette.Orange)
            }
            if (weather.uvIndex >= 8) {
                out += AlertItem("Very high UV (${weather.uvIndex})", "Field workers should cover up; avoid midday exposure.", AgroPalette.Amber)
            }
        }
        // Field-specific alert: lowest-moisture field if it's clearly dry
        val driest = fields.minByOrNull { it.moisturePct }
        if (driest != null && driest.moisturePct < 40) {
            out += AlertItem(
                "${driest.name} moisture low",
                "Soil moisture ${driest.moisturePct}% — irrigate within 24h.",
                AgroPalette.Rose,
            )
        }
        return out
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
