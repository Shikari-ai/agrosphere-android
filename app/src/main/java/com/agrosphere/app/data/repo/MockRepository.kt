package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.AlertItem
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.ScanFinding
import com.agrosphere.app.data.model.WeatherAlert
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.ui.theme.AgroPalette

object MockRepository {

    val fields: List<Field> = listOf(
        Field("f1", "North Paddock", "Wheat", 4.2, 86, 62, "Tillering", 38, AgroPalette.Primary),
        Field("f2", "Lake Plot", "Soybean", 2.6, 71, 48, "Flowering", 64, AgroPalette.Sky),
        Field("f3", "Hilltop", "Maize", 5.1, 92, 70, "V8", 52, AgroPalette.Amber),
        Field("f4", "Riverside", "Rice", 3.4, 58, 84, "Booting", 71, AgroPalette.Iris),
    )

    fun field(id: String): Field? = fields.firstOrNull { it.id == id }

    val weekForecast: List<WeatherDay> = listOf(
        WeatherDay("Today", 31, 22, "Partly cloudy", 0, 54),
        WeatherDay("Sat", 29, 21, "Showers", 12, 71),
        WeatherDay("Sun", 27, 20, "Thunderstorm", 26, 82),
        WeatherDay("Mon", 30, 21, "Clear", 0, 49),
        WeatherDay("Tue", 33, 23, "Hot", 0, 38),
        WeatherDay("Wed", 32, 23, "Humid", 4, 64),
        WeatherDay("Thu", 30, 22, "Cloudy", 2, 58),
    )

    val weatherAlerts: List<WeatherAlert> = listOf(
        WeatherAlert(
            "Storm watch — Sunday",
            "Thunderstorm with 26mm rain forecast. Delay any spraying scheduled for Sun afternoon.",
            WeatherAlert.Severity.Watch,
        ),
        WeatherAlert(
            "Heat stress risk — Tuesday",
            "Highs of 33°C with low humidity. Consider early-morning irrigation on Lake Plot.",
            WeatherAlert.Severity.Warning,
        ),
    )

    val homeAlerts: List<AlertItem> = listOf(
        AlertItem("Riverside moisture low", "Soil moisture 32% — irrigate within 24h", AgroPalette.Rose),
        AlertItem("Lake Plot — flowering", "Optimal window for foliar feed starts today", AgroPalette.Amber),
        AlertItem("North Paddock healthy", "Health score up 4 points this week", AgroPalette.Primary),
    )

    val sampleScan: List<ScanFinding> = listOf(
        ScanFinding("Wheat leaf rust", 0.87f, "Moderate", "Apply triazole fungicide within 3 days. Re-scan in 7 days."),
        ScanFinding("Nitrogen deficiency", 0.42f, "Low", "Consider a foliar urea spray (2%) if symptoms persist."),
    )

    val starterChat: List<ChatMessage> = listOf(
        ChatMessage(1, false, "Hi — I'm your AgroSphere assistant. Ask me anything about your fields, pests, weather windows, or input planning."),
    )

    fun mockReply(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            "weather" in p -> "Sunday looks stormy (26mm). I'd push spraying to Monday morning when winds drop below 8 km/h."
            "rust" in p || "fungus" in p -> "For leaf rust at moderate severity, triazole-based fungicides work well. Spray in the cool of the morning, repeat in 10–14 days."
            "irrigat" in p || "water" in p -> "Riverside is at 32% soil moisture — that's the priority. Schedule 25mm in the next 24h, then reassess."
            "fertili" in p || "nutrient" in p -> "Lake Plot is flowering — apply a balanced NPK with extra potassium this week. Skip nitrogen-heavy mixes."
            else -> "Got it. Based on your current fields, I'd watch the storm window on Sunday and prioritize Riverside irrigation. Want me to draft a 7-day plan?"
        }
    }
}
