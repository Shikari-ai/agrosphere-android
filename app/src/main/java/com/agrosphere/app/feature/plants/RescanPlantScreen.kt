package com.agrosphere.app.feature.plants

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.agrosphere.app.data.model.PlantScanRecord
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.data.repo.VisionScanRepository
import com.agrosphere.app.data.i18n.LocaleManager
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

private enum class RescanStage { Camera, Analyzing, Result }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RescanPlantScreen(
    plantId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val plant = PlantRepository.byId(plantId)
    if (plant == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val perm    = rememberPermissionState(android.Manifest.permission.CAMERA)
    var controller by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(RescanStage.Camera) }
    var record by remember { mutableStateOf<PlantScanRecord?>(null) }
    var error  by remember { mutableStateOf<String?>(null) }

    // Apply torch state whenever the user toggles flash or the controller arrives.
    LaunchedEffect(controller, flashOn) {
        controller?.enableTorch(flashOn)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) loadBitmap(context, uri)?.let { runScan(context, scope, plant.id, it,
            onAnalyzing = { stage = RescanStage.Analyzing },
            onComplete  = { rec ->
                record = rec
                error  = null
                stage  = RescanStage.Result
                PlantRepository.applyScan(plant.id, rec)
            },
            onError     = { msg ->
                error = msg
                stage = RescanStage.Result
            },
        ) }
    }

    LaunchedEffect(Unit) { if (!perm.status.isGranted) perm.launchPermissionRequest() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroPalette.BgDeep),
    ) {
        when (stage) {
            RescanStage.Camera -> {
                if (perm.status.isGranted) {
                    CameraPreview(
                        onControllerReady = { controller = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PermissionPrompt(onEnable = { perm.launchPermissionRequest() })
                }

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .background(Brush.verticalGradient(listOf(AgroPalette.BgDeep.copy(alpha = 0.5f), Color.Transparent))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plant.name, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Health check · ${plant.species}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                    }
                }

                // Framing guide
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                )

                // Bottom controls — Upload (gallery) + Capture (shutter) + Flash toggle, all labelled.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Upload an existing photo from gallery — great if the user already
                    // has a well-lit picture of the plant.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, "Upload from gallery", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    // Capture shutter
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, AgroPalette.Primary, CircleShape)
                                .clickable {
                                    controller?.let { c ->
                                        capturePhoto(c, context) { bmp ->
                                            if (bmp != null) runScan(context, scope, plant.id, bmp,
                                                onAnalyzing = { stage = RescanStage.Analyzing },
                                                onComplete  = { rec ->
                                                    record = rec
                                                    error  = null
                                                    stage  = RescanStage.Result
                                                    PlantRepository.applyScan(plant.id, rec)
                                                },
                                                onError     = { msg ->
                                                    error = msg
                                                    stage = RescanStage.Result
                                                },
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Capture",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    // Flash torch — for dim indoor light.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (flashOn) Color.White else Color.White.copy(alpha = 0.18f))
                                .clickable { flashOn = !flashOn },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (flashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                if (flashOn) "Flash on" else "Flash off",
                                tint = if (flashOn) AgroPalette.BgDeep else Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (flashOn) "Flash on" else "Flash",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (flashOn) Color.White else Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            RescanStage.Analyzing -> AnalyzingView(plantName = plant.name)

            RescanStage.Result -> ResultView(
                plantName  = plant.name,
                record     = record,
                error      = error,
                onRetake   = { stage = RescanStage.Camera; record = null; error = null },
                onDone     = onFinished,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Run scan against the existing VisionScanRepository, package result as a
// PlantScanRecord — the photo is saved to filesDir/plants/{id}-{timestamp}.jpg.
// ─────────────────────────────────────────────────────────────────────────────

private fun runScan(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    plantId: String,
    bitmap: Bitmap,
    onAnalyzing: () -> Unit,
    onComplete: (PlantScanRecord) -> Unit,
    onError: (String) -> Unit,
) {
    onAnalyzing()
    scope.launch {
        try {
            val plant = PlantRepository.byId(plantId)
            val cropType = plant?.species ?: ""
            val diag = VisionScanRepository.analyze(
                bitmap        = bitmap,
                cropType      = cropType,
                fields        = FieldRepository.current(),
                replyLanguage = LocaleManager.activeLanguageTag(),
            )
            val ts = System.currentTimeMillis()
            val photoPath = saveScanPhoto(context, plantId, ts, bitmap)
            val record = PlantScanRecord(
                timestamp       = ts,
                verdict         = diag.diseaseName.ifBlank { "Healthy" },
                healthScore     = PlantRepository.riskToHealthScore(diag.riskLevel, diag.confidence),
                riskLevel       = diag.riskLevel,
                summary         = diag.summary.ifBlank { diag.narrative },
                recommendations = diag.recommendations,
                photoPath       = photoPath,
            )
            // Update growth stage from the AI's read of the photo — only when the
            // model gave us a confident stage (empty string means "couldn't tell",
            // in which case we keep the plant's current stage untouched).
            if (diag.growthStage.isNotBlank()) {
                PlantRepository.setStage(plantId, diag.growthStage)
            }
            onComplete(record)
        } catch (e: Throwable) {
            onError(e.message ?: "Scan failed")
        }
    }
}

private fun saveScanPhoto(context: Context, plantId: String, ts: Long, bitmap: Bitmap): String? = runCatching {
    val dir = java.io.File(context.filesDir, "plants").apply { mkdirs() }
    val file = java.io.File(dir, "$plantId-$ts.jpg")
    java.io.FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
    }
    file.absolutePath
}.getOrNull()

// ─────────────────────────────────────────────────────────────────────────────
// Sub-screens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalyzingView(plantName: String) {
    val inf  = rememberInfiniteTransition(label = "analyze")
    val rot  by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "p")

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.35f * pulse),
                        1f to Color.Transparent,
                    ),
                    radius = size.minDimension / 2f * pulse,
                )
                rotate(rot) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Amber, AgroPalette.Primary)),
                        startAngle = 0f, sweepAngle = 270f, useCenter = false,
                        topLeft = Offset(8f, 8f),
                        size = androidx.compose.ui.geometry.Size(size.width - 16f, size.height - 16f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round),
                    )
                }
            }
            Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Checking $plantName…", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Updating health score and adding to scan history", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
    }
}

@Composable
private fun ResultView(
    plantName: String,
    record: PlantScanRecord?,
    error: String?,
    onRetake: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp),
    ) {
        Text(plantName, style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(4.dp))

        if (error != null || record == null) {
            Text("Scan failed", style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Rose, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(error ?: "Unknown error", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Try again", icon = Icons.Rounded.Refresh, onClick = onRetake)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text  = "Back to plant",
                icon  = Icons.Rounded.CheckCircle,
                brush = androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
                onClick = onDone,
            )
            return@Column
        }

        val color = when (record.riskLevel.lowercase()) {
            "healthy" -> AgroPalette.Primary
            "low"     -> AgroPalette.Sky
            "medium"  -> AgroPalette.Amber
            else      -> AgroPalette.Rose
        }

        Text(record.verdict, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Health updated to ${record.healthScore}/100", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)

        Spacer(Modifier.height(16.dp))
        GlassCard(radius = 16.dp, padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Summary", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                Text(record.summary.ifBlank { "No additional details." }, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
                if (record.recommendations.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("What to do", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                    record.recommendations.take(5).forEach { rec ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", color = color, fontWeight = FontWeight.Bold)
                            Text(rec, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Rescan", icon = Icons.Rounded.Refresh,
            brush = androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
            onClick = onRetake)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "Done", icon = Icons.Rounded.CheckCircle, onClick = onDone)
    }
}

@Composable
private fun PermissionPrompt(onEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.CameraAlt, null, tint = AgroPalette.Primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Camera access needed", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Point at your plant to update its health profile",
            style = MaterialTheme.typography.bodyMedium,
            color = AgroPalette.InkMuted,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Enable camera", icon = Icons.Rounded.CameraAlt, onClick = onEnable)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraX glue (kept independent from AddPlantFlow so each screen owns its own)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraPreview(
    onControllerReady: (LifecycleCameraController) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            this.controller = controller
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    LaunchedEffect(Unit) {
        controller.cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        controller.bindToLifecycle(lifecycleOwner)
        onControllerReady(controller)
    }
    AndroidView(factory = { previewView }, modifier = modifier)
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

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
            d.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()
