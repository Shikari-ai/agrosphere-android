package com.agrosphere.app.data.plants

data class PlantSpecies(
    val name: String,
    val category: String,   // "Flowering" | "Indoor" | "Succulent" | "Herb" | "Tree" | "Climber"
    val wateringIntervalDays: Int,
    val sunlightNeed: String,  // "Full Sun" | "Partial Shade" | "Indoors" | "Low Light"
    val soilType: String,
    val careNote: String,
)

object PlantData {

    val all: List<PlantSpecies> = listOf(

        // ─── Flowering ───────────────────────────────────────────────────────
        PlantSpecies("Rose",             "Flowering", 2, "Full Sun",      "Loamy, well-drained",   "Prune dead blooms regularly for continuous flowering."),
        PlantSpecies("Marigold",         "Flowering", 2, "Full Sun",      "Sandy loam",            "Pinch off spent flowers to encourage new blooms."),
        PlantSpecies("Hibiscus",         "Flowering", 2, "Full Sun",      "Rich, moist",           "Feed with phosphorus-rich fertilizer monthly."),
        PlantSpecies("Jasmine",          "Flowering", 3, "Full Sun",      "Well-drained",          "Train stems along support for best flowering."),
        PlantSpecies("Sunflower",        "Flowering", 3, "Full Sun",      "Well-drained",          "Plant facing south for maximum sun exposure."),
        PlantSpecies("Dahlia",           "Flowering", 2, "Full Sun",      "Rich, moist",           "Stake tall varieties to prevent stem breakage."),
        PlantSpecies("Lavender",         "Flowering", 7, "Full Sun",      "Sandy, alkaline",       "Thrives in dry conditions — avoid overwatering."),
        PlantSpecies("Petunia",          "Flowering", 2, "Full Sun",      "Well-drained",          "Pinch growing tips for bushier, fuller plants."),
        PlantSpecies("Bougainvillea",    "Flowering", 4, "Full Sun",      "Well-drained",          "Needs drought stress to trigger flowering."),
        PlantSpecies("Chrysanthemum",    "Flowering", 2, "Full Sun",      "Rich, moist",           "Pinch tips in summer for autumn blooms."),
        PlantSpecies("Ixora",            "Flowering", 3, "Full Sun",      "Acidic, well-drained",  "Prune after bloom to encourage bushy growth."),
        PlantSpecies("Adenium",          "Flowering", 7, "Full Sun",      "Sandy, gritty",         "Swollen base stores water — don't overwater."),
        PlantSpecies("Plumeria",         "Flowering", 5, "Full Sun",      "Well-drained",          "Let soil dry completely between waterings."),

        // ─── Indoor ──────────────────────────────────────────────────────────
        PlantSpecies("Money Plant (Pothos)", "Indoor", 7, "Low Light",      "Any, well-drained",     "Can grow in water — change weekly if hydroponic."),
        PlantSpecies("Peace Lily",           "Indoor", 5, "Indoors",        "Rich, moist",           "Drooping leaves signal it's thirsty — responds fast."),
        PlantSpecies("Snake Plant",          "Indoor", 14, "Low Light",     "Sandy, well-drained",   "Almost indestructible — perfect for beginners."),
        PlantSpecies("ZZ Plant",             "Indoor", 14, "Low Light",     "Well-drained",          "Stores water in rhizomes — thrives on neglect."),
        PlantSpecies("Spider Plant",         "Indoor", 7, "Partial Shade",  "Well-drained",          "Propagate baby spiderettes easily in water."),
        PlantSpecies("Rubber Plant",         "Indoor", 7, "Partial Shade",  "Well-drained",          "Wipe leaves with damp cloth monthly for shine."),
        PlantSpecies("Philodendron",         "Indoor", 7, "Partial Shade",  "Rich, moist",           "Aerial roots can be guided to a moss pole."),
        PlantSpecies("Chinese Evergreen",    "Indoor", 7, "Low Light",      "Moist, well-drained",   "Tolerates neglect but loves indirect bright light."),
        PlantSpecies("Dracaena",             "Indoor", 10, "Partial Shade", "Well-drained",          "Sensitive to fluoride — use filtered water."),
        PlantSpecies("Fiddle Leaf Fig",      "Indoor", 7, "Partial Shade",  "Rich, well-drained",    "Hates being moved — find its spot and leave it."),
        PlantSpecies("Boston Fern",          "Indoor", 3, "Partial Shade",  "Moist, rich",           "Mist daily in dry weather — loves humidity."),
        PlantSpecies("Monstera",             "Indoor", 7, "Partial Shade",  "Rich, moist",           "Fenestrations appear as plant matures in bright light."),
        PlantSpecies("Areca Palm",           "Indoor", 5, "Partial Shade",  "Well-drained",          "One of the best air-purifying plants for indoors."),
        PlantSpecies("Anthurium",            "Indoor", 5, "Indoors",        "Chunky, well-drained",  "Red spathes last for months with indirect bright light."),

        // ─── Succulent / Cactus ──────────────────────────────────────────────
        PlantSpecies("Aloe Vera",         "Succulent", 14, "Full Sun",      "Sandy, well-drained",  "Gel inside leaves soothes burns and skin irritation."),
        PlantSpecies("Jade Plant",        "Succulent", 14, "Full Sun",      "Sandy, well-drained",  "Reduce watering in winter — goes dormant."),
        PlantSpecies("Echeveria",         "Succulent", 10, "Full Sun",      "Sandy, gritty",        "Water at base — water on rosette causes rot."),
        PlantSpecies("Haworthia",         "Succulent", 10, "Partial Shade", "Sandy, well-drained",  "One of few succulents that thrives in lower light."),
        PlantSpecies("Barrel Cactus",     "Succulent", 21, "Full Sun",      "Sandy, dry",           "Water sparingly — once a month in winter."),
        PlantSpecies("Christmas Cactus",  "Succulent", 7,  "Partial Shade", "Moist, well-drained",  "Needs 14 h darkness for 6 weeks to set buds."),
        PlantSpecies("Sedum",             "Succulent", 14, "Full Sun",      "Sandy, gritty",        "Excellent ground cover — drought-resistant."),
        PlantSpecies("Kalanchoe",         "Succulent", 10, "Full Sun",      "Sandy, well-drained",  "Allow soil to fully dry before next watering."),

        // ─── Herbs & Edibles ─────────────────────────────────────────────────
        PlantSpecies("Basil",           "Herb", 2, "Full Sun",      "Rich, moist",   "Pinch flower buds to keep leaves productive."),
        PlantSpecies("Mint",            "Herb", 2, "Partial Shade", "Moist, rich",   "Grow in containers — spreads aggressively in beds."),
        PlantSpecies("Tulsi",           "Herb", 2, "Full Sun",      "Well-drained",  "Sacred plant — traditionally kept at home entrance."),
        PlantSpecies("Coriander",       "Herb", 2, "Full Sun",      "Well-drained",  "Bolt-prone — sow successionally every 3 weeks."),
        PlantSpecies("Curry Leaf",      "Herb", 4, "Full Sun",      "Well-drained",  "Responds well to regular pruning and fertilizing."),
        PlantSpecies("Lemongrass",      "Herb", 4, "Full Sun",      "Moist, rich",   "Divide clumps every 2–3 years to keep vigorous."),
        PlantSpecies("Tomato",          "Herb", 2, "Full Sun",      "Rich, moist",   "Indeterminate varieties need tall stakes or cages."),
        PlantSpecies("Chilli",          "Herb", 3, "Full Sun",      "Well-drained",  "Slight drought stress increases fruit heat level."),
        PlantSpecies("Spinach",         "Herb", 2, "Partial Shade", "Rich, moist",   "Harvest outer leaves to keep plant producing."),
        PlantSpecies("Methi (Fenugreek)","Herb", 2, "Full Sun",     "Well-drained",  "Sow densely and thin; leaves ready in 3 weeks."),

        // ─── Trees & Shrubs ──────────────────────────────────────────────────
        PlantSpecies("Neem",          "Tree", 5, "Full Sun",      "Well-drained",  "Excellent pest repellent — use neem leaves as mulch."),
        PlantSpecies("Lucky Bamboo",  "Tree", 5, "Partial Shade", "Well-drained",  "Can grow in pebbles and water — change weekly."),
        PlantSpecies("Ficus",         "Tree", 7, "Partial Shade", "Rich, well-drained", "Drops leaves when moved — give it time to adjust."),
        PlantSpecies("Curry Tree",    "Tree", 4, "Full Sun",      "Well-drained",  "Prune regularly for bushy shape and more leaves."),

        // ─── Climbers ────────────────────────────────────────────────────────
        PlantSpecies("Passion Flower",           "Climber", 3, "Full Sun", "Well-drained", "Needs strong support — grows rapidly in warm weather."),
        PlantSpecies("Morning Glory",            "Climber", 3, "Full Sun", "Well-drained", "Self-seeds readily — can become invasive in beds."),
        PlantSpecies("Aparajita (Butterfly Pea)","Climber", 3, "Full Sun", "Well-drained", "Edible blue flowers make stunning natural tea."),
    )

    val categories: List<String> get() = all.map { it.category }.distinct()

    fun byCategory(cat: String): List<PlantSpecies> = all.filter { it.category == cat }

    fun find(name: String): PlantSpecies? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
