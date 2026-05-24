package com.agrosphere.app.data.repo

import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL

data class AiReply(val text: String, val provider: String)

/**
 * Sends chat messages to the AgroSphere Val.town proxy.
 * The proxy holds all AI keys server-side (Gemini → Groq → GitHub fallback)
 * and returns { reply, provider, model }.
 *
 * Request:
 *   POST [PROXY_URL]
 *   Origin: https://agritech-4d1ba.web.app   ← required by proxy CORS check
 *   { question, history:[{role,text}], farmContext:{fields,weather,location} }
 *
 * Response: { reply, provider, model, promptTokens, completionTokens, attempts }
 */
object GeminiRepository {

    private const val PROXY_URL =
        "https://harshwardhanparganiha--992de5aa4d5911f1849eee650bb23af1.web.val.run"

    // Key lives on the server — always ready.
    val isConfigured: Boolean = true

    suspend fun chat(
        question: String,
        history: List<ChatMessage>,       // full message list from ViewModel
        fields: List<Field>,
        weather: WeatherSnapshot?,
        recentScans: List<ScanRecord> = emptyList(),
        forceProvider: String? = null,    // "gemini" | "groq" | "github" | null = auto
    ): AiReply = withContext(Dispatchers.IO) {

        val conn = (URL(PROXY_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            // Val.town proxy checks Origin against its allowlist — Android native
            // requests have no origin, so we set it explicitly.
            setRequestProperty("Origin", "https://agritech-4d1ba.web.app")
            doOutput        = true
            connectTimeout  = 15_000
            readTimeout     = 45_000
        }

        val body = buildJsonObject {

            put("question", question)

            // Pin a provider when the user picked one (val skips the cascade)
            if (forceProvider != null) put("forceProvider", forceProvider)

            // History: skip starter greeting (index 0) and the latest user message
            // (already sent as `question`). Val takes last 8 internally.
            putJsonArray("history") {
                history.drop(1).dropLast(1).forEach { msg ->
                    addJsonObject {
                        put("role", if (msg.fromUser) "user" else "assistant")
                        put("text", msg.text)
                    }
                }
            }

            // Farm context — val's buildContextBlock() formats this for the model
            putJsonObject("farmContext") {
                putJsonArray("fields") {
                    fields.forEach { f ->
                        addJsonObject {
                            put("name", f.name)
                            put("cropType", f.crop)
                            put("areaAcres", f.areaHa * 2.47105)
                        }
                    }
                }
                if (weather != null) {
                    putJsonObject("weather") {
                        put("tempC", weather.tempC.toDouble())
                        put("rhPct", weather.humidityPct.toDouble())
                        put("city", weather.location)
                    }
                    putJsonObject("location") {
                        put("city", weather.location)
                    }
                }
                // Recent scan history — lets the AI say "your last scan found X"
                if (recentScans.isNotEmpty()) {
                    val fmt = SimpleDateFormat("MMM d", Locale.US)
                    putJsonArray("recentScans") {
                        recentScans.take(5).forEach { s ->
                            addJsonObject {
                                put("disease",    s.diseaseName)
                                put("crop",       s.cropType)
                                put("risk",       s.riskLevel)
                                put("confidence", s.confidence)
                                put("date",       fmt.format(Date(s.createdAtMillis)))
                            }
                        }
                    }
                }
            }
        }.toString()

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val raw = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            error("Proxy error $code: $err")
        }

        val root = Json.parseToJsonElement(raw).jsonObject
        val reply = root["reply"]?.jsonPrimitive?.content
            ?: error("No reply in proxy response: $raw")
        val provider = root["provider"]?.jsonPrimitive?.content ?: "ai"

        AiReply(text = reply, provider = provider)
    }
}
