package com.agrosphere.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class RegionalPest(
    val key: String,
    val label: String,
    val totalCount: Int,
    val confidencePct: Int,
    val riskLevel: String,    // "high" | "medium" | "low"
    val inCenter: Boolean,
    val inNeighbours: Boolean,
)

data class NeighbourAlert(val label: String, val confidencePct: Int)

data class RegionalSummary(
    val pests: List<RegionalPest>,
    val neighbourAlert: NeighbourAlert?,
    val sampledCells: Int,
)

/**
 * Reads the privacy-first regional pest index the web app writes to Firestore:
 *   pest_regional_index/{weekKey}/cells/{cellId}
 *     { pests: { [pestKey]: { label, count, intensityEWMA } }, sampleN, ... }
 *
 * Mirrors js/network/pest-regional.js + regional-privacy.js exactly so the
 * cellId / weekKey math lines up with the website's data.
 */
object RegionalPestRepository {

    private const val GRID_STEP = 0.5   // ~55 km cells

    suspend fun load(lat: Double, lng: Double): RegionalSummary = withContext(Dispatchers.IO) {
        val db       = FirebaseFirestore.getInstance()
        val weekKey  = isoWeekKey()
        val centerId = cellId(lat, lng)
        val cells    = neighbourCells(lat, lng)

        val byPest = HashMap<String, MutablePest>()
        var sampled = 0

        for (c in cells) {
            val snap = try {
                db.collection("pest_regional_index")
                    .document(weekKey)
                    .collection("cells")
                    .document(c.cellId)
                    .get()
                    .await()
            } catch (_: Exception) {
                null
            }
            if (snap == null || !snap.exists()) continue

            @Suppress("UNCHECKED_CAST")
            val pests = snap.get("pests") as? Map<String, Any?> ?: continue
            sampled += 1
            val isCenter = c.cellId == centerId

            for ((key, raw) in pests) {
                val e = raw as? Map<*, *> ?: continue
                val label     = e["label"] as? String ?: key
                val count     = (e["count"] as? Number)?.toInt() ?: 0
                val intensity = (e["intensityEWMA"] as? Number)?.toDouble() ?: 0.0

                val agg = byPest.getOrPut(key) { MutablePest(key, label) }
                agg.label = label
                agg.totalCount += count
                agg.maxIntensity = maxOf(agg.maxIntensity, intensity)
                if (isCenter) agg.inCenter = true else agg.inNeighbours = true
            }
        }

        val pests = byPest.values.map { p ->
            // Confidence: more reports + higher intensity ⇒ higher %.
            val volume = minOf(1.0, p.totalCount / 6.0)
            val conf = ((p.maxIntensity * 0.6 + volume * 0.4).coerceIn(0.0, 1.0) * 100).roundToInt()
            val risk = when {
                conf >= 70 -> "high"
                conf >= 45 -> "medium"
                else       -> "low"
            }
            RegionalPest(p.key, p.label, p.totalCount, conf, risk, p.inCenter, p.inNeighbours)
        }.sortedByDescending { it.confidencePct }

        // Cross-border heads-up: pest active in a neighbour cell but not yet in
        // the user's own cell, at meaningful confidence.
        val alert = pests.firstOrNull { it.inNeighbours && !it.inCenter && it.confidencePct >= 45 }
            ?.let { NeighbourAlert(it.label, it.confidencePct) }

        RegionalSummary(pests = pests, neighbourAlert = alert, sampledCells = sampled)
    }

    // ─── grid math (ports regional-privacy.js) ──────────────────────────────────

    private class MutablePest(val key: String, var label: String) {
        var totalCount = 0
        var maxIntensity = 0.0
        var inCenter = false
        var inNeighbours = false
    }

    private data class Cell(val cellId: String, val isCenter: Boolean)

    /** c_{rLat}_{rLng} with '-' → 'm', 2-decimal, matching the web exactly. */
    private fun cellId(lat: Double, lng: Double): String {
        val rLat = (lat / GRID_STEP).roundToLong() * GRID_STEP
        val rLng = (lng / GRID_STEP).roundToLong() * GRID_STEP
        return "c_%.2f_%.2f".format(Locale.US, rLat, rLng).replace("-", "m")
    }

    /** 3×3 grid of coarse cells centred on lat/lng, deduped. */
    private fun neighbourCells(lat: Double, lng: Double): List<Cell> {
        val center = cellId(lat, lng)
        val seen = LinkedHashSet<String>()
        val out = ArrayList<Cell>()
        for (dLat in -1..1) for (dLng in -1..1) {
            val id = cellId(lat + dLat * GRID_STEP, lng + dLng * GRID_STEP)
            if (seen.add(id)) out.add(Cell(id, id == center))
        }
        return out
    }

    /** ISO-8601 week key "yyyy-Www" in UTC — ports isoWeekKey() from the web. */
    private fun isoWeekKey(nowMillis: Long = System.currentTimeMillis()): String {
        val utc = TimeZone.getTimeZone("UTC")
        val src = Calendar.getInstance(utc).apply { timeInMillis = nowMillis }

        // Date-only at UTC midnight.
        val t = Calendar.getInstance(utc).apply {
            clear()
            set(src.get(Calendar.YEAR), src.get(Calendar.MONTH), src.get(Calendar.DAY_OF_MONTH))
        }
        // JS getUTCDay(): Sun=0..Sat=6 → here Mon=1..Sun=7
        val utcDay = (t.get(Calendar.DAY_OF_WEEK) + 6) % 7   // Java SUN=1 → 0
        val day = if (utcDay == 0) 7 else utcDay
        t.add(Calendar.DAY_OF_MONTH, 4 - day)

        val y = t.get(Calendar.YEAR)
        val yearStart = Calendar.getInstance(utc).apply { clear(); set(y, Calendar.JANUARY, 1) }
        val diffDays = (t.timeInMillis - yearStart.timeInMillis) / 86_400_000.0
        val week = ceil((diffDays + 1) / 7).toInt()
        return "%d-W%02d".format(Locale.US, y, week)
    }
}
