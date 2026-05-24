package com.agrosphere.app.feature.scanner

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.i18n.LocaleManager
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.LocalScanStore
import com.agrosphere.app.data.repo.SavedScan
import com.agrosphere.app.data.repo.RegionalContributionRepository
import com.agrosphere.app.data.repo.ScanHistoryRepository
import com.agrosphere.app.data.repo.VisionDiagnosis
import com.agrosphere.app.data.repo.VisionScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val scanning: Boolean = false,
    val diagnosis: VisionDiagnosis? = null,
    val error: String? = null,
)

class ScannerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<SavedScan>>(emptyList())
    val history: StateFlow<List<SavedScan>> = _history.asStateFlow()

    init { loadHistory() }

    /** Run AI vision diagnosis, then auto-save the full result to on-device history. */
    fun scan(bitmap: Bitmap, cropType: String = "") {
        _state.value = ScanUiState(scanning = true)
        viewModelScope.launch {
            _state.value = try {
                val d = VisionScanRepository.analyze(
                    bitmap        = bitmap,
                    cropType      = cropType,
                    fields        = FieldRepository.current(),
                    replyLanguage = LocaleManager.activeLanguageTag(),
                )
                // Save the FULL diagnosis locally and refresh history.
                LocalScanStore.add(
                    getApplication(),
                    SavedScan(System.currentTimeMillis(), cropType, d),
                )
                _history.value = LocalScanStore.load(getApplication())
                // Best-effort cloud backup (won't block or fail the scan).
                launch { runCatching { ScanHistoryRepository.save(d, cropType) } }
                launch { runCatching { RegionalContributionRepository.contribute(getApplication(), d) } }
                ScanUiState(diagnosis = d)
            } catch (e: Exception) {
                ScanUiState(error = e.message ?: "Scan failed")
            }
        }
    }

    fun loadHistory() {
        _history.value = LocalScanStore.load(getApplication())
    }

    fun reset() {
        _state.value = ScanUiState()
    }
}
