package com.agrosphere.app.data.repo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.agrosphere.app.data.model.Field
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
 * Firestore-backed mirror of the user's fields — backend of the Field
 * Analytics surface. Same pattern as [PlantsCloudRepository] /
 * [ScanHistoryRepository]: query filtered by userId, no composite index needed.
 */
object FieldsCloudRepository {

    private const val COLLECTION = "fields"

    private val db get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget upsert. No-op when signed out. */
    fun saveAsync(field: Field) {
        val user = uid ?: return
        scope.launch { runCatching { saveOne(field, user) } }
    }

    /** Fire-and-forget delete. No-op when signed out. */
    fun deleteAsync(fieldId: String) {
        uid ?: return
        scope.launch {
            runCatching { db.collection(COLLECTION).document(fieldId).delete().await() }
        }
    }

    suspend fun pullAll(): List<Field> = withContext(Dispatchers.IO) {
        val user = uid ?: return@withContext emptyList()
        try {
            val snap = db.collection(COLLECTION)
                .whereEqualTo("userId", user)
                .get()
                .await()
            snap.documents.mapNotNull { docToField(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private suspend fun saveOne(field: Field, user: String) {
        val data = hashMapOf<String, Any?>(
            "userId"      to user,
            "name"        to field.name,
            "crop"        to field.crop,
            "areaHa"      to field.areaHa,
            "healthScore" to field.healthScore,
            "moisturePct" to field.moisturePct,
            "stage"       to field.stage,
            "sownDaysAgo" to field.sownDaysAgo,
            "accentArgb"  to (field.accent.toArgb().toLong() and 0xFFFFFFFFL),
            "updatedAt"   to FieldValue.serverTimestamp(),
            "schemaVersion" to 1,
        )
        db.collection(COLLECTION).document(field.id).set(data).await()
    }

    private fun docToField(doc: DocumentSnapshot): Field? = try {
        val argb = doc.getLong("accentArgb")?.toInt() ?: 0xFF00C853.toInt()
        Field(
            id          = doc.id,
            name        = doc.getString("name") ?: return null,
            crop        = doc.getString("crop") ?: "Other",
            areaHa      = doc.getDouble("areaHa") ?: 0.0,
            healthScore = (doc.getLong("healthScore") ?: 70L).toInt(),
            moisturePct = (doc.getLong("moisturePct") ?: 50L).toInt(),
            stage       = doc.getString("stage") ?: "Growing",
            sownDaysAgo = (doc.getLong("sownDaysAgo") ?: 0L).toInt(),
            accent      = Color(argb),
        )
    } catch (_: Exception) { null }
}
