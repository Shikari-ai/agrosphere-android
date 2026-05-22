package com.agrosphere.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val IntroBg = Color(0xFF000A10)

// ─── Data stream background ──────────────────────────────────────────────────

private fun DrawScope.drawDataStreams(t: Float) {
    val w = size.width
    val h = size.height
    val cols = 18
    repeat(cols) { col ->
        val x = w * (col.toFloat() / cols) + w * 0.025f
        val speed = 0.55f + ((col * 17) % 40).toFloat() / 100f
        val phase = ((col * 37) % 100).toFloat() / 100f
        val baseY = ((t * speed + phase) % 1f) * h
        val dotsPerStream = 14
        repeat(dotsPerStream) { dot ->
            val dy = dot.toFloat() * h * 0.075f
            val y = (baseY - dy + h * 2f) % h
            val fade = 1f - dot.toFloat() / dotsPerStream.toFloat()
            val dotAlpha = fade * 0.055f
            drawCircle(
                color  = AgroPalette.Primary.copy(alpha = dotAlpha),
                radius = 1.4f,
                center = Offset(x, y),
            )
        }
    }
}

// ─── HUD corner brackets ─────────────────────────────────────────────────────

private fun DrawScope.drawHudCorners(alpha: Float, glowAlpha: Float) {
    val w = size.width
    val h = size.height
    val arm = 18f
    val thick = 1.8f
    val inset = 0f
    val color = AgroPalette.Primary.copy(alpha = alpha)
    val corners = listOf(
        Offset(inset, inset)                    to Pair( 1,  1),
        Offset(w - inset, inset)                to Pair(-1,  1),
        Offset(inset, h - inset)                to Pair( 1, -1),
        Offset(w - inset, h - inset)            to Pair(-1, -1),
    )
    corners.forEach { (o, dir) ->
        val (dx, dy) = dir
        drawLine(color, o, Offset(o.x + arm * dx, o.y), thick)
        drawLine(color, o, Offset(o.x, o.y + arm * dy), thick)
    }
    // Subtle glow halo on corners
    if (glowAlpha > 0.01f) {
        corners.forEach { (o, _) ->
            drawCircle(
                brush  = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = glowAlpha * 0.35f),
                    1f to Color.Transparent,
                    center = o, radius = 18f,
                ),
                radius = 18f,
                center = o,
            )
        }
    }
}

// ─── Plasma button ring ───────────────────────────────────────────────────────

@Composable
private fun PlasmaRing(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "plasma")
    val rot1 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "r1")
    val rot2 by inf.animateFloat(0f, -360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "r2")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1600)), label = "p")

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r1 = size.minDimension / 2f - 3f
        val r2 = size.minDimension / 2f - 8f

        // Outer rotating arc
        val arcSweep = 220f
        val r1Rad = rot1 * PI.toFloat() / 180f
        drawArc(
            brush      = Brush.sweepGradient(
                0f   to AgroPalette.Primary.copy(alpha = 0f),
                0.4f to AgroPalette.Primary.copy(alpha = 0.5f * pulse),
                0.7f to AgroPalette.Primary,
                1f   to AgroPalette.Sky,
            ),
            startAngle = rot1,
            sweepAngle = arcSweep,
            useCenter  = false,
            topLeft    = Offset(cx - r1, cy - r1),
            size       = Size(r1 * 2f, r1 * 2f),
            style      = Stroke(width = 2.2f),
        )
        // Inner counter-rotating dots
        repeat(4) { i ->
            val a = rot2 * PI.toFloat() / 180f + i * PI.toFloat() / 2f
            val dx = cos(a.toDouble()).toFloat() * r2
            val dy = sin(a.toDouble()).toFloat() * r2
            drawCircle(
                color  = AgroPalette.Sky.copy(alpha = 0.6f * pulse),
                radius = 2.5f,
                center = Offset(cx + dx, cy + dy),
            )
        }
        // Glow behind dots (larger circles)
        repeat(2) { i ->
            val a = (rot1 * 0.5f + i * 180f) * PI.toFloat() / 180f
            val dx = cos(a.toDouble()).toFloat() * (r1 - 2f)
            val dy = sin(a.toDouble()).toFloat() * (r1 - 2f)
            drawCircle(
                brush  = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.45f * pulse),
                    1f to Color.Transparent,
                    center = Offset(cx + dx, cy + dy), radius = 8f,
                ),
                radius = 8f,
                center = Offset(cx + dx, cy + dy),
            )
        }
    }
}

// ─── Feature row ─────────────────────────────────────────────────────────────

@Composable
private fun IntroFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val inf = rememberInfiniteTransition(label = "row-glow")
    val glow by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(2600)), label = "g")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(18.dp))
            .drawBehind {
                // Top neon hairline
                val mid = size.width / 2f
                val len = size.width * 0.6f
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            iconTint.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        startX = mid - len / 2f, endX = mid + len / 2f,
                    ),
                    start = Offset(mid - len / 2f, 0f),
                    end   = Offset(mid + len / 2f, 0f),
                    strokeWidth = 1.3f,
                )
                // Subtle bottom-right glow
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to iconTint.copy(alpha = 0.12f * glow),
                        1f to Color.Transparent,
                        center = Offset(size.width + 20f, size.height + 20f),
                        radius = 120f,
                    ),
                    radius = 120f,
                    center = Offset(size.width + 20f, size.height + 20f),
                )
                // HUD corners
                drawHudCorners(alpha = 0.3f, glowAlpha = 0.12f * glow)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon container with radial glow
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.radialGradient(
                        0f to iconTint.copy(alpha = 0.22f),
                        1f to iconTint.copy(alpha = 0.06f),
                    ),
                    CircleShape,
                )
                .border(1.dp, iconTint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyLarge,
                color      = AgroPalette.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
        }

        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint   = iconTint.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── Main screen ─────────────────────────────────────────────────────────────

/**
 * First-launch immersive intro — shown once after the user signs in.
 *
 *  • Data-stream animated background
 *  • Emblem scales in with plasma burst
 *  • Feature cards drop in one-by-one with spring physics + bounce
 *  • "Get Started" button wrapped in a rotating plasma ring
 *  • Tap → scale-contract + fade exit, then [onGetStarted] fires
 */
@Composable
fun AgroIntroScreen(onGetStarted: () -> Unit) {
    var exiting by remember { mutableStateOf(false) }
    var r1on    by remember { mutableStateOf(false) }
    var r2on    by remember { mutableStateOf(false) }
    var r3on    by remember { mutableStateOf(false) }
    var btnOn   by remember { mutableStateOf(false) }
    var emblOn  by remember { mutableStateOf(false) }

    // Exit animations
    val exitScale by animateFloatAsState(
        if (exiting) 0.90f else 1f,
        tween(320, easing = FastOutSlowInEasing),
        label = "es",
    )
    val exitAlpha by animateFloatAsState(
        if (exiting) 0f else 1f,
        tween(320),
        finishedListener = { if (exiting) onGetStarted() },
        label = "ea",
    )

    // Entrance: staggered
    LaunchedEffect(Unit) {
        delay(80);  emblOn = true
        delay(320); r1on   = true
        delay(150); r2on   = true
        delay(150); r3on   = true
        delay(180); btnOn  = true
    }

    // Emblem
    val emblScale by animateFloatAsState(if (emblOn) 1f else 0.75f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "emS")
    val emblAlpha by animateFloatAsState(if (emblOn) 1f else 0f,     tween(450),                                                         label = "emA")

    // Feature row physics drops (individual spring per row)
    val dy1 by animateFloatAsState(if (r1on) 0f else -90f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "dy1")
    val dy2 by animateFloatAsState(if (r2on) 0f else -90f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "dy2")
    val dy3 by animateFloatAsState(if (r3on) 0f else -90f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "dy3")
    val a1  by animateFloatAsState(if (r1on) 1f else 0f,   tween(380),                                                         label = "a1")
    val a2  by animateFloatAsState(if (r2on) 1f else 0f,   tween(380),                                                         label = "a2")
    val a3  by animateFloatAsState(if (r3on) 1f else 0f,   tween(380),                                                         label = "a3")

    // Button
    val btnA by animateFloatAsState(if (btnOn) 1f else 0f, tween(480), label = "ba")
    val btnY by animateFloatAsState(if (btnOn) 0f else 40f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "by")

    // Infinite transition — data streams + emblem pulse
    val inf = rememberInfiniteTransition(label = "intro-inf")
    val streamT by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "stT")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = exitScale
                scaleY = exitScale
                alpha  = exitAlpha
            }
            .background(IntroBg),
    ) {
        // Data stream backdrop
        Canvas(modifier = Modifier.fillMaxSize()) { drawDataStreams(streamT) }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(8.dp))

            // Emblem + brand
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = emblScale
                    scaleY = emblScale
                    alpha  = emblAlpha
                },
                contentAlignment = Alignment.Center,
            ) {
                // Emblem glow halo
                Canvas(modifier = Modifier.size(220.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f   to AgroPalette.Primary.copy(alpha = 0f),
                            0.3f to AgroPalette.Primary.copy(alpha = 0.14f * emblAlpha),
                            0.7f to AgroPalette.Primary.copy(alpha = 0.04f * emblAlpha),
                            1f   to Color.Transparent,
                            center = Offset(cx, cy), radius = size.minDimension / 2f,
                        ),
                        radius = size.minDimension / 2f,
                        center = Offset(cx, cy),
                    )
                }
                AgroSphereEmblem(
                    modifier   = Modifier.size(160.dp),
                    ringProgress = 1f,
                    leafScale  = 1f,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "AgroSphere",
                style      = MaterialTheme.typography.displaySmall,
                color      = AgroPalette.Ink,
                fontWeight = FontWeight.Black,
                modifier   = Modifier.graphicsLayer { alpha = emblAlpha },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Intelligence · Sustainability · Growth",
                style      = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.4.sp),
                color      = AgroPalette.Primary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.graphicsLayer { alpha = emblAlpha },
            )

            Spacer(Modifier.height(30.dp))

            // Feature cards with physics drops
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IntroFeatureRow(
                    icon     = Icons.Rounded.AutoAwesome,
                    iconTint = AgroPalette.Primary,
                    title    = "AI Farm Assistant",
                    subtitle = "Ask anything — crops, weather, pests, market prices.",
                    modifier = Modifier.graphicsLayer {
                        translationY = dy1
                        alpha        = a1
                    },
                )
                IntroFeatureRow(
                    icon     = Icons.Rounded.BugReport,
                    iconTint = AgroPalette.Amber,
                    title    = "Disease & Pest Detection",
                    subtitle = "Photograph a leaf — get an instant expert diagnosis.",
                    modifier = Modifier.graphicsLayer {
                        translationY = dy2
                        alpha        = a2
                    },
                )
                IntroFeatureRow(
                    icon     = Icons.Rounded.Cloud,
                    iconTint = AgroPalette.Sky,
                    title    = "Live Weather Intelligence",
                    subtitle = "Hyperlocal forecasts with irrigation timing alerts.",
                    modifier = Modifier.graphicsLayer {
                        translationY = dy3
                        alpha        = a3
                    },
                )
            }

            Spacer(Modifier.weight(1f))

            // Get Started button with plasma ring
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = btnY
                        alpha        = btnA
                    }
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Plasma ring sized larger than button
                PlasmaRing(modifier = Modifier.size(300.dp))

                // Actual button — clip and draw inside the ring space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AgroPalette.Primary, AgroPalette.Sky)
                            ),
                            RoundedCornerShape(20.dp),
                        )
                        .clickable { exiting = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Get Started",
                            style      = MaterialTheme.typography.titleMedium,
                            color      = AgroPalette.BgDeep,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint   = AgroPalette.BgDeep,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
