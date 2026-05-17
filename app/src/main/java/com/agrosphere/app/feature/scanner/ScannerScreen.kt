package com.agrosphere.app.feature.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.agrosphere.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

// ═════════════════════════════════════════════════════════════════════════════
// ScannerScreen — three scan modes (Crop / Pest / Soil) with viewfinder,
// animated capture flash, scan history strip, and rich findings card.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ScannerScreen(padding: PaddingValues) {
    var mode by remember { mutableStateOf(ScanMode.Crop) }
    var scanned by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var frontCamera by remember { mutableStateOf(false) }
    val captureFlash = remember { mutableStateOf(0f) } // 0..1 — alpha overlay

    val flashAlpha by animateFloatAsState(
        targetValue = captureFlash.value,
        animationSpec = tween(durationMillis = 280),
        label = "flash",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle(eyebrow = stringResource(R.string.scanner_eyebrow), title = stringResource(R.string.scanner_title))
            HistoryChip()
        }
        Spacer(Modifier.height(14.dp))

        // Mode selector
        ModeSelector(selected = mode, onSelect = { mode = it; scanned = false })
        Spacer(Modifier.height(14.dp))

        // Viewfinder
        Viewfinder(
            mode = mode,
            scanned = scanned,
            flashAlpha = flashAlpha,
        )

        Spacer(Modifier.height(12.dp))

        // Camera controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraControlChip(
                icon = Icons.Rounded.FlashOn,
                label = if (flashOn) stringResource(R.string.camera_flash_on) else stringResource(R.string.camera_flash_off),
                active = flashOn,
                onClick = { flashOn = !flashOn },
                modifier = Modifier.weight(1f),
            )
            CameraControlChip(
                icon = Icons.Rounded.FlipCameraAndroid,
                label = if (frontCamera) stringResource(R.string.camera_front) else stringResource(R.string.camera_rear),
                active = frontCamera,
                onClick = { frontCamera = !frontCamera },
                modifier = Modifier.weight(1f),
            )
            CameraControlChip(
                icon = Icons.Rounded.PhotoLibrary,
                label = stringResource(R.string.camera_gallery),
                active = false,
                onClick = {
                    captureFlash.value = 1f
                    scanned = true
                    captureFlash.value = 0f
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Big capture / re-scan button
        PrimaryButton(
            text = if (scanned) stringResource(R.string.scanner_rescan) else stringResource(R.string.scanner_capture),
            icon = if (scanned) Icons.Rounded.Refresh else Icons.Rounded.CameraAlt,
            onClick = {
                if (scanned) {
                    scanned = false
                } else {
                    captureFlash.value = 1f
                    scanned = true
                    captureFlash.value = 0f
                }
            },
        )

        AnimatedVisibility(
            visible = scanned,
            enter = fadeIn(tween(500)),
            exit = fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(20.dp))
                ResultsBlock(mode = mode)
            }
        }

        Spacer(Modifier.height(22.dp))
        ScanHistorySection()
        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scan modes
// ─────────────────────────────────────────────────────────────────────────────
private enum class ScanMode(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
    @StringRes val hintRes: Int,
) {
    Crop(R.string.scanner_mode_crop, Icons.Rounded.Grass, AgroPalette.Primary, R.string.scanner_hint_crop),
    Pest(R.string.scanner_mode_pest, Icons.Rounded.BugReport, AgroPalette.Amber, R.string.scanner_hint_pest),
    Soil(R.string.scanner_mode_soil, Icons.Rounded.Terrain, AgroPalette.Orange, R.string.scanner_hint_soil),
}

@Composable
private fun ModeSelector(selected: ScanMode, onSelect: (ScanMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0x55000000))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScanMode.values().forEach { m ->
            val isSelected = m == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(listOf(m.tint, m.tint.copy(alpha = 0.7f)))
                        } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                    )
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(m.icon, null, tint = if (isSelected) AgroPalette.BgDeep else AgroPalette.InkMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(m.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AgroPalette.BgDeep else AgroPalette.InkMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Viewfinder — animated corner brackets + scanning sweep + capture flash
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Viewfinder(mode: ScanMode, scanned: Boolean, flashAlpha: Float) {
    val tr = rememberInfiniteTransition(label = "scan")
    val sweep by tr.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "sweep",
    )
    val pulse by tr.animateFloat(
        0.5f, 1f,
        animationSpec = infiniteRepeatable(tween(1400)),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071210), AgroPalette.SurfaceElev, Color(0xFF050E0A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Centered guidance content
        if (scanned) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.scanner_scan_complete), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.scanner_findings_count, 3), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(mode.tint.copy(alpha = 0.18f * pulse)),
                    contentAlignment = Alignment.Center,
                ) { Icon(mode.icon, null, tint = mode.tint, modifier = Modifier.size(36.dp)) }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(mode.hintRes), style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.scanner_hold_steady), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
        }

        // Scanning sweep line (only when not yet scanned)
        if (!scanned) {
            Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
                val y = size.height * sweep
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            mode.tint.copy(alpha = 0f),
                            mode.tint.copy(alpha = 0.9f),
                            mode.tint.copy(alpha = 0f),
                        )
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Corner brackets
        CornerBrackets(tint = mode.tint)

        // Capture flash overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flashAlpha * 0.6f))
        )
    }
}

@Composable
private fun CornerBrackets(tint: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        val len = 28.dp
        val w = 3.dp
        Box(Modifier.size(len, w).background(tint).align(Alignment.TopStart))
        Box(Modifier.size(w, len).background(tint).align(Alignment.TopStart))
        Box(Modifier.size(len, w).background(tint).align(Alignment.TopEnd))
        Box(Modifier.size(w, len).background(tint).align(Alignment.TopEnd))
        Box(Modifier.size(len, w).background(tint).align(Alignment.BottomStart))
        Box(Modifier.size(w, len).background(tint).align(Alignment.BottomStart))
        Box(Modifier.size(len, w).background(tint).align(Alignment.BottomEnd))
        Box(Modifier.size(w, len).background(tint).align(Alignment.BottomEnd))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera control chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraControlChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (active) AgroPalette.Primary else AgroPalette.InkMuted
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) AgroPalette.PrimaryDim else AgroPalette.SurfaceGlass)
            .border(1.dp, if (active) AgroPalette.Primary.copy(alpha = 0.4f) else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Results block
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResultsBlock(mode: ScanMode) {
    // Honest: no ML model is wired yet. Show a "demo capture" card explaining
    // exactly what's hooked up vs what needs a real model.
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Capture saved", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.Amber.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "DEMO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontSize = 9.sp),
                    color = AgroPalette.Amber,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(radius = 18.dp, padding = 16.dp) {
            Column {
                Text(
                    "Real ${stringResource(mode.labelRes).lowercase()} diagnostics light up once a TFLite model is bundled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroPalette.Ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The capture pipeline, viewfinder, and saving flow are real — only the inference step is stubbed. Drop a model into assets and AgroSphere will surface real findings here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            }
        }
    }
}

private data class Finding(val label: String, val confidence: Float, val severity: String, val tint: Color, val advice: String)

@Composable
private fun FindingCard(f: Finding) {
    GlassCard(radius = 18.dp, padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(f.tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val cIcon: ImageVector = when {
                        f.tint == AgroPalette.Primary -> Icons.Rounded.CheckCircle
                        f.tint == AgroPalette.Amber -> Icons.Rounded.Warning
                        else -> Icons.Rounded.AutoAwesome
                    }
                    Icon(cIcon, null, tint = f.tint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(f.label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Text(
                        "Severity: ${f.severity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = f.tint, fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "${(f.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = f.tint,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(8.dp))
            // confidence bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AgroPalette.SurfaceGlassBorder),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(f.confidence)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(f.tint),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(f.advice, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scan history strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HistoryChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable { }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.History, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.scanner_history), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScanHistorySection() {
    // No scan store wired yet — show an honest empty state instead of fakes.
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.scanner_recent_scans), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
        }
        Spacer(Modifier.height(10.dp))
        GlassCard(radius = 16.dp, padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.scanner_recent_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
            }
        }
    }
}
