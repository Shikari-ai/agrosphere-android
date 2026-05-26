package com.agrosphere.app.data.repo

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
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
 */
object PlantIdRepository {

    private const val VISION_URL =
        "https://harshwardhanparganiha--2d3f804c4d5911f1b7baee650bb23af1.web.val.run"

    private const val MAX_DIM = 1280
    private const val JPEG_QUALITY = 90

    private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

    suspend fun identify(bitmap: Bitmap): PlantIdentification = withContext(Dispatchers.IO) {

        val base64 = encode(bitmap)

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
            readTimeout    = 45_000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val raw = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            error("PlantId error $code: $err")
        }

        val root = Json.parseToJsonElement(raw).jsonObject
        root["error"]?.let { error(it.toString()) }
        val reply = root["reply"]?.jsonPrimitive?.contentOrNull
            ?: error("No reply from vision proxy")

        val obj = extractJsonObject(reply) ?: error("Could not parse identification")
        normalize(obj)
    }

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

    private fun extractJsonObject(text: String): JsonObject? {
        val first = text.indexOf('{')
        val last  = text.lastIndexOf('}')
        if (first < 0 || last <= first) return null
        val candidate = text.substring(first, last + 1)
        return try {
            lenientJson.parseToJsonElement(candidate).jsonObject
        } catch (_: Exception) {
            try {
                val cleaned = candidate.replace(Regex(",\\s*([}\\]])"), "$1")
                lenientJson.parseToJsonElement(cleaned).jsonObject
            } catch (_: Exception) { null }
        }
    }

    private fun normalize(o: JsonObject): PlantIdentification {
        fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        fun num(k: String, def: Int) = o[k]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: def

        val allowedCategories = setOf("Flowering","Indoor","Succulent","Herb","Tree","Climber")
        val cat = str("category").let { if (it in allowedCategories) it else "Indoor" }

        val allowedSunlight = setOf("Full Sun","Partial Shade","Indoors","Low Light")
        val sun = str("sunlightNeed").let { if (it in allowedSunlight) it else "Partial Shade" }

        return PlantIdentification(
            commonName            = str("commonName"),
            scientificName        = str("scientificName"),
            category              = cat,
            variety               = str("variety"),
            wateringIntervalDays  = num("wateringIntervalDays", 7).coerceIn(1, 30),
            sunlightNeed          = sun,
            soilType              = str("soilType").ifEmpty { "Well-drained" },
            careNote              = str("careNote"),
            confidence            = num("confidence", 0).coerceIn(0, 100),
            unidentifiable        = o["unidentifiable"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true
                                    || str("commonName").isEmpty(),
        )
    }

    private fun buildPrompt(): String = """
You are an expert botanist and horticulturist. Look carefully at this plant photo and identify the species precisely.

STEP 1 — OBSERVE: Examine leaves, stems, flowers, fruit, growth pattern, and any distinguishing features.
STEP 2 — IDENTIFY: Determine the common name, scientific binomial, and (if identifiable) the specific variety/cultivar.
STEP 3 — PROFILE: Provide accurate care information for this species — watering frequency, sunlight, soil, and a specific care tip.

Return ONLY a single valid JSON object — no prose, no markdown, no code fences:
{
  "commonName": "everyday name e.g. 'Money Plant', 'Rose', 'Snake Plant'",
  "scientificName": "binomial e.g. 'Epipremnum aureum', 'Rosa hybrida'",
  "category": "one of: Flowering | Indoor | Succulent | Herb | Tree | Climber",
  "variety": "specific variety/cultivar if visible (e.g. 'Golden Pothos'), else empty string",
  "wateringIntervalDays": <integer 1-30; how often to water in typical conditions>,
  "sunlightNeed": "one of: Full Sun | Partial Shade | Indoors | Low Light",
  "soilType": "specific e.g. 'Well-drained loam', 'Sandy, gritty', 'Rich, moist'",
  "careNote": "ONE actionable species-specific care tip in 1-2 sentences",
  "confidence": <integer 0-100; below 60 if image is unclear or ambiguous>,
  "unidentifiable": false
}

If you cannot identify the plant (blurry, no plant visible, unrecognisable):
{
  "commonName": "",
  "scientificName": "",
  "category": "Indoor",
  "variety": "",
  "wateringIntervalDays": 7,
  "sunlightNeed": "Partial Shade",
  "soilType": "Well-drained",
  "careNote": "Couldn't identify this plant — try a clearer photo showing the leaves and overall shape.",
  "confidence": 0,
  "unidentifiable": true
}

Return JSON only.
""".trim()
}
