package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.AlertItem
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.HourSlot
import com.agrosphere.app.data.model.ScanFinding
import com.agrosphere.app.data.model.WeatherAlert
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.data.model.WeatherInsight
import com.agrosphere.app.data.model.WeatherMetric
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.ui.theme.AgroPalette

object MockRepository {

    /** Delegates to [FieldRepository] so additions/removals propagate everywhere. */
    val fields: List<Field> get() = FieldRepository.current()

    fun field(id: String): Field? = FieldRepository.byId(id)

    val weekForecast: List<WeatherDay> = listOf(
        WeatherDay("Today", 31, 22, "Partly cloudy", 0, 54),
        WeatherDay("Sat", 29, 21, "Showers", 12, 71),
        WeatherDay("Sun", 27, 20, "Thunderstorm", 26, 82),
        WeatherDay("Mon", 30, 21, "Clear", 0, 49),
        WeatherDay("Tue", 33, 23, "Hot", 0, 38),
        WeatherDay("Wed", 32, 23, "Humid", 4, 64),
        WeatherDay("Thu", 30, 22, "Cloudy", 2, 58),
    )

    val weatherNow: WeatherSnapshot = WeatherSnapshot(
        location = "Nashik, Maharashtra",
        tempC = 31,
        feelsLikeC = 34,
        condition = "Partly cloudy",
        kind = ConditionKind.PartlyCloudy,
        humidityPct = 54,
        windKph = 9,
        rainMm = 0,
        uvIndex = 7,
        sunrise = "06:14",
        sunset = "18:42",
    )

    val hourlyForecast: List<HourSlot> = listOf(
        HourSlot("Now", 31, 5,  ConditionKind.PartlyCloudy),
        HourSlot("2 PM", 32, 10, ConditionKind.PartlyCloudy),
        HourSlot("3 PM", 33, 15, ConditionKind.Clear),
        HourSlot("4 PM", 32, 30, ConditionKind.PartlyCloudy),
        HourSlot("5 PM", 31, 55, ConditionKind.Cloudy),
        HourSlot("6 PM", 29, 70, ConditionKind.Rain),
        HourSlot("7 PM", 27, 80, ConditionKind.Rain),
        HourSlot("8 PM", 26, 60, ConditionKind.Cloudy),
        HourSlot("9 PM", 25, 30, ConditionKind.Cloudy),
        HourSlot("10 PM", 24, 15, ConditionKind.Night),
        HourSlot("11 PM", 24, 8,  ConditionKind.Night),
        HourSlot("12 AM", 23, 5,  ConditionKind.Night),
    )

    val weatherInsights: List<WeatherInsight> = listOf(
        WeatherInsight(
            "Spray window — closing",
            "Winds at 9 km/h are still in spec, but humidity is climbing. Best to finish foliar passes before 4 PM today.",
            WeatherInsight.Verdict.Caution,
        ),
        WeatherInsight(
            "Irrigation — hold off",
            "60–70% rain probability between 6–8 PM is likely to deliver ~12 mm. Skip Riverside's evening irrigation.",
            WeatherInsight.Verdict.Good,
        ),
        WeatherInsight(
            "Avoid heavy machinery — Sunday",
            "Sunday's thunderstorm (26 mm forecast) will leave heavy soils saturated for 36–48 h. Reschedule field work.",
            WeatherInsight.Verdict.Avoid,
        ),
    )

    val realtimeMetrics: List<WeatherMetric> = listOf(
        WeatherMetric("Visibility", "12", "km", "clear horizon", AgroPalette.Sky),
        WeatherMetric("Air quality", "48", "PM2.5", "Moderate", AgroPalette.Amber),
        WeatherMetric("Heat index", "34°", "feels like", "comfort risk: low", AgroPalette.Orange),
        WeatherMetric("Soil moisture", "62", "% est.", "model — top 30 cm", AgroPalette.Primary),
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
