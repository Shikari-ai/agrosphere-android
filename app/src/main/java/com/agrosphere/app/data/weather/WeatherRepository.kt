package com.agrosphere.app.data.weather

import android.content.Context
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.HourSlot
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.data.model.WeatherInsight
import com.agrosphere.app.data.model.WeatherMetric
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Everything the WeatherScreen needs in one immutable bundle. */
data class WeatherBundle(
    val snapshot: WeatherSnapshot,
    val hourly: List<HourSlot>,
    val daily: List<WeatherDay>,
    val insights: List<WeatherInsight>,
    val metrics: List<WeatherMetric>,
)

object WeatherRepository {

    /** Latest weather bundle as a shared flow so every screen stays in sync. */
    private val _bundle = MutableStateFlow<WeatherBundle?>(null)
    val bundleFlow: StateFlow<WeatherBundle?> = _bundle.asStateFlow()
    fun cached(): WeatherBundle? = _bundle.value

    /**
     * Fast load — uses lastLocation (cached on device, no GPS wait) and runs
     * the reverse-geocode in parallel with the Open-Meteo fetch. Cold path is
     * one network round-trip (~1s). Warm path (cached) is ~0s — see [cached].
     */
    suspend fun load(context: Context): WeatherBundle = coroutineScope {
        // Prefer a fresh GPS fix for accuracy; fall back to last-known if it's slow,
        // so Home doesn't get stuck on a stale fix from a previous location.
        val place = withTimeoutOrNull(8_000) { LocationProvider.current(context) }
            ?: LocationProvider.fastCurrent(context)
        // Kick off both in parallel — total time = max(both), not sum.
        val weatherDeferred = async { WeatherApi.fetch(place.latitude, place.longitude) }
        val labelDeferred = async {
            // current() already geocodes; only re-geocode the fast/last-known fallback.
            if (place.label.isNotBlank() && place.label != "Current location") place.label
            else LocationProvider.reverseGeocode(context, place.latitude, place.longitude) ?: place.label
        }
        val r = weatherDeferred.await()
        val label = labelDeferred.await()
        val bundle = WeatherBundle(
            snapshot = toSnapshot(label, r),
            hourly = toHourly(r),
            daily = toDaily(r),
            insights = deriveInsights(r),
            metrics = deriveMetrics(r),
        )
        _bundle.value = bundle
        bundle
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private fun toSnapshot(location: String, r: OpenMeteoResponse): WeatherSnapshot {
        val c = r.current
        val kind = wmoToKind(c.weather_code, c.is_day == 1)
        val sunrise = r.daily.sunrise.firstOrNull()?.let { parseTime(it) } ?: "—"
        val sunset = r.daily.sunset.firstOrNull()?.let { parseTime(it) } ?: "—"
        return WeatherSnapshot(
            location = location,
            tempC = c.temperature_2m.roundToInt(),
            feelsLikeC = c.apparent_temperature.roundToInt(),
            condition = describeWmo(c.weather_code),
            kind = kind,
            humidityPct = c.relative_humidity_2m,
            windKph = c.wind_speed_10m.roundToInt(),
            rainMm = c.precipitation.roundToInt(),
            uvIndex = c.uv_index.roundToInt(),
            sunrise = sunrise,
            sunset = sunset,
        )
    }

    private fun toHourly(r: OpenMeteoResponse): List<HourSlot> {
        val nowIdx = nowIndex(r.hourly.time)
        val end = (nowIdx + 12).coerceAtMost(r.hourly.time.size)
        return (nowIdx until end).mapIndexed { offset, i ->
            HourSlot(
                label = if (offset == 0) "Now" else hourLabel(r.hourly.time[i]),
                tempC = r.hourly.temperature_2m[i].roundToInt(),
                rainProb = r.hourly.precipitation_probability.getOrNull(i) ?: 0,
                kind = wmoToKind(
                    r.hourly.weather_code.getOrNull(i) ?: 0,
                    (r.hourly.is_day.getOrNull(i) ?: 1) == 1,
                ),
            )
        }
    }

    private fun toDaily(r: OpenMeteoResponse): List<WeatherDay> {
        return r.daily.time.mapIndexed { i, dateStr ->
            WeatherDay(
                label = dayLabel(dateStr, isToday = i == 0),
                tempC = r.daily.temperature_2m_max[i].roundToInt(),
                tempLowC = r.daily.temperature_2m_min[i].roundToInt(),
                condition = describeWmo(r.daily.weather_code.getOrNull(i) ?: 0),
                rainMm = (r.daily.precipitation_sum.getOrNull(i) ?: 0.0).roundToInt(),
                humidityPct = 0, // not provided in daily payload
            )
        }
    }

    // ─── Derivations: insights + metrics ──────────────────────────────────

    private fun deriveInsights(r: OpenMeteoResponse): List<WeatherInsight> {
        val out = mutableListOf<WeatherInsight>()
        val c = r.current
        val nowIdx = nowIndex(r.hourly.time)
        val next6 = (nowIdx until (nowIdx + 6).coerceAtMost(r.hourly.time.size))

        // Spray window — wind + humidity
        val wind = c.wind_speed_10m
        val hum = c.relative_humidity_2m
        when {
            wind > 18 -> out += WeatherInsight(
                title = "Spray window — avoid",
                body = "Wind at ${wind.roundToInt()} km/h is above the safe 15 km/h ceiling for foliar sprays.",
                verdict = WeatherInsight.Verdict.Avoid,
            )
            wind in 12.0..18.0 || hum > 85 -> out += WeatherInsight(
                title = "Spray window — caution",
                body = "Wind ${wind.roundToInt()} km/h, humidity $hum%. Drift + slow drying — better in the next early morning.",
                verdict = WeatherInsight.Verdict.Caution,
            )
            else -> out += WeatherInsight(
                title = "Spray window — open",
                body = "Wind ${wind.roundToInt()} km/h, humidity $hum%. Conditions favourable for foliar passes.",
                verdict = WeatherInsight.Verdict.Good,
            )
        }

        // Rain expected → skip irrigation
        val nextRain = next6.mapNotNull { r.hourly.precipitation_probability.getOrNull(it) }.maxOrNull() ?: 0
        if (nextRain >= 60) {
            out += WeatherInsight(
                title = "Irrigation — hold off",
                body = "Up to $nextRain% rain probability in the next 6 h. Save water and let the sky do the work.",
                verdict = WeatherInsight.Verdict.Good,
            )
        } else if (c.temperature_2m >= 33 && c.relative_humidity_2m < 45) {
            out += WeatherInsight(
                title = "Heat stress — irrigate early",
                body = "${c.temperature_2m.roundToInt()}°C with $hum% humidity. Schedule irrigation before 9 AM.",
                verdict = WeatherInsight.Verdict.Caution,
            )
        }

        // Storm warning for week
        val stormDay = r.daily.weather_code.withIndex().firstOrNull { it.value in setOf(95, 96, 99) }
        if (stormDay != null && stormDay.index >= 0) {
            val label = dayLabel(r.daily.time[stormDay.index], isToday = stormDay.index == 0)
            val mm = (r.daily.precipitation_sum.getOrNull(stormDay.index) ?: 0.0).roundToInt()
            out += WeatherInsight(
                title = "Storm watch — $label",
                body = "Thunderstorm forecast with up to ${mm} mm. Re-plan heavy machinery & finish any open spray passes before then.",
                verdict = WeatherInsight.Verdict.Avoid,
            )
        }

        return out
    }

    private fun deriveMetrics(r: OpenMeteoResponse): List<WeatherMetric> {
        val c = r.current
        val heatIndex = approximateHeatIndex(c.temperature_2m, c.relative_humidity_2m)
        return listOf(
            WeatherMetric(
                label = "Heat index",
                value = "${heatIndex.roundToInt()}°",
                unit = "feels like",
                sublabel = heatComfort(heatIndex),
                tint = AgroPalette.Orange,
            ),
            WeatherMetric(
                label = "Wind",
                value = "${c.wind_speed_10m.roundToInt()}",
                unit = "km/h",
                sublabel = "10 m elevation",
                tint = AgroPalette.Primary,
            ),
            WeatherMetric(
                label = "UV exposure",
                value = "${c.uv_index.roundToInt()}",
                unit = "index",
                sublabel = uvNote(c.uv_index.roundToInt()),
                tint = AgroPalette.Amber,
            ),
            WeatherMetric(
                label = "Soil moisture",
                value = soilMoistureEstimate(c.relative_humidity_2m, r.daily.precipitation_sum.firstOrNull() ?: 0.0).toString(),
                unit = "% est.",
                sublabel = "model — top 30 cm",
                tint = AgroPalette.Sky,
            ),
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /** Find the index in hourly.time that matches the current local hour. */
    private fun nowIndex(times: List<String>): Int {
        if (times.isEmpty()) return 0
        val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val idx = times.indexOfFirst {
            try { !LocalDateTime.parse(it, fmt).isBefore(now) } catch (_: Throwable) { false }
        }
        return if (idx >= 0) idx else 0
    }

    private fun parseTime(iso: String): String = try {
        LocalDateTime.parse(iso).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Throwable) { iso }

    private fun hourLabel(iso: String): String = try {
        LocalTime.parse(iso.substringAfter('T')).format(DateTimeFormatter.ofPattern("h a", Locale.US))
    } catch (_: Throwable) { iso }

    private fun dayLabel(isoDate: String, isToday: Boolean): String = try {
        if (isToday) "Today"
        else LocalDate.parse(isoDate).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } catch (_: Throwable) { isoDate }

    /** Maps WMO weather code → our ConditionKind. */
    private fun wmoToKind(code: Int, isDay: Boolean): ConditionKind {
        if (!isDay && code <= 3) return ConditionKind.Night
        return when (code) {
            0 -> ConditionKind.Clear
            1, 2 -> ConditionKind.PartlyCloudy
            3 -> ConditionKind.Cloudy
            45, 48 -> ConditionKind.Cloudy
            in 51..67 -> ConditionKind.Rain
            in 71..77 -> ConditionKind.Cloudy   // snow → grey backdrop for now
            in 80..82 -> ConditionKind.Rain
            in 85..86 -> ConditionKind.Cloudy
            95, 96, 99 -> ConditionKind.Storm
            else -> ConditionKind.Cloudy
        }
    }

    /** Human-readable label for a WMO weather code. */
    private fun describeWmo(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Heavy drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80 -> "Rain showers"
        81 -> "Heavy showers"
        82 -> "Violent showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Severe thunderstorm"
        else -> "—"
    }

    /** Rothfusz approximation — accurate enough for a farming-context surface. */
    private fun approximateHeatIndex(tempC: Double, humidity: Int): Double {
        val tF = tempC * 9.0 / 5.0 + 32.0
        val rh = humidity.toDouble()
        val hi = -42.379 +
            2.04901523 * tF +
            10.14333127 * rh +
            -0.22475541 * tF * rh +
            -0.00683783 * tF * tF +
            -0.05481717 * rh * rh +
            0.00122874 * tF * tF * rh +
            0.00085282 * tF * rh * rh +
            -0.00000199 * tF * tF * rh * rh
        val outC = (hi - 32) * 5.0 / 9.0
        // Below ~27°C the formula isn't valid; fall back to the air temp.
        return if (tempC < 27) tempC else outC
    }

    private fun heatComfort(heatIndexC: Double): String = when {
        heatIndexC < 27 -> "comfort: high"
        heatIndexC < 32 -> "comfort: moderate"
        heatIndexC < 41 -> "stress risk"
        else -> "danger zone"
    }

    private fun uvNote(uv: Int): String = when {
        uv <= 2 -> "minimal risk"
        uv <= 5 -> "moderate exposure"
        uv <= 7 -> "high — cover up"
        uv <= 10 -> "very high"
        else -> "extreme"
    }

    /** Very rough soil-moisture estimate from humidity + today's rain sum. */
    private fun soilMoistureEstimate(humidity: Int, rainMm: Double): Int {
        val base = 35 + humidity / 3
        val rainBoost = (rainMm * 1.4).roundToInt()
        return (base + rainBoost).coerceIn(20, 98)
    }
}
