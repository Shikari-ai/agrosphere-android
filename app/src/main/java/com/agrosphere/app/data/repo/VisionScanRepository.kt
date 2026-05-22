package com.agrosphere.app.data.repo

import android.graphics.Bitmap
import android.util.Base64
import com.agrosphere.app.data.model.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

@Serializable
data class VisionTreatment(
    val type: String,   // chemical | organic | fertilizer | general
    val name: String,
    val usage: String,
)

@Serializable
data class VisionDiagnosis(
    val diseaseName: String,
    val scientificName: String,
    val riskLevel: String,   // healthy | low | medium | high
    val confidence: Int,     // 0..100
    val summary: String,
    val recommendations: List<String>,
    val plantType: String,
    val partOfPlant: String,
    val narrative: String,
    val treatments: List<VisionTreatment>,
)

/**
 * Scanner backend: sends a captured crop photo to the AgriTech vision proxy
 * (Val.town → Gemini 2.5 Flash multimodal) and returns a structured diagnosis.
 * Ports js/ai/vision-scan.js — same proxy URL, payload, prompt and parsing.
 *
 * Request : { question, farmContext?, image: { mimeType, data(base64) } }
 * Response: { reply: "<text containing a JSON diagnosis>", provider, model }
 */
object VisionScanRepository {

    private const val VISION_URL =
        "https://harshwardhanparganiha--2d3f804c4d5911f1b7baee650bb23af1.web.val.run"

    private const val MAX_DIM = 1280
    private const val JPEG_QUALITY = 90

    private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

    suspend fun analyze(
        bitmap: Bitmap,
        cropType: String,
        fields: List<Field>,
    ): VisionDiagnosis = withContext(Dispatchers.IO) {

        val base64 = encode(bitmap)

        val body = buildJsonObject {
            put("question", buildPrompt(cropType))
            if (fields.isNotEmpty()) {
                putJsonObject("farmContext") {
                    putJsonArray("fields") {
                        fields.take(6).forEach { f ->
                            addJsonObject {
                                put("name", f.name)
                                put("cropType", f.crop)
                            }
                        }
                    }
                }
            }
            putJsonObject("image") {
                put("mimeType", "image/jpeg")
                put("data", base64)
            }
        }.toString()

        val conn = (URL(VISION_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            // In case the vision val enforces the same CORS allowlist as the chat val.
            setRequestProperty("Origin", "https://agritech-4d1ba.web.app")
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 45_000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val raw = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            error("Vision error $code: $err")
        }

        val root = Json.parseToJsonElement(raw).jsonObject
        root["error"]?.let { error(it.toString()) }
        val reply = root["reply"]?.jsonPrimitive?.contentOrNull
            ?: error("No reply from vision proxy")

        val obj = extractJsonObject(reply) ?: error("Could not parse diagnosis")
        normalize(obj)
    }

    // ─── image → base64 ─────────────────────────────────────────────────────────

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

    // ─── reply parsing ──────────────────────────────────────────────────────────

    private fun extractJsonObject(text: String): JsonObject? {
        val first = text.indexOf('{')
        val last = text.lastIndexOf('}')
        if (first < 0 || last <= first) return null
        val candidate = text.substring(first, last + 1)
        return try {
            lenientJson.parseToJsonElement(candidate).jsonObject
        } catch (_: Exception) {
            try {
                val cleaned = candidate.replace(Regex(",\\s*([}\\]])"), "$1")
                lenientJson.parseToJsonElement(cleaned).jsonObject
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun normalize(o: JsonObject): VisionDiagnosis {
        fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

        val lvl = str("riskLevel").lowercase()
            .let { if (it in listOf("low", "medium", "high", "healthy")) it else "medium" }
        val conf = (o["confidence"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 60.0)
            .toInt().coerceIn(0, 100)

        val recs = o["recommendations"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(6) ?: emptyList()

        val treatments = o["treatments"]?.jsonArray?.mapNotNull { el ->
            val t = el.jsonObject
            val name = t["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (name.isEmpty()) null else VisionTreatment(
                type = t["type"]?.jsonPrimitive?.contentOrNull?.lowercase()?.trim() ?: "general",
                name = name,
                usage = t["usage"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            )
        }?.take(5) ?: emptyList()

        return VisionDiagnosis(
            diseaseName = str("diseaseName").ifEmpty { "Unknown" },
            scientificName = str("scientificName"),
            riskLevel = lvl,
            confidence = conf,
            summary = str("summary"),
            recommendations = recs,
            plantType = str("plantType"),
            partOfPlant = str("partOfPlant").lowercase(),
            narrative = str("narrative"),
            treatments = treatments,
        )
    }

    // ─── prompt (ported from vision-scan.js buildQuestion) ───────────────────────

    private fun buildPrompt(cropType: String): String {
        val contextLine = if (cropType.isNotBlank()) "\nContext — Crop type: $cropType" else ""
        return """
You are a senior plant pathologist and agronomist. Examine this crop image precisely.

STEP 1 — OBSERVE: Look carefully at leaf texture, color patterns, spots, lesions, margins, veins, stem, fruit surface, mold, insects, webbing, or any abnormality.
STEP 2 — DIAGNOSE: Identify the exact disease or condition. Distinguish from similar-looking issues. If healthy, say so clearly.
STEP 3 — PRESCRIBE: Name specific commercial products (fungicides, pesticides, fertilisers) with doses, not generic categories.$contextLine

Return ONLY a single valid JSON object — no prose, no markdown, no explanation:
{
  "diseaseName": "exact disease name or 'Healthy'",
  "scientificName": "pathogen or '' if none",
  "riskLevel": "healthy | low | medium | high",
  "confidence": <integer 0-100; use below 65 if image is unclear or ambiguous>,
  "summary": "2-3 sentences on visible symptoms and current severity",
  "recommendations": ["specific field action 1", "specific field action 2", "up to 5 total"],
  "plantType": "species + part, e.g. 'Tomato leaf', 'Wheat flag leaf', 'Mango fruit', or 'Unidentified plant'",
  "partOfPlant": "leaf | fruit | stem | whole plant | root | seed | ",
  "narrative": "Start with 'It looks like' — name the plant, describe what you see, give your verdict in 2-3 sentences",
  "treatments": [
    {"type": "chemical",    "name": "exact product e.g. Mancozeb 75% WP", "usage": "e.g. Mix 2.5 g/L, spray every 7-10 days"},
    {"type": "organic",     "name": "e.g. Neem Oil 1500 PPM",            "usage": "application method and timing"},
    {"type": "fertilizer",  "name": "e.g. NPK 19-19-19",                 "usage": "dose and schedule"},
    {"type": "general",     "name": "cultural or mechanical practice",   "usage": "when and how"}
  ]
}

Rules — ALWAYS give the farmer a clear solution:
- If riskLevel is NOT "healthy", you MUST fill "treatments" with at least 2-4 specific items: a chemical control (exact fungicide/pesticide name + dose, e.g. "Mancozeb 75% WP — 2.5 g/L"), an organic option (e.g. "Neem oil 1500 ppm — 5 ml/L"), a fertilizer/nutrient if a deficiency is involved, and one cultural practice. Use real product names and dosages, never vague categories.
- "recommendations" must contain 3-5 concrete, ordered field steps that actually fix or contain the problem (remove affected leaves, spray timing, irrigation/airflow changes, re-scout schedule).
- Leave "treatments" empty ONLY when the plant is genuinely healthy.
- Return JSON only.
""".trim()
    }
}
