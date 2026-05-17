package com.agrosphere.app.feature.regional

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// RegionalScreen — anonymous, opt-in farmer network. Shows what's happening
// on nearby farms (leaf rust, irrigation, harvest windows) so each farmer
// gets early warning + shared signal.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun RegionalScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitle(eyebrow = "Network", title = "Regional intelligence")
            }
            PrivacyChip()
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { RegionalMapHero() }
            item { ConnectedStats() }
            item { Text("Live signals nearby", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            items(signals()) { s -> SignalCard(s) }
            item { Text("This week — your region", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            item { WeeklyOutlook() }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PrivacyChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Primary.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Shield, null, tint = AgroPalette.Primary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("ANONYMOUS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp), color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RegionalMapHero() {
    val tr = rememberInfiniteTransition(label = "radar")
    val pulse by tr.animateFloat(0.4f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "p")
    val sweep by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "s")

    GlassCard(background = AgroBrushes.coolCard, radius = 24.dp, padding = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.4f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2
                val cy = h / 2
                val r = (minOf(w, h)) / 2 * 0.85f
                // concentric range rings
                listOf(0.35f, 0.65f, 1f).forEach { f ->
                    drawCircle(
                        color = AgroPalette.Sky.copy(alpha = 0.2f),
                        radius = r * f,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                    )
                }
                // sweep
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
                // network nodes
                listOf(
                    Triple(0.18f, 0.30f, AgroPalette.Primary),
                    Triple(0.78f, 0.25f, AgroPalette.Amber),
                    Triple(0.35f, 0.72f, AgroPalette.Primary),
                    Triple(0.70f, 0.68f, AgroPalette.Rose),
                    Triple(0.55f, 0.42f, AgroPalette.Primary),
                    Triple(0.42f, 0.48f, AgroPalette.Sky),
                ).forEach { (fx, fy, c) ->
                    val x = w * fx
                    val y = h * fy
                    drawCircle(color = c.copy(alpha = 0.25f), radius = 8f * pulse, center = Offset(x, y))
                    drawCircle(color = c, radius = 4f, center = Offset(x, y))
                }
                // your-farm marker (center)
                drawCircle(color = AgroPalette.Ink, radius = 6f, center = Offset(cx, cy))
            }
            // overlay label
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
                    Text("12 km radius · 23 farms", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ConnectedStats() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatChip("23", "Farms sharing", AgroPalette.Primary, Modifier.weight(1f))
        StatChip("4", "Active alerts", AgroPalette.Amber, Modifier.weight(1f))
        StatChip("96%", "Privacy", AgroPalette.Sky, Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = tint, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
        }
    }
}

private data class Signal(
    val title: String,
    val body: String,
    val tag: String,
    val icon: ImageVector,
    val tint: Color,
    val distance: String,
)

private fun signals(): List<Signal> = listOf(
    Signal(
        "Leaf rust spreading north",
        "5 farms within 8 km flagged wheat leaf rust this week. Watch the windward edge of your fields.",
        "DISEASE", Icons.Rounded.BugReport, AgroPalette.Rose, "≈ 8 km",
    ),
    Signal(
        "Optimal spray window — Tue dawn",
        "9 farms scheduled foliar passes for Tuesday morning. Winds drop region-wide between 4–7 AM.",
        "WINDOW", Icons.Rounded.Bolt, AgroPalette.Primary, "your area",
    ),
    Signal(
        "Aquifer levels recovering",
        "Average measured borewell depth across 14 farms improved 2.1 m after the monsoon system passed.",
        "WATER", Icons.Rounded.WaterDrop, AgroPalette.Sky, "12 km radius",
    ),
    Signal(
        "Sowing began on Riverside corridor",
        "3 farms started kharif sowing this week. AgroSphere will adjust your reference cohort accordingly.",
        "PHENOLOGY", Icons.Rounded.Public, AgroPalette.Iris, "≈ 6 km",
    ),
)

@Composable
private fun SignalCard(s: Signal) {
    GlassCard(radius = 18.dp, padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(s.tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(s.icon, null, tint = s.tint) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.tag, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.sp), color = s.tint, fontWeight = FontWeight.Bold)
                        Text("  ·  ", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                        Text(s.distance, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(s.body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
        }
    }
}

@Composable
private fun WeeklyOutlook() {
    GlassCard(radius = 18.dp) {
        Column {
            val rows = listOf(
                "Wheat yield" to "+3.4% vs last season — based on local cohort",
                "Pest pressure" to "Trending up — aphids spotted on 7 farms",
                "Best harvest window" to "Late next week — most farms plan to harvest Mon/Tue",
                "Input cost index" to "Stable — local fertilizer prices flat WoW",
            )
            rows.forEachIndexed { i, (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                }
                if (i != rows.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AgroPalette.SurfaceGlassBorder))
            }
        }
    }
}
