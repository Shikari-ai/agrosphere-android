package com.agrosphere.app.data.model

import androidx.compose.ui.graphics.Color
import com.agrosphere.app.ui.theme.AgroPalette

data class Field(
    val id: String,
    val name: String,
    val crop: String,
    val areaHa: Double,
    val healthScore: Int,    // 0..100
    val moisturePct: Int,    // 0..100
    val stage: String,
    val sownDaysAgo: Int,
    val accent: Color = AgroPalette.Primary,
)

data class WeatherDay(
    val label: String,
    val tempC: Int,
    val tempLowC: Int,
    val condition: String,
    val rainMm: Int,
    val humidityPct: Int,
)

data class WeatherAlert(
    val title: String,
    val message: String,
    val severity: Severity,
) {
    enum class Severity { Info, Watch, Warning }
}

data class ScanFinding(
    val label: String,
    val confidence: Float,  // 0..1
    val severity: String,
    val advice: String,
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val imageUri: String? = null,   // optional attached photo (content/file uri)
    val createdAtMs: Long = 0L,     // epoch ms — used for session grouping in history drawer
)

data class AlertItem(
    val title: String,
    val subtitle: String,
    val tint: Color,
)

// ─── Home plant / garden ────────────────────────────────────────────────────

/** One snapshot of an AI scan against a specific plant. Persisted on device. */
@kotlinx.serialization.Serializable
data class PlantScanRecord(
    val timestamp: Long,
    val verdict: String,                          // e.g. "Healthy", "Powdery mildew detected"
    val healthScore: Int,                         // derived from riskLevel
    val riskLevel: String,                        // "healthy" | "low" | "medium" | "high"
    val summary: String,                          // 1–2 sentence AI summary
    val recommendations: List<String> = emptyList(),
    val photoPath: String? = null,                // local file path of the scan photo
)

data class PlantEntry(
    val id: String,
    val name: String,                                   // user's nickname, e.g. "Grandma's Rose"
    val species: String,                                // e.g. "Rose" — matches PlantData catalog
    val location: String,                               // "Balcony", "Living Room", "Garden", …
    val potSize: String,                                // "Small pot", "Medium pot", "Large pot", "Ground / Bed"
    val sunlightNeed: String,                           // "Full Sun", "Partial Shade", "Indoors", "Low Light"
    val wateringIntervalDays: Int,
    val lastWateredMs: Long = 0L,                       // epoch ms; 0 = never logged
    val wateringLog: List<Long> = emptyList(),          // epoch ms timestamps, newest first (max 30)
    val healthScore: Int = 75,                          // 0..100; updated by latest scan
    val accent: Color = AgroPalette.Primary,
    val stage: String = "Growing",                      // Seedling | Growing | Mature | Flowering | Fruiting | Dormant | Recovering
    val scanHistory: List<PlantScanRecord> = emptyList(), // newest first, capped at 30
    val lastScanMs: Long = 0L,                          // timestamp of most recent scan
    val photoPath: String? = null,                      // path to the most recent plant photo
    // AI-identification metadata — populated when the plant is added via the
    // camera flow. Detail screens prefer these over PlantData catalog lookups.
    val scientificName: String = "",
    val variety: String = "",
    val soilType: String = "",
    val careNote: String = "",
)

// ─── Weather intelligence ────────────────────────────────────────────────────

enum class ConditionKind { Clear, PartlyCloudy, Cloudy, Rain, Storm, Night, Windy }

data class WeatherSnapshot(
    val location: String,
    val tempC: Int,
    val feelsLikeC: Int,
    val condition: String,
    val kind: ConditionKind,
    val humidityPct: Int,
    val windKph: Int,
    val rainMm: Int,
    val uvIndex: Int,              // 0..11+
    val sunrise: String,           // "06:14"
    val sunset: String,            // "18:42"
)

data class HourSlot(
    val label: String,             // "Now", "2 PM", "5 PM"
    val tempC: Int,
    val rainProb: Int,             // 0..100
    val kind: ConditionKind,
)

data class WeatherInsight(
    val title: String,
    val body: String,
    val verdict: Verdict,
) {
    enum class Verdict { Good, Caution, Avoid }
}

data class WeatherMetric(
    val label: String,
    val value: String,
    val unit: String,
    val sublabel: String,
    val tint: Color,
)
