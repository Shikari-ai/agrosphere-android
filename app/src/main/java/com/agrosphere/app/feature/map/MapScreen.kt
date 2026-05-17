package com.agrosphere.app.feature.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// MapScreen — stylized satellite-style canvas with all fields as rounded
// polygons. Tap a field to focus it in the bottom info card.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun MapScreen(onBack: () -> Unit, onOpenField: (String) -> Unit) {
    val fields by FieldRepository.fields.collectAsState()
    var selected by remember { mutableStateOf(fields.firstOrNull()) }
    var layerStyle by remember { mutableStateOf(LayerStyle.Satellite) }
    var zoom by remember { mutableStateOf(1f) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030A06))) {
        // Map canvas fills the whole screen
        FullMapCanvas(
            fields = fields,
            selectedId = selected?.id,
            layerStyle = layerStyle,
            zoom = zoom,
            onTapField = { id ->
                selected = fields.firstOrNull { it.id == id } ?: selected
            },
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.ArrowBack, onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitle(eyebrow = "Geography", title = "Field map")
            }
        }

        // Right-side action bar
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleIconButton(Icons.Rounded.ZoomIn) { zoom = (zoom * 1.15f).coerceAtMost(2f) }
            CircleIconButton(Icons.Rounded.ZoomOut) { zoom = (zoom / 1.15f).coerceAtLeast(0.6f) }
            CircleIconButton(Icons.Rounded.Layers) {
                layerStyle = LayerStyle.values().let { it[(it.indexOf(layerStyle) + 1) % it.size] }
            }
            CircleIconButton(Icons.Rounded.MyLocation) { zoom = 1f }
        }

        // Bottom info card
        if (selected != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                SelectedFieldCard(field = selected!!, layerStyle = layerStyle, onOpen = { onOpenField(selected!!.id) })
            }
        } else if (fields.isEmpty()) {
            // Friendly empty state in the centre of the map
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            ) {
                GlassCard(radius = 22.dp, padding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.MyLocation, null, tint = AgroPalette.Sky, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No fields to map yet", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                        Spacer(Modifier.height(4.dp))
                        Text("Add a field from the Fields tab — it'll appear here.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
        }
    }
}

private enum class LayerStyle(val label: String, val groundTop: Color, val groundBottom: Color) {
    Satellite("Satellite", Color(0xFF0A1F14), Color(0xFF050E08)),
    Topographic("Topographic", Color(0xFF1A1206), Color(0xFF0D0A04)),
    Heatmap("Health heatmap", Color(0xFF0A0E1F), Color(0xFF030610)),
}

// ─── Full-screen map canvas ─────────────────────────────────────────────────
@Composable
private fun FullMapCanvas(
    fields: List<Field>,
    selectedId: String?,
    layerStyle: LayerStyle,
    zoom: Float,
    onTapField: (String) -> Unit,
) {
    val tr = rememberInfiniteTransition(label = "map")
    val sweep by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "sweep")

    // Cell layout for field positions — fractional (0..1) over canvas
    val cells = remember {
        listOf(
            Offset(0.25f, 0.30f) to 0.30f,
            Offset(0.68f, 0.24f) to 0.26f,
            Offset(0.38f, 0.62f) to 0.28f,
            Offset(0.74f, 0.66f) to 0.24f,
        )
    }

    val centerCanvas = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = true) { /* prevent click pass-through; field taps handled below */ },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            centerCanvas.value = Offset(w / 2, h / 2)

            // Background
            drawRect(brush = Brush.verticalGradient(listOf(layerStyle.groundTop, layerStyle.groundBottom)))

            // Topographic / satellite grid
            val gridSpacing = 36f / zoom
            var gx = 0f
            while (gx < w) {
                drawLine(color = AgroPalette.Primary.copy(alpha = if (layerStyle == LayerStyle.Heatmap) 0.04f else 0.08f), start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 1f)
                gx += gridSpacing
            }
            var gy = 0f
            while (gy < h) {
                drawLine(color = AgroPalette.Primary.copy(alpha = if (layerStyle == LayerStyle.Heatmap) 0.04f else 0.08f), start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 1f)
                gy += gridSpacing
            }

            // Topo contour squiggles if topographic
            if (layerStyle == LayerStyle.Topographic) {
                listOf(0.25f, 0.45f, 0.65f).forEach { yf ->
                    val baseY = h * yf
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        path.lineTo(x, baseY + sin(x * 0.02f + yf * 12f) * 14f)
                        x += 8f
                    }
                    drawPath(path, color = AgroPalette.Amber.copy(alpha = 0.18f), style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)))
                }
            }

            // Roving sweep line for satellite vibe
            if (layerStyle == LayerStyle.Satellite) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, AgroPalette.Primary.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    start = Offset(0f, h * sweep),
                    end = Offset(w, h * sweep),
                    strokeWidth = 1.5f,
                )
            }

            // Render each field polygon
            fields.forEachIndexed { i, f ->
                val (centerFrac, sizeFrac) = cells[i % cells.size]
                val cx = centerFrac.x * w
                val cy = centerFrac.y * h
                val pw = sizeFrac * w * zoom
                val ph = sizeFrac * w * 0.85f * zoom
                val isSelected = f.id == selectedId
                val color = when (layerStyle) {
                    LayerStyle.Heatmap -> healthHeatColor(f.healthScore)
                    else -> f.accent
                }
                val fillAlpha = if (isSelected) 0.65f else 0.30f
                drawRoundRect(
                    color = color.copy(alpha = fillAlpha),
                    topLeft = Offset(cx - pw / 2, cy - ph / 2),
                    size = GeomSize(pw, ph),
                    cornerRadius = CornerRadius(20f),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - pw / 2, cy - ph / 2),
                    size = GeomSize(pw, ph),
                    cornerRadius = CornerRadius(20f),
                    style = Stroke(width = if (isSelected) 3f else 1.4f, cap = StrokeCap.Round),
                )
                if (isSelected) {
                    drawCircle(color = color, radius = 6f, center = Offset(cx, cy))
                    drawCircle(color = color.copy(alpha = 0.3f), radius = 16f, center = Offset(cx, cy))
                }
            }
        }

        // Invisible tap targets over each field for selection
        Box(modifier = Modifier.fillMaxSize()) {
            // Cannot use canvas hit-testing easily — we overlay simple Box buttons sized to match each cell.
            fields.forEachIndexed { i, f ->
                val (centerFrac, sizeFrac) = cells[i % cells.size]
                FieldHitbox(
                    centerFrac = centerFrac,
                    sizeFrac = sizeFrac,
                    label = f.name,
                    onTap = { onTapField(f.id) },
                )
            }
        }
    }
}

@Composable
private fun FieldHitbox(centerFrac: Offset, sizeFrac: Float, label: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Use BoxWithConstraints would be cleaner; here we use fractional offsets via offset modifier via fillMaxSize wrapper.
        // For simplicity we tap on a small badge floating at the field center.
        FieldLabelBadge(
            centerFracX = centerFrac.x,
            centerFracY = centerFrac.y,
            sizeFrac = sizeFrac,
            label = label,
            onTap = onTap,
        )
    }
}

@Composable
private fun FieldLabelBadge(centerFracX: Float, centerFracY: Float, sizeFrac: Float, label: String, onTap: () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val px = (w * centerFracX).toInt()
        val py = (h * centerFracY).toInt()
        // Place a tiny pill at the center of the field
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(px - 60, py - 12) }
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC0A1118))
                .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                .clickable(onClick = onTap)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink)
        }
    }
}

private fun healthHeatColor(score: Int): Color = when {
    score >= 85 -> AgroPalette.Primary
    score >= 70 -> AgroPalette.Sky
    score >= 55 -> AgroPalette.Amber
    else -> AgroPalette.Rose
}

// ─── Top-bar circular icon button ────────────────────────────────────────────
@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC0A1118))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AgroPalette.Ink, modifier = Modifier.size(20.dp))
    }
}

// ─── Selected-field bottom card ──────────────────────────────────────────────
@Composable
private fun SelectedFieldCard(field: Field, layerStyle: LayerStyle, onOpen: () -> Unit) {
    GlassCard(radius = 22.dp, padding = 16.dp, onClick = onOpen) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(field.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, null, tint = field.accent) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(field.name, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    Text("${field.crop} · ${field.areaHa} ha · ${field.stage}", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${field.healthScore}", style = MaterialTheme.typography.headlineSmall, color = field.accent, fontWeight = FontWeight.Black)
                    Text("health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Layer · ${layerStyle.label}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                Text("Tap to open →", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
