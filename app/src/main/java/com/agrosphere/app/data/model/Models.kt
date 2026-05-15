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
)

data class AlertItem(
    val title: String,
    val subtitle: String,
    val tint: Color,
)
