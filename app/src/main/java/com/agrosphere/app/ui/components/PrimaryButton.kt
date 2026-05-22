package com.agrosphere.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

/**
 * Primary CTA button with a sliding shimmer that sweeps left-to-right every ~3 s.
 * Disabled state shows a dimmed glass surface with no shimmer.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = AgroBrushes.primary,
    onClick: () -> Unit,
) {
    // Shimmer sweeps from -0.5x to 1.5x canvas width over the full cycle.
    // Only visible when centre of band is between ~-0.1x and ~1.1x, so there
    // is a natural ~450 ms "dark" pause at either end of each cycle.
    val inf = rememberInfiniteTransition(label = "btn-shimmer")
    val shimmerX by inf.animateFloat(
        initialValue    = -0.5f,
        targetValue     =  1.5f,
        animationSpec   = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label           = "sx",
    )

    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) brush else SolidColor(AgroPalette.SurfaceGlass))
            .drawWithContent {
                drawContent()
                // Shimmer band drawn on top of the gradient background
                if (enabled) {
                    val bandHalf = size.width * 0.22f
                    val cx       = shimmerX * size.width
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f    to Color.Transparent,
                            0.25f to Color.White.copy(alpha = 0.06f),
                            0.50f to Color.White.copy(alpha = 0.16f),
                            0.75f to Color.White.copy(alpha = 0.06f),
                            1f    to Color.Transparent,
                            startX = cx - bandHalf,
                            endX   = cx + bandHalf,
                        ),
                        topLeft = Offset.Zero,
                        size    = Size(size.width, size.height),
                    )
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AgroPalette.BgDeep)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = if (enabled) AgroPalette.BgDeep else AgroPalette.InkDim,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val inf = rememberInfiniteTransition(label = "ghost-shimmer")
    val shimmerX by inf.animateFloat(
        initialValue  = -0.5f,
        targetValue   =  1.5f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label         = "gsx",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(AgroPalette.SurfaceGlass, shape)
            .drawWithContent {
                drawContent()
                // Hairline at top
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f   to Color.Transparent,
                        0.3f to AgroPalette.Primary.copy(alpha = 0.35f),
                        0.7f to AgroPalette.Primary.copy(alpha = 0.35f),
                        1f   to Color.Transparent,
                    ),
                    start = Offset(0f, 0.7f),
                    end   = Offset(size.width, 0.7f),
                    strokeWidth = 1.1f,
                )
                // Very faint sweeping shimmer
                val bandHalf = size.width * 0.20f
                val cx = shimmerX * size.width
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f    to Color.Transparent,
                        0.50f to Color.White.copy(alpha = 0.04f),
                        1f    to Color.Transparent,
                        startX = cx - bandHalf,
                        endX   = cx + bandHalf,
                    ),
                    topLeft = Offset.Zero,
                    size    = Size(size.width, size.height),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AgroPalette.Ink, style = MaterialTheme.typography.labelLarge)
    }
}
