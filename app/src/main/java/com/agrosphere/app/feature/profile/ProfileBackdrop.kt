package com.agrosphere.app.feature.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileBackdrop() {
    val tr = rememberInfiniteTransition(label = "profile-bg")
    val t by tr.animateFloat(
        0f, (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing)),
        label = "t",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF060F0D),
                    0.45f to AgroPalette.BgFarm,
                    1f to AgroPalette.BgDeep,
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ── Orb 1: Emerald, upper-left ───────────────────────────────────
            val ax = w * 0.18f + sin(t) * w * 0.16f
            val ay = h * 0.16f + cos(t * 0.7f) * h * 0.06f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.26f),
                    0.5f to AgroPalette.Primary.copy(alpha = 0.06f),
                    1f to Color.Transparent,
                    center = Offset(ax, ay), radius = w * 0.78f,
                ),
                radius = w * 0.78f, center = Offset(ax, ay),
            )

            // ── Orb 2: Iris, lower-right ─────────────────────────────────────
            val bx = w * 0.85f + cos(t * 0.6f + 1.4f) * w * 0.18f
            val by = h * 0.68f + sin(t * 0.5f + 2f) * h * 0.08f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Iris.copy(alpha = 0.20f),
                    0.6f to AgroPalette.Iris.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                    center = Offset(bx, by), radius = w * 0.72f,
                ),
                radius = w * 0.72f, center = Offset(bx, by),
            )

            // ── Orb 3: Amber, upper-right (smaller accent) ───────────────────
            val cx = w * 0.82f + sin(t * 0.8f + 1f) * w * 0.10f
            val cy = h * 0.12f + cos(t * 0.9f) * h * 0.04f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Amber.copy(alpha = 0.14f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = w * 0.38f,
                ),
                radius = w * 0.38f, center = Offset(cx, cy),
            )

            // ── Flowing mesh lines — 4 slow diagonal passes ──────────────────
            val meshAlphas = listOf(0.04f, 0.03f, 0.05f, 0.03f)
            val meshAngles = listOf(-0.30f, 0.25f, -0.18f, 0.35f)
            val meshSpeeds = listOf(0.12f, 0.09f, 0.15f, 0.07f)
            val meshSpacings = listOf(h * 0.28f, h * 0.42f, h * 0.60f, h * 0.75f)
            meshAlphas.forEachIndexed { i, alpha ->
                val xOffset = ((t / (PI * 2).toFloat() * meshSpeeds[i] * w * 2f + i * w * 0.25f) % (w * 1.5f)) - w * 0.25f
                val yBase = meshSpacings[i]
                val slant = meshAngles[i] * h
                drawLine(
                    color = AgroPalette.Primary.copy(alpha = alpha),
                    start = Offset(xOffset, yBase),
                    end = Offset(xOffset + w * 0.85f, yBase + slant),
                    strokeWidth = 1f,
                    cap = StrokeCap.Round,
                )
            }

            // ── Star field — 35 stars, every 8th gets a glow halo ────────────
            repeat(35) { i ->
                val seed = (i * 137 + 7) % 1000 / 1000f
                val sx = w * (((i * 53) % 100) / 100f)
                val sy = h * 0.42f * (((i * 31) % 90) / 100f)
                val twinkle = 0.4f + 0.6f * (sin(t * 1.6f + seed * 6.28f) * 0.5f + 0.5f)
                val bright = i % 8 == 0
                if (bright) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to AgroPalette.Ink.copy(alpha = 0.35f * twinkle),
                            1f to Color.Transparent,
                            center = Offset(sx, sy), radius = 5f + (i % 3) * 1.5f,
                        ),
                        radius = 5f + (i % 3) * 1.5f, center = Offset(sx, sy),
                    )
                }
                drawCircle(
                    color = AgroPalette.Ink.copy(alpha = (if (bright) 0.18f else 0.08f) + 0.28f * twinkle),
                    radius = if (bright) 1.4f + (i % 3) * 0.3f else 0.7f + (i % 3) * 0.5f,
                    center = Offset(sx, sy),
                )
            }

            // ── Bottom vignette — content lifts above the deep bg ────────────
            drawRect(
                brush = Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to AgroPalette.BgDeep.copy(alpha = 0.55f),
                ),
                size = size,
            )
        }
    }
}
