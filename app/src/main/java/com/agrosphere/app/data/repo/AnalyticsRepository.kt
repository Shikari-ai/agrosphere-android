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
    val careActionsMonth: Int,            // unique (plant, day) pairs with any activity
    val watersByDay14: List<Int>,         // oldest first
    val scansByDay14: List<Int>,
    val watersByDay30: List<Int>,
    val scansByDay30: List<Int>,
    val careActionsByDay30: List<Int>,    // unique plants attended per day
    val healthTrend30: List<Int>,         // avg healthScore per day, 0..100
    // Previous 30-day window for delta computation.
    val watersPrev30: Int,
    val scansPrev30: Int,
    val careActionsPrev30: Int,
    val avgHealthPrev30: Int,
    // Activity flags for this week's calendar — index 0 = Mon, 6 = Sun.
    val weekActivity: List<Boolean>,
    val perPlant: List<PerPlantStats>,
) {
    // ─── Derived totals + rank ──────────────────────────────────────────────
    val scanningPoints: Int    get() = (scansMonth * 110).coerceAtMost(3000)
    val wateringPoints: Int    get() = (watersMonth * 120).coerceAtMost(3000)
    val plantHealthPoints: Int get() = (avgHealth * 30).coerceAtMost(3000)
    val careActionPoints: Int  get() = (careActionsMonth * 42).coerceAtMost(2000)
    val totalScore: Int        get() = scanningPoints + wateringPoints + plantHealthPoints + careActionPoints

    val scorePrev: Int get() {
        val s  = (scansPrev30 * 110).coerceAtMost(3000)
        val w  = (watersPrev30 * 120).coerceAtMost(3000)
        val h  = (avgHealthPrev30 * 30).coerceAtMost(3000)
        val c  = (careActionsPrev30 * 42).coerceAtMost(2000)
        return s + w + h + c
    }
    /** Percentage change vs previous 30 days. Returns 0 when previous window is empty. */
    val scoreDeltaPct: Int get() {
        if (scorePrev == 0) return if (totalScore > 0) 100 else 0
        return (((totalScore - scorePrev).toFloat() / scorePrev) * 100).toInt()
    }
    val watersDelta: Int       get() = watersMonth - watersPrev30
    val scansDelta: Int        get() = scansMonth - scansPrev30
    val careActionsDelta: Int  get() = careActionsMonth - careActionsPrev30
    val healthDelta: Int       get() = avgHealth - avgHealthPrev30

    val rank: PlantRank get() = PlantRank.forScore(totalScore)
}

/** 5-tier gamification ladder. Earned XP = totalScore. */
enum class PlantRank(val displayName: String, val minXp: Int) {
    SEEDLING("Seedling", 0),
    GREEN_GROWER("Green Grower", 500),
    PLANT_EXPERT("Plant Expert", 1500),
    PLANT_MASTER("Plant Master", 3000),
    AGRO_LEGEND("Agro Legend", 6000);

    /** The next tier up, or null if this is the cap. */
    val next: PlantRank? get() = values().getOrNull(ordinal + 1)
    val nextThreshold: Int get() = next?.minXp ?: minXp

    companion object {
        fun forScore(score: Int): PlantRank =
            values().lastOrNull { score >= it.minXp } ?: SEEDLING
    }
}

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
        val cutoff30  = now - 30 * MS_PER_DAY
        val cutoff60  = now - 60 * MS_PER_DAY

        // Day-bucket arrays for the last 30 days (index 0 = 30d ago, 29 = today).
        val watersByDay30      = IntArray(30)
        val scansByDay30       = IntArray(30)
        val careActionsByDay30 = IntArray(30)
        val careActionPairs    = mutableSetOf<Pair<String, Long>>()  // (plantId, day) — unique
        val prevCareActionPairs = mutableSetOf<Pair<String, Long>>()

        plants.forEach { p ->
            p.wateringLog.forEach { ts ->
                val dayBucket = ts / MS_PER_DAY
                if (ts >= cutoff30) {
                    val idx = (29 - (today - dayBucket).toInt()).coerceIn(0, 29)
                    watersByDay30[idx]++
                    careActionPairs.add(p.id to dayBucket)
                } else if (ts >= cutoff60) {
                    prevCareActionPairs.add(p.id to dayBucket)
                }
            }
            p.scanHistory.forEach { rec ->
                val dayBucket = rec.timestamp / MS_PER_DAY
                if (rec.timestamp >= cutoff30) {
                    val idx = (29 - (today - dayBucket).toInt()).coerceIn(0, 29)
                    scansByDay30[idx]++
                    careActionPairs.add(p.id to dayBucket)
                } else if (rec.timestamp >= cutoff60) {
                    prevCareActionPairs.add(p.id to dayBucket)
                }
            }
        }
        // Rebuild careActionsByDay30 from the unique-pair set so the daily series
        // matches the monthly total exactly.
        careActionPairs.forEach { (_, dayBucket) ->
            val idx = (29 - (today - dayBucket).toInt()).coerceIn(0, 29)
            careActionsByDay30[idx]++
        }

        // Previous 30-day window totals — used for the delta arrows on stat tiles.
        var watersPrev = 0
        var scansPrev  = 0
        plants.forEach { p ->
            watersPrev += p.wateringLog.count { it in cutoff60 until cutoff30 }
            scansPrev  += p.scanHistory.count { it.timestamp in cutoff60 until cutoff30 }
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

        // This week's day-by-day activity (Mon..Sun) — used to render check-marked
        // calendar dots on the streak card. Mon = 1 in DAY_OF_WEEK (with our adjustment).
        val cal = java.util.Calendar.getInstance().apply { firstDayOfWeek = java.util.Calendar.MONDAY }
        // Find Monday of this week.
        val mondayMs = run {
            val c = cal.clone() as java.util.Calendar
            while (c.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
                c.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val weekActivity = (0 until 7).map { offset ->
            val targetDay = (mondayMs + offset * MS_PER_DAY) / MS_PER_DAY
            activeDays.contains(targetDay)
        }

        // Health trend — for each day, average healthScore across plants using
        // each plant's latest scan ≤ that day. Plants without any scan default to 75.
        val healthTrend30 = (0 until 30).map { dayOffset ->
            val targetDay  = today - (29 - dayOffset)
            val targetMs   = (targetDay + 1) * MS_PER_DAY
            val scores = plants.mapNotNull { p ->
                val plantAddedMs = p.id.removePrefix("p").toLongOrNull() ?: 0L
                if (plantAddedMs >= targetMs) return@mapNotNull null  // not added yet
                val latest = p.scanHistory.filter { it.timestamp < targetMs }.maxByOrNull { it.timestamp }
                latest?.healthScore ?: 75   // sensible default before first scan
            }
            if (scores.isEmpty()) 0 else scores.average().toInt()
        }

        // Average health for the previous 30d (used for the delta on Plant Health tile).
        // Approximate: avg of healthTrend over days 0..0 of the prev window — for simplicity
        // we use the day-30-ago value as the baseline.
        val avgHealthPrev = healthTrend30.firstOrNull() ?: 0

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
            totalPlants        = plants.size,
            avgHealth          = if (plants.isEmpty()) 0 else plants.map { it.healthScore }.average().toInt(),
            careStreak         = streak,
            watersMonth        = watersByDay30.sum(),
            scansMonth         = scansByDay30.sum(),
            careActionsMonth   = careActionPairs.size,
            watersByDay14      = watersByDay30.takeLast(14),
            scansByDay14       = scansByDay30.takeLast(14),
            watersByDay30      = watersByDay30.toList(),
            scansByDay30       = scansByDay30.toList(),
            careActionsByDay30 = careActionsByDay30.toList(),
            healthTrend30      = healthTrend30,
            watersPrev30       = watersPrev,
            scansPrev30        = scansPrev,
            careActionsPrev30  = prevCareActionPairs.size,
            avgHealthPrev30    = avgHealthPrev,
            weekActivity       = weekActivity,
            perPlant           = perPlant,
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
