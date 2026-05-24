package com.agrosphere.app.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.R
import com.agrosphere.app.data.repo.SavedScan
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ═════════════════════════════════════════════════════════════════════════════
// Scanner — live in-app camera (CameraX) → AI vision diagnosis. No external app.
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(padding: PaddingValues, onOpenHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: ScannerViewModel = viewModel()
    val scan by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    val scrollState = rememberScrollState()

    // When a diagnosis arrives, scroll down so the solution (recommendations +
    // treatments) is immediately visible without manual scrolling.
    LaunchedEffect(scan.diagnosis) {
        if (scan.diagnosis != null) scrollState.animateScrollTo(scrollState.maxValue)
    }

    var mode by remember { mutableStateOf(ScanMode.Crop) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var controller by remember { mutableStateOf<LifecycleCameraController?>(null) }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Gallery upload (robust: photo picker → GetContent fallback)
    fun handlePicked(uri: Uri?) {
        if (uri != null) decodeBitmap(context, uri)?.let { bmp -> vm.scan(bmp) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> handlePicked(uri) }
    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> handlePicked(uri) }
    fun pickImage() {
        runCatching {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure { runCatching { getContentLauncher.launch("image/*") } }
    }

    fun doCapture() {
        val c = controller ?: return
        if (scan.scanning) return
        capturePhoto(c, context) { bmp -> if (bmp != null) vm.scan(bmp) }
    }

    val hasResult = scan.diagnosis != null || scan.error != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle(eyebrow = stringResource(R.string.scanner_eyebrow), title = stringResource(R.string.scanner_title))
            HistoryChip(onClick = onOpenHistory)
        }
        Spacer(Modifier.height(14.dp))

        ModeSelector(selected = mode, onSelect = { mode = it; vm.reset() })
        Spacer(Modifier.height(14.dp))

        // ── Viewfinder ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF050E0A)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                !cameraPermission.status.isGranted -> CameraPermissionPrompt(mode) {
                    cameraPermission.launchPermissionRequest()
                }
                else -> {
                    // Camera controller is initialised in the background for capture;
                    // no live preview surface is bound — keeps the frame clean.
                    CameraReady(lensFacing = lensFacing, onControllerReady = { controller = it })
                    CornerBrackets(tint = mode.tint)

                    if (!scan.scanning && !hasResult) {
                        ScanSweep(mode.tint)
                        // Flip camera button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .clickable {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.FlipCameraAndroid, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }

                    if (scan.scanning) {
                        AiAnalyzingOverlay(tint = mode.tint)
                    } else if (scan.diagnosis != null) {
                        val d = scan.diagnosis!!
                        Box(Modifier.fillMaxSize().background(Color(0xAA02080A)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = riskTint(d.riskLevel), modifier = Modifier.size(54.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(d.diseaseName, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.scan_confidence, d.confidence), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Capture / Upload controls ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton(
                    text = when {
                        scan.scanning -> stringResource(R.string.scan_analyzing)
                        hasResult -> stringResource(R.string.scanner_rescan)
                        else -> stringResource(R.string.scanner_capture)
                    },
                    icon = if (hasResult) Icons.Rounded.Refresh else Icons.Rounded.CameraAlt,
                    onClick = {
                        if (!scan.scanning) {
                            if (hasResult) vm.reset() else doCapture()
                        }
                    },
                )
            }
            UploadChip(onClick = { if (!scan.scanning) pickImage() })
        }

        AnimatedVisibility(visible = scan.diagnosis != null, enter = fadeIn(tween(400)), exit = fadeOut()) {
            scan.diagnosis?.let {
                Column {
                    Spacer(Modifier.height(20.dp))
                    DiagnosisCard(it)
                }
            }
        }
        AnimatedVisibility(visible = scan.error != null, enter = fadeIn(), exit = fadeOut()) {
            Column { Spacer(Modifier.height(20.dp)); ErrorBlock() }
        }

        Spacer(Modifier.height(22.dp))
        ScanHistorySection(history)
        Spacer(Modifier.height(20.dp))
    }
}

// ─── CameraX — headless controller (no preview surface) ──────────────────────
// Binds the camera for still capture only; no PreviewView so no live feed
// leaks into the viewfinder.

@Composable
private fun CameraReady(
    lensFacing: Int,
    onControllerReady: (LifecycleCameraController) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }

    LaunchedEffect(lensFacing) {
        controller.cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        controller.bindToLifecycle(lifecycleOwner)
        onControllerReady(controller)
    }
}

private fun capturePhoto(controller: LifecycleCameraController, context: Context, onResult: (Bitmap?) -> Unit) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val bmp = try { image.toBitmap() } catch (_: Throwable) { null }
                image.close()
                onResult(bmp?.let { rotateBitmap(it, rotation) })
            }
            override fun onError(exc: ImageCaptureException) { onResult(null) }
        },
    )
}

private fun rotateBitmap(b: Bitmap, deg: Int): Bitmap {
    if (deg == 0) return b
    return try {
        Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(deg.toFloat()) }, true)
    } catch (_: Throwable) { b }
}

// ─── Permission prompt ───────────────────────────────────────────────────────

@Composable
private fun CameraPermissionPrompt(mode: ScanMode, onEnable: () -> Unit) {
    Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(mode.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.CameraAlt, null, tint = mode.tint, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.scan_camera_needed), style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.scan_camera_needed_sub),
            style = MaterialTheme.typography.bodySmall,
            color = AgroPalette.InkMuted,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(mode.tint)
                .clickable(onClick = onEnable)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) { Text(stringResource(R.string.scan_enable_camera), style = MaterialTheme.typography.labelLarge, color = AgroPalette.BgDeep, fontWeight = FontWeight.Bold) }
    }
}

// ─── Scan modes ──────────────────────────────────────────────────────────────
private enum class ScanMode(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
) {
    Crop(R.string.scanner_mode_crop, Icons.Rounded.Grass, AgroPalette.Primary),
    Pest(R.string.scanner_mode_pest, Icons.Rounded.BugReport, AgroPalette.Amber),
    Soil(R.string.scanner_mode_soil, Icons.Rounded.Terrain, AgroPalette.Orange),
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
                        if (isSelected) Brush.horizontalGradient(listOf(m.tint, m.tint.copy(alpha = 0.7f)))
                        else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
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

// ─── Enhanced scan sweep — brighter line with trailing energy glow ───────────

@Composable
private fun ScanSweep(tint: Color) {
    val tr = rememberInfiniteTransition(label = "scan")
    val sweep by tr.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sweep",
    )
    Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val y = size.height * sweep

        // Trailing glow above sweep line
        val trailH = (size.height * 0.10f).coerceAtMost(y + 1f)
        if (trailH > 2f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to tint.copy(alpha = 0.10f),
                    startY = (y - trailH).coerceAtLeast(0f),
                    endY   = y,
                ),
                topLeft = Offset(0f, (y - trailH).coerceAtLeast(0f)),
                size    = Size(size.width, trailH.coerceAtLeast(0f)),
            )
        }

        // Main sweep line — wide gradient with bright centre
        drawLine(
            brush = Brush.horizontalGradient(
                0f    to Color.Transparent,
                0.08f to tint.copy(alpha = 0.45f),
                0.30f to tint.copy(alpha = 0.90f),
                0.50f to tint,
                0.70f to tint.copy(alpha = 0.90f),
                0.92f to tint.copy(alpha = 0.45f),
                1f    to Color.Transparent,
            ),
            start = Offset(0f, y),
            end   = Offset(size.width, y),
            strokeWidth = 2.5f,
            cap   = StrokeCap.Round,
        )

        // Side reflection dots
        listOf(0f, size.width).forEach { edgeX ->
            drawCircle(
                brush = Brush.radialGradient(
                    0f to tint.copy(alpha = 0.7f),
                    1f to Color.Transparent,
                    center = Offset(edgeX, y), radius = 12f,
                ),
                radius = 12f, center = Offset(edgeX, y),
            )
        }
    }
}

// ─── Enhanced corner brackets — animated in, pulsing glow ────────────────────

@Composable
private fun CornerBrackets(tint: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        visible = true
    }
    val armP by animateFloatAsState(
        if (visible) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "arm",
    )
    val inf = rememberInfiniteTransition(label = "brackets")
    val glowP by inf.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "gp",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset  = 18.dp.toPx()
        val armLen = 30.dp.toPx() * armP
        val thick  = 2.6f

        val corners = listOf(
            Offset(inset, inset),
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            Offset(size.width - inset, size.height - inset),
        )
        val dirs = listOf(
            Pair(1f,  1f), Pair(-1f,  1f),
            Pair(1f, -1f), Pair(-1f, -1f),
        )

        corners.zip(dirs).forEach { (o, d) ->
            val (dx, dy) = d
            // Horizontal arm
            drawLine(tint, o, Offset(o.x + armLen * dx, o.y), thick, cap = StrokeCap.Round)
            // Vertical arm
            drawLine(tint, o, Offset(o.x, o.y + armLen * dy), thick, cap = StrokeCap.Round)
            // Corner glow
            drawCircle(
                brush = Brush.radialGradient(
                    0f to tint.copy(alpha = 0.45f * glowP * armP),
                    1f to Color.Transparent,
                    center = o, radius = 22f,
                ),
                radius = 22f, center = o,
            )
        }

        // Subtle centre targeting reticle (crosshair dot)
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(
            color  = tint.copy(alpha = 0.20f * glowP),
            radius = 4.dp.toPx(),
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to tint.copy(alpha = 0.12f * glowP),
                1f to Color.Transparent,
                center = Offset(cx, cy), radius = 28.dp.toPx(),
            ),
            radius = 28.dp.toPx(), center = Offset(cx, cy),
        )
    }
}

// ─── AI analyzing overlay ────────────────────────────────────────────────────

private val analyzeSteps = listOf(
    "Examining leaf texture…",
    "Detecting pathogens…",
    "Analyzing symptom patterns…",
    "Consulting disease database…",
    "Generating field diagnosis…",
)

@Composable
private fun AiAnalyzingOverlay(tint: Color) {
    val inf = rememberInfiniteTransition(label = "ai-scan")
    val rot1 by inf.animateFloat( 0f,  360f,  infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "r1")
    val rot2 by inf.animateFloat( 0f, -360f,  infiniteRepeatable(tween(4100, easing = LinearEasing)), label = "r2")
    val rot3 by inf.animateFloat( 0f,  360f,  infiniteRepeatable(tween(6800, easing = LinearEasing)), label = "r3")
    val pulse by inf.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse),     label = "p")
    val ptT   by inf.animateFloat( 0f,  1f,  infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "pt")

    var stepIdx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2400)
            stepIdx = (stepIdx + 1) % analyzeSteps.size
        }
    }

    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(Color(0xCC020A0F)),
        contentAlignment  = Alignment.Center,
    ) {
        // Floating particle field
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(22) { i ->
                val seed  = ((i * 37) % 1000).toFloat() / 1000f
                val x     = size.width  * ((i * 53) % 100 / 100f)
                val t     = (ptT + seed) % 1f
                val y     = (1f - t) * (size.height + 60f) - 30f
                val alpha = when {
                    t < 0.12f -> t / 0.12f * 0.28f
                    t > 0.88f -> (1f - t) / 0.12f * 0.28f
                    else -> 0.28f
                }
                drawCircle(tint.copy(alpha = alpha), radius = 1.4f + (i % 3) * 0.6f, center = Offset(x, y))
            }
        }

        // Multi-ring scanner
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val cx = size.width  / 2f
                    val cy = size.height / 2f
                    val s  = size.minDimension

                    // Ring 3 — outer, slow
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, tint.copy(alpha = 0.6f), tint, Color.Transparent),
                        ),
                        startAngle = rot1, sweepAngle = 195f, useCenter = false,
                        topLeft = Offset(cx - s * 0.46f, cy - s * 0.46f),
                        size    = Size(s * 0.92f, s * 0.92f),
                        style   = Stroke(width = 2.4f, cap = StrokeCap.Round),
                    )
                    // Ring 2 — counter
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, tint.copy(alpha = 0.5f), Color.Transparent),
                        ),
                        startAngle = rot2, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(cx - s * 0.35f, cy - s * 0.35f),
                        size    = Size(s * 0.70f, s * 0.70f),
                        style   = Stroke(width = 2.0f, cap = StrokeCap.Round),
                    )
                    // Ring 1 — inner, slowest
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, AgroPalette.Sky.copy(alpha = 0.5f), Color.Transparent),
                        ),
                        startAngle = rot3, sweepAngle = 90f, useCenter = false,
                        topLeft = Offset(cx - s * 0.24f, cy - s * 0.24f),
                        size    = Size(s * 0.48f, s * 0.48f),
                        style   = Stroke(width = 1.6f, cap = StrokeCap.Round),
                    )
                    // Centre glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f   to tint.copy(alpha = 0.85f * pulse),
                            0.4f to tint.copy(alpha = 0.30f * pulse),
                            1f   to Color.Transparent,
                            center = Offset(cx, cy), radius = s * 0.14f,
                        ),
                        radius = s * 0.14f, center = Offset(cx, cy),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // "AI ANALYZING" label
            Text(
                "AI ANALYZING",
                style      = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp),
                color      = tint,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(6.dp))

            // Cycling status text
            AnimatedContent(
                targetState = stepIdx,
                transitionSpec = {
                    fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                },
                label = "step",
            ) { idx ->
                Text(
                    analyzeSteps[idx],
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun UploadChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.PhotoLibrary, stringResource(R.string.camera_gallery), tint = AgroPalette.InkMuted) }
}

// ─── image decode (OOM-safe, downsampled) ────────────────────────────────────

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val max = maxOf(info.size.width, info.size.height)
            if (max > 1600) {
                val scale = 1600f / max
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
} catch (_: Throwable) {
    null
}

// ─── Results ─────────────────────────────────────────────────────────────────
// DiagnosisCard, TreatmentRow, riskTint, riskLabelRes live in ScanDiagnosisCard.kt

@Composable
private fun ErrorBlock() {
    GlassCard(radius = 16.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = AgroPalette.Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.scan_failed), style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
        }
    }
}

// ─── History ───────────────────────────────────────────────────────────────────
@Composable
private fun HistoryChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.History, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.scanner_history), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScanHistorySection(history: List<SavedScan>) {
    Column {
        Text(stringResource(R.string.scanner_recent_scans), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            GlassCard(radius = 16.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.scanner_recent_empty), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(4).forEach { ScanHistoryRow(it) }
            }
        }
    }
}

@Composable
private fun ScanHistoryRow(s: SavedScan) {
    val tint = riskTint(s.diagnosis.riskLevel)
    val time = if (s.createdAtMillis > 0)
        DateUtils.getRelativeTimeSpanString(
            s.createdAtMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        ).toString() else ""
    GlassCard(radius = 14.dp, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(s.diagnosis.diseaseName, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                if (time.isNotBlank()) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
            }
            Text("${s.diagnosis.confidence}%", style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.Bold)
        }
    }
}
