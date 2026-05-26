package com.agrosphere.app.feature.plants

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.repo.PlantRepository
import kotlinx.coroutines.flow.StateFlow

class PlantsViewModel : ViewModel() {

    val plants: StateFlow<List<PlantEntry>> = PlantRepository.plants

    fun addPlant(
        name: String,
        species: String,
        location: String,
        potSize: String,
        sunlightNeed: String,
        wateringIntervalDays: Int,
        accent: Color,
    ) = PlantRepository.addPlant(name, species, location, potSize, sunlightNeed, wateringIntervalDays, accent)

    fun deletePlant(id: String) = PlantRepository.removePlant(id)

    val locationPresets    get() = PlantRepository.locationPresets
    val sunlightPresets    get() = PlantRepository.sunlightPresets
    val potSizePresets     get() = PlantRepository.potSizePresets
    val accentPresets      get() = PlantRepository.accentPresets

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlantsViewModel() as T
        }
    }
}
