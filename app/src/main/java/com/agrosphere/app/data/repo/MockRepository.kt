package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.Field

/**
 * Thin compatibility shim around real repositories.
 *
 * Originally seeded everything with mock data; now everything truly mock has
 * been deleted — what remains is a forwarding facade for callers that still
 * import this object and a single assistant greeting (UI copy, not data).
 */
object MockRepository {

    /** Delegates to [FieldRepository] so additions/removals propagate everywhere. */
    val fields: List<Field> get() = FieldRepository.current()

    fun field(id: String): Field? = FieldRepository.byId(id)

    /**
     * The very first thing the AI assistant says when the user lands on the
     * Ask tab. UI copy — not synthesized data.
     */
    val starterChat: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1,
            fromUser = false,
            text = "Hi — I'm your AgroSphere assistant. " +
                "I'm running in demo mode right now: real LLM answers light up once you wire your API key. " +
                "Ask anything to see the conversation flow.",
        ),
    )

    /**
     * Deterministic stub used by the assistant in demo mode. Replace by a
     * Claude/Gemini call when an API key is configured.
     */
    fun demoReply(prompt: String): String {
        val p = prompt.lowercase().trim()
        return when {
            p.isBlank() -> "Could you rephrase?"
            "weather" in p -> "Check the Weather tab for live conditions and the 7-day outlook drawn from Open-Meteo."
            "field" in p || "plot" in p -> "Add fields from the Fields tab; everything on Home + Map + Profile updates the moment you do."
            "scan" in p || "disease" in p || "pest" in p -> "Open the Scanner tab to capture a leaf. Real ML inference plugs in via a TFLite model in v1.1."
            "irrigat" in p || "water" in p -> "Once you've added a field, AgroSphere will read its soil moisture estimate alongside today's rain probability."
            else -> "I'm in demo mode — wire an LLM (Claude, Gemini, or local) to get a real answer. Until then I'll try to nudge you to the right tab."
        }
    }
}
