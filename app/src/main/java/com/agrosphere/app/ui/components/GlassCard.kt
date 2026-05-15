package com.agrosphere.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    background: Brush = SolidColor(AgroPalette.SurfaceGlass),
    border: BorderStroke? = BorderStroke(1.dp, AgroPalette.SurfaceGlassBorder),
    radius: Dp = 22.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var m: Modifier = modifier
        .clip(shape)
        .background(background, shape)
    if (border != null) m = m.border(border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(padding)) { content() }
}
