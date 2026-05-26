package com.agrosphere.app.data.repo

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

@Serializable
data class PlantIdentification(
    val commonName: String,
    val scientificName: String,
    val category: String,            // "Flowering" | "Indoor" | "Succulent" | "Herb" | "Tree" | "Climber"
    val variety: String,
    val wateringIntervalDays: Int,   // 1..30
    val sunlightNeed: String,        // "Full Sun" | "Partial Shade" | "Indoors" | "Low Light"
    val soilType: String,
    val careNote: String,
    val confidence: Int,             // 0..100
    val unidentifiable: Boolean,
)

/**
 * Identifies a plant from a photo using the same Val.town → Gemini 2.5 Flash
 * proxy that powers the disease scanner. Returns a structured species profile
 * we can auto-fill into PlantRepository.addPlant().
 *
 * Resilience:
 * - 3 attempts with exponential backoff for transient 5xx / IOException.
 * - 2 extra attempts on parse failure (model occasionally wraps JSON in prose).
 * - Prompt removes the explicit "I can't identify" escape hatch — the model
 *   must always make a best guess; uncertainty is expressed via confidence.
 *   Empty commonName + scientificName means "no plant in frame" (rare).
 */
object PlantIdRepository {

    private const val TAG = "PlantId"

    private const val VISION_URL =
        "https://harshwardhanparganiha--2d3f804c4d5911f1b7baee650bb23af1.web.val.run"

    // Higher fidelity than the disease scanner — species identification needs leaf
    // detail and growth-pattern cues that get washed out at lower resolutions.
    private const val MAX_DIM = 1600
    private const val JPEG_QUALITY = 92

    private const val MAX_ATTEMPTS = 3
    private const val BASE_BACKOFF_MS = 500L

    private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

    suspend fun identify(bitmap: Bitmap): PlantIdentification = withContext(Dispatchers.IO) {
        val base64 = encode(bitmap)

        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return@withContext identifyOnce(base64)
            } catch (e: RetryableHttpException) {
                lastError = e
                Log.w(TAG, "Retryable error on attempt ${attempt + 1}: ${e.message}")
                delay(BASE_BACKOFF_MS * pow3(attempt))
            } catch (e: ParseFailure) {
                lastError = e
                Log.w(TAG, "Parse failure on attempt ${attempt + 1}: ${e.message}")
                // Quicker retry for parse failures — the model just needs another shot.
                delay(400L)
            } catch (e: IOException) {
                lastError = e
                Log.w(TAG, "IO error on attempt ${attempt + 1}: ${e.message}")
                delay(BASE_BACKOFF_MS * pow3(attempt))
            }
        }
        throw lastError ?: IOException("Plant identification unavailable")
    }

    private fun identifyOnce(base64: String): PlantIdentification {
        val body = buildJsonObject {
            put("question", buildPrompt())
            putJsonObject("image") {
                put("mimeType", "image/jpeg")
                put("data", base64)
            }
        }.toString()

        val conn = (URL(VISION_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Origin", "https://agritech-4d1ba.web.app")
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 60_000
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            when {
                code in 200..299 -> {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = Json.parseToJsonElement(raw).jsonObject
                    root["error"]?.let { throw RetryableHttpException("Proxy returned error: $it") }
                    val reply = root["reply"]?.jsonPrimitive?.contentOrNull
                        ?: throw ParseFailure("No reply from vision proxy")
                    val obj = extractJsonObject(reply)
                        ?: throw ParseFailure("Could not extract JSON from reply: ${reply.take(200)}")
                    return normalize(obj)
                }
                code in 500..599 -> throw RetryableHttpException("PlantId HTTP $code")
                else -> {
                    val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
                    error("PlantId HTTP $code${if (err.isNotBlank()) " — ${err.take(200)}" else ""}")
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    // ─── Image encoding ──────────────────────────────────────────────────────

    private fun encode(bitmap: Bitmap): String {
        val scaled = scaleToMax(bitmap, MAX_DIM)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleToMax(b: Bitmap, max: Int): Bitmap {
        val s = min(1f, max.toFloat() / maxOf(b.width, b.height))
        if (s >= 1f) return b
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }

    // ─── Response parsing ────────────────────────────────────────────────────

    private fun extractJsonObject(text: String): JsonObject? {
        // Strip Markdown code fences if Gemini wrapped its JSON in ```json ... ```
        val cleaned = text
            .replace(Regex("^\\s*```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*```\\s*"), "")
            .replace(Regex("\\s*```\\s*$"), "")
        val first = cleaned.indexOf('{')
        val last  = cleaned.lastIndexOf('}')
        if (first < 0 || last <= first) return null
        val candidate = cleaned.substring(first, last + 1)
        return try {
            lenientJson.parseToJsonElement(candidate).jsonObject
        } catch (_: Exception) {
            try {
                // Tolerate trailing commas that strict parsers reject.
                val tidied = candidate.replace(Regex(",\\s*([}\\]])"), "$1")
                lenientJson.parseToJsonElement(tidied).jsonObject
            } catch (_: Exception) { null }
        }
    }

    private fun normalize(o: JsonObject): PlantIdentification {
        fun str(k: String): String = o[k]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        fun num(k: String, def: Int): Int =
            o[k]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: def

        val common   = str("commonName")
        val sciName  = str("scientificName")

        val allowedCategories = setOf("Flowering", "Indoor", "Succulent", "Herb", "Tree", "Climber")
        // Best-effort category mapping — Gemini sometimes returns 'Houseplant', 'Foliage', etc.
        val rawCat = str("category")
        val cat = when {
            rawCat in allowedCategories -> rawCat
            rawCat.equals("Houseplant",  true) || rawCat.equals("Foliage", true) -> "Indoor"
            rawCat.equals("Vine",        true) || rawCat.equals("Trailing", true) -> "Climber"
            rawCat.equals("Cactus",      true) -> "Succulent"
            rawCat.equals("Shrub",       true) -> "Tree"
            else -> "Indoor"
        }

        val allowedSunlight = setOf("Full Sun", "Partial Shade", "Indoors", "Low Light")
        val rawSun = str("sunlightNeed")
        val sun = when {
            rawSun in allowedSunlight -> rawSun
            rawSun.contains("full",   true) && rawSun.contains("sun", true) -> "Full Sun"
            rawSun.contains("indirect", true) || rawSun.contains("bright", true) -> "Partial Shade"
            rawSun.contains("shade",  true) || rawSun.contains("partial", true) -> "Partial Shade"
            rawSun.contains("indoor", true) -> "Indoors"
            rawSun.contains("low",    true) -> "Low Light"
            else -> "Partial Shade"
        }

        // "unidentifiable" = no plant in frame at all. Any name OR a confidence > 20
        // counts as a real identification — the review screen surfaces low confidence
        // separately so the user knows the model is guessing.
        val confidence = num("confidence", 0).coerceIn(0, 100)
        val unidentifiable = common.isEmpty() && sciName.isEmpty()

        return PlantIdentification(
            commonName            = common,
            scientificName        = sciName,
            category              = cat,
            variety               = str("variety"),
            wateringIntervalDays  = num("wateringIntervalDays", 7).coerceIn(1, 30),
            sunlightNeed          = sun,
            soilType              = str("soilType").ifEmpty { "Well-drained" },
            careNote              = str("careNote"),
            confidence            = confidence,
            unidentifiable        = unidentifiable,
        )
    }

    // ─── Prompt — best-effort identification, never refuses ───────────────────

    private fun buildPrompt(): String = """
You are PlantID, an expert botanist that identifies plants from photos. You have years of experience with houseplants, garden flowers, succulents, herbs, vegetables, and ornamental species worldwide.

YOUR TASK
Look at this photo and identify the plant. ALWAYS make a best-guess identification — never refuse, never say "I can't tell". If the image is unclear, still pick the most likely species and reflect your uncertainty in the confidence field.

OUTPUT — return ONLY this JSON object. No prose, no markdown fences, no commentary before or after:
{
  "commonName": "everyday name, e.g. 'Money Plant', 'Rose', 'Snake Plant', 'Aloe Vera'",
  "scientificName": "binomial, e.g. 'Epipremnum aureum', 'Rosa hybrida'",
  "category": "EXACTLY one of: Flowering | Indoor | Succulent | Herb | Tree | Climber",
  "variety": "specific variety / cultivar if you can tell, else empty string",
  "wateringIntervalDays": <integer 1-30; typical watering frequency in days>,
  "sunlightNeed": "EXACTLY one of: Full Sun | Partial Shade | Indoors | Low Light",
  "soilType": "short soil description, e.g. 'Well-drained loam', 'Sandy gritty', 'Rich moist'",
  "careNote": "ONE actionable care tip in 1–2 sentences specific to this species",
  "confidence": <integer 0-100; reflect uncertainty here — use 40-60 if unsure, 70-95 if confident, 20-40 if guessing from a poor photo>
}

RULES — read carefully:
1. ALWAYS attempt identification. Never set commonName to empty just because you're unsure — drop confidence instead.
2. If the photo genuinely shows NO plant at all (a wall, an animal, a person, blank space), return all string fields as "" and confidence as 0. This is the only case where empty names are allowed.
3. The category and sunlightNeed values MUST match the exact strings listed — no variations.
4. JSON ONLY. No "Here is the plant…", no markdown fences, no trailing notes.

EXAMPLE (for a Money Plant photo):
{"commonName":"Money Plant","scientificName":"Epipremnum aureum","category":"Indoor","variety":"Golden Pothos","wateringIntervalDays":7,"sunlightNeed":"Low Light","soilType":"Well-drained, peat-based","careNote":"Let the top 2 cm of soil dry out between waterings; trim leggy vines to encourage bushy growth.","confidence":92}
""".trim()

    // ─── Exceptions + helpers ────────────────────────────────────────────────

    private fun pow3(n: Int): Long = when (n) { 0 -> 1L; 1 -> 3L; 2 -> 9L; else -> 27L }

    private class RetryableHttpException(message: String) : IOException(message)
    private class ParseFailure(message: String) : Exception(message)
}
