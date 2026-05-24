package com.agrosphere.app.feature.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.agrosphere.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.weather.LocationProvider
import com.agrosphere.app.data.weather.WeatherBundle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.HourSlot
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.data.model.WeatherInsight
import com.agrosphere.app.data.model.WeatherMetric
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// WeatherScreen — condition-aware backdrop + farming intelligence layout.
// Sections: location header, hero, chip row, hourly forecast with mini temp
// curve, farming intelligence, 7-day outlook, realtime metrics grid (2×2).
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherScreen(
    padding: PaddingValues,
    vm: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    // Ask for location once, on entry. If granted, re-fetch so we get device-local weather.
    val permissions = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    LaunchedEffect(Unit) {
        if (!LocationProvider.hasLocationPermission(context) && !permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }
    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) vm.refresh()
    }

    // Background brush follows the *current* condition, with a sensible
    // default while we're still loading or in an error state.
    val kind = (state as? WeatherUiState.Loaded)?.data?.snapshot?.kind ?: ConditionKind.PartlyCloudy

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrushFor(kind))) {
        ConditionOverlay(kind)

        when (val s = state) {
            is WeatherUiState.Loading -> CenteredLoader()
            is WeatherUiState.Error -> ErrorBlock(message = s.message, onRetry = vm::refresh)
            is WeatherUiState.Loaded -> LoadedContent(bundle = s.data, padding = padding)
        }
    }
}

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AgroPalette.Primary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.weather_loading), style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(radius = 20.dp, padding = 20.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Cloud, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.weather_unavailable), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
            }
        }
    }
}

@Composable
private fun LoadedContent(bundle: WeatherBundle, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LocationHeader(location = bundle.snapshot.location) }
        item { HeroBlock(snapshot = bundle.snapshot) }
        item { ChipRow(snapshot = bundle.snapshot) }

        item { SectionHeader(title = stringResource(R.string.weather_section_hourly)) }
        item { HourlyStrip(hours = bundle.hourly) }

        if (bundle.insights.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.weather_section_farming_intel)) }
            items(bundle.insights) { ins -> InsightCard(insight = ins) }
        }

        item { SectionHeader(title = stringResource(R.string.weather_section_7day)) }
        items(bundle.daily) { day -> DayRow(day = day) }

        item { SectionHeader(title = stringResource(R.string.weather_section_realtime)) }
        item { MetricsGrid(metrics = bundle.metrics) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dynamic background
// ─────────────────────────────────────────────────────────────────────────────
private fun backgroundBrushFor(kind: ConditionKind): Brush = when (kind) {
    ConditionKind.Clear -> Brush.verticalGradient(
        listOf(Color(0xFF0B1A14), Color(0xFF051912), AgroPalette.BgDeep)
    )
    ConditionKind.PartlyCloudy -> Brush.verticalGradient(
        listOf(Color(0xFF0A1820), Color(0xFF06120E), AgroPalette.BgDeep)
    )
    ConditionKind.Cloudy -> Brush.verticalGradient(
        listOf(Color(0xFF0E1620), Color(0xFF0B121A), AgroPalette.BgDeep)
    )
    ConditionKind.Rain -> Brush.verticalGradient(
        listOf(Color(0xFF051824), Color(0xFF07101A), AgroPalette.BgDeep)
    )
    ConditionKind.Storm -> Brush.verticalGradient(
        listOf(Color(0xFF1A0F26), Color(0xFF0A0814), AgroPalette.BgDeep)
    )
    ConditionKind.Night -> Brush.verticalGradient(
        listOf(Color(0xFF02060E), Color(0xFF030610), AgroPalette.BgDeep)
    )
}

/** A condition-specific atmospheric overlay (sun glow / drifting rain / storm pulse / stars). */
@Composable
private fun ConditionOverlay(kind: ConditionKind) {
    val tr = rememberInfiniteTransition(label = "overlay")
    val t by tr.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "t",
    )
    when (kind) {
        ConditionKind.Clear, ConditionKind.PartlyCloudy -> SunGlow()
        ConditionKind.Cloudy -> CloudWash()
        ConditionKind.Rain -> RainStreaks(t)
        ConditionKind.Storm -> StormFlash(t)
        ConditionKind.Night -> Starfield(t)
    }
}

@Composable
private fun SunGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Amber.copy(alpha = 0.25f),
                0.6f to AgroPalette.Amber.copy(alpha = 0.04f),
                1f to Color.Transparent,
                center = Offset(size.width * 0.85f, size.height * 0.1f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.85f, size.height * 0.1f),
        )
    }
}

@Composable
private fun CloudWash() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.InkMuted.copy(alpha = 0.12f),
                1f to Color.Transparent,
                center = Offset(size.width * 0.3f, size.height * 0.2f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.3f, size.height * 0.2f),
        )
    }
}

@Composable
private fun RainStreaks(t: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val streaks = 28
        for (i in 0 until streaks) {
            val seed = (i * 73 + 11) % 100 / 100f
            val x = w * (((i * 47) % 100) / 100f)
            val travel = h + 80f
            val y = ((t + seed) % 1f) * travel - 40f
            val len = 14f + (i % 4) * 4f
            drawLine(
                color = AgroPalette.Sky.copy(alpha = 0.45f),
                start = Offset(x, y),
                end = Offset(x + 2f, y + len),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StormFlash(t: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val pulse = sin(t * PI.toFloat() * 2f) * 0.5f + 0.5f
        val flash = if (((t * 6f) % 1f) > 0.92f) 0.18f else 0f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Iris.copy(alpha = 0.18f * (0.4f + pulse * 0.6f) + flash),
                1f to Color.Transparent,
                center = Offset(size.width * 0.5f, size.height * 0.15f),
                radius = size.width * 0.9f,
            ),
            radius = size.width * 0.9f,
            center = Offset(size.width * 0.5f, size.height * 0.15f),
        )
    }
}

@Composable
private fun Starfield(t: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 45) {
            val seed = (i * 137 + 7) % 1000 / 1000f
            val x = w * (((i * 53) % 100) / 100f)
            val y = h * 0.55f * (((i * 31) % 90) / 100f)
            val twinkle = 0.4f + 0.6f * (sin((t + seed) * 6.28f) * 0.5f + 0.5f)
            drawCircle(
                color = AgroPalette.Ink.copy(alpha = 0.15f + 0.35f * twinkle),
                radius = 0.8f + (i % 3) * 0.4f,
                center = Offset(x, y),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header — location chip with radar pulse
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LocationHeader(location: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ScreenTitle(eyebrow = stringResource(R.string.weather_forecast_eyebrow), title = stringResource(R.string.weather_forecast_title))
        RadarChip(label = location)
    }
}

@Composable
private fun RadarChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadarDot()
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.Sky, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, maxLines = 1)
    }
}

@Composable
private fun RadarDot() {
    val tr = rememberInfiniteTransition(label = "radar")
    val scale by tr.animateFloat(
        1f, 2.6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "scale",
    )
    val alpha by tr.animateFloat(
        0.55f, 0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "alpha",
    )
    Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((6 * scale).dp)
                .clip(CircleShape)
                .background(AgroPalette.Primary.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AgroPalette.Primary)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero — big temp + condition + sunrise/sunset
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroBlock(snapshot: WeatherSnapshot) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(heroBrushFor(snapshot.kind, snapshot.tempC), shape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, shape),
    ) {
        // Atmospheric overlay tied to the current condition.
        HeroAtmosphere(
            kind = snapshot.kind,
            windKph = snapshot.windKph,
            modifier = Modifier.matchParentSize().clip(shape),
        )

        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.weather_right_now),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = AgroPalette.Sky,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${snapshot.tempC}°",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 92.sp),
                        color = heroTempColor(snapshot.tempC),
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.weather_feels_like, snapshot.feelsLikeC),
                        style = MaterialTheme.typography.labelMedium,
                        color = AgroPalette.InkMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(snapshot.condition, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                }
                BigConditionIcon(kind = snapshot.kind)
            }
            Spacer(Modifier.height(18.dp))
            SunTrack(sunrise = snapshot.sunrise, sunset = snapshot.sunset)
        }
    }
}

/** Background gradient per condition, with warm/cool blend by temperature. */
private fun heroBrushFor(kind: ConditionKind, tempC: Int): Brush {
    val heat = ((tempC + 5) / 45f).coerceIn(0f, 1f)
    val warmth = androidx.compose.ui.graphics.lerp(
        Color(0x111E40AF),
        Color(0x22F59E0B),
        heat,
    )
    val base: List<Color> = when (kind) {
        ConditionKind.Clear -> listOf(Color(0xFF1F2937), Color(0xFF0F172A))
        ConditionKind.PartlyCloudy -> listOf(Color(0xFF1E293B), Color(0xFF0B1220))
        ConditionKind.Cloudy -> listOf(Color(0xFF1F2937), Color(0xFF111827))
        ConditionKind.Rain -> listOf(Color(0xFF0F2A3F), Color(0xFF06121C))
        ConditionKind.Storm -> listOf(Color(0xFF1A1335), Color(0xFF06030F))
        ConditionKind.Night -> listOf(Color(0xFF050B18), Color(0xFF02040A))
    }
    return Brush.linearGradient(listOf(base[0].overlay(warmth), base[1]))
}

private fun Color.overlay(other: Color): Color =
    androidx.compose.ui.graphics.lerp(this, other.copy(alpha = 1f), other.alpha)

private fun heroTempColor(tempC: Int): Color = when {
    tempC < 5 -> Color(0xFF60A5FA)
    tempC < 15 -> Color(0xFFA5B4FC)
    tempC < 25 -> AgroPalette.Ink
    tempC < 32 -> Color(0xFFFCD34D)
    tempC < 38 -> Color(0xFFFB923C)
    else -> Color(0xFFEF4444)
}

@Composable
private fun BigConditionIcon(kind: ConditionKind) {
    val (icon, tint) = iconAndTintFor(kind)
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    0f to tint.copy(alpha = 0.22f),
                    1f to Color.Transparent,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun SunTrack(sunrise: String, sunset: String) {
    val now = java.time.LocalTime.now()
    val sunriseT = parseHm(sunrise) ?: java.time.LocalTime.of(6, 0)
    val sunsetT = parseHm(sunset) ?: java.time.LocalTime.of(18, 0)

    // Daylight progress 0..1 — clamps to edges outside the day.
    val totalDayMins = java.time.Duration.between(sunriseT, sunsetT).toMinutes().coerceAtLeast(1)
    val sinceSunriseMins = java.time.Duration.between(sunriseT, now).toMinutes().coerceIn(0, totalDayMins)
    val daylightProgress = sinceSunriseMins / totalDayMins.toFloat()
    val isDaytime = !now.isBefore(sunriseT) && !now.isAfter(sunsetT)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WbTwilight, null, tint = AgroPalette.Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.weather_sunrise, sunrise), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.weather_sunset, sunset), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.NightlightRound, null, tint = AgroPalette.Iris, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        // Curved sun-path: half-circle arc from sunrise to sunset with a glowing
        // sun marker pinned at the current daylight fraction.
        val pulse by rememberInfiniteTransition(label = "sun").animateFloat(
            0.65f, 1f,
            infiniteRepeatable(tween(2200), RepeatMode.Reverse),
            label = "p",
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            val w = size.width
            val h = size.height
            val padX = 8f
            val baselineY = h - 4f
            val arcWidth = w - padX * 2
            val arcHeight = h - 6f

            // The dashed half-arc itself
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(padX, baselineY)
                quadraticBezierTo(
                    x1 = w / 2f, y1 = -arcHeight * 0.8f,
                    x2 = padX + arcWidth, y2 = baselineY,
                )
            }
            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    listOf(AgroPalette.Amber, AgroPalette.Sky, AgroPalette.Iris)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                ),
            )

            if (isDaytime) {
                // Sample the bezier at daylightProgress for the sun marker position.
                val t = daylightProgress.coerceIn(0f, 1f)
                val x = (1 - t) * (1 - t) * padX +
                    2 * (1 - t) * t * (w / 2f) +
                    t * t * (padX + arcWidth)
                val y = (1 - t) * (1 - t) * baselineY +
                    2 * (1 - t) * t * (-arcHeight * 0.8f) +
                    t * t * baselineY

                val sunColor = androidx.compose.ui.graphics.lerp(AgroPalette.Amber, AgroPalette.Orange, t)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to sunColor.copy(alpha = 0.50f * pulse),
                        1f to Color.Transparent,
                    ),
                    radius = 18f * pulse,
                    center = Offset(x, y),
                )
                drawCircle(color = sunColor, radius = 5f, center = Offset(x, y))
            }
        }
    }
}

/** Parse "HH:mm" or "HH:mm:ss" — returns null on anything else. */
private fun parseHm(text: String): java.time.LocalTime? = try {
    java.time.LocalTime.parse(text.take(5))
} catch (_: Throwable) { null }

// ─────────────────────────────────────────────────────────────────────────────
// Chip row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChipRow(snapshot: WeatherSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SmallChip("${snapshot.humidityPct}%", stringResource(R.string.weather_humidity), Icons.Rounded.WaterDrop, AgroPalette.Sky, Modifier.weight(1f))
        SmallChip("${snapshot.windKph} km/h", stringResource(R.string.weather_wind), Icons.Rounded.Air, AgroPalette.Primary, Modifier.weight(1f))
        SmallChip("${snapshot.rainMm} mm", stringResource(R.string.weather_rain), Icons.Rounded.Cloud, AgroPalette.InkMuted, Modifier.weight(1f))
        UvChip(value = snapshot.uvIndex, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SmallChip(value: String, label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

/** UV chip uses a tiny gauge to show severity 0..11. */
@Composable
private fun UvChip(value: Int, modifier: Modifier = Modifier) {
    val severity = when {
        value <= 2 -> stringResource(R.string.weather_uv_low) to AgroPalette.Primary
        value <= 5 -> stringResource(R.string.weather_uv_moderate) to AgroPalette.Amber
        value <= 7 -> stringResource(R.string.weather_uv_high) to AgroPalette.Orange
        value <= 10 -> stringResource(R.string.weather_uv_very_high) to AgroPalette.Rose
        else -> stringResource(R.string.weather_uv_extreme) to AgroPalette.Iris
    }
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Canvas(modifier = Modifier.size(16.dp)) {
                val pct = (value.toFloat() / 11f).coerceIn(0f, 1f)
                drawArc(
                    color = AgroPalette.SurfaceGlassBorder,
                    startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = severity.second,
                    startAngle = 135f, sweepAngle = 270f * pct, useCenter = false,
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("$value", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.weather_uv_chip, severity.first), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hourly strip + mini temp curve
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HourlyStrip(hours: List<HourSlot>) {
    val minT = hours.minOf { it.tempC }
    val maxT = hours.maxOf { it.tempC }
    val range = max(1, maxT - minT)

    GlassCard(radius = 22.dp, padding = 16.dp) {
        Column {
            HourCurve(hours = hours, minT = minT, range = range)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(hours) { h -> HourCell(slot = h) }
            }
        }
    }
}

@Composable
private fun HourCurve(hours: List<HourSlot>, minT: Int, range: Int) {
    Canvas(modifier = Modifier.fillMaxWidth().height(68.dp)) {
        if (hours.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val padX = 14f
        val padY = 10f
        val usableW = w - padX * 2
        val usableH = h - padY * 2
        val step = usableW / (hours.size - 1)

        // 1) Filled area gradient under the curve.
        val fillPath = Path()
        hours.forEachIndexed { i, slot ->
            val nx = padX + step * i
            val ratio = (slot.tempC - minT).toFloat() / range
            val ny = padY + usableH - ratio * usableH
            if (i == 0) {
                fillPath.moveTo(nx, h - padY)
                fillPath.lineTo(nx, ny)
            } else {
                fillPath.lineTo(nx, ny)
            }
        }
        fillPath.lineTo(padX + usableW, h - padY)
        fillPath.close()
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                0f to AgroPalette.Amber.copy(alpha = 0.35f),
                0.6f to AgroPalette.Primary.copy(alpha = 0.18f),
                1f to Color.Transparent,
            ),
        )

        // 2) Stroke the curve on top.
        val linePath = Path()
        hours.forEachIndexed { i, slot ->
            val nx = padX + step * i
            val ratio = (slot.tempC - minT).toFloat() / range
            val ny = padY + usableH - ratio * usableH
            if (i == 0) linePath.moveTo(nx, ny) else linePath.lineTo(nx, ny)
        }
        drawPath(
            linePath,
            brush = Brush.horizontalGradient(listOf(AgroPalette.Amber, AgroPalette.Primary, AgroPalette.Sky)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )

        // 3) Per-point markers; the first ("Now") is bigger with a halo.
        hours.forEachIndexed { i, slot ->
            val nx = padX + step * i
            val ratio = (slot.tempC - minT).toFloat() / range
            val ny = padY + usableH - ratio * usableH
            if (i == 0) {
                drawCircle(color = AgroPalette.Primary.copy(alpha = 0.35f), radius = 8f, center = Offset(nx, ny))
                drawCircle(color = AgroPalette.Primary, radius = 4f, center = Offset(nx, ny))
            } else {
                drawCircle(color = AgroPalette.Ink, radius = 2.4f, center = Offset(nx, ny))
            }
        }
    }
}

@Composable
private fun HourCell(slot: HourSlot) {
    val (icon, tint) = iconAndTintFor(slot.kind)
    val isNow = slot.label == "Now"
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val cellModifier = Modifier
        .width(60.dp)
        .clip(shape)
        .then(
            if (isNow) Modifier
                .background(
                    Brush.verticalGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.20f),
                        1f to AgroPalette.Primary.copy(alpha = 0.05f),
                    )
                )
                .border(1.dp, AgroPalette.Primary.copy(alpha = 0.45f), shape)
            else Modifier
        )
        .padding(vertical = 10.dp, horizontal = 6.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = cellModifier,
    ) {
        Text(
            slot.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isNow) AgroPalette.Primary else AgroPalette.InkMuted,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "${slot.tempC}°",
            style = MaterialTheme.typography.titleSmall,
            color = AgroPalette.Ink,
            fontWeight = if (isNow) FontWeight.Black else FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (slot.rainProb > 0) "${slot.rainProb}%" else "—",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = AgroPalette.Sky,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insight cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InsightCard(insight: WeatherInsight) {
    val (icon, tint) = when (insight.verdict) {
        WeatherInsight.Verdict.Good -> Icons.Rounded.CheckCircle to AgroPalette.Primary
        WeatherInsight.Verdict.Caution -> Icons.Rounded.Warning to AgroPalette.Amber
        WeatherInsight.Verdict.Avoid -> Icons.Rounded.Bolt to AgroPalette.Rose
    }
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Spacer(Modifier.height(4.dp))
                Text(insight.body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7-day outlook with temp range bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayRow(day: WeatherDay) {
    val kind = when {
        "thunder" in day.condition.lowercase() -> ConditionKind.Storm
        "shower" in day.condition.lowercase() || day.rainMm > 0 -> ConditionKind.Rain
        "cloud" in day.condition.lowercase() -> ConditionKind.Cloudy
        else -> ConditionKind.Clear
    }
    val (icon, tint) = iconAndTintFor(kind)
    GlassCard(radius = 16.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.width(56.dp))
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            TempRangeBar(low = day.tempLowC, high = day.tempC, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${day.tempC}°/${day.tempLowC}°", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(
                    if (day.rainMm > 0) "${day.rainMm} mm" else stringResource(R.string.weather_dry),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.rainMm > 0) AgroPalette.Sky else AgroPalette.InkDim,
                )
            }
        }
    }
}

@Composable
private fun TempRangeBar(low: Int, high: Int, modifier: Modifier = Modifier) {
    val scaleMin = 18
    val scaleMax = 38
    val span = (scaleMax - scaleMin).toFloat()
    val startFrac = ((low - scaleMin).coerceAtLeast(0)) / span
    val endFrac = ((high - scaleMin).coerceAtMost(scaleMax - scaleMin)) / span
    Canvas(modifier = modifier.height(8.dp)) {
        val cornerR = 4f
        drawRoundRect(
            color = AgroPalette.SurfaceGlassBorder,
            topLeft = Offset(0f, size.height / 2 - cornerR),
            size = GeomSize(size.width, cornerR * 2),
            cornerRadius = CornerRadius(cornerR),
        )
        val x0 = size.width * startFrac
        val x1 = size.width * endFrac
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(AgroPalette.Sky, AgroPalette.Primary, AgroPalette.Amber)
            ),
            topLeft = Offset(x0, size.height / 2 - cornerR),
            size = GeomSize((x1 - x0).coerceAtLeast(6f), cornerR * 2),
            cornerRadius = CornerRadius(cornerR),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Realtime metrics grid (2×2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MetricsGrid(metrics: List<WeatherMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    MetricCard(metric = m, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(metric: WeatherMetric, modifier: Modifier = Modifier) {
    val inf    = rememberInfiniteTransition(label = "metric")
    val sweepX by inf.animateFloat(
        -0.35f, 1.35f,
        infiniteRepeatable(tween(3800, easing = LinearEasing)),
        label = "sx",
    )
    val liveA  by inf.animateFloat(
        0.15f, 1f,
        infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Reverse),
        label = "la",
    )
    GlassCard(modifier = modifier, radius = 18.dp, padding = 0.dp) {
        Box {
            Canvas(modifier = Modifier.matchParentSize()) {
                val wx    = sweepX * size.width
                val bandW = size.width * 0.30f
                drawRect(
                    brush   = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            metric.tint.copy(alpha = 0.05f),
                            metric.tint.copy(alpha = 0.09f),
                            metric.tint.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        startX = wx - bandW, endX = wx + bandW,
                    ),
                    topLeft = Offset.Zero,
                    size    = size,
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(metric.label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.value,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                        color = metric.tint,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        metric.unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkMuted,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Canvas(modifier = Modifier.size(6.dp).padding(bottom = 4.dp)) {
                        drawCircle(color = metric.tint.copy(alpha = liveA), radius = size.minDimension / 2f)
                    }
                }
                Text(metric.sublabel, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
private fun iconAndTintFor(kind: ConditionKind): Pair<ImageVector, Color> = when (kind) {
    ConditionKind.Clear -> Icons.Rounded.WbSunny to AgroPalette.Amber
    ConditionKind.PartlyCloudy -> Icons.Rounded.WbSunny to AgroPalette.Amber
    ConditionKind.Cloudy -> Icons.Rounded.Cloud to AgroPalette.InkMuted
    ConditionKind.Rain -> Icons.Rounded.WaterDrop to AgroPalette.Sky
    ConditionKind.Storm -> Icons.Rounded.Thunderstorm to AgroPalette.Iris
    ConditionKind.Night -> Icons.Rounded.NightlightRound to AgroPalette.Iris
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero atmosphere — animated overlay drawn inside the hero card.
// (Same pattern as the Home weather card; duplicated here so the two screens
// can evolve independently.)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroAtmosphere(kind: ConditionKind, windKph: Int, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "hero-fx")
    val t by tr.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4_000, easing = LinearEasing)),
        label = "t",
    )
    val windScalar = (windKph / 15f).coerceIn(0.10f, 2.5f)

    Canvas(modifier = modifier) {
        when (kind) {
            ConditionKind.Clear -> heroSunHalo()
            ConditionKind.PartlyCloudy -> { heroSunHalo(); heroClouds(t, windScalar) }
            ConditionKind.Cloudy -> heroClouds(t, windScalar)
            ConditionKind.Rain -> { heroCloudWash(); heroRain(t) }
            ConditionKind.Storm -> { heroStorm(t); heroRain(t) }
            ConditionKind.Night -> heroStars(t)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroSunHalo() {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFCD34D).copy(alpha = 0.35f),
            0.55f to Color(0xFFF59E0B).copy(alpha = 0.08f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.85f, size.height * 0.20f),
            radius = size.width * 0.7f,
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.85f, size.height * 0.20f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroCloudWash() {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFCBD5E1).copy(alpha = 0.15f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.25f, size.height * 0.35f),
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = Offset(size.width * 0.25f, size.height * 0.35f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroClouds(t: Float, windScalar: Float) {
    val w = size.width
    val h = size.height
    val puffs = listOf(
        Triple( 0.00f,  0.05f, 1.00f),
        Triple(-0.55f,  0.15f, 0.70f),
        Triple( 0.55f,  0.15f, 0.70f),
        Triple(-0.30f, -0.25f, 0.80f),
        Triple( 0.30f, -0.30f, 0.75f),
        Triple( 0.00f, -0.45f, 0.55f),
    )
    fun puff(cx: Float, cy: Float, scale: Float, alpha: Float) {
        if (alpha < 0.01f) return
        puffs.forEach { (dx, dy, sf) ->
            val r = scale * sf
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFE5E7EB).copy(alpha = alpha),
                    0.55f to Color(0xFFE5E7EB).copy(alpha = alpha * 0.5f),
                    1f to Color.Transparent,
                    center = Offset(cx + dx * scale, cy + dy * scale),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx + dx * scale, cy + dy * scale),
            )
        }
    }
    val baseScale = h * 0.22f
    listOf(
        Triple(0.30f, 0.40f, Pair(0.20f, 0.00f)) to Triple(baseScale,        0.22f,  1f),
        Triple(0.70f, 0.25f, Pair(0.16f, 0.42f)) to Triple(baseScale * 0.80f, 0.18f, -1f),
        Triple(0.50f, 0.55f, Pair(0.22f, 0.78f)) to Triple(baseScale * 0.65f, 0.14f,  1f),
    ).forEach { (a, b) ->
        val (ax, ay, drift) = a
        val (scale, maxAlpha, dir) = b
        val periodFactor = 0.16f
        val angle = ((t * periodFactor * windScalar + drift.second) % 1f) * 2f * PI.toFloat()
        val cx = w * ax + sin(angle) * w * drift.first * dir
        val cy = h * ay + sin(angle * 1.7f + 0.6f) * h * 0.04f
        val fade = sin(angle).let { it * it }
        val alpha = maxAlpha * fade
        val s = scale * (0.92f + 0.16f * sin(angle * 0.5f + 1.2f))
        puff(cx, cy, s, alpha)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroRain(t: Float) {
    val w = size.width
    val h = size.height
    val count = 42
    for (i in 0 until count) {
        val seed = (i * 73 + 11) % 100 / 100f
        val x = w * (((i * 47) % 100) / 100f)
        val travel = h + 60f
        val y = ((t + seed) % 1f) * travel - 30f
        val len = 14f + (i % 4) * 4f
        drawLine(
            color = Color(0xFF93C5FD).copy(alpha = 0.55f),
            start = Offset(x, y),
            end = Offset(x + 1.5f, y + len),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroStorm(t: Float) {
    val pulse = sin(t * PI.toFloat() * 2f) * 0.5f + 0.5f
    val flash = if (((t * 8f) % 1f) > 0.92f) 0.28f else 0f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFA78BFA).copy(alpha = 0.22f * (0.4f + pulse * 0.6f) + flash),
            1f to Color.Transparent,
            center = Offset(size.width * 0.5f, size.height * 0.20f),
            radius = size.width * 0.9f,
        ),
        radius = size.width * 0.9f,
        center = Offset(size.width * 0.5f, size.height * 0.20f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.heroStars(t: Float) {
    val w = size.width
    val h = size.height
    repeat(34) { i ->
        val seed = (i * 137 + 7) % 1000 / 1000f
        val sx = w * (((i * 53) % 100) / 100f)
        val sy = h * (((i * 31) % 95) / 100f)
        val twinkle = 0.45f + 0.55f * (sin(t * 1.6f + seed * 6.28f) * 0.5f + 0.5f)
        drawCircle(
            color = Color(0xFFF8FAFC).copy(alpha = 0.10f + 0.35f * twinkle),
            radius = 1.0f + (i % 3) * 0.4f,
            center = Offset(sx, sy),
        )
    }
}
