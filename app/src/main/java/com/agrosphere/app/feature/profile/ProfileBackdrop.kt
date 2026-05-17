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
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Subtle atmospheric backdrop for the profile screens.
 *
 * Two slow-drifting radial orbs (emerald + iris) over a vertical gradient,
 * plus a sparse star layer at the top. Quieter than the auth backdrop so it
 * never fights with the menu content.
 */
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
                    0f to Color(0xFF071210),
                    0.55f to AgroPalette.BgFarm,
                    1f to AgroPalette.BgDeep,
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Emerald orb, upper-left
            val ax = w * 0.20f + sin(t) * w * 0.18f
            val ay = h * 0.18f + cos(t * 0.7f) * h * 0.06f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Primary.copy(alpha = 0.22f),
                    0.55f to AgroPalette.Primary.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                    center = Offset(ax, ay),
                    radius = w * 0.75f,
                ),
                radius = w * 0.75f,
                center = Offset(ax, ay),
            )
            // Iris orb, lower-right
            val bx = w * 0.85f + cos(t * 0.6f + 1.4f) * w * 0.20f
            val by = h * 0.70f + sin(t * 0.5f + 2f) * h * 0.08f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AgroPalette.Iris.copy(alpha = 0.16f),
                    0.6f to AgroPalette.Iris.copy(alpha = 0.04f),
                    1f to Color.Transparent,
                    center = Offset(bx, by),
                    radius = w * 0.7f,
                ),
                radius = w * 0.7f,
                center = Offset(bx, by),
            )

            // Star dust — top third only
            repeat(22) { i ->
                val seed = (i * 137 + 7) % 1000 / 1000f
                val sx = w * (((i * 53) % 100) / 100f)
                val sy = h * 0.35f * (((i * 31) % 90) / 100f)
                val twinkle = 0.4f + 0.6f * (sin(t * 1.6f + seed * 6.28f) * 0.5f + 0.5f)
                drawCircle(
                    color = AgroPalette.Ink.copy(alpha = 0.10f + 0.30f * twinkle),
                    radius = 0.9f + (i % 3) * 0.5f,
                    center = Offset(sx, sy),
                )
            }
        }
    }
}
