package com.agrosphere.app.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Open-Meteo client — no API key, generous free tier.
 *
 * Single endpoint pulls current conditions + hourly + daily in one shot,
 * keeps total payload small (~12 KB), and includes WMO weather codes
 * that we map to our ConditionKind enum.
 */
object WeatherApi {
    private const val BASE = "https://api.open-meteo.com/v1/forecast"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetch(latitude: Double, longitude: Double): OpenMeteoResponse = withContext(Dispatchers.IO) {
        val url = URL(
            "$BASE?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,uv_index" +
                "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum,sunrise,sunset" +
                "&timezone=auto&forecast_days=7"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AgroSphere-Android/0.1")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Open-Meteo HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(OpenMeteoResponse.serializer(), body)
        } finally {
            conn.disconnect()
        }
    }
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
