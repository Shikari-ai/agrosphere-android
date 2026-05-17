package com.agrosphere.app.data.geocoding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * OpenStreetMap Nominatim geocoder — same ecosystem as our Esri/OSM tile
 * stack, no API key, no quota dashboards. Returns up to [limit] places
 * for a free-form query like "Nashik" or "Lake Plot, Nashik District".
 *
 * Nominatim's usage policy asks for a real User-Agent identifying the
 * client and a max rate of ~1 req/s — we surface a single result list per
 * keyboard submit, well inside that budget.
 */
object GeocodingApi {

    private const val BASE = "https://nominatim.openstreetmap.org/search"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun search(query: String, limit: Int = 6): List<NominatimPlace> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val url = URL("$BASE?q=$encoded&format=json&limit=$limit&addressdetails=0")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AgroSphere-Android/0.1 (github.com/Shikari-ai/agrosphere-android)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<NominatimPlace>>(body)
        } catch (_: Throwable) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }
}

@Serializable
data class NominatimPlace(
    val place_id: Long? = null,
    val display_name: String,
    val lat: String,
    val lon: String,
    val type: String? = null,
    val name: String? = null,
    val importance: Double? = null,
    val `class`: String? = null,
) {
    val latitude: Double get() = lat.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = lon.toDoubleOrNull() ?: 0.0

    /** Best short label — "name" if Nominatim gave us one, else first chunk of display_name. */
    val shortLabel: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: display_name.substringBefore(',').trim()
}
