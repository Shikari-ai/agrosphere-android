package com.agrosphere.app.feature.assistant

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.GeminiRepository
import com.agrosphere.app.data.repo.VisionDiagnosis
import com.agrosphere.app.data.repo.VisionScanRepository
import com.agrosphere.app.data.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(app: Application) : AndroidViewModel(app) {

    private val _messages = MutableStateFlow(initialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Which provider answered last (shown in the badge when on Auto)
    private val _provider = MutableStateFlow("ai")
    val provider: StateFlow<String> = _provider.asStateFlow()

    // User's pinned provider — null means Auto (val cascades gemini→groq→github)
    private val _selectedProvider = MutableStateFlow<String?>(null)
    val selectedProvider: StateFlow<String?> = _selectedProvider.asStateFlow()

    private var nextId = 2L
    private var weatherCache: WeatherSnapshot? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Pin a provider ("gemini"/"groq"/"github") or pass null for Auto. */
    fun selectProvider(provider: String?) {
        _selectedProvider.value = provider
    }

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return

        // Append user bubble first so history is up to date before the call
        _messages.value = _messages.value + ChatMessage(nextId++, true, question)
        _isTyping.value = true

        viewModelScope.launch {
            try {
                ensureWeather()
                val result = GeminiRepository.chat(
                    question      = question,
                    history       = _messages.value,   // repo drops greeting + last msg internally
                    fields        = FieldRepository.current(),
                    weather       = weatherCache,
                    forceProvider = _selectedProvider.value,
                )
                _messages.value = _messages.value + ChatMessage(nextId++, false, result.text)
                _provider.value = result.provider
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    nextId++, false,
                    "Connection failed — please check your internet and try again.",
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    /** Analyse an attached photo with the vision backend and reply with a diagnosis. */
    fun sendImage(bitmap: Bitmap, imageUri: String, caption: String) {
        val userText = caption.trim().ifEmpty { "📷 Photo" }
        _messages.value = _messages.value + ChatMessage(nextId++, true, userText, imageUri = imageUri)
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val d = VisionScanRepository.analyze(
                    bitmap = bitmap,
                    cropType = "",
                    fields = FieldRepository.current(),
                )
                _messages.value = _messages.value + ChatMessage(nextId++, false, formatDiagnosis(d))
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    nextId++, false,
                    "I couldn't analyse that image — please check your connection and try again.",
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun formatDiagnosis(d: VisionDiagnosis): String = buildString {
        append(d.narrative.ifBlank { d.summary }.ifBlank { d.diseaseName })
        if (d.confidence > 0) append(" (${d.confidence}% confidence)")
        if (d.recommendations.isNotEmpty()) {
            append("\n\nWhat to do:")
            d.recommendations.forEach { append("\n• $it") }
        }
        if (d.treatments.isNotEmpty()) {
            append("\n\nTreatments:")
            d.treatments.forEach { t ->
                append("\n• ${t.name}")
                if (t.usage.isNotBlank()) append(" — ${t.usage}")
            }
        }
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    /** Fetch weather once per session and cache it. */
    private suspend fun ensureWeather() {
        if (weatherCache != null) return
        try {
            weatherCache = WeatherRepository.load(getApplication()).snapshot
        } catch (_: Exception) {
            // Weather context is optional — carry on without it.
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun initialMessages(): List<ChatMessage> = listOf(
    ChatMessage(
        id = 1L,
        fromUser = false,
        text = "Hi — I'm your AgroSphere AI assistant. Ask me anything about your crops, weather, pests, or irrigation.",
    )
)
