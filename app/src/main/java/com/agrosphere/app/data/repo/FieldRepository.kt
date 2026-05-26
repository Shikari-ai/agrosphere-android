package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.Field
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for the user's fields.
 *
 * Seeded with four demo fields so the UX has something to show on first run.
 * In-memory for v1; swappable for Firestore later without touching the UI —
 * the [fields] StateFlow is what every screen observes.
 */
object FieldRepository {

    private val _fields = MutableStateFlow(initialFields())
    val fields: StateFlow<List<Field>> = _fields.asStateFlow()

    fun current(): List<Field> = _fields.value
    fun byId(id: String): Field? = _fields.value.firstOrNull { it.id == id }

    /** Background scope for fire-and-forget Firestore sync. */
    private val cloudScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var pulledFromCloud = false

    /** Idempotent — fetch cloud fields once on first access. Safe to call
     *  multiple times. Auto-fires lazily via [current] / [fields] read paths;
     *  Application.onCreate calls it explicitly to warm the cache. */
    fun ensurePulled() {
        if (pulledFromCloud) return
        pulledFromCloud = true
        cloudScope.launch {
            runCatching {
                val cloud = FieldsCloudRepository.pullAll()
                if (cloud.isEmpty()) return@runCatching
                val localIds = _fields.value.map { it.id }.toSet()
                val newcomers = cloud.filter { it.id !in localIds }
                if (newcomers.isNotEmpty()) _fields.update { it + newcomers }
            }
        }
    }

    fun addField(name: String, crop: String, areaHa: Double, stage: String, moisturePct: Int, accent: androidx.compose.ui.graphics.Color) {
        val id = "f${System.currentTimeMillis()}"
        val newField = Field(
            id = id,
            name = name.trim(),
            crop = crop,
            areaHa = areaHa,
            healthScore = 70,        // sensible starter — will be overwritten by next scan
            moisturePct = moisturePct,
            stage = stage,
            sownDaysAgo = 0,
            accent = accent,
        )
        _fields.update { it + newField }
        FieldsCloudRepository.saveAsync(newField)
    }

    fun removeField(id: String) {
        _fields.update { list -> list.filterNot { it.id == id } }
        FieldsCloudRepository.deleteAsync(id)
    }

    /** Empty by default — every field comes from the user via Add Field. */
    private fun initialFields(): List<Field> = emptyList()

    /** Preset accent colors offered in the Add field sheet. */
    val accentPresets: List<androidx.compose.ui.graphics.Color> = listOf(
        AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Amber, AgroPalette.Iris,
        AgroPalette.Orange, AgroPalette.Rose,
    )

    val cropPresets: List<String> = listOf(
        "Wheat", "Maize", "Rice", "Soybean", "Cotton", "Sugarcane",
        "Chickpea", "Tomato", "Onion", "Mustard", "Sorghum", "Other",
    )

    val stagePresets: List<String> = listOf(
        "Sowing", "Germination", "Tillering", "V4", "V8", "Flowering",
        "Booting", "Grain fill", "Maturity",
    )
}
