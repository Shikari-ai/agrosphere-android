package com.agrosphere.app.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

data class PestScenario(
    val title: String,
    val likelihoodPct: Int,
    val detail: String,
)

data class PestSource(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String,   // "wikipedia" | "duckduckgo"
)

data class VerifiedPrediction(
    val verified: Boolean,
    val species: String,
    val confidencePct: Int,
    val summary: String,
    val agreements: List<String>,
    val disagreements: List<String>,
    val scenarios: List<PestScenario>,
    val sources: List<PestSource>,
    val region: String,
)

/**
 * Calls the AgriTech internet-verified pest prediction backend (Val.town).
 * Search-only pipeline (Wikipedia + DuckDuckGo) — no keys needed, CORS is open.
 *
 * Request : { pestHint, cropType, symptoms, district, state, lat, lng }
 * Response: { verified, species, confidencePct, summary, agreements[],
 *             disagreements[], scenarios[], sources[], region, ... }
 */
object PestVerifyRepository {

    private const val VERIFY_URL =
        "https://harshwardhanparganiha--14eb18304d5911f189cfee650bb23af1.web.val.run"

    suspend fun verify(
        pestHint: String,
        cropType: String,
        symptoms: String,
        district: String,
        state: String,
        lat: Double? = null,
        lng: Double? = null,
    ): VerifiedPrediction = withContext(Dispatchers.IO) {

        val conn = (URL(VERIFY_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 60_000   // billed multi-source pipeline; slow cold start
        }

        val body = buildJsonObject {
            put("pestHint", pestHint)
            put("cropType", cropType)
            put("symptoms", symptoms)
            put("district", district)
            put("state", state)
            if (lat != null) put("lat", lat) else put("lat", JsonNull)
            if (lng != null) put("lng", lng) else put("lng", JsonNull)
        }.toString()

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val raw = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            error("Verification error $code: $err")
        }

        val root = Json.parseToJsonElement(raw).jsonObject
        root["error"]?.let { error(it.jsonPrimitive.content) }

        fun strList(key: String): List<String> =
            root[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNullSafe() } ?: emptyList()

        val scenarios = root["scenarios"]?.jsonArray?.mapNotNull { el ->
            val o = el.jsonObject
            PestScenario(
                title = o["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                likelihoodPct = o["likelihoodPct"]?.jsonPrimitive?.intOrZero() ?: 0,
                detail = o["detail"]?.jsonPrimitive?.content ?: "",
            )
        } ?: emptyList()

        val sources = root["sources"]?.jsonArray?.mapNotNull { el ->
            val o = el.jsonObject
            val url = o["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            PestSource(
                title = o["title"]?.jsonPrimitive?.content ?: url,
                url = url,
                snippet = o["snippet"]?.jsonPrimitive?.content ?: "",
                source = o["source"]?.jsonPrimitive?.content ?: "",
            )
        } ?: emptyList()

        VerifiedPrediction(
            verified = root["verified"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            species = root["species"]?.jsonPrimitive?.content ?: pestHint,
            confidencePct = root["confidencePct"]?.jsonPrimitive?.intOrZero() ?: 0,
            summary = root["summary"]?.jsonPrimitive?.content ?: "",
            agreements = strList("agreements"),
            disagreements = strList("disagreements"),
            scenarios = scenarios,
            sources = sources,
            region = root["region"]?.jsonPrimitive?.content ?: "",
        )
    }
}

// ─── tiny safe parsers ─────────────────────────────────────────────────────────

private fun kotlinx.serialization.json.JsonPrimitive.intOrZero(): Int =
    try { int } catch (_: Exception) { content.toDoubleOrNull()?.toInt() ?: 0 }

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
    content.takeIf { it.isNotBlank() }
