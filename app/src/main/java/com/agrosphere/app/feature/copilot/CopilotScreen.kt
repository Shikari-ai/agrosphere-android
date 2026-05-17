package com.agrosphere.app.feature.copilot

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import kotlinx.coroutines.delay
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// CopilotScreen — realtime/voice-style assistant. Press the mic to "listen",
// see a simulated live transcript appear, then receive instant action cards
// the user can act on. Distinct from Assistant (chat) — this is "speak +
// get an answer fast" with a live ambient orb.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun CopilotScreen(onBack: () -> Unit) {
    var listening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    val actions = remember { mutableStateListOf<ActionItem>() }
    var hasResponse by remember { mutableStateOf(false) }

    LaunchedEffect(listening) {
        if (listening) {
            // Simulate a live transcript appearing word by word.
            val phrase = "Will tomorrow be a good day to spray fungicide on the wheat?"
            transcript = ""
            phrase.split(' ').forEach { word ->
                delay(160)
                transcript = if (transcript.isEmpty()) word else "$transcript $word"
            }
            delay(420)
            listening = false
            actions.clear()
            actions.addAll(sampleActions())
            hasResponse = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AgroBrushes.canvas)) {
        AmbientWaveBackground(active = listening)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp), color = AgroPalette.Primary)
                    Text("Copilot", style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                }
                LiveBadge(live = listening)
            }

            Spacer(Modifier.height(8.dp))

            // Central orb + status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CenterOrb(active = listening, hasResponse = hasResponse)
                Spacer(Modifier.height(20.dp))
                Text(
                    when {
                        listening -> "Listening…"
                        hasResponse -> "Tap any action below"
                        else -> "Press the mic to speak"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = AgroPalette.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        listening -> transcript.ifEmpty { "Hold your phone close" }
                        hasResponse -> "I prepared what I'd do next — pick one or hold to redo."
                        else -> "Ask about weather, spray timing, irrigation, pests, prices."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroPalette.InkMuted,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action cards (live response)
            if (hasResponse) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(actions) { a -> ActionCard(a) }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Try saying",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = AgroPalette.InkMuted,
                    )
                    suggestions().forEach { s -> SuggestionChip(s) }
                }
            }

            // Mic control bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostButton(
                    text = if (hasResponse) "Ask again" else "Type instead",
                    onClick = {
                        if (hasResponse) {
                            hasResponse = false
                            actions.clear()
                            transcript = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                MicButton(listening = listening, onTap = { listening = !listening })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Visuals
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AmbientWaveBackground(active: Boolean) {
    val tr = rememberInfiniteTransition(label = "ambient")
    val t by tr.animateFloat(
        0f, (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "t",
    )
    val intensity by animateFloatAsState(if (active) 1f else 0.3f, animationSpec = tween(700), label = "i")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f + sin(t) * w * 0.1f
        val cy = h * 0.3f + cos(t * 0.6f) * h * 0.05f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Primary.copy(alpha = 0.25f * intensity),
                0.55f to AgroPalette.Primary.copy(alpha = 0.06f * intensity),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = w * 0.85f,
            ),
            radius = w * 0.85f,
            center = Offset(cx, cy),
        )
        val ix = w * 0.7f + cos(t * 0.7f + 1.5f) * w * 0.12f
        val iy = h * 0.7f + sin(t * 0.5f) * h * 0.08f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Iris.copy(alpha = 0.18f * intensity),
                1f to Color.Transparent,
                center = Offset(ix, iy),
                radius = w * 0.7f,
            ),
            radius = w * 0.7f,
            center = Offset(ix, iy),
        )
    }
}

@Composable
private fun CenterOrb(active: Boolean, hasResponse: Boolean) {
    val tr = rememberInfiniteTransition(label = "orb")
    val pulse by tr.animateFloat(0.85f, 1.18f, infiniteRepeatable(tween(if (active) 600 else 1800)), label = "p")
    val rot by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(if (active) 4_000 else 18_000, easing = LinearEasing)), label = "r")
    val scale by animateFloatAsState(if (active) 1.08f else 1f, animationSpec = tween(400), label = "s")
    val coreTint = when {
        active -> AgroPalette.Primary
        hasResponse -> AgroPalette.Sky
        else -> AgroPalette.Primary
    }

    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        // Outer halo
        Box(
            modifier = Modifier
                .size((160 * pulse).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to coreTint.copy(alpha = 0.30f),
                        0.7f to coreTint.copy(alpha = 0.05f),
                        1f to Color.Transparent,
                    )
                )
        )
        // Rotating sweep ring
        Canvas(modifier = Modifier.size((124 * scale).dp)) {
            withTransform({ rotate(rot) }) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            coreTint.copy(alpha = 0f),
                            coreTint.copy(alpha = 0.85f),
                            AgroPalette.Sky.copy(alpha = 0.55f),
                            coreTint.copy(alpha = 0f),
                        ),
                        center = center,
                    ),
                    radius = size.minDimension / 2 - 1f,
                    style = Stroke(width = 2.4f),
                )
            }
        }
        // Inner core
        Box(
            modifier = Modifier
                .size((88 * scale).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to coreTint.copy(alpha = 0.55f),
                        1f to coreTint.copy(alpha = 0.15f),
                    )
                )
                .border(1.dp, coreTint.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome, null,
                tint = AgroPalette.Ink, modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun LiveBadge(live: Boolean) {
    val tr = rememberInfiniteTransition(label = "live")
    val pulse by tr.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900)), label = "p")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (live) AgroPalette.Rose.copy(alpha = 0.16f) else AgroPalette.SurfaceGlass)
            .border(1.dp, if (live) AgroPalette.Rose.copy(alpha = 0.4f) else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background((if (live) AgroPalette.Rose else AgroPalette.InkMuted).copy(alpha = pulse))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (live) "LIVE" else "READY",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontSize = 9.sp),
            color = if (live) AgroPalette.Rose else AgroPalette.InkMuted,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MicButton(listening: Boolean, onTap: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "mic")
    val pulse by tr.animateFloat(0.7f, 1.2f, infiniteRepeatable(tween(800)), label = "p")
    val color = if (listening) AgroPalette.Rose else AgroPalette.Primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        if (listening) {
            Box(
                modifier = Modifier
                    .size((68 * pulse).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f))
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                null, tint = AgroPalette.BgDeep,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable { /* future: pre-fill transcript */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PlayArrow, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
    }
}

private fun suggestions(): List<String> = listOf(
    "Is tomorrow safe to spray?",
    "Which field needs irrigation today?",
    "Did the storm window shift?",
    "What's the best fertilizer for Lake Plot this week?",
)

// ─────────────────────────────────────────────────────────────────────────────
// Action cards
// ─────────────────────────────────────────────────────────────────────────────
private data class ActionItem(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val tint: Color,
    val primaryAction: String,
)

private fun sampleActions(): List<ActionItem> = listOf(
    ActionItem(
        title = "Spray window — tomorrow morning",
        body = "Wind drops to 6 km/h between 5–8 AM and humidity is 62%. Apply the triazole pass for wheat leaf rust then.",
        icon = Icons.Rounded.Air,
        tint = AgroPalette.Primary,
        primaryAction = "Schedule reminder",
    ),
    ActionItem(
        title = "Skip Sunday afternoon",
        body = "26 mm thunderstorm forecast 2–6 PM Sunday. Anything sprayed gets washed off.",
        icon = Icons.Rounded.Bolt,
        tint = AgroPalette.Rose,
        primaryAction = "Add to calendar",
    ),
    ActionItem(
        title = "Re-scan in 7 days",
        body = "Schedule a follow-up scan on North Paddock to confirm the fungicide is working.",
        icon = Icons.Rounded.BugReport,
        tint = AgroPalette.Amber,
        primaryAction = "Set follow-up",
    ),
)

@Composable
private fun ActionCard(a: ActionItem) {
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(a.tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(a.icon, null, tint = a.tint) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(a.body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(a.tint)
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.primaryAction, style = MaterialTheme.typography.labelMedium, color = AgroPalette.BgDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
