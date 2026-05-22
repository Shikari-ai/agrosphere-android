package com.agrosphere.app.data.repo

import android.content.Context
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.weather.LocationProvider
import com.agrosphere.app.data.repo.RegionalGridMath.cellId
import com.agrosphere.app.data.repo.RegionalGridMath.dayKey
import com.agrosphere.app.data.repo.RegionalGridMath.isoWeekKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

object RegionalContributionRepository {

    private val auth = AuthRepository()
    private val db get() = FirebaseFirestore.getInstance()

    // ─────────────────────────────────────────────────────────────────────────
    // Pest-signal contribution (called after every non-healthy scan)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Best-effort: publish an anonymized scan signal to the regional pest index.
     * Silently no-ops if user isn't signed in / hasn't opted in / scan is healthy
     * or below confidence threshold. Never throws.
     */
    suspend fun contribute(context: Context, diagnosis: VisionDiagnosis) = withContext(Dispatchers.IO) {
        if (diagnosis.riskLevel == "healthy") return@withContext
        if (diagnosis.confidence < 50) return@withContext

        val userId = auth.currentUser?.uid ?: return@withContext

        val optedIn = try {
            db.collection("regional_intel_settings").document(userId).get().await()
                .getBoolean("optIn") ?: false
        } catch (_: Exception) { false }
        if (!optedIn) return@withContext

        val place = try { LocationProvider.fastCurrent(context) } catch (_: Exception) { return@withContext }

        val weekKey = isoWeekKey()
        val cellId  = cellId(place.latitude, place.longitude)
        val dayKey  = dayKey()
        val pestKey = diagnosis.diseaseName
            .lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').take(80)

        val cellRef  = db.collection("pest_regional_index").document(weekKey)
                         .collection("cells").document(cellId)
        val quotaRef = db.collection("users").document(userId)
                         .collection("regional_contrib_quota").document(dayKey)

        try {
            db.runTransaction { txn ->
                val quotaCount = (txn.get(quotaRef).getLong("count") ?: 0L).toInt()
                if (quotaCount >= 10) return@runTransaction

                val cellSnap = txn.get(cellRef)
                @Suppress("UNCHECKED_CAST")
                val pestsMap = (cellSnap.get("pests") as? Map<String, Any?>)?.toMutableMap()
                    ?: mutableMapOf()

                @Suppress("UNCHECKED_CAST")
                val prevEntry   = pestsMap[pestKey] as? Map<String, Any?>
                val prevEWMA    = (prevEntry?.get("intensityEWMA") as? Number)?.toDouble() ?: 0.0
                val prevCount   = (prevEntry?.get("count") as? Number)?.toInt() ?: 0
                val label       = prevEntry?.get("label") as? String ?: diagnosis.diseaseName
                val newEWMA     = prevEWMA * 0.75 + (diagnosis.confidence / 100.0) * 0.25

                pestsMap[pestKey] = mapOf("label" to label, "count" to (prevCount + 1), "intensityEWMA" to newEWMA)

                txn.set(
                    cellRef,
                    mapOf(
                        "cellId"    to cellId,
                        "pests"     to pestsMap,
                        "sampleN"   to ((cellSnap.getLong("sampleN") ?: 0L).toInt() + 1),
                        "updatedAt" to Timestamp.now(),
                    ),
                    SetOptions.merge(),
                )
                txn.set(quotaRef, mapOf("count" to (quotaCount + 1)), SetOptions.merge())
            }.await()
        } catch (_: Exception) { /* best-effort */ }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Farm registry (opt-in / opt-out)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Register this user's coarse grid cell in the network for the current week.
     * Idempotent — a second call in the same week is a no-op.
     */
    suspend fun registerFarm(context: Context, userId: String) = withContext(Dispatchers.IO) {
        try {
            val place   = LocationProvider.fastCurrent(context)
            val weekKey = isoWeekKey()
            val cellId  = cellId(place.latitude, place.longitude)

            val cellRef = db.collection("regional_network").document(weekKey)
                            .collection("cells").document(cellId)
            val regRef  = db.collection("users").document(userId)
                            .collection("regional_network_reg").document(weekKey)

            db.runTransaction { txn ->
                if (txn.get(regRef).exists()) return@runTransaction  // already registered
                txn.set(regRef, mapOf("cellId" to cellId, "weekKey" to weekKey))
                txn.set(cellRef, mapOf("farmCount" to FieldValue.increment(1)), SetOptions.merge())
            }.await()
        } catch (_: Exception) { /* best-effort */ }
    }

    /**
     * Remove this user's farm count from the network for the current week.
     * Reads their stored cellId so the decrement hits the right cell even
     * if they moved since opting in.
     */
    suspend fun deregisterFarm(userId: String) = withContext(Dispatchers.IO) {
        try {
            val weekKey = isoWeekKey()
            val regRef  = db.collection("users").document(userId)
                            .collection("regional_network_reg").document(weekKey)

            val regSnap = regRef.get().await()
            if (!regSnap.exists()) return@withContext

            val cellId  = regSnap.getString("cellId") ?: return@withContext
            val cellRef = db.collection("regional_network").document(weekKey)
                            .collection("cells").document(cellId)

            db.runTransaction { txn ->
                val reg = txn.get(regRef)
                if (!reg.exists()) return@runTransaction
                val prev = txn.get(cellRef).getLong("farmCount") ?: 0L
                txn.set(cellRef, mapOf("farmCount" to maxOf(0L, prev - 1)), SetOptions.merge())
                txn.delete(regRef)
            }.await()
        } catch (_: Exception) { /* best-effort */ }
    }
}
