package com.agrosphere.app.feature.regional

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.repo.NeighbourAlert
import com.agrosphere.app.data.repo.RegionalContributionRepository
import com.agrosphere.app.data.repo.RegionalGridMath
import com.agrosphere.app.data.repo.RegionalPest
import com.agrosphere.app.data.repo.RegionalPestRepository
import com.agrosphere.app.data.weather.LocationProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// UI state
// ─────────────────────────────────────────────────────────────────────────────

sealed class RegionalUiState {
    object Loading : RegionalUiState()

    data class Ready(
        val connectedFarms: Int,
        val optedIn: Boolean,
        val joiningInProgress: Boolean = false,
        val pests: List<RegionalPest> = emptyList(),
        val neighbourAlert: NeighbourAlert? = null,
        val userId: String? = null,
        val errorToast: String? = null,
    ) : RegionalUiState()

    data class Error(val message: String) : RegionalUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class RegionalViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<RegionalUiState>(RegionalUiState.Loading)
    val state: StateFlow<RegionalUiState> = _state.asStateFlow()

    private val auth = AuthRepository()
    private val db   = FirebaseFirestore.getInstance()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = RegionalUiState.Loading
            try {
                val context = getApplication<Application>().applicationContext
                val place   = LocationProvider.fastCurrent(context)
                val userId  = auth.currentUser?.uid

                // Opt-in flag
                val optedIn = userId?.let {
                    try {
                        db.collection("regional_intel_settings").document(it).get().await()
                            .getBoolean("optIn") ?: false
                    } catch (_: Exception) { false }
                } ?: false

                // Farm count: sum farmCount across the 3×3 coarse grid from the
                // opt-in registry — goes up immediately when a user joins, not
                // only when they first scan.
                val connectedFarms = fetchFarmCount(place.latitude, place.longitude)

                // Pest signals (unchanged — still driven by actual scan contributions)
                val summary = RegionalPestRepository.load(place.latitude, place.longitude)

                _state.value = RegionalUiState.Ready(
                    connectedFarms = connectedFarms,
                    optedIn        = optedIn,
                    pests          = summary.pests,
                    neighbourAlert = summary.neighbourAlert,
                    userId         = userId,
                )
            } catch (e: Exception) {
                _state.value = RegionalUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    fun joinWaitList() {
        val current = _state.value as? RegionalUiState.Ready ?: return
        val userId  = current.userId ?: run {
            _state.update { s -> (s as? RegionalUiState.Ready)?.copy(errorToast = "Sign in to join") ?: s }
            return
        }
        _state.update { s -> (s as? RegionalUiState.Ready)?.copy(joiningInProgress = true) ?: s }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext

                // 1. Persist opt-in flag
                db.collection("regional_intel_settings").document(userId)
                    .set(mapOf("optIn" to true, "joinedAt" to FieldValue.serverTimestamp()))
                    .await()

                // 2. Register farm in the coarse-grid registry (increments farmCount)
                RegionalContributionRepository.registerFarm(context, userId)

                _state.update { s ->
                    (s as? RegionalUiState.Ready)?.copy(
                        optedIn           = true,
                        joiningInProgress = false,
                        connectedFarms    = (s.connectedFarms + 1),
                    ) ?: s
                }
            } catch (_: Exception) {
                _state.update { s ->
                    (s as? RegionalUiState.Ready)?.copy(
                        joiningInProgress = false,
                        errorToast        = "Could not save opt-in. Try again.",
                    ) ?: s
                }
            }
        }
    }

    // ── Leave ─────────────────────────────────────────────────────────────────

    fun leaveNetwork() {
        val current = _state.value as? RegionalUiState.Ready ?: return
        val userId  = current.userId ?: run {
            _state.update { s -> (s as? RegionalUiState.Ready)?.copy(errorToast = "Sign in required") ?: s }
            return
        }
        _state.update { s -> (s as? RegionalUiState.Ready)?.copy(joiningInProgress = true) ?: s }

        viewModelScope.launch {
            try {
                // 1. Decrement farm registry first (reads stored cellId)
                RegionalContributionRepository.deregisterFarm(userId)

                // 2. Flip opt-in flag
                db.collection("regional_intel_settings").document(userId)
                    .set(mapOf("optIn" to false), SetOptions.merge())
                    .await()

                _state.update { s ->
                    (s as? RegionalUiState.Ready)?.copy(
                        optedIn           = false,
                        joiningInProgress = false,
                        connectedFarms    = maxOf(0, (s.connectedFarms - 1)),
                    ) ?: s
                }
            } catch (_: Exception) {
                _state.update { s ->
                    (s as? RegionalUiState.Ready)?.copy(
                        joiningInProgress = false,
                        errorToast        = "Could not leave network. Try again.",
                    ) ?: s
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Sum farmCount across the 3×3 coarse grid from regional_network registry. */
    private suspend fun fetchFarmCount(lat: Double, lng: Double): Int {
        return try {
            val weekKey = RegionalGridMath.isoWeekKey()
            val cells   = RegionalGridMath.neighbourCellIds(lat, lng)
            var total   = 0
            for (cellId in cells) {
                val snap = db.collection("regional_network").document(weekKey)
                    .collection("cells").document(cellId)
                    .get().await()
                total += (snap.getLong("farmCount") ?: 0L).toInt()
            }
            total
        } catch (_: Exception) { 0 }
    }

    fun clearToast() {
        _state.update { s -> (s as? RegionalUiState.Ready)?.copy(errorToast = null) ?: s }
    }

    fun retry() { load() }
}
