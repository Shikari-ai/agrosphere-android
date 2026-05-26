package com.agrosphere.app.data.repo

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.model.PlantScanRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * On-device persistence for the user's home plants. Same pattern as
 * [LocalScanStore]: SharedPreferences + JSON, no DB, works offline.
 *
 * [PlantEntry] contains a Compose [Color] which isn't @Serializable, so we
 * round-trip through [PersistedPlant] which stores the colour as a packed ARGB Long.
 */
@Serializable
private data class PersistedPlant(
    val id: String,
    val name: String,
    val species: String,
    val location: String,
    val potSize: String,
    val sunlightNeed: String,
    val wateringIntervalDays: Int,
    val lastWateredMs: Long = 0L,
    val wateringLog: List<Long> = emptyList(),
    val healthScore: Int = 75,
    val accentArgb: Long,                          // packed ARGB Int → Long for safety
    val stage: String = "Growing",
    val scanHistory: List<PlantScanRecord> = emptyList(),
    val lastScanMs: Long = 0L,
    val photoPath: String? = null,
    val scientificName: String = "",
    val variety: String = "",
    val soilType: String = "",
    val careNote: String = "",
)

object LocalPlantStore {

    private const val PREFS = "agro_plant_store"
    private const val KEY   = "plants_v1"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<PlantEntry> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<PersistedPlant>>(raw).map { it.toEntry() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, plants: List<PlantEntry>) {
        val persisted = plants.map { it.toPersisted() }
        prefs(context).edit().putString(KEY, json.encodeToString(persisted)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private fun PersistedPlant.toEntry(): PlantEntry = PlantEntry(
        id                    = id,
        name                  = name,
        species               = species,
        location              = location,
        potSize               = potSize,
        sunlightNeed          = sunlightNeed,
        wateringIntervalDays  = wateringIntervalDays,
        lastWateredMs         = lastWateredMs,
        wateringLog           = wateringLog,
        healthScore           = healthScore,
        accent                = Color(accentArgb.toInt()),
        stage                 = stage,
        scanHistory           = scanHistory,
        lastScanMs            = lastScanMs,
        photoPath             = photoPath,
        scientificName        = scientificName,
        variety               = variety,
        soilType              = soilType,
        careNote              = careNote,
    )

    private fun PlantEntry.toPersisted(): PersistedPlant = PersistedPlant(
        id                    = id,
        name                  = name,
        species               = species,
        location              = location,
        potSize               = potSize,
        sunlightNeed          = sunlightNeed,
        wateringIntervalDays  = wateringIntervalDays,
        lastWateredMs         = lastWateredMs,
        wateringLog           = wateringLog,
        healthScore           = healthScore,
        accentArgb            = accent.toArgb().toLong() and 0xFFFFFFFFL,
        stage                 = stage,
        scanHistory           = scanHistory,
        lastScanMs            = lastScanMs,
        photoPath             = photoPath,
        scientificName        = scientificName,
        variety               = variety,
        soilType              = soilType,
        careNote              = careNote,
    )
}
