package com.agrosphere.app.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.agrosphere.app.R
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.Path

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
    onOpenPestPrediction: () -> Unit = {},
    onOpenAssistant: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenFields: () -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.state.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }
    // rememberSaveable survives NavHost destination disposal (tab switches, back-stack
    // recreation).  Once true, every future HomeScreen composition seeds itemVisible
    // with true → animateFloatAsState(1f) snaps instantly — no re-animation ever.
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    // itemVisible lives at HomeScreen level so LazyColumn item disposal (scroll)
    // doesn't reset it.  Seeded from entrancePlayed so nav-back is also instant.
    val itemVisible = remember { Array(7) { mutableStateOf(entrancePlayed) } }
    LaunchedEffect(Unit) {
        if (!entrancePlayed) {
            for (i in 0..6) {
                delay(i * 55L)
                itemVisible[i].value = true
            }
            entrancePlayed = true
        }
    }

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
            // ── [0] Immersive hero — greeting + live weather + farm health ────
            item { EntranceItem(itemVisible[0]) {
                HeroBand(
                    name = state.displayName,
                    photoUrl = state.photoUrl,
                    timeOfDay = state.timeOfDay,
                    snapshot = state.weather,
                    loading = state.weatherLoading,
                    cropHealth = state.cropHealth,
                    systemHealthy = state.systemHealthy,
                    notifCount = state.notificationCount,
                    onAvatarTap = onOpenProfile,
                    onBellTap = { showNotifications = true },
                    onWeatherTap = onOpenWeather,
                )
            } }
            // ── [1] Quick actions ─────────────────────────────────────────────
            item { EntranceItem(itemVisible[1]) {
                QuickActionsRow(
                    onScan = onOpenScanner,
                    onAddField = onOpenFields,
                    onAssistant = onOpenAssistant,
                )
            } }
            // ── [2] Live data ribbon — glowing scrolling metrics ──────────────
            item { EntranceItem(itemVisible[2]) { LiveDataRibbon(state = state) } }
            // ── [3] Field operations ──────────────────────────────────────────
            item { EntranceItem(itemVisible[3]) { FieldOperationsCard(onOpenFields = onOpenFields, hasFields = state.fieldsCount > 0) } }
            // ── Alerts ────────────────────────────────────────────────────────
            item { EntranceItem(itemVisible[4]) { SectionHeader(title = stringResource(R.string.section_recent_alerts), trailing = if (state.alerts.isEmpty()) null else stringResource(R.string.section_see_all)) } }
            if (state.alerts.isEmpty()) {
                item { EntranceItem(itemVisible[5]) { AlertsEmptyCard(hasFields = state.fieldsCount > 0) } }
            } else {
                items(state.alerts.take(3)) { alert -> EntranceItem(itemVisible[5]) { AlertCard(alert) } }
            }
            // ── [6] Crop health + pest prediction ─────────────────────────────
            item { EntranceItem(itemVisible[6]) {
                CropHealthCard(
                    score = state.cropHealth,
                    verdict = state.cropHealthVerdict,
                    hasFields = state.fieldsCount > 0,
                    onTap = onOpenScanner,
                )
            } }
            item {
                PestPredictionCard(
                    riskLevel = state.pestRiskLevel,
                    blipRadiusFraction = state.pestRiskBlip,
                    hasFields = state.fieldsCount > 0,
                    onTap = onOpenPestPrediction,
                )
            }
            // ── At a glance grid ──────────────────────────────────────────────
            item { SectionHeader(title = stringResource(R.string.section_at_a_glance)) }
            item { AtAGlanceGrid(state = state) }
            // ── My fields carousel ────────────────────────────────────────────
            item { SectionHeader(title = stringResource(R.string.section_my_fields), trailing = stringResource(R.string.section_manage_all)) }
            item { MyFieldsCarousel(onOpenField = onOpenField, onAddField = onOpenFields) }
            // ── Insights ──────────────────────────────────────────────────────
            item { SectionHeader(title = stringResource(R.string.section_insights)) }
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
// Staggered fade + slide-up entrance.
// visibleState lives at HomeScreen level so it is NEVER lost when LazyColumn
// disposes an off-screen item.  When an item recomposes after scrolling back
// in, visibleState.value is already true → animateFloatAsState is first called
// with target=1f → snaps instantly to 1f, no re-animation.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EntranceItem(visibleState: State<Boolean>, content: @Composable () -> Unit) {
    val visible by visibleState
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(durationMillis = 430), label = "entranceAlpha")
    val ty    by animateFloatAsState(if (visible) 0f else 26f, tween(durationMillis = 430), label = "entranceY")
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = ty.dp.toPx()
        }
    ) { content() }
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
                "${androidx.compose.ui.res.stringResource(timeOfDay.greetingRes)},",
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
    val label = androidx.compose.ui.res.stringResource(
        if (healthy) com.agrosphere.app.R.string.status_all_systems_active
        else com.agrosphere.app.R.string.status_degraded
    )
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
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AgroPalette.SurfaceGlass)
                .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape),
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
        // Live atmospheric overlay — all data params wired to WeatherSnapshot.
        WeatherAtmosphere(
            kind       = kind,
            windKph    = snapshot?.windKph    ?: 0,
            tempC      = temp,
            rainMm     = snapshot?.rainMm     ?: 0,
            humidityPct= snapshot?.humidityPct ?: 60,
            uvIndex    = snapshot?.uvIndex    ?: 5,
            modifier   = Modifier.matchParentSize().clip(shape),
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
                            snapshot?.let { localizedConditionLabel(it.kind) } ?: if (loading) "…" else "—",
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
                WeatherMetric(stringResource(R.string.weather_humidity), snapshot?.humidityPct?.let { "$it%" } ?: "—", Icons.Rounded.WaterDrop, AgroPalette.Sky)
                WeatherMetric(stringResource(R.string.weather_wind), snapshot?.windKph?.let { "$it km/h" } ?: "—", Icons.Rounded.Air, AgroPalette.Primary)
                WeatherMetric(stringResource(R.string.weather_rain), snapshot?.rainMm?.let { "$it mm" } ?: "—", Icons.Rounded.Cloud, AgroPalette.InkMuted)
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
                    Text(stringResource(R.string.weather_sunrise, snapshot?.sunrise ?: "—"), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.weather_view_forecast), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Cinematic per-condition backgrounds. Clear shifts warmer with temperature. */
private fun weatherCardBrush(kind: ConditionKind, tempC: Int): Brush {
    val heat = ((tempC + 5) / 45f).coerceIn(0f, 1f)
    return when (kind) {
        ConditionKind.Clear -> {
            val top = androidx.compose.ui.graphics.lerp(Color(0xFF0F1A0A), Color(0xFF1C0900), heat)
            Brush.verticalGradient(listOf(top, Color(0xFF071209)))
        }
        ConditionKind.PartlyCloudy -> Brush.verticalGradient(
            0f to Color(0xFF1A0805),
            0.55f to Color(0xFF0D0618),
            1f to Color(0xFF05060F),
        )
        ConditionKind.Cloudy -> Brush.verticalGradient(
            listOf(Color(0xFF0A0D10), Color(0xFF040608))
        )
        ConditionKind.Rain -> Brush.verticalGradient(
            listOf(Color(0xFF021528), Color(0xFF010609))
        )
        ConditionKind.Storm -> Brush.verticalGradient(
            listOf(Color(0xFF0B0220), Color(0xFF030009))
        )
        ConditionKind.Night -> Brush.verticalGradient(
            listOf(Color(0xFF01020F), Color(0xFF000005))
        )
    }
}

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
// Cinematic atmospheric overlay — 6 immersive themes wired to live weather data.
// tempC, rainMm, humidityPct, uvIndex all drive per-condition visual intensity.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherAtmosphere(
    kind: ConditionKind,
    windKph: Int,
    tempC: Int = 0,
    rainMm: Int = 0,
    humidityPct: Int = 60,
    uvIndex: Int = 5,
    modifier: Modifier = Modifier,
) {
    val tr = rememberInfiniteTransition(label = "weather-fx")
    val t  by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(4_000,  easing = LinearEasing)), label = "t")
    val t2 by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2_000,  easing = LinearEasing)), label = "t2")
    val t3 by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "t3")
    val windScalar = (windKph / 15f).coerceIn(0.10f, 2.5f)

    Canvas(modifier = modifier) {
        when (kind) {
            ConditionKind.Clear        -> drawSunScene(t, t3, tempC, uvIndex)
            ConditionKind.PartlyCloudy -> drawSunsetScene(t, t3, windScalar)
            ConditionKind.Cloudy       -> drawFogScene(t, t3, humidityPct)
            ConditionKind.Rain         -> drawRainScene(t, t2, rainMm)
            ConditionKind.Storm        -> drawStormScene(t, t2, windKph)
            ConditionKind.Night        -> drawNightScene(t, t2, t3)
        }
    }
}

// ── ☀️ Clear / Summer ─────────────────────────────────────────────────────────
// Light rays count = 4 + (uvIndex/3). Dust motes drift upward.
// Heat shimmer drawn at tempC > 32°C.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunScene(
    t: Float, t3: Float, tempC: Int, uvIndex: Int,
) {
    val w = size.width; val h = size.height
    val cx = w * 0.82f; val cy = h * 0.13f

    // Sun corona
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFCD34D).copy(alpha = 0.55f),
            0.35f to Color(0xFFF59E0B).copy(alpha = 0.18f),
            1f to Color.Transparent,
            center = Offset(cx, cy), radius = w * 0.52f,
        ),
        radius = w * 0.52f, center = Offset(cx, cy),
    )

    // Crepuscular light rays — count wired to UV index
    val rayCount = 4 + (uvIndex / 3).coerceAtMost(4)
    val rayAlpha = 0.08f + 0.07f * (sin(t3 * PI.toFloat() * 2f) * 0.5f + 0.5f)
    val spreadTotal = 1.10f; val midAngle = 2.30f
    repeat(rayCount) { i ->
        val angle = midAngle + (i - rayCount / 2f) * (spreadTotal / rayCount)
        val rayLen = w * (0.85f + (i % 3) * 0.18f)
        val ex = cx + cos(angle) * rayLen
        val ey = cy + sin(angle) * rayLen
        drawLine(
            brush = Brush.linearGradient(
                0f to Color(0xFFFCD34D).copy(alpha = rayAlpha),
                1f to Color.Transparent,
                start = Offset(cx, cy), end = Offset(ex, ey),
            ),
            start = Offset(cx, cy), end = Offset(ex, ey),
            strokeWidth = 20f - i * 1.5f, cap = StrokeCap.Round,
        )
    }

    // Golden dust motes drifting upward
    repeat(40) { i ->
        val seed = (i * 73 + 17) % 1000 / 1000f
        val anchorX = w * (((i * 47) % 100) / 100f)
        val startY  = h * (0.10f + ((i * 31) % 90) / 100f)
        val speed   = 0.18f + seed * 0.28f
        val y = ((startY - t * speed * h + h * 2f) % h)
        val x = anchorX + sin(t * (1.5f + seed) * PI.toFloat() * 2f + seed * 6.28f) * w * 0.03f
        drawCircle(
            color  = Color(0xFFFCD34D).copy(alpha = (0.12f + 0.18f * seed) * (1f - y / h * 0.5f).coerceIn(0f, 1f)),
            radius = 0.8f + seed * 1.4f, center = Offset(x, y),
        )
    }

    // Heat shimmer lines at very high temperatures
    if (tempC > 32) {
        repeat(7) { i ->
            val phase = i * (PI.toFloat() / 3.5f)
            val yOff = sin(t * PI.toFloat() * 6f + phase) * 3f
            drawLine(
                color = Color(0xFFFCD34D).copy(alpha = 0.04f),
                start = Offset(w * (i / 7f), h * 0.84f + yOff),
                end   = Offset(w * ((i + 1) / 7f), h * 0.84f - yOff),
                strokeWidth = 1.5f,
            )
        }
    }
}

// ── 🌅 PartlyCloudy / Sunset ──────────────────────────────────────────────────
// Orange horizon glow, warm amber clouds, lens flare, silhouette hills.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunsetScene(
    t: Float, t3: Float, windScalar: Float,
) {
    val w = size.width; val h = size.height

    // Orange horizon glow band
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color(0xFFFF7B00).copy(alpha = 0.20f),
            0.75f to Color(0xFFFF4500).copy(alpha = 0.14f),
            1f to Color.Transparent,
        ),
        size = androidx.compose.ui.geometry.Size(w, h),
    )

    // Sunset-tinted warm clouds
    drawSunsetClouds(t, windScalar)

    // Lens flare from top-right
    val flareAlpha = 0.45f + 0.30f * (sin(t3 * PI.toFloat() * 2f) * 0.5f + 0.5f)
    val fx = w * 0.82f; val fy = h * 0.12f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFCD34D).copy(alpha = 0.65f * flareAlpha),
            0.4f to Color(0xFFFB923C).copy(alpha = 0.20f * flareAlpha),
            1f to Color.Transparent,
            center = Offset(fx, fy), radius = 32f,
        ),
        radius = 32f, center = Offset(fx, fy),
    )
    // Secondary flare glints along a line toward centre
    listOf(0.22f, 0.42f, 0.62f).forEachIndexed { i, dist ->
        drawCircle(
            color  = Color(0xFFFF7B00).copy(alpha = (0.25f - i * 0.07f) * flareAlpha),
            radius = 7f - i * 2f,
            center = Offset(fx + (w * 0.5f - fx) * dist, fy + (h * 0.5f - fy) * dist),
        )
    }

    // Dark silhouette hill ridge at bottom
    val hill = Path()
    hill.moveTo(0f, h); hill.lineTo(0f, h * 0.84f)
    hill.quadraticTo(w * 0.12f, h * 0.77f, w * 0.22f, h * 0.82f)
    hill.quadraticTo(w * 0.33f, h * 0.87f, w * 0.44f, h * 0.76f)
    hill.quadraticTo(w * 0.56f, h * 0.68f, w * 0.66f, h * 0.79f)
    hill.quadraticTo(w * 0.76f, h * 0.84f, w * 0.86f, h * 0.74f)
    hill.quadraticTo(w * 0.93f, h * 0.69f, w, h * 0.78f)
    hill.lineTo(w, h); hill.close()
    drawPath(hill, Color(0xFF020001).copy(alpha = 0.70f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunsetClouds(t: Float, windScalar: Float) {
    val w = size.width; val h = size.height
    val puffs = listOf(
        Triple( 0.00f, 0.05f, 1.00f), Triple(-0.55f, 0.15f, 0.70f),
        Triple( 0.55f, 0.15f, 0.70f), Triple(-0.30f,-0.25f, 0.80f),
        Triple( 0.30f,-0.30f, 0.75f),
    )
    data class SC(val ax: Float, val ay: Float, val dX: Float, val wY: Float, val per: Float, val ph: Float, val sc: Float, val ma: Float, val d: Float)
    val baseS = h * 0.16f
    listOf(
        SC(0.25f, 0.30f, 0.18f, 0.06f, 0.20f, 0.00f, baseS,         0.16f,  1f),
        SC(0.65f, 0.20f, 0.14f, 0.05f, 0.16f, 0.42f, baseS * 0.80f, 0.12f, -1f),
    ).forEach { c ->
        val ang = ((t * c.per * windScalar + c.ph) % 1f) * 2f * PI.toFloat()
        val cx = w * c.ax + sin(ang) * w * c.dX * c.d
        val cy = h * c.ay + sin(ang * 1.7f + 0.6f) * h * c.wY
        val alpha = c.ma * sin(ang).let { it * it }
        val scale = c.sc * (0.92f + 0.16f * sin(ang * 0.5f + 1.2f))
        if (alpha < 0.01f) return@forEach
        puffs.forEach { (dx, dy, sf) ->
            val r = scale * sf; val px = cx + dx * scale; val py = cy + dy * scale
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFFFB347).copy(alpha = alpha),
                    0.5f to Color(0xFFFF7B00).copy(alpha = alpha * 0.35f),
                    1f to Color.Transparent,
                    center = Offset(px, py), radius = r,
                ),
                radius = r, center = Offset(px, py),
            )
        }
    }
}

// ── 🌧️ Rain ───────────────────────────────────────────────────────────────────
// Streak count = 30 + rainMm*2 (max 70). Angled streaks, splash rings, neon fog.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRainScene(
    t: Float, t2: Float, rainMm: Int,
) {
    val w = size.width; val h = size.height

    // Neon teal fog orbs drifting horizontally
    val fogDrift = sin(t2 * PI.toFloat() * 2f) * w * 0.08f
    listOf(
        Offset(w * 0.25f + fogDrift, h * 0.42f) to w * 0.60f,
        Offset(w * 0.75f - fogDrift * 0.7f, h * 0.62f) to w * 0.50f,
    ).forEach { (pos, r) ->
        drawCircle(
            brush = Brush.radialGradient(0f to Color(0xFF22D3EE).copy(alpha = 0.08f), 1f to Color.Transparent, center = pos, radius = r),
            radius = r, center = pos,
        )
    }
    // Rising mist from bottom
    drawRect(
        brush = Brush.verticalGradient(0.60f to Color.Transparent, 1f to Color(0xFF93C5FD).copy(alpha = 0.09f)),
        size  = androidx.compose.ui.geometry.Size(w, h),
    )
    // Angled rain streaks — density driven by rainMm
    val count = (30 + rainMm * 2).coerceIn(30, 70)
    repeat(count) { i ->
        val seed = (i * 73 + 11) % 100 / 100f
        val x = w * (((i * 47) % 100) / 100f)
        val y = ((t + seed) % 1f) * (h + 60f) - 30f
        val len = 10f + (i % 5) * 5f
        drawLine(
            color = Color(0xFF93C5FD).copy(alpha = 0.40f + (i % 3) * 0.08f),
            start = Offset(x, y), end = Offset(x + len * 0.07f, y + len),
            strokeWidth = 0.7f + (i % 3) * 0.4f, cap = StrokeCap.Round,
        )
    }
    // Raindrop splash rings at bottom edge
    repeat(10) { i ->
        val r = ((t2 + i / 10f) % 1f) * 10f
        val alpha = (1f - r / 10f) * 0.40f
        if (alpha > 0.02f) {
            drawCircle(
                color  = Color(0xFF93C5FD).copy(alpha = alpha),
                radius = r + 1f,
                center = Offset(w * (((i * 83) % 100) / 100f), h * (0.89f + (i % 3) * 0.035f)),
                style  = Stroke(width = 0.8f),
            )
        }
    }
}

// ── ⛈️ Storm ──────────────────────────────────────────────────────────────────
// Violet storm mass, 65 fast streaks, jagged zig-zag lightning with glow aura.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStormScene(
    t: Float, t2: Float, windKph: Int,
) {
    val w = size.width; val h = size.height
    val pulse = sin(t * PI.toFloat() * 2f) * 0.5f + 0.5f

    // Rolling dark storm masses
    drawCircle(
        brush = Brush.radialGradient(0f to Color(0xFF7C3AED).copy(alpha = 0.24f * (0.4f + pulse * 0.6f)), 1f to Color.Transparent, center = Offset(w * 0.50f, h * 0.16f), radius = w * 0.95f),
        radius = w * 0.95f, center = Offset(w * 0.50f, h * 0.16f),
    )
    drawCircle(
        brush = Brush.radialGradient(0f to Color(0xFF4C1D95).copy(alpha = 0.32f), 1f to Color.Transparent, center = Offset(w * 0.18f, h * 0.08f), radius = w * 0.52f),
        radius = w * 0.52f, center = Offset(w * 0.18f, h * 0.08f),
    )

    // Heavy fast rain
    repeat(65) { i ->
        val seed = (i * 73 + 11) % 100 / 100f
        val y = ((t * 1.65f + seed) % 1f) * (h + 60f) - 30f
        val len = 12f + (i % 4) * 5f
        drawLine(
            color = Color(0xFF93C5FD).copy(alpha = 0.30f),
            start = Offset(w * (((i * 47) % 100) / 100f), y),
            end   = Offset(w * (((i * 47) % 100) / 100f) + len * 0.10f, y + len),
            strokeWidth = 0.9f, cap = StrokeCap.Round,
        )
    }

    // Lightning bolt flash triggered at t2 phase > 0.88
    val lp = (t2 * 3f) % 1f
    if (lp > 0.88f) {
        val raw = (lp - 0.88f) / 0.12f
        val fa = (raw * 2f).coerceAtMost(1f) * ((1f - raw) * 2f).coerceAtMost(1f)
        drawRect(Color.White.copy(alpha = 0.10f * fa)) // full-card electric flash

        repeat(2) { bolt ->
            val startX = w * (0.38f + bolt * 0.32f)
            val boltPath = Path().also { p ->
                p.moveTo(startX, 0f)
                var cx = startX; var cy = 0f
                val segH = h * 0.55f / 5f
                repeat(5) { seg ->
                    cx += ((bolt * 137 + seg * 53) % 50 - 25).toFloat()
                    cy += segH
                    p.lineTo(cx, cy)
                }
            }
            drawPath(boltPath, Color(0xFF7C3AED).copy(alpha = 0.45f * fa), style = Stroke(width = 10f, cap = StrokeCap.Round))
            drawPath(boltPath, Color.White.copy(alpha = 0.92f * fa),        style = Stroke(width = 2f,  cap = StrokeCap.Round))
        }
    }
}

// ── 🌫️ Cloudy / Fog ───────────────────────────────────────────────────────────
// 4 drifting fog layers (opacity ∝ humidityPct), cold vignette, frost specks.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFogScene(
    t: Float, t3: Float, humidityPct: Int,
) {
    val w = size.width; val h = size.height
    val fogDensity = 0.06f + humidityPct * 0.00028f

    // 4 extremely slow drifting fog layers at different heights
    listOf(
        Triple(h * 0.20f, 0.00f,  1f),
        Triple(h * 0.38f, 0.25f, -1f),
        Triple(h * 0.55f, 0.55f,  1f),
        Triple(h * 0.72f, 0.75f, -1f),
    ).forEach { (y, phase, dir) ->
        val drift = sin((t3 + phase) * PI.toFloat() * 2f) * w * 0.10f * dir
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.20f to Color(0xFFCBD5E1).copy(alpha = fogDensity),
                0.50f to Color(0xFFE2E8F0).copy(alpha = fogDensity * 1.4f),
                0.80f to Color(0xFFCBD5E1).copy(alpha = fogDensity),
                1f to Color.Transparent,
                startX = drift, endX = w + drift,
            ),
            topLeft = Offset(0f, y - 18f),
            size    = androidx.compose.ui.geometry.Size(w, 55f),
        )
    }

    // Cold vignette — dark at edges, lighter in centre
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            0.60f to Color(0xFF0D0F13).copy(alpha = 0.18f),
            1f to Color(0xFF0D0F13).copy(alpha = 0.52f),
            center = Offset(w / 2f, h / 2f), radius = w * 0.96f,
        ),
        radius = w * 0.96f, center = Offset(w / 2f, h / 2f),
    )

    // Frost specks falling very slowly
    repeat(25) { i ->
        val seed = (i * 137 + 7) % 1000 / 1000f
        val y = (h * (((i * 31) % 90) / 100f) + t3 * h * 0.42f) % h
        drawCircle(
            color  = Color.White.copy(alpha = 0.07f + 0.13f * seed),
            radius = 0.5f + seed * 0.7f,
            center = Offset(w * (((i * 53) % 100) / 100f), y),
        )
    }
}

// ── 🌌 Night ──────────────────────────────────────────────────────────────────
// 55 twinkling stars (10 with halo), periodic shooting star, 4 neon nodes,
// 3 horizontal neon particle trails (cyan + iris).
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNightScene(
    t: Float, t2: Float, t3: Float,
) {
    val w = size.width; val h = size.height

    // Rich starfield — 55 stars, every 10th gets a glow halo
    repeat(55) { i ->
        val seed    = (i * 137 + 7) % 1000 / 1000f
        val sx      = w * (((i * 53) % 100) / 100f)
        val sy      = h * (((i * 31) % 95) / 100f)
        val twinkle = 0.45f + 0.55f * (sin(t * PI.toFloat() * 3.2f + seed * 6.28f) * 0.5f + 0.5f)
        val bright  = i % 10 == 0
        if (bright) {
            drawCircle(
                brush = Brush.radialGradient(0f to Color(0xFFF8FAFC).copy(alpha = 0.38f * twinkle), 1f to Color.Transparent, center = Offset(sx, sy), radius = 6f + (i % 3) * 2f),
                radius = 6f + (i % 3) * 2f, center = Offset(sx, sy),
            )
        }
        drawCircle(
            color  = Color(0xFFF8FAFC).copy(alpha = (if (bright) 0.18f else 0.08f) + 0.32f * twinkle),
            radius = if (bright) 1.5f + (i % 3) * 0.3f else 0.7f + (i % 3) * 0.35f,
            center = Offset(sx, sy),
        )
    }

    // Shooting star — appears when t3 phase > 0.90
    val sp = (t3 * 2f) % 1f
    if (sp > 0.90f) {
        val raw = (sp - 0.90f) / 0.10f
        val sa  = (raw * 2.5f).coerceAtMost(1f) * ((1f - raw) * 2.5f).coerceAtMost(1f)
        drawLine(
            brush = Brush.linearGradient(0f to Color.Transparent, 0.35f to Color.White.copy(alpha = 0.85f * sa), 1f to Color.Transparent, start = Offset(w * 0.08f, h * 0.07f), end = Offset(w * 0.50f, h * 0.26f)),
            start = Offset(w * 0.08f, h * 0.07f), end = Offset(w * 0.50f, h * 0.26f),
            strokeWidth = 1.8f,
        )
    }

    // Neon glow nodes — 2 cyan + 2 iris, pulsing at independent rates
    listOf(
        Offset(w * 0.14f, h * 0.24f) to Color(0xFF22D3EE),
        Offset(w * 0.76f, h * 0.44f) to Color(0xFF22D3EE),
        Offset(w * 0.42f, h * 0.14f) to Color(0xFFA78BFA),
        Offset(w * 0.88f, h * 0.19f) to Color(0xFFA78BFA),
    ).forEachIndexed { i, (pos, tint) ->
        val pulse = 0.5f + 0.5f * sin((t + i * 0.25f) * PI.toFloat() * 2f)
        val r = 4f + 2.5f * pulse
        drawCircle(brush = Brush.radialGradient(0f to tint.copy(alpha = 0.55f + 0.30f * pulse), 1f to Color.Transparent, center = pos, radius = r), radius = r, center = pos)
        drawCircle(tint.copy(alpha = 0.85f), radius = 1.4f, center = pos)
    }

    // Neon particle trails drifting across (cyan alternating with iris)
    repeat(3) { i ->
        val tint   = if (i % 2 == 0) Color(0xFF22D3EE) else Color(0xFFA78BFA)
        val ancY   = h * (0.28f + i * 0.20f)
        val xBase  = ((t * (0.12f + i * 0.07f) * (w + 100f) + i.toFloat() * (w / 3f)) % (w + 80f)) - 40f
        val trailL = w * 0.09f
        drawLine(
            brush = Brush.linearGradient(0f to Color.Transparent, 1f to tint.copy(alpha = 0.50f), start = Offset(xBase, ancY), end = Offset(xBase + trailL, ancY)),
            start = Offset(xBase, ancY), end = Offset(xBase + trailL, ancY),
            strokeWidth = 1.5f,
        )
        drawCircle(tint.copy(alpha = 0.75f), radius = 1.8f, center = Offset(xBase + trailL, ancY))
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
// HeroBand — immersive merged header + weather + farm-health card.
// Replaces the separate StickyHeader + WeatherHeroCard with a single premium
// full-width panel: greeting, time-of-day name, live weather, animated Canvas
// overlays (orbiting particle, neon hairline, ambient glow), and a health bar.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroBand(
    name: String,
    photoUrl: String?,
    timeOfDay: TimeOfDay,
    snapshot: WeatherSnapshot?,
    loading: Boolean,
    cropHealth: Int,
    systemHealthy: Boolean,
    notifCount: Int,
    onAvatarTap: () -> Unit,
    onBellTap: () -> Unit,
    onWeatherTap: () -> Unit,
) {
    val kind        = snapshot?.kind ?: ConditionKind.Cloudy
    val temp        = snapshot?.tempC ?: 0
    val (wIcon, wTint) = if (snapshot != null) iconAndTintFor(snapshot.kind) else Icons.Rounded.Cloud to AgroPalette.InkMuted
    val tempColor   = colorForTemp(temp, snapshot != null)

    // Animated health bar
    val healthProgress by animateFloatAsState(
        targetValue  = (cropHealth / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1600, easing = LinearOutSlowInEasing),
        label = "hero-health",
    )
    val healthTint = when {
        cropHealth >= 80 -> AgroPalette.Primary
        cropHealth >= 60 -> AgroPalette.Amber
        else -> AgroPalette.Rose
    }

    // Shared infinite transition for all canvas effects
    val inf = rememberInfiniteTransition(label = "hero")
    val orbAngle by inf.animateFloat(
        0f, (PI * 2).toFloat(),
        infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "orb",
    )
    val glowPulse by inf.animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "glow",
    )
    val iconPulse by inf.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "ip",
    )

    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(weatherCardBrush(kind, temp), shape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, shape)
            .clickable(onClick = onWeatherTap),
    ) {
        // Condition atmosphere (rain streaks, drifting clouds, starfield, etc.)
        WeatherAtmosphere(
            kind        = kind,
            windKph     = snapshot?.windKph    ?: 0,
            tempC       = snapshot?.tempC      ?: 0,
            rainMm      = snapshot?.rainMm     ?: 0,
            humidityPct = snapshot?.humidityPct ?: 60,
            uvIndex     = snapshot?.uvIndex    ?: 5,
            modifier    = Modifier.matchParentSize().clip(shape),
        )

        // ── Premium Canvas layer ──────────────────────────────────────────────
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height

            // Neon top hairline — pulses with the condition tint
            val mid = w / 2f; val halfLen = w * 0.46f
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        wTint.copy(alpha = 0.35f),
                        wTint.copy(alpha = 0.70f * glowPulse),
                        wTint.copy(alpha = 0.35f),
                        Color.Transparent,
                    ),
                    startX = mid - halfLen, endX = mid + halfLen,
                ),
                start = Offset(mid - halfLen, 1f), end = Offset(mid + halfLen, 1f),
                strokeWidth = 1.5f,
            )

            // Orbiting particle cluster in top-right (near weather icon)
            val orbCx = w * 0.80f; val orbCy = h * 0.26f; val orbR = w * 0.14f
            repeat(7) { i ->
                val trailAng = orbAngle - i * 0.30f
                val tr = (0.70f - i * 0.10f).coerceAtLeast(0f) * glowPulse
                val r  = (5.5f - i * 0.7f).coerceAtLeast(1f)
                drawCircle(
                    color  = wTint.copy(alpha = tr),
                    radius = r,
                    center = Offset(orbCx + cos(trailAng) * orbR, orbCy + sin(trailAng) * orbR),
                )
            }
            val px = orbCx + cos(orbAngle) * orbR
            val py = orbCy + sin(orbAngle) * orbR
            drawCircle(
                brush = Brush.radialGradient(
                    0f to wTint.copy(alpha = 0.95f), 1f to Color.Transparent,
                    center = Offset(px, py), radius = 8f,
                ),
                radius = 8f, center = Offset(px, py),
            )

            // Ambient radial glow behind the weather icon
            drawCircle(
                brush = Brush.radialGradient(
                    0f to wTint.copy(alpha = 0.20f * glowPulse), 1f to Color.Transparent,
                    center = Offset(w * 0.78f, h * 0.36f), radius = w * 0.46f,
                ),
                radius = w * 0.46f, center = Offset(w * 0.78f, h * 0.36f),
            )

            // Subtle emerald glow bottom-left (farm energy feel)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.12f * glowPulse), 1f to Color.Transparent,
                    center = Offset(w * 0.08f, h * 0.85f), radius = w * 0.55f,
                ),
                radius = w * 0.55f, center = Offset(w * 0.08f, h * 0.85f),
            )
        }

        // ── Content ───────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(18.dp)) {

            // Row 1: location | bell | avatar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.Sky, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        snapshot?.location ?: if (loading) "Detecting…" else "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkMuted, maxLines = 1,
                    )
                }
                NotificationBell(count = notifCount, onClick = onBellTap)
                Spacer(Modifier.width(8.dp))
                AvatarChip(photoUrl = photoUrl, name = name, onTap = onAvatarTap)
            }

            Spacer(Modifier.height(18.dp))

            // Row 2: greeting + name (left)  |  weather icon + temp (right)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(timeOfDay.greetingRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroPalette.InkMuted,
                    )
                    Text(
                        "$name ${timeOfDay.emoji}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp),
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    SystemBadge(healthy = systemHealthy)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    0f to wTint.copy(alpha = 0.38f * iconPulse),
                                    0.6f to wTint.copy(alpha = 0.08f * iconPulse),
                                    1f to Color.Transparent,
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(wIcon, null, tint = wTint, modifier = Modifier.size(38.dp)) }
                    Text(
                        snapshot?.let { "${it.tempC}°" } ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        color = tempColor, fontWeight = FontWeight.Black,
                    )
                    Text(
                        snapshot?.let { localizedConditionLabel(it.kind) } ?: if (loading) "…" else "—",
                        style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted,
                    )
                }
            }

            if (cropHealth > 0) {
                Spacer(Modifier.height(16.dp))

                // Farm health bar — only shown when real data is available
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Farm Health",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkMuted,
                        modifier = Modifier.width(82.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AgroPalette.SurfaceGlass),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(healthProgress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(healthTint.copy(alpha = 0.7f), healthTint)
                                    )
                                ),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$cropHealth%",
                        style = MaterialTheme.typography.labelMedium,
                        color = healthTint, fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bottom metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeatherMetric(stringResource(R.string.weather_humidity), snapshot?.humidityPct?.let { "$it%" } ?: "—", Icons.Rounded.WaterDrop, AgroPalette.Sky)
                WeatherMetric(stringResource(R.string.weather_wind), snapshot?.windKph?.let { "$it km/h" } ?: "—", Icons.Rounded.Air, AgroPalette.Primary)
                WeatherMetric(stringResource(R.string.weather_rain), snapshot?.rainMm?.let { "$it mm" } ?: "—", Icons.Rounded.Cloud, AgroPalette.InkMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.weather_view_forecast), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiveDataRibbon — horizontally scrolling live-stat chips with blinking dots.
// Pulls from WeatherSnapshot + HomeUiState field metrics so every chip shows
// real data.  Empty states are skipped, so the ribbon only renders if there
// is at least one live value to show.
// ─────────────────────────────────────────────────────────────────────────────
private data class RibbonChip(val value: String, val icon: ImageVector, val tint: Color)

@Composable
private fun LiveDataRibbon(state: HomeUiState) {
    val inf   = rememberInfiniteTransition(label = "ribbon")
    val blink by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "b",
    )

    val chips = buildList {
        state.weather?.let { w ->
            add(RibbonChip("${w.tempC}°C", Icons.Rounded.WbSunny, AgroPalette.Amber))
            add(RibbonChip("${w.humidityPct}%", Icons.Rounded.WaterDrop, AgroPalette.Sky))
            add(RibbonChip("${w.windKph} km/h", Icons.Rounded.Air, AgroPalette.Primary))
            if (w.rainMm > 0) add(RibbonChip("${w.rainMm}mm rain", Icons.Rounded.Cloud, AgroPalette.Sky))
        }
        if (state.fieldsCount > 0) {
            val hTint = when {
                state.cropHealth >= 80 -> AgroPalette.Primary
                state.cropHealth >= 60 -> AgroPalette.Amber
                else -> AgroPalette.Rose
            }
            add(RibbonChip("Health ${state.cropHealth}", Icons.Rounded.Eco, hTint))
            if (state.avgMoisture > 0) add(RibbonChip("${state.avgMoisture}% soil", Icons.Rounded.WaterDrop, AgroPalette.Sky))
            add(RibbonChip("${state.fieldsCount} field${if (state.fieldsCount == 1) "" else "s"}", Icons.Rounded.Grass, AgroPalette.Primary))
        }
    }

    if (chips.isEmpty()) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(chips) { chip ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(chip.tint.copy(alpha = 0.10f))
                    .border(1.dp, chip.tint.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Blinking live indicator dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(chip.tint.copy(alpha = 0.40f + 0.60f * blink)),
                )
                Spacer(Modifier.width(6.dp))
                Icon(chip.icon, null, tint = chip.tint, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    chip.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick actions row (horizontally scrollable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsRow(
    onScan: () -> Unit,
    onAddField: () -> Unit,
    onAssistant: () -> Unit,
) {
    val actions = listOf(
        QuickActionData(androidx.compose.ui.res.stringResource(com.agrosphere.app.R.string.action_scan_crop), Icons.Rounded.CameraAlt, AgroPalette.Primary, onScan),
        QuickActionData(androidx.compose.ui.res.stringResource(com.agrosphere.app.R.string.action_add_field), Icons.Rounded.Grass, AgroPalette.Sky, onAddField),
        QuickActionData(androidx.compose.ui.res.stringResource(com.agrosphere.app.R.string.action_ai_assistant), Icons.Rounded.AutoAwesome, AgroPalette.Iris, onAssistant),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { a -> QuickActionPill(a, modifier = Modifier.weight(1f)) }
    }
}

private data class QuickActionData(val label: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)

@Composable
private fun QuickActionPill(action: QuickActionData, modifier: Modifier = Modifier) {
    val inf  = rememberInfiniteTransition(label = "pill-${action.label}")
    val glow by inf.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "g",
    )
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // Tinted top plasma hairline (unique color per action)
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f   to Color.Transparent,
                        0.5f to action.tint.copy(alpha = 0.75f * glow),
                        1f   to Color.Transparent,
                    ),
                    start       = Offset(0f, 0.7f),
                    end         = Offset(size.width, 0.7f),
                    strokeWidth = 1.5f,
                )
                // Soft radial glow behind icon area
                val iconCy = size.height * 0.30f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to action.tint.copy(alpha = 0.20f * glow),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2f, iconCy),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2f, iconCy),
                )
            }
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, action.tint.copy(alpha = 0.22f), shape)
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
                Text(stringResource(R.string.section_field_operations), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.nav_fields), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(4.dp))
            if (!hasFields) {
                Text(
                    stringResource(R.string.field_ops_no_fields_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            } else if (ops.isEmpty()) {
                Text(
                    stringResource(R.string.field_ops_all_clear),
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            } else {
                Text(stringResource(R.string.field_ops_actions_suggested, ops.size, if (ops.size == 1) "" else "s"), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
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
                Text(stringResource(R.string.empty_no_alerts_title), style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(
                    stringResource(if (hasFields) R.string.alerts_empty_with_fields else R.string.alerts_empty_no_fields),
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
                Text(stringResource(R.string.section_crop_health), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                if (hasFields) Text(stringResource(R.string.section_view_all), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            if (!hasFields) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthRing(progress = 0f, score = 0, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.crop_no_data_title), style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.crop_no_data_body),
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
                        Text(stringResource(R.string.crop_overall_health), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(verdict, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.crop_avg_summary, fields.size, if (fields.size == 1) "" else "s"),
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
    // Pulsing tip dot glow
    val inf    = rememberInfiniteTransition(label = "ring-tip")
    val tipPulse by inf.animateFloat(
        0.55f, 1.45f,
        infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Reverse),
        label = "tp",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9f
            val inset  = stroke / 2f
            val arc    = size.minDimension - stroke
            val arcR   = arc / 2f

            // Track ring
            drawArc(
                color = AgroPalette.SurfaceGlassBorder,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size    = androidx.compose.ui.geometry.Size(arc, arc),
                style   = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Primary)
                ),
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = Offset(inset, inset),
                size    = androidx.compose.ui.geometry.Size(arc, arc),
                style   = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Glowing dot at the arc tip
            if (progress > 0.02f) {
                val tipAngle = ((-90f + 360f * progress) * PI / 180.0)
                val cx  = size.width  / 2f
                val cy  = size.height / 2f
                val tipX = cx + arcR * cos(tipAngle).toFloat()
                val tipY = cy + arcR * sin(tipAngle).toFloat()
                val glowR = stroke * 2.8f * tipPulse.coerceIn(0.5f, 1.5f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Sky.copy(alpha = 0.90f),
                        1f to Color.Transparent,
                        center = Offset(tipX, tipY), radius = glowR,
                    ),
                    radius = glowR, center = Offset(tipX, tipY),
                )
                drawCircle(AgroPalette.Ink, radius = stroke * 0.52f, center = Offset(tipX, tipY))
            }
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
    val descLow = stringResource(R.string.pest_desc_low)
    val descModerate = stringResource(R.string.pest_desc_moderate)
    val descHigh = stringResource(R.string.pest_desc_high)
    val descSevere = stringResource(R.string.pest_desc_severe)
    val descNoData = stringResource(R.string.pest_no_data_body)
    val (tint, desc) = when (riskLevel.lowercase()) {
        "low" -> AgroPalette.Primary to descLow
        "moderate" -> AgroPalette.Amber to descModerate
        "high" -> AgroPalette.Rose to descHigh
        "severe" -> AgroPalette.Rose to descSevere
        else -> AgroPalette.InkMuted to descNoData
    }
    GlassCard(radius = 22.dp, padding = 18.dp, onClick = onTap) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = AgroPalette.Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.section_pest_prediction), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                if (hasFields) Text(stringResource(R.string.section_view_details), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PestRadar(
                    blipRadiusFraction = if (hasFields) blipRadiusFraction else 0f,
                    modifier = Modifier.size(if (hasFields) 110.dp else 90.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.pest_risk_level), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
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
            val r  = size.minDimension / 2
            val cx = size.width  / 2
            val cy = size.height / 2

            // 3 concentric rings — slightly brighter than before
            listOf(0.38f, 0.68f, 1f).forEach { f ->
                drawCircle(
                    color = AgroPalette.Primary.copy(alpha = 0.28f),
                    radius = r * f, center = Offset(cx, cy),
                    style = Stroke(width = 1f),
                )
            }
            // Crosshair
            drawLine(AgroPalette.Primary.copy(alpha = 0.15f), Offset(cx - r, cy), Offset(cx + r, cy), 1f)
            drawLine(AgroPalette.Primary.copy(alpha = 0.15f), Offset(cx, cy - r), Offset(cx, cy + r), 1f)

            // ── Trailing sector: wide dim layer ──────────────────────────────
            drawArc(
                color      = AgroPalette.Primary.copy(alpha = 0.07f),
                startAngle = angle - 65f, sweepAngle = 65f, useCenter = true,
                topLeft    = Offset(cx - r, cy - r),
                size       = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            )
            // ── Trailing sector: narrow bright layer near the sweep line ─────
            drawArc(
                color      = AgroPalette.Primary.copy(alpha = 0.13f),
                startAngle = angle - 28f, sweepAngle = 28f, useCenter = true,
                topLeft    = Offset(cx - r, cy - r),
                size       = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            )

            // ── Sweep line — brighter + thicker ──────────────────────────────
            val rad      = angle * PI.toFloat() / 180f
            val sweepLen = r * 0.95f
            val sx = cx + cos(rad) * sweepLen
            val sy = cy + sin(rad) * sweepLen
            drawLine(
                brush = Brush.linearGradient(
                    0f   to AgroPalette.Primary.copy(alpha = 0.0f),
                    0.4f to AgroPalette.Primary.copy(alpha = 0.55f),
                    1f   to AgroPalette.Primary.copy(alpha = 1.00f),
                    start = Offset(cx, cy), end = Offset(sx, sy),
                ),
                start = Offset(cx, cy), end = Offset(sx, sy),
                strokeWidth = 2.8f, cap = StrokeCap.Round,
            )
            // Bright glint at sweep tip
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Sky.copy(alpha = 0.85f),
                    1f to Color.Transparent,
                    center = Offset(sx, sy), radius = 8f,
                ),
                radius = 8f, center = Offset(sx, sy),
            )

            // ── Blip — pulsing amber dot ──────────────────────────────────────
            val blipAngle = 35f * PI.toFloat() / 180f
            val blipR     = r * blipRadiusFraction
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
        GlanceItem(stringResource(R.string.glance_fields), "${state.fieldsCount}", stringResource(R.string.glance_active), Icons.Rounded.Grass, AgroPalette.Primary),
        GlanceItem(stringResource(R.string.glance_crops), "${state.cropsCount}", stringResource(R.string.glance_growing), Icons.Rounded.Eco, AgroPalette.Primary),
        GlanceItem(stringResource(R.string.glance_irrigation), if (state.irrigationEfficiency > 0) "${state.irrigationEfficiency}%" else "—", stringResource(R.string.glance_efficiency), Icons.Rounded.WaterDrop, AgroPalette.Sky),
        GlanceItem(stringResource(R.string.glance_soil), "${state.avgMoisture}%", stringResource(R.string.glance_moisture), Icons.Rounded.WaterDrop, AgroPalette.Sky),
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
                    Text(stringResource(R.string.empty_no_fields_title), style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Text(stringResource(R.string.empty_no_fields_body), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
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
    val context = LocalContext.current
    val fields by FieldRepository.fields.collectAsState()
    val insights = remember(fields, weather) { deriveInsights(context, fields, weather) }
    if (insights.isEmpty()) {
        GlassCard(radius = 20.dp, padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.insights_empty_body), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
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
                    Text(stringResource(R.string.onboarding_setup_title), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.onboarding_setup_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NudgeStep("①", stringResource(R.string.nudge_add_field), AgroPalette.Primary)
                NudgeStep("②", stringResource(R.string.nudge_run_scan), AgroPalette.Sky)
                NudgeStep("③", stringResource(R.string.nudge_get_insights), AgroPalette.Iris)
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
    context: Context,
    fields: List<com.agrosphere.app.data.model.Field>,
    weather: WeatherSnapshot?,
): List<Triple<String, String, Color>> {
    val out = mutableListOf<Triple<String, String, Color>>()
    if (weather != null) {
        when {
            weather.kind == ConditionKind.Storm ->
                out += Triple(
                    context.getString(R.string.insight_storm_title),
                    context.getString(R.string.insight_storm_body),
                    AgroPalette.Iris,
                )
            weather.humidityPct >= 80 ->
                out += Triple(
                    context.getString(R.string.insight_humidity_title),
                    context.getString(R.string.insight_humidity_body, weather.humidityPct),
                    AgroPalette.Sky,
                )
            weather.tempC >= 33 ->
                out += Triple(
                    context.getString(R.string.insight_hot_day_title),
                    context.getString(R.string.insight_hot_day_body, weather.tempC.toInt()),
                    AgroPalette.Orange,
                )
            weather.windKph <= 8 && weather.kind != ConditionKind.Rain ->
                out += Triple(
                    context.getString(R.string.insight_calm_spray_title),
                    context.getString(R.string.insight_calm_spray_body, weather.windKph.toInt()),
                    AgroPalette.Primary,
                )
        }
    }
    if (fields.isNotEmpty()) {
        val avgHealth = fields.map { it.healthScore }.average().toInt()
        val driest = fields.minByOrNull { it.moisturePct }
        if (driest != null && driest.moisturePct < fields.map { it.moisturePct }.average() - 10) {
            out += Triple(
                context.getString(R.string.insight_soil_moisture_title),
                context.getString(R.string.insight_soil_moisture_body, driest.name),
                AgroPalette.Sky,
            )
        }
        if (avgHealth >= 85) {
            out += Triple(
                context.getString(R.string.insight_crops_strong_title),
                context.getString(R.string.insight_crops_strong_body, avgHealth, fields.size),
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSheet(
    alerts: List<AlertItem>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
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
                        if (alerts.isEmpty()) stringResource(R.string.notif_sheet_title_empty) else stringResource(R.string.notif_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (alerts.isEmpty())
                            stringResource(R.string.notif_empty_body)
                        else
                            stringResource(R.string.notif_sheet_subtitle, alerts.size),
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
                            stringResource(R.string.notif_empty_hint),
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

/**
 * Localized weather-condition label. Maps ConditionKind → R.string.weather_condition_*.
 * Used by the Home weather card so the condition text follows the active locale
 * (instead of returning the English-only WMO description from WeatherRepository).
 */
@Composable
private fun localizedConditionLabel(kind: ConditionKind): String = stringResource(when (kind) {
    ConditionKind.Clear -> R.string.weather_condition_clear
    ConditionKind.PartlyCloudy -> R.string.weather_condition_partly_cloudy
    ConditionKind.Cloudy -> R.string.weather_condition_cloudy
    ConditionKind.Rain -> R.string.weather_condition_rain
    ConditionKind.Storm -> R.string.weather_condition_storm
    ConditionKind.Night -> R.string.weather_condition_night
})
