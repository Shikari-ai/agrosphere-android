package com.agrosphere.app.feature.pest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.R
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.NeighbourAlert
import com.agrosphere.app.data.repo.PestScenario
import com.agrosphere.app.data.repo.PestVerifyRepository
import com.agrosphere.app.data.repo.RegionalPest
import com.agrosphere.app.data.repo.RegionalPestRepository
import com.agrosphere.app.data.repo.VerifiedPrediction
import com.agrosphere.app.data.weather.LocationProvider
import com.agrosphere.app.data.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-field pest pressure derived locally from weather + crop. */
data class FieldPestRisk(
    val fieldName: String,
    val crop: String,
    val level: RiskLevel,
    val pressurePct: Int,        // 0..100
    val threats: List<String>,
    val factors: String,         // "28°C · 82% humidity"
)

enum class RiskLevel { HIGH, MEDIUM, LOW }

data class PestUiState(
    val loading: Boolean = true,
    val hasFields: Boolean = false,
    val region: String = "",
    val highCount: Int = 0,
    val medCount: Int = 0,
    val lowCount: Int = 0,
    val risks: List<FieldPestRisk> = emptyList(),
    val scenarios: List<PestScenario> = emptyList(),
    // Regional activity (Firestore, shared with the web app)
    val regionalPests: List<RegionalPest> = emptyList(),
    val neighbourAlert: NeighbourAlert? = null,
    val sampledCells: Int = 0,
    // Verification (backend)
    val verifying: Boolean = false,
    val verifyResult: VerifiedPrediction? = null,
    val verifyError: String? = null,
)

class PestPredictionViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PestUiState())
    val state: StateFlow<PestUiState> = _state.asStateFlow()

    private var weather: WeatherSnapshot? = null

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            weather = try { WeatherRepository.load(getApplication()).snapshot } catch (_: Exception) { null }
            compute()
            loadRegional()
        }
    }

    /** Pull the shared regional pest index from Firestore (same data as the web). */
    private suspend fun loadRegional() {
        val place = try { LocationProvider.fastCurrent(getApplication()) } catch (_: Exception) { return }
        val summary = try {
            RegionalPestRepository.load(place.latitude, place.longitude)
        } catch (_: Exception) {
            return
        }
        _state.update {
            it.copy(
                regionalPests = summary.pests,
                neighbourAlert = summary.neighbourAlert,
                sampledCells = summary.sampledCells,
            )
        }
    }

    private fun compute() {
        val fields = FieldRepository.current()
        val w = weather
        val humid = (w?.humidityPct ?: 0) >= 70
        val hot   = (w?.tempC ?: 0) >= 30

        val risks = fields.map { f -> riskFor(f, w, humid, hot) }
        val top = risks.maxByOrNull { it.pressurePct }

        _state.update {
            it.copy(
                loading = false,
                hasFields = fields.isNotEmpty(),
                region = w?.location ?: "",
                highCount = risks.count { r -> r.level == RiskLevel.HIGH },
                medCount  = risks.count { r -> r.level == RiskLevel.MEDIUM },
                lowCount  = risks.count { r -> r.level == RiskLevel.LOW },
                risks = risks.sortedByDescending { r -> r.pressurePct },
                scenarios = if (top != null) localScenarios(top.threats.firstOrNull() ?: "pest pressure", humid) else emptyList(),
            )
        }
    }

    // ─── Verification (Val.town backend) ────────────────────────────────────────

    /** Run the internet-verified prediction for the highest-risk field's top threat. */
    fun runVerification() {
        val top = _state.value.risks.firstOrNull() ?: return
        val hint = top.threats.firstOrNull() ?: "pest pressure"
        val (district, state) = splitRegion(_state.value.region)

        _state.update { it.copy(verifying = true, verifyError = null) }
        viewModelScope.launch {
            try {
                val result = PestVerifyRepository.verify(
                    pestHint = hint,
                    cropType = top.crop,
                    symptoms = top.threats.joinToString(", "),
                    district = district,
                    state = state,
                )
                _state.update { it.copy(verifying = false, verifyResult = result) }
            } catch (e: Exception) {
                _state.update { it.copy(verifying = false, verifyError = e.message ?: "Verification failed") }
            }
        }
    }

    // ─── Risk model ─────────────────────────────────────────────────────────────

    private fun riskFor(f: Field, w: WeatherSnapshot?, humid: Boolean, hot: Boolean): FieldPestRisk {
        // Warm + humid favours most pests/fungi. Same heuristic as the home card,
        // computed per field so the screen can rank them.
        val pressure = if (w == null) 0
        else ((w.humidityPct / 2) + (w.tempC - 20).coerceAtLeast(0)).coerceIn(0, 100)

        val level = when {
            pressure >= 65 -> RiskLevel.HIGH
            pressure >= 35 -> RiskLevel.MEDIUM
            else           -> RiskLevel.LOW
        }
        val ctx = getApplication<Application>()
        val factors = if (w != null)
            ctx.getString(R.string.pest_factors, w.tempC, w.humidityPct)
        else
            ctx.getString(R.string.pest_factors_nodata)
        return FieldPestRisk(
            fieldName = f.name,
            crop = f.crop,
            level = level,
            pressurePct = pressure,
            threats = threatsFor(f.crop, humid, hot),
            factors = factors,
        )
    }

    /** Likely threats from crop + current conditions — proactive (no scan needed). */
    private fun threatsFor(crop: String, humid: Boolean, hot: Boolean): List<String> {
        val c = crop.lowercase()
        val cropPests = when {
            "cotton" in c -> listOf("Bollworm", "Whitefly", "Aphid")
            "rice" in c || "paddy" in c -> listOf("Stem borer", "Brown planthopper", "Leaf folder")
            "wheat" in c -> listOf("Yellow rust", "Aphid", "Armyworm")
            "maize" in c || "corn" in c -> listOf("Fall armyworm", "Stem borer")
            "tomato" in c -> listOf("Early blight", "Fruit borer", "Whitefly")
            "soybean" in c -> listOf("Girdle beetle", "Aphid", "Rust")
            "sugarcane" in c -> listOf("Early shoot borer", "Pyrilla")
            "chickpea" in c || "gram" in c -> listOf("Pod borer", "Wilt")
            "onion" in c -> listOf("Thrips", "Purple blotch")
            "mustard" in c -> listOf("Aphid", "Alternaria blight")
            "sorghum" in c -> listOf("Shoot fly", "Stem borer")
            else -> listOf("Aphid", "Leaf spot")
        }
        val weatherPests = buildList {
            if (humid) { add("Fungal blight"); add("Powdery mildew") }
            if (hot && !humid) add("Spider mites")
        }
        return (cropPests + weatherPests).distinct().take(3)
    }

    private fun localScenarios(label: String, humid: Boolean): List<PestScenario> {
        val ctx = getApplication<Application>()
        return listOf(
            PestScenario(
                ctx.getString(R.string.pest_scenario_localised_title), 40,
                ctx.getString(R.string.pest_scenario_localised_detail, label),
            ),
            PestScenario(
                ctx.getString(R.string.pest_scenario_spreading_title), 35,
                ctx.getString(R.string.pest_scenario_spreading_detail, label),
            ),
            if (humid) PestScenario(
                ctx.getString(R.string.pest_scenario_flare_title), 45,
                ctx.getString(R.string.pest_scenario_flare_detail, label),
            ) else PestScenario(
                ctx.getString(R.string.pest_scenario_stable_title), 20,
                ctx.getString(R.string.pest_scenario_stable_detail, label),
            ),
        )
    }

    private fun splitRegion(region: String): Pair<String, String> {
        val parts = region.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> parts[0] to parts[1]
            parts.size == 1 -> parts[0] to ""
            else -> "" to ""
        }
    }
}
