package com.agrosphere.app.data.repo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.model.PlantScanRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firestore-backed mirror of the user's home plants — the "backend" of the
 * Plant Analytics surface. Mirrors the [ScanHistoryRepository] pattern: query
 * is filtered by userId only so no composite Firestore index is required.
 *
 * Wire-up:
 * - Every mutation on [PlantRepository] (add / delete / logWatering / applyScan /
 *   setStage) fires a best-effort [saveAsync] / [deleteAsync] to Firestore.
 * - On app start, [PlantRepository.init] kicks off [pullAll] and merges
 *   cloud-only plants into the local cache so new-device sign-ins recover
 *   the user's full inventory.
 *
 * Documents live at `plants/{plantId}` and contain the full plant state
 * including the watering log and scan history as embedded arrays — well
 * within Firestore's 1 MB per-doc limit (history is capped at 30 entries).
 */
object PlantsCloudRepository {

    private const val COLLECTION = "plants"

    private val db get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    /** Survives the lifetime of the app — best-effort background work that
     *  must never bubble exceptions up to the UI thread. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget upsert of a plant doc. Silently no-ops when signed out. */
    fun saveAsync(plant: PlantEntry) {
        val user = uid ?: return
        scope.launch { runCatching { saveOne(plant, user) } }
    }

    /** Fire-and-forget delete. Silently no-ops when signed out. */
    fun deleteAsync(plantId: String) {
        uid ?: return
        scope.launch {
            runCatching { db.collection(COLLECTION).document(plantId).delete().await() }
        }
    }

    /**
     * Pull every plant for the signed-in user. Returns an empty list when
     * offline, signed out, or on any other failure — never throws.
     */
    suspend fun pullAll(): List<PlantEntry> = withContext(Dispatchers.IO) {
        val user = uid ?: return@withContext emptyList()
        try {
            val snap = db.collection(COLLECTION)
                .whereEqualTo("userId", user)
                .get()
                .await()
            snap.documents.mapNotNull { docToEntry(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private suspend fun saveOne(plant: PlantEntry, user: String) {
        val data = hashMapOf<String, Any?>(
            "userId"               to user,
            "name"                 to plant.name,
            "species"              to plant.species,
            "location"             to plant.location,
            "potSize"              to plant.potSize,
            "sunlightNeed"         to plant.sunlightNeed,
            "wateringIntervalDays" to plant.wateringIntervalDays,
            "lastWateredMs"        to plant.lastWateredMs,
            "wateringLog"          to plant.wateringLog,
            "healthScore"          to plant.healthScore,
            "accentArgb"           to (plant.accent.toArgb().toLong() and 0xFFFFFFFFL),
            "stage"                to plant.stage,
            "lastScanMs"           to plant.lastScanMs,
            "scientificName"       to plant.scientificName,
            "variety"              to plant.variety,
            "soilType"             to plant.soilType,
            "careNote"             to plant.careNote,
            "scanHistory"          to plant.scanHistory.map { rec ->
                mapOf(
                    "timestamp"       to rec.timestamp,
                    "verdict"         to rec.verdict,
                    "healthScore"     to rec.healthScore,
                    "riskLevel"       to rec.riskLevel,
                    "summary"         to rec.summary,
                    "recommendations" to rec.recommendations,
                    // photoPath is a local file path; not portable across devices.
                )
            },
            "updatedAt"            to FieldValue.serverTimestamp(),
            "schemaVersion"        to 1,
        )
        db.collection(COLLECTION).document(plant.id).set(data).await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToEntry(doc: DocumentSnapshot): PlantEntry? = try {
        val argb = doc.getLong("accentArgb")?.toInt() ?: 0xFF00C853.toInt()
        val scanHistory = (doc.get("scanHistory") as? List<Map<String, Any?>>).orEmpty().mapNotNull { raw ->
            try {
                PlantScanRecord(
                    timestamp       = (raw["timestamp"] as? Number)?.toLong() ?: return@mapNotNull null,
                    verdict         = raw["verdict"] as? String ?: "Healthy",
                    healthScore     = (raw["healthScore"] as? Number)?.toInt() ?: 60,
                    riskLevel       = raw["riskLevel"] as? String ?: "low",
                    summary         = raw["summary"] as? String ?: "",
                    recommendations = (raw["recommendations"] as? List<String>).orEmpty(),
                    photoPath       = null,
                )
            } catch (_: Exception) { null }
        }
        val wateringLog = (doc.get("wateringLog") as? List<Number>).orEmpty().map { it.toLong() }

        PlantEntry(
            id                   = doc.id,
            name                 = doc.getString("name") ?: return null,
            species              = doc.getString("species") ?: "",
            location             = doc.getString("location") ?: "Living Room",
            potSize              = doc.getString("potSize") ?: "Medium pot",
            sunlightNeed         = doc.getString("sunlightNeed") ?: "Partial Shade",
            wateringIntervalDays = (doc.getLong("wateringIntervalDays") ?: 7L).toInt(),
            lastWateredMs        = doc.getLong("lastWateredMs") ?: 0L,
            wateringLog          = wateringLog,
            healthScore          = (doc.getLong("healthScore") ?: 75L).toInt(),
            accent               = Color(argb),
            stage                = doc.getString("stage") ?: "Growing",
            scanHistory          = scanHistory,
            lastScanMs           = doc.getLong("lastScanMs") ?: 0L,
            photoPath            = null,
            scientificName       = doc.getString("scientificName") ?: "",
            variety              = doc.getString("variety") ?: "",
            soilType             = doc.getString("soilType") ?: "",
            careNote             = doc.getString("careNote") ?: "",
        )
    } catch (_: Exception) { null }
}
