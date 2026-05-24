package com.agrosphere.app.feature.assistant

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.GeminiRepository
import com.agrosphere.app.data.repo.ScanHistoryRepository
import com.agrosphere.app.data.repo.ScanRecord
import com.agrosphere.app.data.repo.VisionDiagnosis
import com.agrosphere.app.data.repo.VisionScanRepository
import com.agrosphere.app.data.weather.WeatherRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AssistantViewModel(app: Application) : AndroidViewModel(app) {

    private val _messages  = MutableStateFlow(initialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping  = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    /** Provider that last answered (shown in badge when on Auto). */
    private val _provider  = MutableStateFlow("ai")
    val provider: StateFlow<String> = _provider.asStateFlow()

    /** User-pinned provider — null = Auto (proxy cascades gemini→groq→github). */
    private val _selectedProvider = MutableStateFlow<String?>(null)
    val selectedProvider: StateFlow<String?> = _selectedProvider.asStateFlow()

    private var nextId        = 2L
    private var weatherCache: WeatherSnapshot? = null
    private var scanCache:    List<ScanRecord>  = emptyList()

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadContext()
        loadHistory()
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun selectProvider(provider: String?) { _selectedProvider.value = provider }

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return

        val userMsg = ChatMessage(nextId++, true, question)
        _messages.value = _messages.value + userMsg
        _isTyping.value = true
        persistMessage(userMsg)

        viewModelScope.launch {
            try {
                val result = GeminiRepository.chat(
                    question      = question,
                    history       = _messages.value,
                    fields        = FieldRepository.current(),
                    weather       = weatherCache,
                    recentScans   = scanCache,
                    forceProvider = _selectedProvider.value,
                )
                val aiMsg = ChatMessage(nextId++, false, result.text)
                _messages.value = _messages.value + aiMsg
                _provider.value = result.provider
                persistMessage(aiMsg)
            } catch (e: Exception) {
                val err = ChatMessage(
                    nextId++, false,
                    "Connection failed — please check your internet and try again.",
                )
                _messages.value = _messages.value + err
                persistMessage(err)
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun sendImage(bitmap: Bitmap, imageUri: String, caption: String) {
        val userText = caption.trim().ifEmpty { "📷 Photo" }
        val userMsg  = ChatMessage(nextId++, true, userText, imageUri = imageUri)
        _messages.value = _messages.value + userMsg
        _isTyping.value = true
        persistMessage(userMsg)

        viewModelScope.launch {
            try {
                val d = VisionScanRepository.analyze(
                    bitmap   = bitmap,
                    cropType = "",
                    fields   = FieldRepository.current(),
                )
                val aiMsg = ChatMessage(nextId++, false, formatDiagnosis(d))
                _messages.value = _messages.value + aiMsg
                persistMessage(aiMsg)
            } catch (e: Exception) {
                val err = ChatMessage(
                    nextId++, false,
                    "I couldn't analyse that image — please check your connection and try again.",
                )
                _messages.value = _messages.value + err
                persistMessage(err)
            } finally {
                _isTyping.value = false
            }
        }
    }

    // ─── Context loading ──────────────────────────────────────────────────────

    /** Pre-load weather and recent scans once, so first message has full context. */
    private fun loadContext() {
        viewModelScope.launch {
            // Weather (optional — carry on without it)
            runCatching {
                weatherCache = WeatherRepository.load(getApplication()).snapshot
            }
            // Recent scans — gives the AI "your last scan found X on Y"
            runCatching {
                scanCache = ScanHistoryRepository.recent(limit = 5)
            }
        }
    }

    // ─── Message persistence ──────────────────────────────────────────────────

    /**
     * Load the last 30 messages for the signed-in user from Firestore.
     * Replaces the starter greeting only if actual history exists.
     */
    private fun loadHistory() {
        val userId = uid ?: return
        viewModelScope.launch {
            try {
                val snap = db.collection("assistant_messages")
                    .whereEqualTo("userId", userId)
                    .limit(30)
                    .get()
                    .await()

                val loaded = snap.documents.mapNotNull { doc ->
                    val id       = doc.getLong("msgId")       ?: return@mapNotNull null
                    val fromUser = doc.getBoolean("fromUser") ?: return@mapNotNull null
                    val text     = doc.getString("text")      ?: return@mapNotNull null
                    val imgUri   = doc.getString("imageUri")
                    val ts       = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    Pair(ts, ChatMessage(id, fromUser, text, imgUri))
                }.sortedBy { it.first }.map { it.second }

                if (loaded.isNotEmpty()) {
                    nextId = (loaded.maxOf { it.id } + 1).coerceAtLeast(nextId)
                    _messages.value = loaded
                }
            } catch (_: Exception) { /* keep starter greeting */ }
        }
    }

    /** Best-effort: save a single message to Firestore `assistant_messages`. */
    private fun persistMessage(msg: ChatMessage) {
        val userId = uid ?: return
        viewModelScope.launch {
            runCatching {
                val docId = "${userId}_${msg.id}"
                db.collection("assistant_messages").document(docId).set(
                    mapOf(
                        "userId"    to userId,
                        "msgId"     to msg.id,
                        "fromUser"  to msg.fromUser,
                        "text"      to msg.text,
                        "imageUri"  to msg.imageUri,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
            }
        }
    }

    // ─── Formatting ───────────────────────────────────────────────────────────

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
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun initialMessages(): List<ChatMessage> = listOf(
    ChatMessage(
        id       = 1L,
        fromUser = false,
        text     = "Hi — I'm your AgroSphere AI assistant. Ask me anything about your crops, weather, pests, or irrigation.",
    )
)
