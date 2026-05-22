package com.agrosphere.app.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun Float.fPow(exp: Double) = toDouble().pow(exp).toFloat()
private fun Float.lerp(to: Float, fraction: Float) = this + (to - this) * fraction
private fun fSin(x: Float) = sin(x.toDouble()).toFloat()
private fun fCos(x: Float) = cos(x.toDouble()).toFloat()

private val SplashBg = Color(0xFF000810)

// ─── Particle data ───────────────────────────────────────────────────────────

private data class SplashParticle(
    val dirX: Float,
    val dirY: Float,
    val speed: Float,
    val size: Float,
    val alphaMult: Float,
    val tint: Color,
)

private fun buildSplashParticles(count: Int): List<SplashParticle> = List(count) { i ->
    val base = i.toDouble() / count * 2.0 * PI
    val jitter = ((i * 23) % 100).toDouble() / 100.0 * 0.45 - 0.225
    val angle = (base + jitter).toFloat()
    val tint = when (i % 5) {
        0, 2 -> AgroPalette.Primary
        1    -> AgroPalette.Sky
        3    -> Color(0xFFB6E85A)          // lime
        else -> AgroPalette.Primary
    }
    SplashParticle(
        dirX      = fCos(angle),
        dirY      = fSin(angle),
        speed     = 0.72f + ((i * 17) % 55).toFloat() / 100f,
        size      = 2.0f + (i % 5) * 0.75f,
        alphaMult = 0.55f + ((i * 31) % 50).toFloat() / 100f,
        tint      = tint,
    )
}

// ─── Draw helpers ────────────────────────────────────────────────────────────

private fun DrawScope.drawSplashStars(t: Float) {
    val w = size.width; val h = size.height
    repeat(64) { i ->
        val seed = ((i * 137 + 7) % 1000).toFloat() / 1000f
        val sx = w * ((i * 53) % 100 / 100f)
        val sy = h * ((i * 31) % 97 / 100f)
        val twinkle = 0.25f + 0.75f * (fSin(t * PI.toFloat() * 2f * 1.3f + seed * 12.56f) * 0.5f + 0.5f)
        drawCircle(
            color  = Color.White.copy(alpha = 0.04f + 0.22f * twinkle),
            radius = 0.55f + (i % 3) * 0.4f,
            center = Offset(sx, sy),
        )
    }
}

private fun DrawScope.drawGlowParticle(x: Float, y: Float, r: Float, alpha: Float, tint: Color) {
    val gr = r * 2.4f
    drawCircle(
        brush  = Brush.radialGradient(
            0f  to tint.copy(alpha = alpha),
            0.4f to tint.copy(alpha = alpha * 0.35f),
            1f  to Color.Transparent,
            center = Offset(x, y), radius = gr,
        ),
        radius = gr,
        center = Offset(x, y),
    )
}

// ─── Main composable ─────────────────────────────────────────────────────────

/**
 * Cinematic launch splash:
 *  1. Starfield  2. Particle burst from center  3. Particles converge onto globe surface
 *  4. Globe materialises  5. Orbit ring sweeps  6. Shockwave rings + HUD scan line
 *  7. Brand text reveals  8. Hold  9. Radial-flash portal exit → [onDone]
 */
@Composable
fun AgroSplashScreen(onDone: () -> Unit) {

    // ── Phase state machine ────────────────────────────────────────────────
    // 0 = initial, 1 = burst, 2 = converge+globe, 3 = ring+glow,
    // 4 = shockwave+scan+text, 5 = hold, 6 = exit-flash
    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(60);  phase = 1   // burst
        delay(430); phase = 2   // converge
        delay(680); phase = 3   // ring + glow
        delay(520); phase = 4   // shock + scan + text
        delay(680); phase = 5   // hold
        delay(400); phase = 6   // exit
        delay(380); onDone()
    }

    // ── Animated values ────────────────────────────────────────────────────
    val burstP    by animateFloatAsState(if (phase >= 1) 1f else 0f, tween(370, easing = FastOutLinearInEasing), label = "burst")
    val convergeP by animateFloatAsState(if (phase >= 2) 1f else 0f, tween(650, easing = LinearOutSlowInEasing), label = "conv")
    val globeP    by animateFloatAsState(if (phase >= 2) 1f else 0f, tween(500, delayMillis = 180, easing = FastOutSlowInEasing), label = "globe")
    val ringP     by animateFloatAsState(if (phase >= 3) 1f else 0f, tween(900, easing = LinearOutSlowInEasing), label = "ring")
    val glowP     by animateFloatAsState(if (phase >= 3) 1f else 0f, tween(600), label = "glow")
    val shockP    by animateFloatAsState(if (phase >= 4) 1f else 0f, tween(480, easing = FastOutLinearInEasing), label = "shock")
    val scanP     by animateFloatAsState(if (phase >= 4) 1f else 0f, tween(520, delayMillis = 40, easing = LinearEasing), label = "scan")
    val textP     by animateFloatAsState(if (phase >= 4) 1f else 0f, tween(520, delayMillis = 180), label = "text")
    val flashP    by animateFloatAsState(if (phase >= 6) 1f else 0f, tween(190), label = "flash")
    val fadeP     by animateFloatAsState(if (phase >= 6) 0f else 1f, tween(380, delayMillis = 210), label = "fade")

    // ── Stable particle list ───────────────────────────────────────────────
    val particles = remember { buildSplashParticles(56) }

    // ── Infinite transitions ───────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "splash-inf")
    val starT  by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6500, easing = LinearEasing)), label = "st")
    val pulseT by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "pt")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fadeP }
            .background(SplashBg),
        contentAlignment = Alignment.Center,
    ) {

        // ── Background canvas (stars, particles, shockwave, scan, flash) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val globeR = size.minDimension * 0.185f

            // Starfield
            drawSplashStars(starT)

            // Particles — burst then converge
            val particlesVisible = burstP > 0.01f && (phase < 3 || globeP < 0.92f)
            if (particlesVisible) {
                val maxBurst = globeR * 2.65f
                particles.forEach { p ->
                    val bx = cx + p.dirX * maxBurst * burstP * p.speed
                    val by = cy + p.dirY * maxBurst * burstP * p.speed
                    val tx = cx + p.dirX * globeR
                    val ty = cy + p.dirY * globeR

                    val (px, py, pa) = if (phase < 2) {
                        // Still bursting outward
                        Triple(
                            cx.lerp(bx, burstP),
                            cy.lerp(by, burstP),
                            burstP * p.alphaMult * 0.9f,
                        )
                    } else {
                        // Converging toward globe surface
                        val cp = convergeP.fPow(1.35)
                        Triple(
                            bx.lerp(tx, cp),
                            by.lerp(ty, cp),
                            (1f - cp * 0.75f) * p.alphaMult * (1f - globeP * 0.97f),
                        )
                    }

                    if (pa > 0.015f) {
                        val r = p.size * (1f + (1f - convergeP) * 1.5f)
                        drawGlowParticle(px, py, r, pa, p.tint)
                    }
                }
            }

            // Globe halo glow
            if (glowP > 0.01f) {
                val pulse = fSin(pulseT * PI.toFloat() * 2f) * 0.18f + 0.82f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f   to AgroPalette.Primary.copy(alpha = 0f),
                        0.35f to AgroPalette.Primary.copy(alpha = 0.14f * glowP * pulse),
                        0.65f to AgroPalette.Primary.copy(alpha = 0.05f * glowP),
                        1f   to Color.Transparent,
                        center = Offset(cx, cy), radius = globeR * 3.6f,
                    ),
                    radius = globeR * 3.6f,
                    center = Offset(cx, cy),
                )
            }

            // Shockwave rings
            if (shockP > 0.01f) {
                val s1 = shockP.fPow(0.55)
                val a1 = (1f - shockP) * 0.75f
                if (a1 > 0.01f) {
                    drawCircle(
                        color  = AgroPalette.Primary.copy(alpha = a1),
                        radius = s1 * size.minDimension * 0.82f,
                        center = Offset(cx, cy),
                        style  = Stroke(width = 2.8f * (1f - s1 * 0.6f)),
                    )
                }
                val raw2 = ((shockP - 0.22f) / 0.78f).coerceIn(0f, 1f)
                if (raw2 > 0.01f) {
                    val s2 = raw2.fPow(0.55)
                    drawCircle(
                        color  = AgroPalette.Sky.copy(alpha = (1f - raw2) * 0.4f),
                        radius = s2 * size.minDimension * 0.65f,
                        center = Offset(cx, cy),
                        style  = Stroke(width = 1.6f),
                    )
                }
                // Third faint ring
                val raw3 = ((shockP - 0.45f) / 0.55f).coerceIn(0f, 1f)
                if (raw3 > 0.01f) {
                    val s3 = raw3.fPow(0.55)
                    drawCircle(
                        color  = AgroPalette.Primary.copy(alpha = (1f - raw3) * 0.20f),
                        radius = s3 * size.minDimension * 0.50f,
                        center = Offset(cx, cy),
                        style  = Stroke(width = 1.2f),
                    )
                }
            }

            // HUD horizontal scan line
            if (scanP in 0.01f..0.99f) {
                val scanY = scanP * size.height
                val sFade = if (scanP < 0.5f) scanP * 2f else (1f - scanP) * 2f
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f   to Color.Transparent,
                        0.12f to AgroPalette.Primary.copy(alpha = 0.40f * sFade),
                        0.5f to AgroPalette.Primary.copy(alpha = 0.72f * sFade),
                        0.88f to AgroPalette.Primary.copy(alpha = 0.40f * sFade),
                        1f   to Color.Transparent,
                    ),
                    start = Offset(0f, scanY),
                    end   = Offset(size.width, scanY),
                    strokeWidth = 1.8f,
                )
                // Soft glow trail behind the scan
                val trailHeight = (size.height * 0.08f).coerceAtMost(scanY)
                if (trailHeight > 2f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to AgroPalette.Primary.copy(alpha = 0.025f * sFade),
                            startY = scanY - trailHeight,
                            endY   = scanY,
                        ),
                        topLeft = Offset(0f, scanY - trailHeight),
                        size    = Size(size.width, trailHeight),
                    )
                }
            }

            // Exit radial flash (portal opening effect)
            if (flashP > 0.01f) {
                val fR = flashP.fPow(0.6) * size.minDimension * 1.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f   to Color.White.copy(alpha = flashP * 0.85f),
                        0.25f to AgroPalette.Primary.copy(alpha = flashP * 0.55f),
                        0.7f to AgroPalette.Primary.copy(alpha = flashP * 0.08f),
                        1f   to Color.Transparent,
                        center = Offset(cx, cy), radius = fR,
                    ),
                    radius = fR,
                    center = Offset(cx, cy),
                )
            }
        }

        // ── Emblem + brand text (drawn as Compose layer over Canvas) ──────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            AgroSphereEmblem(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = globeP
                        scaleY = globeP
                        alpha  = globeP
                    },
                ringProgress = ringP,
                leafScale    = globeP,
            )

            Spacer(Modifier.height(22.dp))

            // Brand text — fades in with textP
            Column(
                modifier               = Modifier.graphicsLayer { alpha = textP },
                horizontalAlignment    = Alignment.CenterHorizontally,
            ) {
                Text(
                    "AgroSphere",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = AgroPalette.Ink,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "INTELLIGENCE  ·  SUSTAINABILITY  ·  GROWTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.8.sp,
                        fontSize      = 9.sp,
                    ),
                    color = AgroPalette.Primary,
                )
            }
        }
    }
}
