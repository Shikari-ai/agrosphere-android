package com.agrosphere.app.feature.weather

import androidx.compose.animation.core.LinearEasing
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
            Text("Reading the sky…", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
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
                Text("Weather unavailable", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Retry", onClick = onRetry)
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

        item { SectionHeader(title = "Hourly forecast") }
        item { HourlyStrip(hours = bundle.hourly) }

        if (bundle.insights.isNotEmpty()) {
            item { SectionHeader(title = "Farming intelligence") }
            items(bundle.insights) { ins -> InsightCard(insight = ins) }
        }

        item { SectionHeader(title = "7-day outlook") }
        items(bundle.daily) { day -> DayRow(day = day) }

        item { SectionHeader(title = "Realtime metrics") }
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
        ScreenTitle(eyebrow = "Forecast", title = "Weather")
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
    GlassCard(radius = 26.dp, padding = 22.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RIGHT NOW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = AgroPalette.Sky,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${snapshot.tempC}°",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                            color = AgroPalette.Ink,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "feels ${snapshot.feelsLikeC}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = AgroPalette.InkMuted,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                    Text(snapshot.condition, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                }
                BigConditionIcon(kind = snapshot.kind)
            }
            Spacer(Modifier.height(16.dp))
            SunTrack(sunrise = snapshot.sunrise, sunset = snapshot.sunset)
        }
    }
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
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WbTwilight, null, tint = AgroPalette.Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sunrise $sunrise", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sunset $sunset", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.NightlightRound, null, tint = AgroPalette.Iris, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(AgroPalette.Amber, AgroPalette.Sky, AgroPalette.Iris)
                ),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChipRow(snapshot: WeatherSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SmallChip("${snapshot.humidityPct}%", "Humidity", Icons.Rounded.WaterDrop, AgroPalette.Sky, Modifier.weight(1f))
        SmallChip("${snapshot.windKph} km/h", "Wind", Icons.Rounded.Air, AgroPalette.Primary, Modifier.weight(1f))
        SmallChip("${snapshot.rainMm} mm", "Rain", Icons.Rounded.Cloud, AgroPalette.InkMuted, Modifier.weight(1f))
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
        value <= 2 -> "Low" to AgroPalette.Primary
        value <= 5 -> "Moderate" to AgroPalette.Amber
        value <= 7 -> "High" to AgroPalette.Orange
        value <= 10 -> "Very high" to AgroPalette.Rose
        else -> "Extreme" to AgroPalette.Iris
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
            Text("UV · ${severity.first}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
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
    Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
        if (hours.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val padX = 12f
        val padY = 8f
        val usableW = w - padX * 2
        val usableH = h - padY * 2
        val step = usableW / (hours.size - 1)

        val path = Path()
        hours.forEachIndexed { i, slot ->
            val nx = padX + step * i
            val ratio = (slot.tempC - minT).toFloat() / range
            val ny = padY + usableH - ratio * usableH
            if (i == 0) path.moveTo(nx, ny) else path.lineTo(nx, ny)
        }
        drawPath(
            path,
            brush = Brush.horizontalGradient(listOf(AgroPalette.Amber, AgroPalette.Primary, AgroPalette.Sky)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )
        hours.forEachIndexed { i, slot ->
            val nx = padX + step * i
            val ratio = (slot.tempC - minT).toFloat() / range
            val ny = padY + usableH - ratio * usableH
            drawCircle(color = AgroPalette.Ink, radius = 2.4f, center = Offset(nx, ny))
        }
    }
}

@Composable
private fun HourCell(slot: HourSlot) {
    val (icon, tint) = iconAndTintFor(slot.kind)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(54.dp),
    ) {
        Text(
            slot.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (slot.label == "Now") AgroPalette.Primary else AgroPalette.InkMuted,
            fontWeight = if (slot.label == "Now") FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text("${slot.tempC}°", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
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
                    if (day.rainMm > 0) "${day.rainMm} mm" else "dry",
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
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
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
            }
            Text(metric.sublabel, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
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
