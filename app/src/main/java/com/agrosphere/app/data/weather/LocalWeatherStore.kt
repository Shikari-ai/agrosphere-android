package com.agrosphere.app.data.weather

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.HourSlot
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.data.model.WeatherInsight
import com.agrosphere.app.data.model.WeatherMetric
import com.agrosphere.app.data.model.WeatherSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * On-device persistence for the last successful [WeatherBundle].
 *
 * Lets the UI render real data immediately on cold start — and stay populated
 * even when Open-Meteo's CDN is having a bad few minutes. Same pattern as
 * [com.agrosphere.app.data.repo.LocalPlantStore]: SharedPreferences + JSON.
 *
 * [WeatherMetric.tint] holds a Compose [Color] which isn't @Serializable, so
 * we round-trip through a mirror class storing it as a packed ARGB long.
 */
object LocalWeatherStore {

    private const val PREFS = "agro_weather_store"
    private const val KEY   = "bundle_v1"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): WeatherBundle? {
        val raw = prefs(context).getString(KEY, null) ?: return null
        return try {
            json.decodeFromString<PersistedBundle>(raw).toBundle()
        } catch (_: Exception) {
            null
        }
    }

    fun save(context: Context, bundle: WeatherBundle) {
        try {
            val persisted = bundle.toPersisted()
            prefs(context).edit().putString(KEY, json.encodeToString(persisted)).apply()
        } catch (_: Exception) { /* best-effort — disk full / serialisation glitch shouldn't break weather */ }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    // ─── Mirror models (with @Serializable-safe types) ───────────────────────

    @Serializable
    private data class PersistedBundle(
        val snapshot: PersistedSnapshot,
        val hourly: List<PersistedHour>,
        val daily: List<PersistedDay>,
        val insights: List<PersistedInsight>,
        val metrics: List<PersistedMetric>,
        val savedAtMs: Long = System.currentTimeMillis(),
    )

    @Serializable
    private data class PersistedSnapshot(
        val location: String,
        val tempC: Int, val feelsLikeC: Int,
        val condition: String, val kindName: String,
        val humidityPct: Int, val windKph: Int, val rainMm: Int, val uvIndex: Int,
        val sunrise: String, val sunset: String,
    )

    @Serializable
    private data class PersistedHour(val label: String, val tempC: Int, val rainProb: Int, val kindName: String)

    @Serializable
    private data class PersistedDay(
        val label: String, val tempC: Int, val tempLowC: Int,
        val condition: String, val rainMm: Int, val humidityPct: Int,
    )

    @Serializable
    private data class PersistedInsight(val title: String, val body: String, val verdictName: String)

    @Serializable
    private data class PersistedMetric(
        val label: String, val value: String, val unit: String,
        val sublabel: String, val tintArgb: Long,
    )

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private fun WeatherBundle.toPersisted() = PersistedBundle(
        snapshot = PersistedSnapshot(
            location = snapshot.location, tempC = snapshot.tempC, feelsLikeC = snapshot.feelsLikeC,
            condition = snapshot.condition, kindName = snapshot.kind.name,
            humidityPct = snapshot.humidityPct, windKph = snapshot.windKph,
            rainMm = snapshot.rainMm, uvIndex = snapshot.uvIndex,
            sunrise = snapshot.sunrise, sunset = snapshot.sunset,
        ),
        hourly = hourly.map { PersistedHour(it.label, it.tempC, it.rainProb, it.kind.name) },
        daily  = daily.map {
            PersistedDay(it.label, it.tempC, it.tempLowC, it.condition, it.rainMm, it.humidityPct)
        },
        insights = insights.map { PersistedInsight(it.title, it.body, it.verdict.name) },
        metrics  = metrics.map {
            PersistedMetric(it.label, it.value, it.unit, it.sublabel, it.tint.toArgb().toLong() and 0xFFFFFFFFL)
        },
    )

    private fun PersistedBundle.toBundle() = WeatherBundle(
        snapshot = WeatherSnapshot(
            location = snapshot.location, tempC = snapshot.tempC, feelsLikeC = snapshot.feelsLikeC,
            condition = snapshot.condition,
            kind = runCatching { ConditionKind.valueOf(snapshot.kindName) }.getOrDefault(ConditionKind.Clear),
            humidityPct = snapshot.humidityPct, windKph = snapshot.windKph,
            rainMm = snapshot.rainMm, uvIndex = snapshot.uvIndex,
            sunrise = snapshot.sunrise, sunset = snapshot.sunset,
        ),
        hourly = hourly.map {
            HourSlot(
                label = it.label, tempC = it.tempC, rainProb = it.rainProb,
                kind = runCatching { ConditionKind.valueOf(it.kindName) }.getOrDefault(ConditionKind.Clear),
            )
        },
        daily = daily.map { WeatherDay(it.label, it.tempC, it.tempLowC, it.condition, it.rainMm, it.humidityPct) },
        insights = insights.map {
            WeatherInsight(
                title = it.title, body = it.body,
                verdict = runCatching { WeatherInsight.Verdict.valueOf(it.verdictName) }.getOrDefault(WeatherInsight.Verdict.Good),
            )
        },
        metrics = metrics.map {
            WeatherMetric(
                label = it.label, value = it.value, unit = it.unit, sublabel = it.sublabel,
                tint = Color(it.tintArgb.toInt()),
            )
        },
    )
}
