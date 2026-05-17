package com.agrosphere.app.feature.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.agrosphere.app.data.weather.WeatherBundle
import com.agrosphere.app.data.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Loaded(val data: WeatherBundle) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<WeatherUiState>(
        // Cached bundle → render instantly; cold start → Loading spinner.
        WeatherRepository.cached()?.let { WeatherUiState.Loaded(it) } ?: WeatherUiState.Loading
    )
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    init {
        // Refresh in the background no matter what — if we showed cache, this
        // silently swaps in fresher numbers; if we showed the spinner, this
        // resolves it. Either way the user never waits longer than the first
        // real fetch.
        refresh(showLoadingIfNoCache = false)
    }

    fun refresh() = refresh(showLoadingIfNoCache = true)

    private fun refresh(showLoadingIfNoCache: Boolean) {
        if (showLoadingIfNoCache && _state.value !is WeatherUiState.Loaded) {
            _state.value = WeatherUiState.Loading
        }
        viewModelScope.launch {
            try {
                val bundle = WeatherRepository.load(getApplication())
                _state.value = WeatherUiState.Loaded(bundle)
            } catch (t: Throwable) {
                // Don't blow away a cached view just because the refresh failed.
                if (_state.value !is WeatherUiState.Loaded) {
                    _state.value = WeatherUiState.Error(t.message ?: "Couldn't reach the weather service.")
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { WeatherViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application) }
        }
    }
}
