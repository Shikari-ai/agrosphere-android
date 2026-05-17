package com.agrosphere.app.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.state.collectAsState()

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
                    onCopilot = onOpenAssistant,
                    onAssistant = onOpenAssistant,
                )
            }
            item { FieldOperationsCard(onOpenFields = onOpenFields) }
            item { SectionHeader(title = "Recent alerts", trailing = "See all") }
            items(state.alerts.take(3)) { alert -> AlertCard(alert) }

            item {
                CropHealthCard(
                    score = state.cropHealth,
                    verdict = state.cropHealthVerdict,
                    onTap = onOpenScanner,
                )
            }
            item {
                PestPredictionCard(
                    riskLevel = state.pestRiskLevel,
                    blipRadiusFraction = state.pestRiskBlip,
                    onTap = onOpenScanner,
                )
            }

            item { SectionHeader(title = "At a glance") }
            item { AtAGlanceGrid(state = state) }

            item { SectionHeader(title = "My fields", trailing = "Manage all") }
            item { MyFieldsCarousel(onOpenField = onOpenField) }

            item { SectionHeader(title = "Insights") }
            item { InsightsCarousel() }

            item { Spacer(Modifier.height(8.dp)) }
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
        NotificationBell(count = notifCount)
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
private fun NotificationBell(count: Int) {
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AgroPalette.SurfaceGlass)
                .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape)
                .clickable { /* future: notification center */ },
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
    val (icon, tint) = if (snapshot != null) iconAndTintFor(snapshot.kind) else Icons.Rounded.Cloud to AgroPalette.InkMuted
    GlassCard(
        background = AgroBrushes.coolCard,
        radius = 24.dp,
        padding = 18.dp,
        onClick = onTap,
    ) {
        Column {
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
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                            color = AgroPalette.Ink,
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                0f to tint.copy(alpha = 0.25f),
                                1f to Color.Transparent,
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.height(12.dp))
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
private fun FieldOperationsCard(onOpenFields: () -> Unit) {
    val ops = listOf(
        FieldOp("Irrigate Riverside", "Today · 25 mm before 9 AM", AgroPalette.Sky),
        FieldOp("Foliar feed Lake Plot", "Tomorrow · NPK 19-19-19", AgroPalette.Primary),
        FieldOp("Storm prep — close greenhouse vents", "Sunday morning", AgroPalette.Amber),
    )
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onOpenFields) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Field operations", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Text("Fields", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(4.dp))
            Text("${ops.size} actions queued — AgroSphere prioritised these for you.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
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

private data class FieldOp(val title: String, val when_: String, val tint: Color)

// ─────────────────────────────────────────────────────────────────────────────
// Alert card
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

// ─────────────────────────────────────────────────────────────────────────────
// Crop Health Monitor — animated sweep gradient ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CropHealthCard(score: Int, verdict: String, onTap: () -> Unit) {
    val target = (score / 100f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
        label = "crop-progress",
    )
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crop health monitor", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Text("View all", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HealthRing(progress = progress, score = score, modifier = Modifier.size(98.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Overall crop health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(verdict, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "12 healthy plants · 1 leaf rust spotted today. Re-scan in 5 days for trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Per-crop mini strip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Wheat" to 86, "Soybean" to 71, "Maize" to 92, "Rice" to 58,
                ).forEach { (crop, value) ->
                    CropChip(name = crop, value = value, modifier = Modifier.weight(1f))
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
private fun PestPredictionCard(riskLevel: String, blipRadiusFraction: Float, onTap: () -> Unit) {
    val (tint, desc) = when (riskLevel.lowercase()) {
        "low" -> AgroPalette.Primary to "Conditions unfavourable for pest activity. Continue routine scouting."
        "moderate" -> AgroPalette.Amber to "Watch for early signs in vulnerable crops. Re-scan in 3 days."
        "high" -> AgroPalette.Rose to "Pest pressure climbing. Inspect tomorrow; preventive spray ready."
        else -> AgroPalette.InkMuted to "Analyzing patterns…"
    }
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = AgroPalette.Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pest prediction", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Text("View details", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PestRadar(blipRadiusFraction = blipRadiusFraction, modifier = Modifier.size(110.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Risk level", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        riskLevel,
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
private fun MyFieldsCarousel(onOpenField: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(MockRepository.fields) { field ->
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
private fun InsightsCarousel() {
    val insights = listOf(
        Triple("Ambient context", "Soil temperature stable at 24°C. Microbial activity is in optimal window — good for foliar uptake.", AgroPalette.Iris),
        Triple("Regional network", "Farms 12 km north reported leaf rust this week. Watch the southern edge of Lake Plot.", Color(0xFF22D3EE)),
        Triple("Soil moisture trend", "Riverside is drying faster than the others. Drip irrigation may need recalibration.", AgroPalette.Sky),
    )
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
