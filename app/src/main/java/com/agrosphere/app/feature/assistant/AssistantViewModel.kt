package com.agrosphere.app.feature.assistant

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Session model (history drawer) ──────────────────────────────────────────

data class ChatSession(
    val dateLabel: String,          // "Today", "Yesterday", "May 20"
    val preview: String,            // first user message, truncated to 45 chars
    val messages: List<ChatMessage>,
)

// ─── Call mode ────────────────────────────────────────────────────────────────

enum class CallMode { NONE, VOICE, VIDEO }

// ─────────────────────────────────────────────────────────────────────────────

class AssistantViewModel(app: Application) : AndroidViewModel(app) {

    private val _messages = MutableStateFlow(initialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    /** Provider that last answered (shown in badge when on Auto). */
    private val _provider = MutableStateFlow("ai")
    val provider: StateFlow<String> = _provider.asStateFlow()

    /** User-pinned provider — null = Auto. */
    private val _selectedProvider = MutableStateFlow<String?>(null)
    val selectedProvider: StateFlow<String?> = _selectedProvider.asStateFlow()

    /** Recent sessions derived from loaded history — feeds the drawer. */
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    /** Active call mode (NONE / VOICE / VIDEO). */
    private val _callMode = MutableStateFlow(CallMode.NONE)
    val callMode: StateFlow<CallMode> = _callMode.asStateFlow()

    private var nextId        = 2L
    private var weatherCache: WeatherSnapshot? = null
    private var scanCache:    List<ScanRecord>  = emptyList()

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private var tts: TextToSpeech? = null

    init {
        loadContext()
        loadHistory()
        initTts()
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun selectProvider(provider: String?) { _selectedProvider.value = provider }

    fun startVoiceCall() { _callMode.value = CallMode.VOICE }
    fun startVideoCall() { _callMode.value = CallMode.VIDEO }
    fun endCall()        { _callMode.value = CallMode.NONE; tts?.stop() }

    /** Load a past session into the active chat. */
    fun loadSession(session: ChatSession) {
        _messages.value = session.messages
        nextId = (session.messages.maxOfOrNull { it.id } ?: 0L) + 1L
        tts?.stop()
    }

    /**
     * Reset chat to the welcome greeting and write a clearedAt marker so
     * loadHistory skips old messages on next app start.
     */
    fun clearChat() {
        _messages.value = initialMessages()
        nextId = System.currentTimeMillis() / 1000L + 1L
        tts?.stop()
        val userId = uid ?: return
        viewModelScope.launch {
            runCatching {
                db.collection("app_settings").document(userId)
                    .set(
                        mapOf("chatClearedAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge(),
                    ).await()
            }
        }
    }

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return

        val userMsg = ChatMessage(nextId++, true, question,
            createdAtMs = System.currentTimeMillis())
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
                val aiMsg = ChatMessage(nextId++, false, result.text,
                    createdAtMs = System.currentTimeMillis())
                _messages.value = _messages.value + aiMsg
                _provider.value = result.provider
                persistMessage(aiMsg)
                // Speak aloud during voice / video call
                if (_callMode.value != CallMode.NONE) speakReply(result.text)
            } catch (e: Exception) {
                val err = ChatMessage(
                    nextId++, false,
                    "Connection failed — please check your internet and try again.",
                    createdAtMs = System.currentTimeMillis(),
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
        val userMsg  = ChatMessage(nextId++, true, userText, imageUri = imageUri,
            createdAtMs = System.currentTimeMillis())
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
                val aiMsg = ChatMessage(nextId++, false, formatDiagnosis(d),
                    createdAtMs = System.currentTimeMillis())
                _messages.value = _messages.value + aiMsg
                persistMessage(aiMsg)
            } catch (e: Exception) {
                val err = ChatMessage(
                    nextId++, false,
                    "I couldn't analyse that image — please check your connection and try again.",
                    createdAtMs = System.currentTimeMillis(),
                )
                _messages.value = _messages.value + err
                persistMessage(err)
            } finally {
                _isTyping.value = false
            }
        }
    }

    // ─── TTS ──────────────────────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language    = Locale.US
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
            }
        }
    }

    /** Speak an AI reply — strips markdown and caps at 600 chars. */
    fun speakReply(text: String) {
        val plain = text
            .replace(Regex("\\[.*?]\\(.*?\\)"), "") // markdown links
            .replace(Regex("[*_#`~]"), "")            // bold / italic / code
            .replace(Regex("\\n+"), ". ")             // newlines → pauses
            .take(600)
        tts?.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    // ─── Context loading ──────────────────────────────────────────────────────

    private fun loadContext() {
        viewModelScope.launch {
            runCatching { weatherCache = WeatherRepository.load(getApplication()).snapshot }
            runCatching { scanCache    = ScanHistoryRepository.recent(limit = 5) }
        }
    }

    // ─── Message persistence ──────────────────────────────────────────────────

    /**
     * Load the last 30 messages, skip any before chatClearedAt,
     * restore messages + build session list for the drawer.
     */
    private fun loadHistory() {
        val userId = uid ?: return
        viewModelScope.launch {
            try {
                val clearedAtMillis: Long = runCatching {
                    db.collection("app_settings").document(userId).get().await()
                        .getTimestamp("chatClearedAt")?.toDate()?.time ?: 0L
                }.getOrDefault(0L)

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
                    if (ts <= clearedAtMillis) return@mapNotNull null
                    Pair(ts, ChatMessage(id, fromUser, text, imgUri, ts))
                }.sortedBy { it.first }.map { it.second }

                if (loaded.isNotEmpty()) {
                    nextId = (loaded.maxOf { it.id } + 1).coerceAtLeast(nextId)
                    _messages.value = loaded
                    _sessions.value = groupIntoSessions(loaded)
                }
            } catch (_: Exception) { /* keep starter greeting */ }
        }
    }

    private fun groupIntoSessions(messages: List<ChatMessage>): List<ChatSession> {
        if (messages.isEmpty()) return emptyList()
        val todayCal = Calendar.getInstance()
        val fmt = SimpleDateFormat("MMM d", Locale.US)

        return messages
            .filter { it.createdAtMs > 0 }
            .groupBy { msg ->
                val c = Calendar.getInstance().also { it.timeInMillis = msg.createdAtMs }
                "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
            }
            .entries
            .sortedByDescending { it.key }
            .map { (_, msgs) ->
                val dayMs  = msgs.first().createdAtMs
                val dayCal = Calendar.getInstance().also { it.timeInMillis = dayMs }
                val label = when {
                    isSameDay(dayCal, todayCal)      -> "Today"
                    isYesterday(dayCal, todayCal)    -> "Yesterday"
                    else                              -> fmt.format(Date(dayMs))
                }
                val preview = msgs.firstOrNull { it.fromUser }?.text?.take(45) ?: "Chat"
                ChatSession(label, preview, msgs)
            }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(a: Calendar, b: Calendar): Boolean {
        val prev = (b.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(a, prev)
    }

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
                        "createdAt" to FieldValue.serverTimestamp(),
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
