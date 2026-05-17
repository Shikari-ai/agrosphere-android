package com.agrosphere.app.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agrosphere.app.data.model.AlertItem
import com.agrosphere.app.data.model.ConditionKind
import com.agrosphere.app.data.model.WeatherSnapshot
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// HomeScreen — the AgroSphere dashboard.
// Sections: header (greeting + avatar + bell), compact live weather, quick
// actions, field operations, recent alerts, crop health monitor with animated
// ring, pest prediction with animated radar, at-a-glance grid, my fields,
// insights. All over a time-of-day adaptive atmospheric backdrop.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    padding: PaddingValues,
    onOpenProfile: () -> Unit,
    onOpenField: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenFields: () -> Unit = {},
    onOpenCopilot: () -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.state.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackdrop(isDay = state.timeOfDay.isDay)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = padding.calculateBottomPadding()),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StickyHeader(
                    name = state.displayName,
                    photoUrl = state.photoUrl,
                    timeOfDay = state.timeOfDay,
                    systemHealthy = state.systemHealthy,
                    notifCount = state.notificationCount,
                    onAvatarTap = onOpenProfile,
                    onBellTap = { showNotifications = true },
                )
            }
            item {
                WeatherHeroCard(snapshot = state.weather, loading = state.weatherLoading, onTap = onOpenWeather)
            }
            item {
                QuickActionsRow(
                    onScan = onOpenScanner,
                    onAddField = onOpenFields,
                    onIrrigation = onOpenWeather,
                    onCopilot = onOpenCopilot,
                    onAssistant = onOpenAssistant,
                )
            }
            // Section order mirrors the web home (index.html):
            // Field operations → Recent alerts → Crop health → Pest prediction.
            // Every card stays mounted; each shows its own inline empty state
            // when there's no data yet.
            item { FieldOperationsCard(onOpenFields = onOpenFields, hasFields = state.fieldsCount > 0) }

            item { SectionHeader(title = "Recent alerts", trailing = if (state.alerts.isEmpty()) null else "See all") }
            if (state.alerts.isEmpty()) {
                item { AlertsEmptyCard(hasFields = state.fieldsCount > 0) }
            } else {
                items(state.alerts.take(3)) { alert -> AlertCard(alert) }
            }

            item {
                CropHealthCard(
                    score = state.cropHealth,
                    verdict = state.cropHealthVerdict,
                    hasFields = state.fieldsCount > 0,
                    onTap = onOpenScanner,
                )
            }
            item {
                PestPredictionCard(
                    riskLevel = state.pestRiskLevel,
                    blipRadiusFraction = state.pestRiskBlip,
                    hasFields = state.fieldsCount > 0,
                    onTap = onOpenScanner,
                )
            }

            item { SectionHeader(title = "At a glance") }
            item { AtAGlanceGrid(state = state) }

            item { SectionHeader(title = "My fields", trailing = "Manage all") }
            item { MyFieldsCarousel(onOpenField = onOpenField, onAddField = onOpenFields) }

            item { SectionHeader(title = "Insights") }
            item { InsightsCarousel(weather = state.weather) }

            item { Spacer(Modifier.height(8.dp)) }
        }

        if (showNotifications) {
            NotificationsSheet(
                alerts = state.alerts,
                onDismiss = { showNotifications = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Time-of-day adaptive atmospheric backdrop
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeBackdrop(isDay: Boolean) {
    val tr = rememberInfiniteTransition(label = "home-bg")
    val t by tr.animateFloat(
        0f, (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "t",
    )
    val gradient = if (isDay) {
        Brush.verticalGradient(
            0f to Color(0xFF0A1A14),
            0.55f to AgroPalette.BgFarm,
            1f to AgroPalette.BgDeep,
        )
    } else {
        Brush.verticalGradient(
            0f to Color(0xFF02060E),
            0.55f to Color(0xFF050C14),
            1f to AgroPalette.BgDeep,
        )
    }
    Box(modifier = Modifier.fillMaxSize().background(gradient)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Sun (day) or moon (night) glow in top-right
            val cx = w * 0.85f
            val cy = h * 0.12f
            val glow = if (isDay) AgroPalette.Amber else AgroPalette.Iris
            drawCircle(
                brush = Brush.radialGradient(
                    0f to glow.copy(alpha = 0.22f),
                    0.55f to glow.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = w * 0.7f,
                ),
                radius = w * 0.7f,
                center = Offset(cx, cy),
            )
            // Soft drifting orb bottom-left
            val ox = w * 0.15f + sin(t) * w * 0.15f
            val oy = h * 0.65f + cos(t * 0.6f) * h * 0.05f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.18f),
                    0.6f to AgroPalette.Primary.copy(alpha = 0.04f),
                    1f to Color.Transparent,
                    center = Offset(ox, oy),
                    radius = w * 0.7f,
                ),
                radius = w * 0.7f,
                center = Offset(ox, oy),
            )
            // Stars at night
            if (!isDay) {
                repeat(28) { i ->
                    val seed = (i * 137 + 7) % 1000 / 1000f
                    val sx = w * (((i * 53) % 100) / 100f)
                    val sy = h * 0.4f * (((i * 31) % 90) / 100f)
                    val twinkle = 0.4f + 0.6f * (sin(t * 1.5f + seed * 6.28f) * 0.5f + 0.5f)
                    drawCircle(
                        color = AgroPalette.Ink.copy(alpha = 0.12f + 0.3f * twinkle),
                        radius = 0.8f + (i % 3) * 0.5f,
                        center = Offset(sx, sy),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sticky header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StickyHeader(
    name: String,
    photoUrl: String?,
    timeOfDay: TimeOfDay,
    systemHealthy: Boolean,
    notifCount: Int,
    onAvatarTap: () -> Unit,
    onBellTap: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${timeOfDay.greeting},",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.width(6.dp))
                Text(timeOfDay.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(4.dp))
            SystemBadge(healthy = systemHealthy)
        }
        Spacer(Modifier.width(8.dp))
        NotificationBell(count = notifCount, onClick = onBellTap)
        Spacer(Modifier.width(10.dp))
        AvatarChip(photoUrl = photoUrl, name = name, onTap = onAvatarTap)
    }
}

@Composable
private fun SystemBadge(healthy: Boolean) {
    val tint = if (healthy) AgroPalette.Primary else AgroPalette.Amber
    val label = if (healthy) "All systems active" else "Degraded service"
    val tr = rememberInfiniteTransition(label = "pulse")
    val pulse by tr.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1400)), label = "p")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = pulse))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NotificationBell(count: Int, onClick: () -> Unit = {}) {
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AgroPalette.SurfaceGlass)
                .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Notifications, null, tint = AgroPalette.Ink, modifier = Modifier.size(20.dp))
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Rose)
                    .border(2.dp, AgroPalette.BgFarm, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AvatarChip(photoUrl: String?, name: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(AgroPalette.PrimaryDim)
            .border(2.dp, AgroPalette.Primary.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
        } else {
            Text(
                initialsOf(name),
                style = MaterialTheme.typography.labelLarge,
                color = AgroPalette.Primary,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }
    return when (parts.size) {
        0 -> "?"
        1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact live weather card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherHeroCard(snapshot: WeatherSnapshot?, loading: Boolean, onTap: () -> Unit) {
    val kind = snapshot?.kind ?: ConditionKind.Cloudy
    val temp = snapshot?.tempC ?: 0
    val (icon, tint) = if (snapshot != null) iconAndTintFor(snapshot.kind) else Icons.Rounded.Cloud to AgroPalette.InkMuted
    val tempColor = colorForTemp(temp, snapshot != null)

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(weatherCardBrush(kind, temp), shape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, shape)
            .clickable(onClick = onTap),
    ) {
        // Live atmospheric overlay tied to the current condition + wind speed.
        WeatherAtmosphere(
            kind = kind,
            windKph = snapshot?.windKph ?: 0,
            modifier = Modifier.matchParentSize().clip(shape),
        )

        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.Sky, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            snapshot?.location ?: if (loading) "Detecting location…" else "Location unavailable",
                            style = MaterialTheme.typography.labelMedium,
                            color = AgroPalette.InkMuted,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            snapshot?.let { "${it.tempC}°" } ?: "—",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                            color = tempColor,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            snapshot?.condition ?: if (loading) "Loading…" else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.InkMuted,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                // Pulsing condition icon halo
                val tr = rememberInfiniteTransition(label = "icon-pulse")
                val pulse by tr.animateFloat(
                    0.55f, 1f,
                    infiniteRepeatable(tween(2200), RepeatMode.Reverse),
                    label = "p",
                )
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                0f to tint.copy(alpha = 0.35f * pulse),
                                0.7f to tint.copy(alpha = 0.08f * pulse),
                                1f to Color.Transparent,
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(44.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WeatherMetric("Humidity", snapshot?.humidityPct?.let { "$it%" } ?: "—", Icons.Rounded.WaterDrop, AgroPalette.Sky)
                WeatherMetric("Wind", snapshot?.windKph?.let { "$it km/h" } ?: "—", Icons.Rounded.Air, AgroPalette.Primary)
                WeatherMetric("Rain", snapshot?.rainMm?.let { "$it mm" } ?: "—", Icons.Rounded.Cloud, AgroPalette.InkMuted)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WbSunny, null, tint = AgroPalette.Amber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sunrise ${snapshot?.sunrise ?: "—"}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View forecast", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Backgrounds per condition; rotated through a warm/cold lerp by temperature. */
private fun weatherCardBrush(kind: ConditionKind, tempC: Int): Brush {
    // 0..1 hotness — 0°C = cold, 40°C = scorching.
    val heat = ((tempC + 5) / 45f).coerceIn(0f, 1f)
    val warmTint = androidx.compose.ui.graphics.lerp(
        Color(0x111E40AF), // cool steel-blue tint
        Color(0x22F59E0B), // warm amber tint
        heat,
    )
    return when (kind) {
        ConditionKind.Clear -> Brush.linearGradient(
            listOf(Color(0xFF1F2937).blendOver(warmTint), Color(0xFF0F172A))
        )
        ConditionKind.PartlyCloudy -> Brush.linearGradient(
            listOf(Color(0xFF1E293B).blendOver(warmTint), Color(0xFF0B1220))
        )
        ConditionKind.Cloudy -> Brush.linearGradient(
            listOf(Color(0xFF1F2937), Color(0xFF111827))
        )
        ConditionKind.Rain -> Brush.linearGradient(
            listOf(Color(0xFF0F2A3F), Color(0xFF06121C))
        )
        ConditionKind.Storm -> Brush.linearGradient(
            listOf(Color(0xFF1A1335), Color(0xFF06030F))
        )
        ConditionKind.Night -> Brush.linearGradient(
            listOf(Color(0xFF050B18), Color(0xFF02040A))
        )
    }
}

private fun Color.blendOver(other: Color): Color =
    androidx.compose.ui.graphics.lerp(this, other.copy(alpha = 1f), other.alpha)

/** Temperature colours the temp number itself — cold = blue, hot = orange/red. */
private fun colorForTemp(tempC: Int, hasData: Boolean): Color {
    if (!hasData) return AgroPalette.Ink
    return when {
        tempC < 5 -> Color(0xFF60A5FA)   // icy blue
        tempC < 15 -> Color(0xFFA5B4FC)  // cool indigo
        tempC < 25 -> AgroPalette.Ink    // neutral comfort
        tempC < 32 -> Color(0xFFFCD34D)  // warm amber
        tempC < 38 -> Color(0xFFFB923C)  // hot orange
        else -> Color(0xFFEF4444)        // scorch red
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Atmospheric overlay — picks a Canvas animation by condition.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherAtmosphere(kind: ConditionKind, windKph: Int, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "weather-fx")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "t")

    // Wind-derived speed scalar — slower scalar = clouds linger longer.
    // 0 km/h ≈ 0.10x (almost still), 15 km/h ≈ 1x (baseline), 40+ km/h ≈ 2.5x (zippy).
    val windScalar = (windKph / 15f).coerceIn(0.10f, 2.5f)

    Canvas(modifier = modifier) {
        when (kind) {
            ConditionKind.Clear -> drawSunHalo()
            ConditionKind.PartlyCloudy -> { drawSunHalo(); drawDriftingClouds(t, windScalar) }
            ConditionKind.Cloudy -> drawDriftingClouds(t, windScalar)
            ConditionKind.Rain -> { drawCloudWash(); drawRainStreaks(t) }
            ConditionKind.Storm -> { drawStormPulse(t); drawRainStreaks(t) }
            ConditionKind.Night -> drawStarfield(t)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunHalo() {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFCD34D).copy(alpha = 0.30f),
            0.55f to Color(0xFFF59E0B).copy(alpha = 0.06f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.85f, size.height * 0.15f),
            radius = size.width * 0.7f,
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.85f, size.height * 0.15f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudWash() {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFCBD5E1).copy(alpha = 0.12f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.25f, size.height * 0.3f),
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = Offset(size.width * 0.25f, size.height * 0.3f),
    )
}

/**
 * Hovering puffy clouds. Each one is a cluster of soft radial circles that
 * fades into view, drifts gently along a sinusoidal path with vertical wobble,
 * then dissolves back out. No conveyor belt — every cloud has its own period,
 * direction, and lifecycle phase, so the card breathes naturally.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDriftingClouds(t: Float, windScalar: Float = 1f) {
    val w = size.width
    val h = size.height

    // Cumulus silhouette — offsets relative to cloud centre.
    val puffs = listOf(
        Triple( 0.00f,  0.05f, 1.00f),
        Triple(-0.55f,  0.15f, 0.70f),
        Triple( 0.55f,  0.15f, 0.70f),
        Triple(-0.30f, -0.25f, 0.80f),
        Triple( 0.30f, -0.30f, 0.75f),
        Triple( 0.00f, -0.45f, 0.55f),
        Triple(-0.85f,  0.20f, 0.45f),
        Triple( 0.85f,  0.25f, 0.50f),
    )

    fun puffCloud(cx: Float, cy: Float, scale: Float, alpha: Float) {
        if (alpha < 0.01f) return // entirely faded — skip the whole draw
        puffs.forEach { (dx, dy, sizeFraction) ->
            val radius = scale * sizeFraction
            val px = cx + dx * scale
            val py = cy + dy * scale
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFE5E7EB).copy(alpha = alpha),
                    0.55f to Color(0xFFE5E7EB).copy(alpha = alpha * 0.5f),
                    1f to Color.Transparent,
                    center = Offset(px, py),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(px, py),
            )
        }
    }

    // Each cloud has a unique base anchor + drift radius + period + phase.
    // localPhase ∈ 0..1 cycles independently per cloud.
    data class CloudSpec(
        val anchorX: Float, val anchorY: Float,
        val driftX: Float, val wobbleY: Float,
        val periodFactor: Float, val phaseOffset: Float,
        val baseScale: Float, val maxAlpha: Float,
        val driftDirection: Float,
    )

    val baseScale = h * 0.22f
    // Halve the base period factors so clouds linger longer; we re-scale by
    // the live wind so calm days look almost still and breezy days animate.
    val clouds = listOf(
        // (anchor, drift, period, phase, scale, alpha, dir)
        CloudSpec(0.30f, 0.40f, 0.20f, 0.06f, 0.22f, 0.00f, baseScale,        0.22f,  1f),
        CloudSpec(0.70f, 0.25f, 0.16f, 0.05f, 0.16f, 0.42f, baseScale * 0.80f, 0.18f, -1f),
        CloudSpec(0.50f, 0.55f, 0.22f, 0.04f, 0.12f, 0.78f, baseScale * 0.65f, 0.14f,  1f),
    )

    clouds.forEach { c ->
        // Each cloud's own clock — independent period gives organic feel.
        // Multiply by windScalar so 0 km/h = barely-moving, 40+ km/h = brisk.
        val localPhase = ((t * c.periodFactor * windScalar + c.phaseOffset) % 1f)
        val angle = localPhase * 2f * PI.toFloat()

        // Drift along a horizontal sine wave (direction signed), plus a small
        // vertical wobble at a different rate so the path doesn't loop visibly.
        val cx = w * c.anchorX + sin(angle) * w * c.driftX * c.driftDirection
        val cy = h * c.anchorY + sin(angle * 1.7f + 0.6f) * h * c.wobbleY

        // Alpha bell-curve over the cycle: invisible → visible → invisible.
        // sin(angle) maps to 0..1 alpha when squared.
        val fade = (sin(angle).let { it * it }) // 0..1 with bell shape
        val alpha = c.maxAlpha * fade

        // Subtle scale pulse so the cloud breathes while it hovers.
        val scale = c.baseScale * (0.92f + 0.16f * sin(angle * 0.5f + 1.2f))

        puffCloud(cx, cy, scale, alpha)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRainStreaks(t: Float) {
    val w = size.width
    val h = size.height
    val count = 36
    for (i in 0 until count) {
        val seed = (i * 73 + 11) % 100 / 100f
        val x = w * (((i * 47) % 100) / 100f)
        val travel = h + 60f
        val y = ((t + seed) % 1f) * travel - 30f
        val len = 12f + (i % 4) * 4f
        drawLine(
            color = Color(0xFF93C5FD).copy(alpha = 0.55f),
            start = Offset(x, y),
            end = Offset(x + 1.5f, y + len),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStormPulse(t: Float) {
    val pulse = sin(t * PI.toFloat() * 2f) * 0.5f + 0.5f
    val flash = if (((t * 8f) % 1f) > 0.92f) 0.25f else 0f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFA78BFA).copy(alpha = 0.20f * (0.4f + pulse * 0.6f) + flash),
            1f to Color.Transparent,
            center = Offset(size.width * 0.5f, size.height * 0.20f),
            radius = size.width * 0.9f,
        ),
        radius = size.width * 0.9f,
        center = Offset(size.width * 0.5f, size.height * 0.20f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarfield(t: Float) {
    val w = size.width
    val h = size.height
    repeat(28) { i ->
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

@Composable
private fun WeatherMetric(label: String, value: String, icon: ImageVector, tint: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

private fun iconAndTintFor(kind: ConditionKind): Pair<ImageVector, Color> = when (kind) {
    ConditionKind.Clear, ConditionKind.PartlyCloudy -> Icons.Rounded.WbSunny to AgroPalette.Amber
    ConditionKind.Cloudy -> Icons.Rounded.Cloud to AgroPalette.InkMuted
    ConditionKind.Rain -> Icons.Rounded.WaterDrop to AgroPalette.Sky
    ConditionKind.Storm -> Icons.Rounded.Bolt to AgroPalette.Iris
    ConditionKind.Night -> Icons.Rounded.NightlightRound to AgroPalette.Iris
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick actions row (horizontally scrollable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsRow(
    onScan: () -> Unit,
    onAddField: () -> Unit,
    onIrrigation: () -> Unit,
    onCopilot: () -> Unit,
    onAssistant: () -> Unit,
) {
    val actions = listOf(
        QuickActionData("Scan crop", Icons.Rounded.CameraAlt, AgroPalette.Primary, onScan),
        QuickActionData("Add field", Icons.Rounded.Grass, AgroPalette.Sky, onAddField),
        QuickActionData("Irrigation", Icons.Rounded.WaterDrop, AgroPalette.Sky, onIrrigation),
        QuickActionData("AI copilot", Icons.Rounded.Bolt, Color(0xFF67D4F0), onCopilot),
        QuickActionData("AI assistant", Icons.Rounded.AutoAwesome, AgroPalette.Iris, onAssistant),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(actions) { a -> QuickActionPill(a) }
    }
}

private data class QuickActionData(val label: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)

@Composable
private fun QuickActionPill(action: QuickActionData) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = action.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(action.tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(action.icon, null, tint = action.tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(action.label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Field operations — today's tasks
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FieldOperationsCard(onOpenFields: () -> Unit, hasFields: Boolean) {
    val fields by FieldRepository.fields.collectAsState()
    val ops = remember(fields) { deriveFieldOps(fields) }

    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onOpenFields) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Field operations", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Text("Fields", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(4.dp))
            if (!hasFields) {
                Text(
                    "Add a field to see today's prioritised operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            } else if (ops.isEmpty()) {
                Text(
                    "All clear — no priority actions queued right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            } else {
                Text("${ops.size} action${if (ops.size == 1) "" else "s"} suggested from your fields.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(12.dp))
                ops.forEach { op ->
                    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(op.tint))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(op.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                            Text(op.when_, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted)
                    }
                }
            }
        }
    }
}

private data class FieldOp(val title: String, val when_: String, val tint: Color)

/** Derives concrete operations from the actual fields the user has added. */
private fun deriveFieldOps(fields: List<com.agrosphere.app.data.model.Field>): List<FieldOp> {
    val out = mutableListOf<FieldOp>()
    // Lowest-moisture field needs water
    fields.minByOrNull { it.moisturePct }?.let { f ->
        if (f.moisturePct < 50) {
            out += FieldOp("Irrigate ${f.name}", "Soil at ${f.moisturePct}% — schedule a 20 mm pass", AgroPalette.Sky)
        }
    }
    // Flowering / booting stage → foliar feed window
    fields.firstOrNull { it.stage in setOf("Flowering", "Booting") }?.let { f ->
        out += FieldOp("Foliar feed ${f.name}", "${f.stage} stage — balanced NPK + potassium boost", AgroPalette.Primary)
    }
    // Lowest-health field → scout it
    fields.minByOrNull { it.healthScore }?.let { f ->
        if (f.healthScore < 65) {
            out += FieldOp("Scout ${f.name}", "Health ${f.healthScore} — open Scanner to check leaves", AgroPalette.Amber)
        }
    }
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Alert card + empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlertCard(alert: AlertItem) {
    GlassCard(radius = 18.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(alert.tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Bolt, null, tint = alert.tint) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(alert.subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}

@Composable
private fun AlertsEmptyCard(hasFields: Boolean) {
    GlassCard(radius = 18.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.SurfaceGlassBorder),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Bolt, null, tint = AgroPalette.InkMuted) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("No alerts right now", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(
                    if (hasFields) "Storm watches, heat stress, and dry-soil pings will appear here as they're detected."
                    else "Add a field — weather + field-level alerts will appear here.",
                    style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Crop Health Monitor — animated sweep gradient ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CropHealthCard(score: Int, verdict: String, hasFields: Boolean, onTap: () -> Unit) {
    val target = (score / 100f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
        label = "crop-progress",
    )
    val fields by FieldRepository.fields.collectAsState()
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crop health monitor", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                if (hasFields) Text("View all", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            if (!hasFields) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthRing(progress = 0f, score = 0, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("No data yet", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add a field and run a scan to start tracking health.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthRing(progress = progress, score = score, modifier = Modifier.size(98.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Overall crop health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(verdict, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Average across ${fields.size} field${if (fields.size == 1) "" else "s"}. Run a scan to refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Per-crop mini strip — derived from real field data
                val byCrop = remember(fields) {
                    fields.groupBy { it.crop }
                        .mapValues { (_, list) -> list.map { it.healthScore }.average().toInt() }
                        .toList()
                        .take(4)
                }
                if (byCrop.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        byCrop.forEach { (crop, value) ->
                            CropChip(name = crop, value = value, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRing(progress: Float, score: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9f
            val inset = stroke / 2f
            val arc = size.minDimension - stroke
            drawArc(
                color = AgroPalette.SurfaceGlassBorder,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(arc, arc),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Primary)
                ),
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(arc, arc),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp), color = AgroPalette.Ink, fontWeight = FontWeight.Black)
            Text("/ 100", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

@Composable
private fun CropChip(name: String, value: Int, modifier: Modifier = Modifier) {
    val tint = when {
        value >= 85 -> AgroPalette.Primary
        value >= 70 -> AgroPalette.Sky
        value >= 55 -> AgroPalette.Amber
        else -> AgroPalette.Rose
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(2.dp))
        Text("$value", style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pest Prediction — animated radar canvas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PestPredictionCard(riskLevel: String, blipRadiusFraction: Float, hasFields: Boolean, onTap: () -> Unit) {
    val (tint, desc) = when (riskLevel.lowercase()) {
        "low" -> AgroPalette.Primary to "Conditions unfavourable for pest activity. Continue routine scouting."
        "moderate" -> AgroPalette.Amber to "Watch for early signs in vulnerable crops. Re-scan in 3 days."
        "high" -> AgroPalette.Rose to "Pest pressure climbing. Inspect tomorrow; preventive spray ready."
        "severe" -> AgroPalette.Rose to "Severe pressure — preventive spray strongly advised."
        else -> AgroPalette.InkMuted to "Add a field to enable predictions based on weather + crop."
    }
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = AgroPalette.Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pest prediction", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                if (hasFields) Text("View details", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PestRadar(
                    blipRadiusFraction = if (hasFields) blipRadiusFraction else 0f,
                    modifier = Modifier.size(if (hasFields) 110.dp else 90.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Risk level", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (hasFields) riskLevel else "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = tint, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}

@Composable
private fun PestRadar(blipRadiusFraction: Float, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "radar")
    val angle by tr.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "angle",
    )
    val blipPulse by tr.animateFloat(
        0.6f, 1.4f,
        animationSpec = infiniteRepeatable(tween(1400)),
        label = "blip",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val cx = size.width / 2
            val cy = size.height / 2
            // 3 concentric rings
            listOf(0.4f, 0.7f, 1f).forEach { f ->
                drawCircle(
                    color = AgroPalette.Primary.copy(alpha = 0.25f),
                    radius = r * f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f),
                )
            }
            // crosshair
            drawLine(
                color = AgroPalette.Primary.copy(alpha = 0.15f),
                start = Offset(cx - r, cy), end = Offset(cx + r, cy),
                strokeWidth = 1f,
            )
            drawLine(
                color = AgroPalette.Primary.copy(alpha = 0.15f),
                start = Offset(cx, cy - r), end = Offset(cx, cy + r),
                strokeWidth = 1f,
            )
            // rotating sweep — a gradient triangle
            val rad = angle * PI.toFloat() / 180f
            val sweepLen = r * 0.95f
            val sx = cx + cos(rad) * sweepLen
            val sy = cy + sin(rad) * sweepLen
            drawLine(
                brush = Brush.linearGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.0f),
                    1f to AgroPalette.Primary.copy(alpha = 0.85f),
                    start = Offset(cx, cy), end = Offset(sx, sy),
                ),
                start = Offset(cx, cy), end = Offset(sx, sy),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
            // blip — pulsing dot at a fixed radius/angle
            val blipAngle = 35f * PI.toFloat() / 180f
            val blipR = r * blipRadiusFraction
            val bx = cx + cos(blipAngle) * blipR
            val by = cy + sin(blipAngle) * blipR
            drawCircle(color = AgroPalette.Amber.copy(alpha = 0.4f), radius = 6f * blipPulse, center = Offset(bx, by))
            drawCircle(color = AgroPalette.Amber, radius = 3f, center = Offset(bx, by))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// At a Glance grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AtAGlanceGrid(state: HomeUiState) {
    val items = listOf(
        GlanceItem("Fields", "${state.fieldsCount}", "Active", Icons.Rounded.Grass, AgroPalette.Primary),
        GlanceItem("Crops", "${state.cropsCount}", "Growing", Icons.Rounded.Eco, AgroPalette.Primary),
        GlanceItem("Irrigation", "92%", "Efficiency", Icons.Rounded.WaterDrop, AgroPalette.Sky),
        GlanceItem("Soil", "${state.avgMoisture}%", "Moisture", Icons.Rounded.WaterDrop, AgroPalette.Sky),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { GlanceCard(item = it, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private data class GlanceItem(val label: String, val value: String, val sub: String, val icon: ImageVector, val tint: Color)

@Composable
private fun GlanceCard(item: GlanceItem, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(item.label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Text(item.value, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
            Text(item.sub, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// My Fields carousel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MyFieldsCarousel(onOpenField: (String) -> Unit, onAddField: () -> Unit = {}) {
    val fields by FieldRepository.fields.collectAsState()
    if (fields.isEmpty()) {
        GlassCard(radius = 22.dp, padding = 18.dp, onClick = onAddField) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Primary) }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("No fields yet", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Text("Tap to add your first plot — takes 20 seconds.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary)
            }
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(fields) { field ->
            GlassCard(
                modifier = Modifier.width(220.dp),
                radius = 22.dp,
                padding = 16.dp,
                onClick = { onOpenField(field.id) },
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(field.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Grass, null, tint = field.accent) }
                    Spacer(Modifier.height(10.dp))
                    Text(field.name, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                    Text("${field.crop} · ${field.areaHa} ha", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(14.dp))
                    Text("Health ${field.healthScore}", style = MaterialTheme.typography.labelMedium, color = field.accent)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AgroPalette.SurfaceGlassBorder),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(field.healthScore / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(field.accent),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insights carousel — calm, narrative cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InsightsCarousel(weather: WeatherSnapshot? = null) {
    val fields by FieldRepository.fields.collectAsState()
    val insights = remember(fields, weather) { deriveInsights(fields, weather) }
    if (insights.isEmpty()) {
        GlassCard(radius = 20.dp, padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("No insights yet — add a field and AgroSphere will start surfacing context.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(insights) { (title, body, tint) ->
            GlassCard(modifier = Modifier.width(280.dp), radius = 20.dp, padding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.AutoAwesome, null, tint = tint, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Onboarding nudge — shown on Home when the user has no fields yet.
// Replaces the field-ops + alerts + crop-health + pest cards which are all
// meaningless without at least one field.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingNudge(onAddField: () -> Unit) {
    GlassCard(background = AgroBrushes.leafCard, radius = 24.dp, padding = 22.dp, onClick = onAddField) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Primary) }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Set up your farm to see live insights", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Field operations, crop health, and pest predictions kick in the moment you add your first field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NudgeStep("①", "Add a field", AgroPalette.Primary)
                NudgeStep("②", "Run a scan", AgroPalette.Sky)
                NudgeStep("③", "Get insights", AgroPalette.Iris)
            }
        }
    }
}

@Composable
private fun NudgeStep(num: String, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(num, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink)
    }
}

/** Insights derived from the user's real fields + the current weather snapshot. */
private fun deriveInsights(
    fields: List<com.agrosphere.app.data.model.Field>,
    weather: WeatherSnapshot?,
): List<Triple<String, String, Color>> {
    val out = mutableListOf<Triple<String, String, Color>>()
    if (weather != null) {
        when {
            weather.kind == ConditionKind.Storm ->
                out += Triple(
                    "Storm overhead",
                    "Avoid field operations until conditions settle. The Weather tab has the next clear window.",
                    AgroPalette.Iris,
                )
            weather.humidityPct >= 80 ->
                out += Triple(
                    "Ambient humidity high",
                    "${weather.humidityPct}% humidity — disease pressure climbs. Inspect lower canopy on susceptible crops.",
                    AgroPalette.Sky,
                )
            weather.tempC >= 33 ->
                out += Triple(
                    "Hot day ahead",
                    "${weather.tempC}°C reading. Shift labour windows to early morning; check soil moisture this evening.",
                    AgroPalette.Orange,
                )
            weather.windKph <= 8 && weather.kind != ConditionKind.Rain ->
                out += Triple(
                    "Calm spray window",
                    "Wind at ${weather.windKph} km/h — ideal for foliar passes if you have one queued.",
                    AgroPalette.Primary,
                )
        }
    }
    if (fields.isNotEmpty()) {
        val avgHealth = fields.map { it.healthScore }.average().toInt()
        val driest = fields.minByOrNull { it.moisturePct }
        if (driest != null && driest.moisturePct < fields.map { it.moisturePct }.average() - 10) {
            out += Triple(
                "Soil moisture trend",
                "${driest.name} is drying faster than your other plots — check the drip layout.",
                AgroPalette.Sky,
            )
        }
        if (avgHealth >= 85) {
            out += Triple(
                "Crops trending strong",
                "Average health $avgHealth across ${fields.size} field${if (fields.size == 1) "" else "s"}. Keep current cadence.",
                AgroPalette.Primary,
            )
        }
    }
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Notifications bottom sheet — opened from the header bell. Shows the same
// derived alerts list the dashboard sources, plus an empty state when nothing
// is firing right now.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSheet(
    alerts: List<AlertItem>,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AgroPalette.Surface,
        scrimColor = Color(0xAA000000),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Notifications, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (alerts.isEmpty()) "All quiet" else "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (alerts.isEmpty())
                            "Nothing actionable from weather or your fields right now."
                        else
                            "${alerts.size} signal${if (alerts.size == 1) "" else "s"} from weather + fields",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (alerts.isEmpty()) {
                GlassCard(radius = 18.dp, padding = 20.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Notifications, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Notifications appear here when storms approach, soil moisture drops, or UV climbs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                alerts.forEach { a ->
                    AlertCard(a)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
