package com.agrosphere.app.feature.regional

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.repo.NeighbourAlert
import com.agrosphere.app.data.repo.RegionalPest
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// RegionalScreen — anonymous farmer-network signals.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun RegionalScreen(
    onBack: () -> Unit,
    vm: RegionalViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    // Handle errorToast auto-clear
    val readyState = state as? RegionalUiState.Ready
    val errorToast = readyState?.errorToast
    if (errorToast != null) {
        LaunchedEffect(errorToast) {
            delay(2_500)
            vm.clearToast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // ── Top bar (always visible) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitle(eyebrow = "Network", title = "Regional intelligence")
            }
            BetaChip()
        }

        // ── Body ────────────────────────────────────────────────────────────
        when (val s = state) {
            is RegionalUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AgroPalette.Primary)
                }
            }

            is RegionalUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.InkMuted,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = "Retry",
                            icon = Icons.Rounded.Refresh,
                            modifier = Modifier.padding(horizontal = 48.dp),
                            onClick = vm::retry,
                        )
                    }
                }
            }

            is RegionalUiState.Ready -> {
                ReadyContent(state = s, vm = vm)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ready content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(state: RegionalUiState.Ready, vm: RegionalViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Radar hero with dynamic farm count
        item {
            RadarHero(connectedFarms = state.connectedFarms)
        }

        if (!state.optedIn) {
            // ── Not opted in: coming-soon card + feature rows + join button ─
            item {
                GlassCard(background = AgroBrushes.coolCard, radius = 22.dp, padding = 22.dp) {
                    Column {
                        Text(
                            "Anonymous farmer network",
                            style = MaterialTheme.typography.titleLarge,
                            color = AgroPalette.Ink,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Coming with the v1.1 backend update. AgroSphere will surface signals from farms within ~12 km of you — leaf rust outbreaks, irrigation timing, harvest windows — all stripped of personally-identifiable info before they leave any device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
            }
            item {
                Text(
                    "What you'll see",
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Ink,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            item { FeatureRow("Pest & disease spread", "Spotted on 5 farms within 8 km this week.") }
            item { FeatureRow("Region-wide spray windows", "Wind-calm hours aggregated across nearby farms.") }
            item { FeatureRow("Phenology cohort", "When farms in your corridor are sowing / harvesting.") }
            item { FeatureRow("Aquifer recovery", "Average borewell depth trend across the catchment.") }
            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "Join the wait-list (opt-in)",
                    icon = Icons.Rounded.Public,
                    enabled = !state.joiningInProgress,
                    onClick = vm::joinWaitList,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nothing is shared until you tap Join — and even then we publish only stripped, aggregated signals.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkDim,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            // ── Opted in ────────────────────────────────────────────────────

            // Contributing status + opt-out
            item { ContributingCard(joiningInProgress = state.joiningInProgress, onLeave = vm::leaveNetwork) }

            if (state.pests.isEmpty()) {
                item {
                    GlassCard(background = AgroBrushes.coolCard, radius = 22.dp, padding = 22.dp) {
                        Column {
                            Text(
                                "Network active",
                                style = MaterialTheme.typography.titleLarge,
                                color = AgroPalette.Ink,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Signals will appear here as farms in your region contribute scans.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AgroPalette.InkMuted,
                            )
                        }
                    }
                }
            } else {
                // Neighbour alert banner (if present)
                state.neighbourAlert?.let { alert ->
                    item { NeighbourAlertBanner(alert) }
                }
                // Per-pest signal cards
                items(state.pests) { pest ->
                    PestSignalCard(pest)
                }
            }
        }

        // Toast overlay (rendered as an inline item at bottom for simplicity)
        state.errorToast?.let { toast ->
            item {
                GlassCard(
                    background = Brush.linearGradient(
                        listOf(AgroPalette.Rose.copy(alpha = 0.22f), AgroPalette.Rose.copy(alpha = 0.10f))
                    ),
                    radius = 16.dp,
                    padding = 14.dp,
                ) {
                    Text(
                        text = toast,
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.Rose,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pest signal card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PestSignalCard(pest: RegionalPest) {
    val riskColor = when (pest.riskLevel) {
        "high" -> AgroPalette.Rose
        "medium" -> AgroPalette.Amber
        else -> AgroPalette.Primary
    }
    val riskBg = riskColor.copy(alpha = 0.15f)

    GlassCard(radius = 18.dp, padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: label + zone sub-label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pest.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                val zoneLabel = when {
                    pest.inCenter -> "In your zone"
                    pest.inNeighbours -> "In nearby zone"
                    else -> "Regional signal"
                }
                Text(
                    text = zoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Right: confidence % + risk badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${pest.confidencePct}%",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = AgroPalette.InkMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(riskBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(riskColor),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = pest.riskLevel.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Neighbour alert banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeighbourAlertBanner(alert: NeighbourAlert) {
    GlassCard(
        background = Brush.linearGradient(
            listOf(AgroPalette.Amber.copy(alpha = 0.20f), AgroPalette.Orange.copy(alpha = 0.10f))
        ),
        radius = 18.dp,
        padding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = AgroPalette.Amber,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Approaching from a nearby zone",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.Amber,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${alert.label} · ${alert.confidencePct}% confidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BetaChip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BetaChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Iris.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Shield, null, tint = AgroPalette.Iris, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            "BETA SOON",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
            color = AgroPalette.Iris,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RadarHero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RadarHero(connectedFarms: Int) {
    val tr = rememberInfiniteTransition(label = "radar")
    val sweep by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "s")
    val pulse by tr.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1800)), label = "p")

    val statusText = if (connectedFarms > 0) {
        "$connectedFarms farms connected"
    } else {
        "0 farms connected · be first to contribute"
    }

    GlassCard(radius = 22.dp, padding = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = minOf(size.width, size.height) / 2 * 0.85f
                listOf(0.35f, 0.65f, 1f).forEach { f ->
                    drawCircle(
                        color = AgroPalette.Sky.copy(alpha = 0.20f),
                        radius = r * f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f),
                    )
                }
                val rad = sweep * PI.toFloat() / 180f
                drawLine(
                    brush = Brush.linearGradient(
                        0f to AgroPalette.Sky.copy(alpha = 0f),
                        1f to AgroPalette.Sky.copy(alpha = 0.85f),
                        start = Offset(cx, cy), end = Offset(cx + cos(rad) * r, cy + sin(rad) * r),
                    ),
                    start = Offset(cx, cy), end = Offset(cx + cos(rad) * r, cy + sin(rad) * r),
                    strokeWidth = 2f,
                )
                drawCircle(color = AgroPalette.Ink, radius = 6f * pulse, center = Offset(cx, cy))
                drawCircle(color = AgroPalette.Ink, radius = 4f, center = Offset(cx, cy))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xAA000000))
                    .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, null, tint = AgroPalette.Sky, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contributing status card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContributingCard(joiningInProgress: Boolean, onLeave: () -> Unit) {
    GlassCard(
        background = Brush.linearGradient(
            listOf(AgroPalette.Primary.copy(alpha = 0.18f), AgroPalette.Sky.copy(alpha = 0.08f))
        ),
        radius = 18.dp,
        padding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pulsing green dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Primary),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Contributing anonymously",
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Your scans feed the regional network — no farm details leave your device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.Rose.copy(alpha = 0.15f))
                    .clickable(enabled = !joiningInProgress, onClick = onLeave)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (joiningInProgress) "Leaving…" else "Stop",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (joiningInProgress) AgroPalette.InkDim else AgroPalette.Rose,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FeatureRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(title: String, body: String) {
    GlassCard(radius = 18.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Sky)
                    .padding(top = 6.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}
