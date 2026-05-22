package com.agrosphere.app.data.repo

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A fully-detailed saved scan kept on the device. */
@Serializable
data class SavedScan(
    val createdAtMillis: Long,
    val cropType: String,
    val diagnosis: VisionDiagnosis,
)

/**
 * On-device scan history (SharedPreferences + JSON). Stores the FULL diagnosis
 * for each scan so the history screen can show every detail. Works offline and
 * needs no login.
 */
object LocalScanStore {

    private const val PREFS = "agro_scan_history"
    private const val KEY = "scans_v2"
    private const val MAX = 100

    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun add(context: Context, scan: SavedScan) {
        val updated = (load(context) + scan)
            .sortedByDescending { it.createdAtMillis }
            .take(MAX)
        prefs(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }

    fun load(context: Context): List<SavedScan> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedScan>>(raw).sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }
}
