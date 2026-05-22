package com.agrosphere.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** A saved scan, summarised for the history list. Serializable for on-device storage. */
@Serializable
data class ScanRecord(
    val diseaseName: String,
    val riskLevel: String,      // healthy | low | medium | high
    val confidence: Int,
    val cropType: String,
    val createdAtMillis: Long,
)

/**
 * Persists scans to Firestore (`crop_scans`) and reads the signed-in user's
 * recent scans for the history list.
 *
 * Query is filtered by userId only (auto-indexed) and sorted client-side, so
 * no composite Firestore index needs to be created by hand.
 */
object ScanHistoryRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun save(d: VisionDiagnosis, cropType: String): Boolean = withContext(Dispatchers.IO) {
        val user = uid ?: return@withContext false
        val data = hashMapOf(
            "userId" to user,
            "source" to "android_vision",
            "diseaseName" to d.diseaseName,
            "scientificName" to d.scientificName,
            "riskLevel" to d.riskLevel,
            "confidence" to d.confidence,
            "summary" to d.summary,
            "plantType" to d.plantType,
            "narrative" to d.narrative,
            "cropType" to cropType,
            "recommendations" to d.recommendations,
            "treatments" to d.treatments.map {
                mapOf("type" to it.type, "name" to it.name, "usage" to it.usage)
            },
            "createdAt" to FieldValue.serverTimestamp(),
            "schemaVersion" to 1,
        )
        try {
            db.collection("crop_scans").add(data).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun recent(limit: Int = 10): List<ScanRecord> = withContext(Dispatchers.IO) {
        val user = uid ?: return@withContext emptyList()
        try {
            val snap = db.collection("crop_scans")
                .whereEqualTo("userId", user)
                .limit(50)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                ScanRecord(
                    diseaseName = doc.getString("diseaseName") ?: return@mapNotNull null,
                    riskLevel = doc.getString("riskLevel") ?: "low",
                    confidence = (doc.getLong("confidence") ?: 0L).toInt(),
                    cropType = doc.getString("cropType") ?: "",
                    createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                )
            }.sortedByDescending { it.createdAtMillis }.take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
