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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// RegionalScreen — anonymous farmer-network signals.
//
// The real feature needs at least three things wired up:
//  · a Firestore collection of anonymized farm signals
//  · spatial queries (GeoFirestore / S2) so we only pull nearby reports
//  · explicit opt-in consent flow (PII-clean schema)
// Until those are in place we'd be inventing data — which doesn't help anyone.
// So the screen ships as a clear coming-soon with a working opt-in placeholder.
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
            BetaChip()
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { RadarHero() }
            item {
                GlassCard(background = AgroBrushes.coolCard, radius = 22.dp, padding = 22.dp) {
                    Column {
                        Text("Anonymous farmer network", style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Coming with the v1.1 backend update. AgroSphere will surface signals from farms within ~12 km of you — leaf rust outbreaks, irrigation timing, harvest windows — all stripped of personally-identifiable info before they leave any device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
            }
            item { Text("What you'll see", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            item { FeatureRow("Pest & disease spread", "Spotted on 5 farms within 8 km this week.") }
            item { FeatureRow("Region-wide spray windows", "Wind-calm hours aggregated across nearby farms.") }
            item { FeatureRow("Phenology cohort", "When farms in your corridor are sowing / harvesting.") }
            item { FeatureRow("Aquifer recovery", "Average borewell depth trend across the catchment.") }

            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "Join the wait-list (opt-in)",
                    icon = Icons.Rounded.Public,
                    onClick = { /* future: write opt-in flag to user doc */ },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nothing is shared until you tap Join — and even then we publish only stripped, aggregated signals.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkDim,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

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
        Text("BETA SOON", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp), color = AgroPalette.Iris, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RadarHero() {
    val tr = rememberInfiniteTransition(label = "radar")
    val sweep by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "s")
    val pulse by tr.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1800)), label = "p")

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
                    Text("0 farms connected · waiting for backend", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

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
