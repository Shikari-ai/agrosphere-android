package com.agrosphere.app.feature.fields

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.repo.FieldRepository
import kotlinx.coroutines.flow.StateFlow

class FieldsViewModel : ViewModel() {

    val fields: StateFlow<List<Field>> = FieldRepository.fields

    fun addField(name: String, crop: String, areaHa: Double, stage: String, moisturePct: Int, accent: Color) {
        FieldRepository.addField(name, crop, areaHa, stage, moisturePct, accent)
    }

    fun deleteField(id: String) {
        FieldRepository.removeField(id)
    }

    val cropPresets get() = FieldRepository.cropPresets
    val stagePresets get() = FieldRepository.stagePresets
    val accentPresets get() = FieldRepository.accentPresets

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FieldsViewModel() as T
        }
    }
}
