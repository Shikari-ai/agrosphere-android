package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.Field
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    }

    fun removeField(id: String) {
        _fields.update { list -> list.filterNot { it.id == id } }
    }

    private fun initialFields(): List<Field> = listOf(
        Field("f1", "North Paddock", "Wheat", 4.2, 86, 62, "Tillering", 38, AgroPalette.Primary),
        Field("f2", "Lake Plot", "Soybean", 2.6, 71, 48, "Flowering", 64, AgroPalette.Sky),
        Field("f3", "Hilltop", "Maize", 5.1, 92, 70, "V8", 52, AgroPalette.Amber),
        Field("f4", "Riverside", "Rice", 3.4, 58, 84, "Booting", 71, AgroPalette.Iris),
    )

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
