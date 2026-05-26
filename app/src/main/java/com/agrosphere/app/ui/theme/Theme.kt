package com.agrosphere.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AgroColors = darkColorScheme(
    primary = AgroPalette.Primary,
    onPrimary = AgroPalette.BgDeep,
    primaryContainer = AgroPalette.PrimaryDim,
    onPrimaryContainer = AgroPalette.Ink,
    secondary = AgroPalette.Sky,
    onSecondary = AgroPalette.BgDeep,
    tertiary = AgroPalette.Iris,
    onTertiary = AgroPalette.BgDeep,
    background = AgroPalette.BgFarm,
    onBackground = AgroPalette.Ink,
    surface = AgroPalette.Surface,
    onSurface = AgroPalette.Ink,
    surfaceVariant = AgroPalette.SurfaceElev,
    onSurfaceVariant = AgroPalette.InkMuted,
    error = AgroPalette.Rose,
    onError = AgroPalette.Ink,
    outline = AgroPalette.SurfaceGlassBorder,
)

private val AgroLightColors = lightColorScheme(
    primary              = AgroPalette.Primary,
    onPrimary            = Color.White,
    primaryContainer     = AgroPalette.Primary.copy(alpha = 0.14f),
    onPrimaryContainer   = Color(0xFF032918),
    secondary            = AgroPalette.Sky,
    onSecondary          = Color.White,
    tertiary             = AgroPalette.Iris,
    onTertiary           = Color.White,
    background           = Color(0xFFF0FDF4),
    onBackground         = Color(0xFF0A1F0E),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF0A1F0E),
    surfaceVariant       = Color(0xFFE2F5EB),
    onSurfaceVariant     = Color(0xFF2D5A3D),
    error                = AgroPalette.Rose,
    onError              = Color.White,
    outline              = Color(0xFFB4D9C0),
)

val AgroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object AgroBrushes {
    val canvas: Brush
        get() = Brush.verticalGradient(
            0f to AgroPalette.BgFarm,
            0.55f to AgroPalette.BgFarm,
            1f to AgroPalette.BgDeep,
        )
    val primary: Brush
        get() = Brush.linearGradient(
            listOf(AgroPalette.Primary, AgroPalette.Sky)
        )
    val warmCard: Brush
        get() = Brush.linearGradient(
            listOf(AgroPalette.Amber.copy(alpha = 0.18f), AgroPalette.Orange.copy(alpha = 0.08f))
        )
    val coolCard: Brush
        get() = Brush.linearGradient(
            listOf(AgroPalette.Sky.copy(alpha = 0.18f), AgroPalette.Iris.copy(alpha = 0.08f))
        )
    val leafCard: Brush
        get() = Brush.linearGradient(
            listOf(AgroPalette.Primary.copy(alpha = 0.22f), AgroPalette.Sky.copy(alpha = 0.06f))
        )
}

@Composable
fun AgroSphereTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AgroColors else AgroLightColors,
        typography = AgroTypography,
        shapes = AgroShapes,
    ) {
        val bg = if (darkTheme) AgroBrushes.canvas else Brush.verticalGradient(
            0f to Color(0xFFF0FDF4),
            0.6f to Color(0xFFE8F5ED),
            1f to Color(0xFFDCF0E4),
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().background(bg)
        ) { content() }
    }
}
