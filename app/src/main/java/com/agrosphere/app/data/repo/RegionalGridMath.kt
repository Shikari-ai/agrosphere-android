package com.agrosphere.app.data.repo

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Shared grid maths — ports regional-privacy.js exactly.
 * Used by RegionalContributionRepository and RegionalViewModel so
 * the cell/week keys always match the web app and each other.
 */
internal object RegionalGridMath {

    const val GRID_STEP = 0.5   // ~55 km coarse cells

    /** c_{rLat}_{rLng} with '-' → 'm', 2-decimal. */
    fun cellId(lat: Double, lng: Double): String {
        val rLat = (lat / GRID_STEP).roundToLong() * GRID_STEP
        val rLng = (lng / GRID_STEP).roundToLong() * GRID_STEP
        return "c_%.2f_%.2f".format(Locale.US, rLat, rLng).replace("-", "m")
    }

    /** ISO-8601 week key "yyyy-Www" in UTC. */
    fun isoWeekKey(nowMillis: Long = System.currentTimeMillis()): String {
        val utc = TimeZone.getTimeZone("UTC")
        val src = Calendar.getInstance(utc).apply { timeInMillis = nowMillis }
        val t = Calendar.getInstance(utc).apply {
            clear()
            set(src.get(Calendar.YEAR), src.get(Calendar.MONTH), src.get(Calendar.DAY_OF_MONTH))
        }
        val utcDay = (t.get(Calendar.DAY_OF_WEEK) + 6) % 7
        val day = if (utcDay == 0) 7 else utcDay
        t.add(Calendar.DAY_OF_MONTH, 4 - day)
        val y = t.get(Calendar.YEAR)
        val yearStart = Calendar.getInstance(utc).apply { clear(); set(y, Calendar.JANUARY, 1) }
        val diffDays = (t.timeInMillis - yearStart.timeInMillis) / 86_400_000.0
        val week = ceil((diffDays + 1) / 7).toInt()
        return "%d-W%02d".format(Locale.US, y, week)
    }

    /** UTC date key "yyyy-MM-dd". */
    fun dayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(nowMillis)
    }

    /** Deduped list of cell IDs for the 3×3 grid centred on lat/lng. */
    fun neighbourCellIds(lat: Double, lng: Double): List<String> {
        val seen = LinkedHashSet<String>()
        for (dLat in -1..1) for (dLng in -1..1)
            seen.add(cellId(lat + dLat * GRID_STEP, lng + dLng * GRID_STEP))
        return seen.toList()
    }
}
