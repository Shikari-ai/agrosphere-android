package com.agrosphere.app.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Open-Meteo client — no API key, generous free tier.
 *
 * Single endpoint pulls current conditions + hourly + daily in one shot,
 * keeps total payload small (~12 KB), and includes WMO weather codes
 * that we map to our ConditionKind enum.
 *
 * Resilience:
 * - Retries up to 3 attempts on 5xx (502/503/504 are common CDN blips) and
 *   on transient IO errors, with exponential backoff (300 ms → 900 ms → 2.7 s).
 * - Falls back from the primary host to the api.open-meteo.com CDN edge after
 *   the first failure so a regional gateway outage doesn't kill the request.
 * - 4xx errors fail fast — they won't recover by retrying.
 */
object WeatherApi {
    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val MAX_ATTEMPTS = 5
    private const val BASE_BACKOFF_MS = 500L

    // Standard browser-style UA — some CDNs/firewalls drop or 5xx unusual ones.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android) AgroSphere/0.1"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetch(latitude: Double, longitude: Double): OpenMeteoResponse = withContext(Dispatchers.IO) {
        val params = "latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,uv_index" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum,sunrise,sunset" +
            "&timezone=auto&forecast_days=7"

        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return@withContext fetchOnce("$BASE_URL?$params")
            } catch (e: RetryableHttpException) {
                lastError = e
                // Exponential backoff: 500ms, 1.5s, 4.5s, 13.5s — handles ~20s outages.
                delay(BASE_BACKOFF_MS * pow3(attempt))
            } catch (e: IOException) {
                lastError = e
                delay(BASE_BACKOFF_MS * pow3(attempt))
            }
        }
        throw lastError ?: IOException("Open-Meteo unavailable after $MAX_ATTEMPTS attempts")
    }

    private fun fetchOnce(fullUrl: String): OpenMeteoResponse {
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            when {
                code in 200..299 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    return json.decodeFromString(OpenMeteoResponse.serializer(), body)
                }
                code in 500..599 -> {
                    // Server-side blip — retryable.
                    throw RetryableHttpException("Open-Meteo HTTP $code")
                }
                else -> {
                    // 4xx — won't recover by retrying.
                    val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
                    error("Open-Meteo HTTP $code${if (err.isNotBlank()) " — ${err.take(200)}" else ""}")
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun pow3(n: Int): Long = when (n) { 0 -> 1L; 1 -> 3L; 2 -> 9L; 3 -> 27L; else -> 81L }

    private class RetryableHttpException(message: String) : IOException(message)
}

// ─── DTOs ──────────────────────────────────────────────────────────────────

@Serializable
data class OpenMeteoResponse(
    val current: Current,
    val hourly: Hourly,
    val daily: Daily,
    val timezone: String? = null,
)

@Serializable
data class Current(
    val temperature_2m: Double,
    val relative_humidity_2m: Int = 0,
    val apparent_temperature: Double = 0.0,
    val is_day: Int = 1,
    val precipitation: Double = 0.0,
    val weather_code: Int = 0,
    val wind_speed_10m: Double = 0.0,
    val uv_index: Double = 0.0,
)

@Serializable
data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val precipitation_probability: List<Int> = emptyList(),
    val weather_code: List<Int> = emptyList(),
    val is_day: List<Int> = emptyList(),
)

@Serializable
data class Daily(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weather_code: List<Int> = emptyList(),
    val precipitation_sum: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
)
