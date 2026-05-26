package com.agrosphere.app.data.repo

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.model.PlantScanRecord
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class WateringStatus {
    data object NeverLogged : WateringStatus()
    data object DueToday    : WateringStatus()
    data class  DueIn(val days: Int) : WateringStatus()
    data class  Overdue(val days: Int) : WateringStatus()
}

/**
 * Single source of truth for the user's home plants.
 *
 * Persisted to device storage via [LocalPlantStore] — every mutation saves
 * immediately so plants and their scan history survive process death and
 * reinstallation (within app data scope).
 *
 * Call [init] once at app start (from [com.agrosphere.app.AgroSphereApplication])
 * with the Application context so the flow seeds from disk before any screen reads it.
 */
object PlantRepository {

    private val _plants = MutableStateFlow<List<PlantEntry>>(emptyList())
    val plants: StateFlow<List<PlantEntry>> = _plants.asStateFlow()

    /** Held statically so mutation methods can persist without re-passing context. */
    @Volatile private var appContext: Context? = null

    /** Background scope for fire-and-forget cloud sync. */
    private val cloudScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Idempotent — call from Application.onCreate(). Loads from disk
     *  immediately, then kicks off a cloud-pull in the background that merges
     *  any plants we don't have locally (new-device sign-in recovery). */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        _plants.value = LocalPlantStore.load(appContext!!)
        cloudScope.launch {
            runCatching {
                val cloud = PlantsCloudRepository.pullAll()
                if (cloud.isEmpty()) return@runCatching
                // Cloud-only plants get merged in. For plants present in both
                // we keep the local copy as-is (current session is freshest);
                // the next mutation will push it back to cloud anyway.
                val localIds = _plants.value.map { it.id }.toSet()
                val newcomers = cloud.filter { it.id !in localIds }
                if (newcomers.isNotEmpty()) {
                    _plants.update { it + newcomers }
                    persist()
                }
            }
        }
    }

    fun current(): List<PlantEntry> = _plants.value
    fun byId(id: String): PlantEntry? = _plants.value.firstOrNull { it.id == id }

    fun addPlant(
        name: String,
        species: String,
        location: String,
        potSize: String,
        sunlightNeed: String,
        wateringIntervalDays: Int,
        accent: Color,
        initialScan: PlantScanRecord? = null,
        photoPath: String? = null,
        scientificName: String = "",
        variety: String = "",
        soilType: String = "",
        careNote: String = "",
    ): PlantEntry {
        val now = System.currentTimeMillis()
        val entry = PlantEntry(
            id                    = "p$now",
            name                  = name.trim(),
            species               = species,
            location              = location,
            potSize               = potSize,
            sunlightNeed          = sunlightNeed,
            wateringIntervalDays  = wateringIntervalDays.coerceIn(1, 30),
            accent                = accent,
            healthScore           = initialScan?.healthScore ?: 75,
            scanHistory           = initialScan?.let { listOf(it) } ?: emptyList(),
            lastScanMs            = if (initialScan != null) now else 0L,
            photoPath             = photoPath,
            scientificName        = scientificName,
            variety               = variety,
            soilType              = soilType,
            careNote              = careNote,
        )
        _plants.update { it + entry }
        persist()
        PlantsCloudRepository.saveAsync(entry)
        return entry
    }

    fun removePlant(id: String) {
        _plants.update { it.filterNot { p -> p.id == id } }
        persist()
        PlantsCloudRepository.deleteAsync(id)
    }

    /** Records a watering event — updates lastWateredMs and prepends to wateringLog. */
    fun logWatering(id: String) {
        val now = System.currentTimeMillis()
        _plants.update { list ->
            list.map { plant ->
                if (plant.id == id) plant.copy(
                    lastWateredMs = now,
                    wateringLog   = (listOf(now) + plant.wateringLog).take(30),
                ) else plant
            }
        }
        persist()
        byId(id)?.let { PlantsCloudRepository.saveAsync(it) }
    }

    /**
     * Apply the result of an AI scan to a plant — updates healthScore, lastScanMs,
     * photoPath, and prepends to scanHistory (capped at 30). Called after a re-scan
     * from the plant detail screen.
     */
    fun applyScan(id: String, record: PlantScanRecord) {
        _plants.update { list ->
            list.map { plant ->
                if (plant.id == id) plant.copy(
                    healthScore = record.healthScore,
                    lastScanMs  = record.timestamp,
                    photoPath   = record.photoPath ?: plant.photoPath,
                    scanHistory = (listOf(record) + plant.scanHistory).take(30),
                ) else plant
            }
        }
        persist()
        byId(id)?.let { PlantsCloudRepository.saveAsync(it) }
    }

    /** Manually set the growth stage (user picks from a chip in the detail screen). */
    fun setStage(id: String, stage: String) {
        _plants.update { list ->
            list.map { if (it.id == id) it.copy(stage = stage) else it }
        }
        persist()
        byId(id)?.let { PlantsCloudRepository.saveAsync(it) }
    }

    /** Updates healthScore for a plant — called after a scan result is processed. */
    fun updateHealth(id: String, score: Int) {
        _plants.update { list ->
            list.map { if (it.id == id) it.copy(healthScore = score.coerceIn(0, 100)) else it }
        }
        persist()
        byId(id)?.let { PlantsCloudRepository.saveAsync(it) }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private fun persist() {
        appContext?.let { LocalPlantStore.save(it, _plants.value) }
    }

    // ─── Derived helpers ─────────────────────────────────────────────────────

    fun wateringStatus(plant: PlantEntry): WateringStatus {
        if (plant.lastWateredMs == 0L) return WateringStatus.NeverLogged
        val daysSince = ((System.currentTimeMillis() - plant.lastWateredMs) / MS_PER_DAY).toInt()
        val daysUntil = plant.wateringIntervalDays - daysSince
        return when {
            daysUntil < 0  -> WateringStatus.Overdue(-daysUntil)
            daysUntil == 0 -> WateringStatus.DueToday
            else           -> WateringStatus.DueIn(daysUntil)
        }
    }

    fun isDueOrOverdue(plant: PlantEntry): Boolean =
        wateringStatus(plant).let { it is WateringStatus.DueToday || it is WateringStatus.Overdue }

    /** Maps an AI risk level + confidence → 0..100 health score.
     *
     *  Each risk tier is a band, not a single value — the AI's confidence
     *  modulates where the score lands within its band. So two healthy
     *  scans can produce 88 vs 97 if the model felt different levels of
     *  certainty, and the user sees genuine movement after every rescan
     *  rather than the same 5 fixed numbers. */
    fun riskToHealthScore(riskLevel: String, confidence: Int = 70): Int {
        val base = when (riskLevel.lowercase()) {
            "healthy" -> 92
            "low"     -> 76
            "medium"  -> 50
            "high"    -> 22
            else      -> 60
        }
        // -8 (very unsure, confidence ≤ 30) … +6 (very confident, confidence ≥ 100)
        val swing = ((confidence.coerceIn(30, 100) - 70) / 5).coerceIn(-8, 6)
        return when (riskLevel.lowercase()) {
            "healthy" -> (base + swing).coerceIn(82, 99)
            "low"     -> (base + swing / 2).coerceIn(66, 84)
            "medium"  -> (base - swing / 2).coerceIn(40, 60)
            "high"    -> (base - swing).coerceIn(8, 35)
            else      -> (base + swing / 2).coerceIn(45, 70)
        }
    }

    // ─── Preset options ──────────────────────────────────────────────────────

    val locationPresets = listOf(
        "Balcony", "Living Room", "Garden", "Bedroom",
        "Kitchen", "Terrace", "Windowsill", "Corridor",
    )
    val sunlightPresets = listOf("Full Sun", "Partial Shade", "Indoors", "Low Light")
    val potSizePresets  = listOf("Small pot", "Medium pot", "Large pot", "Ground / Bed")

    val stagePresets = listOf("Seedling", "Growing", "Mature", "Flowering", "Fruiting", "Dormant", "Recovering")

    val accentPresets: List<Color> = listOf(
        AgroPalette.Primary, AgroPalette.Rose, AgroPalette.Amber,
        AgroPalette.Iris, AgroPalette.Sky, AgroPalette.Orange,
    )

    private const val MS_PER_DAY = 86_400_000L
}
