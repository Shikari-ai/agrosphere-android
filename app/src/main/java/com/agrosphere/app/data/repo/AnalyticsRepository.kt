package com.agrosphere.app.data.repo

import android.content.Context
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.PlantEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// ═════════════════════════════════════════════════════════════════════════════
// Public analytics types — used by the home dashboard cards/sheets and by
// any other surface that wants to render plant or field analytics from
// either local or cloud-backed data.
// ═════════════════════════════════════════════════════════════════════════════

data class PlantAnalytics(
    val totalPlants: Int,
    val avgHealth: Int,
    val careStreak: Int,                  // consecutive days of any care activity
    val watersMonth: Int,                 // total waterings, last 30 days
    val scansMonth: Int,                  // total scans, last 30 days
    val watersByDay14: List<Int>,         // oldest first
    val scansByDay14: List<Int>,
    val watersByDay30: List<Int>,
    val scansByDay30: List<Int>,
    val perPlant: List<PerPlantStats>,
)

data class PerPlantStats(
    val plant: PlantEntry,
    val watersMonth: Int,
    val scansMonth: Int,
    val plantStreak: Int,                 // consecutive days this plant was watered
)

data class FieldAnalytics(
    val totalFields: Int,
    val totalAreaHa: Double,
    val avgHealth: Int,
    val avgMoisture: Int,
    val scansMonth: Int,                  // total scans across all fields, last 30 days
    val scansByDay14: List<Int>,
    val scansByDay30: List<Int>,
    val perField: List<Field>,
)

// ═════════════════════════════════════════════════════════════════════════════
// AnalyticsRepository — the backend-facing API for analytics.
//
// Two surfaces:
//
//   1. compute*: pure functions that turn a list of plants/fields/scans into
//      analytics structures. Caller-provided data, no I/O — used by the
//      dashboard cards which already collect local state via Flow.
//
//   2. query*: suspend functions that pull the freshest data from Firestore
//      (plus on-device scan history) and return ready-to-render analytics.
//      Used when you want to refresh from the source of truth, e.g. on a
//      pull-to-refresh or when opening a long-lived analytics screen.
//
// Both paths return identical structures so consumers don't have to branch.
// ═════════════════════════════════════════════════════════════════════════════
object AnalyticsRepository {

    private const val MS_PER_DAY = 86_400_000L

    // ─── Pure compute ────────────────────────────────────────────────────────

    fun computePlantAnalytics(plants: List<PlantEntry>): PlantAnalytics {
        val now = System.currentTimeMillis()
        val today = now / MS_PER_DAY
        val cutoff30 = now - 30 * MS_PER_DAY

        val watersByDay30 = IntArray(30)
        val scansByDay30  = IntArray(30)
        plants.forEach { p ->
            p.wateringLog.filter { it >= cutoff30 }.forEach {
                val idx = (29 - (today - it / MS_PER_DAY).toInt()).coerceIn(0, 29)
                watersByDay30[idx]++
            }
            p.scanHistory.filter { it.timestamp >= cutoff30 }.forEach {
                val idx = (29 - (today - it.timestamp / MS_PER_DAY).toInt()).coerceIn(0, 29)
                scansByDay30[idx]++
            }
        }

        // Global care streak — consecutive days with ANY activity ending today (yesterday as grace).
        val activeDays = mutableSetOf<Long>()
        plants.forEach { p ->
            p.wateringLog.forEach { activeDays += it / MS_PER_DAY }
            p.scanHistory.forEach { activeDays += it.timestamp / MS_PER_DAY }
        }
        var streak = 0
        var day = today
        if (!activeDays.contains(day) && activeDays.contains(day - 1)) day = day - 1
        while (activeDays.contains(day)) { streak++; day-- }

        // Per-plant breakdown, sorted by most-recent activity (most engaged plant first).
        val perPlant = plants.map { p ->
            val pWaters = p.wateringLog.count { it >= cutoff30 }
            val pScans  = p.scanHistory.count { it.timestamp >= cutoff30 }
            val pDays   = p.wateringLog.map { it / MS_PER_DAY }.toSet()
            var pStreak = 0
            var d = today
            if (!pDays.contains(d) && pDays.contains(d - 1)) d = d - 1
            while (pDays.contains(d)) { pStreak++; d-- }
            PerPlantStats(p, pWaters, pScans, pStreak)
        }.sortedByDescending { it.watersMonth + it.scansMonth }

        return PlantAnalytics(
            totalPlants   = plants.size,
            avgHealth     = if (plants.isEmpty()) 0 else plants.map { it.healthScore }.average().toInt(),
            careStreak    = streak,
            watersMonth   = watersByDay30.sum(),
            scansMonth    = scansByDay30.sum(),
            watersByDay14 = watersByDay30.takeLast(14),
            scansByDay14  = scansByDay30.takeLast(14),
            watersByDay30 = watersByDay30.toList(),
            scansByDay30  = scansByDay30.toList(),
            perPlant      = perPlant,
        )
    }

    fun computeFieldAnalytics(fields: List<Field>, scans: List<SavedScan>): FieldAnalytics {
        val now = System.currentTimeMillis()
        val today = now / MS_PER_DAY
        val cutoff30 = now - 30 * MS_PER_DAY

        val scansByDay30 = IntArray(30)
        scans.filter { it.createdAtMillis >= cutoff30 }.forEach {
            val idx = (29 - (today - it.createdAtMillis / MS_PER_DAY).toInt()).coerceIn(0, 29)
            scansByDay30[idx]++
        }

        return FieldAnalytics(
            totalFields  = fields.size,
            totalAreaHa  = fields.sumOf { it.areaHa },
            avgHealth    = if (fields.isEmpty()) 0 else fields.map { it.healthScore }.average().toInt(),
            avgMoisture  = if (fields.isEmpty()) 0 else fields.map { it.moisturePct }.average().toInt(),
            scansMonth   = scansByDay30.sum(),
            scansByDay14 = scansByDay30.takeLast(14),
            scansByDay30 = scansByDay30.toList(),
            perField     = fields.sortedByDescending { it.healthScore },
        )
    }

    // ─── Cloud-backed queries ────────────────────────────────────────────────

    /**
     * Pull the user's plants from Firestore and return fully-computed analytics.
     * Used for true cross-device reads — the dashboard usually calls
     * [computePlantAnalytics] on the local flow for instant updates.
     *
     * Returns empty analytics on offline / signed-out / failure — never throws.
     */
    suspend fun queryPlantAnalytics(): PlantAnalytics {
        val plants = PlantsCloudRepository.pullAll()
        return computePlantAnalytics(plants)
    }

    /**
     * Pull fields from Firestore and local scan history (scans aren't yet
     * cloud-attributed per-field), then compute. Same fail-safe semantics.
     */
    suspend fun queryFieldAnalytics(context: Context): FieldAnalytics = coroutineScope {
        val fieldsAsync = async(Dispatchers.IO) { FieldsCloudRepository.pullAll() }
        val scansAsync  = async(Dispatchers.IO) { LocalScanStore.load(context) }
        computeFieldAnalytics(fieldsAsync.await(), scansAsync.await())
    }
}
