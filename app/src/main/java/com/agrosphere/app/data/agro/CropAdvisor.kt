package com.agrosphere.app.data.agro

import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.WeatherSnapshot

data class CropStage(
    val name: String,
    val daysRemaining: Int,   // approximate days until next stage (0 = final stage)
    val description: String,  // weather-aware, context-rich explanation
)

object CropAdvisor {

    // ── Public API ─────────────────────────────────────────────────────────────

    fun stageFor(crop: String, sownDaysAgo: Int, weather: WeatherSnapshot?): CropStage {
        val defs = stagesFor(crop)
        val current = defs.lastOrNull { sownDaysAgo >= it.fromDay } ?: defs.first()
        val next    = defs.firstOrNull { it.fromDay > sownDaysAgo }
        val daysLeft = next?.let { (it.fromDay - sownDaysAgo).coerceAtLeast(1) } ?: 0
        return CropStage(
            name         = current.name,
            daysRemaining = daysLeft,
            description  = stageDescription(current.name, daysLeft, weather),
        )
    }

    fun nextOperation(crop: String, sownDaysAgo: Int, field: Field, weather: WeatherSnapshot?): String {
        val stageName = stagesFor(crop).lastOrNull { sownDaysAgo >= it.fromDay }?.name ?: "Germination"

        // Priority 1 — weather urgency
        if (weather != null) {
            if (weather.kind == ConditionKind.Storm)
                return "Inspect crop for lodging — storm conditions"
            if (weather.tempC >= 40)
                return "Heat alert: irrigate before 8 AM today (${weather.tempC}°C)"
            if (weather.tempC >= 36 && stageName in POLLINATION_STAGES)
                return "Heat stress during $stageName — irrigate immediately"
            if (weather.windKph > 25)
                return "Hold spray — wind at ${weather.windKph} km/h (safe limit: 15)"
            if (weather.kind == ConditionKind.Rain || weather.rainMm >= 10)
                return "Delay foliar application — rain active"
            if (weather.humidityPct >= 85)
                return "Fungicide window open — humidity ${weather.humidityPct}%, spray today"
        }

        // Priority 2 — soil moisture
        if (field.moisturePct < 30)
            return "Irrigate urgently — soil moisture at ${field.moisturePct}%"
        if (field.moisturePct < 45 && (weather == null || weather.rainMm < 5))
            return "Irrigate within 24 h — soil moisture ${field.moisturePct}%"

        // Priority 3 — stage-driven routine
        return stageOperation(stageName)
    }

    fun riskFlags(field: Field, weather: WeatherSnapshot?): String {
        val flags = mutableListOf<String>()
        when {
            field.healthScore < 55 -> flags += "Crop health critical"
            field.healthScore < 70 -> flags += "Health below threshold"
        }
        if (weather != null) {
            if (weather.humidityPct >= 80 && weather.tempC >= 24) flags += "Fungal pressure"
            if (weather.tempC >= 38)                                flags += "Heat stress"
            if (weather.kind == ConditionKind.Storm)               flags += "Storm/lodging risk"
            if (weather.uvIndex >= 9)                              flags += "UV scorch risk"
            if (weather.windKph >= 35)                             flags += "Wind damage risk"
        }
        if (field.moisturePct < 25) flags += "Soil moisture critical"
        return if (flags.isEmpty()) "None" else flags.joinToString(" · ")
    }

    // ── Crop phenology tables (DAS = days after sowing) ───────────────────────

    private data class StageDef(val name: String, val fromDay: Int)

    private fun stagesFor(crop: String): List<StageDef> {
        val c = crop.lowercase()
        return when {
            "rice" in c || "paddy" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 7),
                StageDef("Tillering", 21),
                StageDef("Stem elongation", 40),
                StageDef("Booting", 55),
                StageDef("Heading", 65),
                StageDef("Flowering", 72),
                StageDef("Grain fill", 85),
                StageDef("Maturity", 110),
            )
            "wheat" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 7),
                StageDef("Tillering", 25),
                StageDef("Jointing", 45),
                StageDef("Booting", 65),
                StageDef("Heading", 75),
                StageDef("Flowering", 85),
                StageDef("Grain fill", 95),
                StageDef("Maturity", 120),
            )
            "maize" in c || "corn" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 7),
                StageDef("Vegetative", 20),
                StageDef("Rapid vegetative", 42),
                StageDef("Silking", 65),
                StageDef("Grain fill", 80),
                StageDef("Maturity", 100),
            )
            "cotton" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 10),
                StageDef("Squaring", 30),
                StageDef("Flowering", 55),
                StageDef("Boll development", 80),
                StageDef("Boll opening", 120),
            )
            "soy" in c || "soya" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 7),
                StageDef("Vegetative", 20),
                StageDef("Flowering", 45),
                StageDef("Pod development", 65),
                StageDef("Seed fill", 85),
                StageDef("Maturity", 110),
            )
            "tomato" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 10),
                StageDef("Vegetative", 25),
                StageDef("Flowering", 45),
                StageDef("Fruit set", 60),
                StageDef("Fruit fill", 75),
                StageDef("Harvest", 90),
            )
            "potato" in c -> listOf(
                StageDef("Sprout emergence", 0),
                StageDef("Vegetative", 20),
                StageDef("Tuber initiation", 45),
                StageDef("Tuber bulking", 65),
                StageDef("Maturity", 90),
            )
            "sugar" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 15),
                StageDef("Tillering", 40),
                StageDef("Grand growth", 90),
                StageDef("Maturity", 270),
            )
            "groundnut" in c || "peanut" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 8),
                StageDef("Vegetative", 20),
                StageDef("Flowering", 35),
                StageDef("Pegging", 50),
                StageDef("Pod fill", 70),
                StageDef("Maturity", 100),
            )
            "onion" in c -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 10),
                StageDef("Bulb initiation", 30),
                StageDef("Bulb development", 55),
                StageDef("Maturity", 90),
            )
            else -> listOf(
                StageDef("Germination", 0),
                StageDef("Seedling", 7),
                StageDef("Vegetative", 21),
                StageDef("Flowering", 45),
                StageDef("Fruiting", 70),
                StageDef("Maturity", 90),
            )
        }
    }

    // ── Stage descriptions — weather-aware ────────────────────────────────────

    private val POLLINATION_STAGES = setOf(
        "Flowering", "Anthesis", "Silking", "Heading", "Booting", "Fruit set", "Pegging",
    )

    private fun stageDescription(name: String, daysLeft: Int, weather: WeatherSnapshot?): String {
        val base = when (name) {
            "Germination"      -> "Seeds absorbing moisture and breaking dormancy. Consistent soil moisture and temperature are critical right now."
            "Seedling"         -> "First true leaves emerging and root system establishing. Guard against pests, waterlogging, and intense sun."
            "Tillering"        -> "Active lateral branching — the primary yield-forming stage. Ideal window for nitrogen top-dress and weed control."
            "Vegetative"       -> "Rapid leaf and stem growth with high nutrient demand. Weed competition must be eliminated this week."
            "Squaring"         -> "Flower buds forming on branches. Begin pest scouting schedule; first spray application if threshold crossed."
            "Jointing"         -> "Stem nodes elongating rapidly. Monitor for stem borers and maintain adequate soil moisture."
            "Rapid vegetative" -> "Peak vegetative growth phase — highest water and nutrient uptake period of the season."
            "Booting"          -> "Panicle developing inside the leaf sheath. Any stress at this stage directly reduces grain number."
            "Heading"          -> "Panicle emerging — one of the most yield-sensitive stages. Maintain full irrigation and scout for blast."
            "Flowering",
            "Anthesis"         -> "Active pollination window. Avoid all pesticide spray. Water availability is critical for grain set."
            "Silking"          -> "Silk emergence and pollination. Avoid spray. Any moisture stress now causes direct kernel loss."
            "Grain fill",
            "Seed fill"        -> "Photosynthates moving to grain. Gradually reduce irrigation; monitor for aphids and sucking pests."
            "Boll development" -> "Bolls filling with fibre. Maintain moisture; boll weevil and whitefly peak pressure now."
            "Boll opening"     -> "Crop approaching harvest. Reduce irrigation; watch for grey mildew and boll rot."
            "Pod development"  -> "Pods elongating and filling. Maintain nutrition; begin reducing irrigation gradually."
            "Pegging"          -> "Pegs (gynophores) penetrating the soil to form pods. Keep soil loose and moist — critical for pod set."
            "Pod fill"         -> "Pods filling with kernels underground. Maintain consistent moisture."
            "Fruit set"        -> "Fruitlets setting — avoid water stress and calcium deficiency to prevent blossom-end rot."
            "Fruit fill"       -> "Fruit developing rapidly. Consistent potassium nutrition and water needed for quality and size."
            "Tuber initiation" -> "Tubers beginning to form at stolons. Keep soil cool, loose, and evenly moist."
            "Tuber bulking"    -> "Rapid tuber size increase — peak water requirement of the crop."
            "Sprout emergence" -> "Sprouts breaking through soil. Uniform moisture promotes even emergence."
            "Bulb initiation"  -> "Bulb beginning to swell. Reduce nitrogen; increase potassium for bulb quality."
            "Bulb development" -> "Bulb expanding rapidly. Maintain moisture; reduce irrigation 2 weeks before harvest."
            "Grand growth"     -> "Rapid cane elongation — highest water and nutrient demand of the entire season."
            "Harvest"          -> "Crop is at or approaching harvest maturity. Monitor quality parameters and plan logistics."
            "Maturity"         -> "Crop nearing physiological maturity. Reduce irrigation and plan for timely harvest to avoid field losses."
            else               -> "Crop in the ${name.lowercase()} phase. Continue routine monitoring and scouting."
        }

        val weatherNote = when {
            weather == null -> ""
            weather.kind == ConditionKind.Storm -> " Storm conditions — check for lodging after event."
            weather.tempC >= 38 -> " Extreme heat (${weather.tempC}°C) — irrigate in early morning."
            weather.tempC >= 34 && name in POLLINATION_STAGES -> " Heat during $name can reduce pollination — monitor closely."
            weather.humidityPct >= 80 && weather.tempC >= 24 -> " High humidity favours fungal disease — scout leaf surfaces."
            weather.kind == ConditionKind.Rain -> " Rainfall is beneficial at this stage."
            else -> ""
        }

        val nextNote = if (daysLeft > 0) " Next stage in ~$daysLeft days." else ""
        return "$base$weatherNote$nextNote"
    }

    // ── Stage-specific routine operations ────────────────────────────────────

    private fun stageOperation(stageName: String): String = when (stageName) {
        "Germination"      -> "Maintain even soil moisture for uniform emergence"
        "Seedling"         -> "Scout for early pest pressure; thin if overcrowded"
        "Tillering"        -> "Top-dress nitrogen — peak uptake window open now"
        "Vegetative",
        "Rapid vegetative" -> "Weed control critical — remove competition this week"
        "Squaring"         -> "Pest scouting due — check for thrips and mites"
        "Jointing"         -> "Monitor for stem borers; avoid any waterlogging"
        "Booting"          -> "Maintain full irrigation — stress now cuts yield directly"
        "Heading"          -> "Scout for blast / blight; keep irrigation consistent"
        "Flowering",
        "Anthesis",
        "Silking"          -> "No pesticide spray — pollination window is active"
        "Grain fill",
        "Seed fill"        -> "Reduce irrigation; monitor for aphids and sucking pests"
        "Boll development" -> "Boll weevil scout due; maintain moisture for fibre quality"
        "Boll opening"     -> "Plan harvest logistics; check lint for moisture damage"
        "Pod development"  -> "Reduce irrigation gradually; check for pod borers"
        "Pegging"          -> "Keep soil around pegs loose and moist — critical for pod set"
        "Fruit set"        -> "Apply foliar calcium if tip-burn observed"
        "Fruit fill"       -> "Maintain potassium nutrition for fruit size and quality"
        "Tuber initiation" -> "Keep soil cool; mound up around plants"
        "Tuber bulking"    -> "Peak irrigation period — do not let soil dry out"
        "Bulb initiation"  -> "Switch to high-potassium feed; reduce nitrogen"
        "Grand growth"     -> "High fertiliser and irrigation demand — maintain schedule"
        "Harvest",
        "Maturity",
        "Boll opening"     -> "Plan harvest date; reduce irrigation now"
        else               -> "Routine scouting — check for pest or disease signs"
    }
}
