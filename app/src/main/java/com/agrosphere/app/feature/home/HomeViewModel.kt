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
import com.agrosphere.app.data.repo.LocalScanStore
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.data.weather.WeatherRepository
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.annotation.StringRes
import com.agrosphere.app.R
import java.time.LocalTime
import kotlin.math.exp

data class TimeOfDay(
    @StringRes val greetingRes: Int,
    val emoji: String,
    val isDay: Boolean,
)

data class HomeUiState(
    val displayName: String = "there",
    val photoUrl: String? = null,
    val timeOfDay: TimeOfDay = TimeOfDay(R.string.greeting_morning, "👋", true),
    val systemHealthy: Boolean = true,
    val notificationCount: Int = 0,
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = true,
    val alerts: List<AlertItem> = emptyList(),
    val cropHealth: Int = 0,
    val cropHealthVerdict: String = "—",
    val hasScan: Boolean = false,
    val pestRiskLevel: String = "—",
    val pestRiskBlip: Float = 0f,
    val fieldsCount: Int = 0,
    val plantsCount: Int = 0,
    val totalAreaHa: Double = 0.0,
    val cropsCount: Int = 0,
    val avgMoisture: Int = 0,
    val irrigationEfficiency: Int = 0,   // 0 = no weather yet
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
                recompute(
                    fields = fields,
                    weather = _state.value.weather,
                    plantsCount = PlantRepository.current().size,
                )
            }
        }
        // Plants — pest pressure and alerts should react to plants too.
        viewModelScope.launch {
            PlantRepository.plants.collect { plants ->
                recompute(
                    fields = FieldRepository.current(),
                    weather = _state.value.weather,
                    plantsCount = plants.size,
                )
            }
        }
        // Stay in sync with whatever screen last loaded weather (e.g. the Weather
        // screen getting a fresher GPS fix) so Home never shows a stale location.
        viewModelScope.launch {
            WeatherRepository.bundleFlow.collect { bundle ->
                if (bundle != null) {
                    _state.update { it.copy(weather = bundle.snapshot, weatherLoading = false) }
                    recompute(
                        fields = FieldRepository.current(),
                        weather = bundle.snapshot,
                        plantsCount = PlantRepository.current().size,
                    )
                }
            }
        }
        // Load scan history — health score comes from real scan results, not field defaults
        viewModelScope.launch {
            val scans = LocalScanStore.load(getApplication())
            if (scans.isNotEmpty()) {
                val avgScore = scans.take(5).map { scanToHealthScore(it.diagnosis.riskLevel) }.average().toInt()
                _state.update {
                    it.copy(
                        hasScan = true,
                        cropHealth = avgScore,
                        cropHealthVerdict = healthVerdict(avgScore),
                    )
                }
            }
        }
        refreshWeather()
    }

    fun onScanCompleted() {
        val scans = LocalScanStore.load(getApplication())
        if (scans.isNotEmpty()) {
            val avgScore = scans.take(5).map { scanToHealthScore(it.diagnosis.riskLevel) }.average().toInt()
            _state.update {
                it.copy(
                    hasScan = true,
                    cropHealth = avgScore,
                    cropHealthVerdict = healthVerdict(avgScore),
                )
            }
        }
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
                recompute(
                    fields = FieldRepository.current(),
                    weather = bundle.snapshot,
                    plantsCount = PlantRepository.current().size,
                )
            } catch (_: Throwable) {
                _state.update { it.copy(weatherLoading = false) }
            }
        }
    }

    /** Recomputes everything that depends on (fields, weather, plants count). */
    private fun recompute(fields: List<Field>, weather: WeatherSnapshot?, plantsCount: Int = _state.value.plantsCount) {
        val hasFields = fields.isNotEmpty()
        val hasPlants = plantsCount > 0
        val avgHealth = if (hasFields) fields.map { it.healthScore }.average().toInt() else 0
        // Pest pressure now activates as soon as you have ANY green space — fields
        // or plants — because warm-humid conditions threaten both.
        val pest = derivePestRisk(weather, hasFields || hasPlants)
        val alerts = if (hasFields) deriveAlerts(fields, weather) else emptyList()

        _state.update {
            it.copy(
                fieldsCount = fields.size,
                plantsCount = plantsCount,
                totalAreaHa = fields.sumOf { f -> f.areaHa },
                cropsCount = fields.map { f -> f.crop }.distinct().size,
                avgMoisture = if (hasFields) fields.map { f -> f.moisturePct }.average().toInt() else 0,
                irrigationEfficiency = deriveIrrigationEfficiency(weather),
                // Only use field default health if no real scan data exists yet
                cropHealth = if (_state.value.hasScan) _state.value.cropHealth else avgHealth,
                cropHealthVerdict = if (_state.value.hasScan) _state.value.cropHealthVerdict
                    else when {
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

    /**
     * Irrigation efficiency for the user's location: the fraction of applied
     * water that actually reaches the root zone vs. lost to evaporation, spray
     * drift and runoff. Driven entirely by the location's live weather.
     *
     * The dominant physical driver of evaporative loss is the Vapour Pressure
     * Deficit (VPD) — how "thirsty" the air is — computed from temperature and
     * relative humidity via the Tetens saturation-vapour-pressure equation.
     * Wind adds drift losses, solar load (UV proxy) adds radiative evaporation,
     * and recent/active rain means applied water runs off or hits already-wet
     * soil. Result is clamped to a realistic 45–97% band.
     */
    private fun deriveIrrigationEfficiency(weather: WeatherSnapshot?): Int {
        weather ?: return 0
        val t  = weather.tempC.toDouble()
        val rh = weather.humidityPct.coerceIn(0, 100).toDouble()

        // Saturation vapour pressure (kPa) → deficit at the current humidity.
        val svp = 0.6108 * exp(17.27 * t / (t + 237.3))
        val vpd = (svp * (1.0 - rh / 100.0)).coerceAtLeast(0.0)

        val vpdLoss   = vpd * 4.0                                 // evaporative demand
        val windLoss  = weather.windKph.coerceAtLeast(0) * 0.25   // spray drift / spread
        val solarLoss = weather.uvIndex.coerceAtLeast(0) * 0.6    // radiative load (UV proxy)
        val rainLoss  = weather.rainMm.coerceAtLeast(0) * 0.8 +   // runoff on wet soil
            if (weather.kind == ConditionKind.Rain || weather.kind == ConditionKind.Storm) 8.0 else 0.0

        return (100.0 - vpdLoss - windLoss - solarLoss - rainLoss).coerceIn(45.0, 97.0).toInt()
    }

    /** Derives pest pressure from current humidity + temperature (warm + humid = higher).
     *  Active when the user has any green space — fields or home plants — since both
     *  face pressure from the same climatic drivers. */
    private fun derivePestRisk(weather: WeatherSnapshot?, hasGreenSpace: Boolean): Pair<String, Float> {
        if (!hasGreenSpace || weather == null) return "—" to 0f
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
            in 5..11  -> TimeOfDay(R.string.greeting_morning,   "☀️",  isDay = true)
            in 12..16 -> TimeOfDay(R.string.greeting_afternoon, "🌤️", isDay = true)
            in 17..21 -> TimeOfDay(R.string.greeting_evening,   "🌇",  isDay = true)
            else      -> TimeOfDay(R.string.greeting_night,     "🌙",  isDay = false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application) }
        }
    }
}

private fun scanToHealthScore(riskLevel: String): Int = when (riskLevel.lowercase()) {
    "healthy" -> 90
    "low"     -> 72
    "medium"  -> 45
    "high"    -> 22
    else      -> 60
}

private fun healthVerdict(score: Int): String = when {
    score >= 85 -> "Excellent"
    score >= 70 -> "Strong"
    score >= 55 -> "Watch"
    else        -> "At risk"
}
