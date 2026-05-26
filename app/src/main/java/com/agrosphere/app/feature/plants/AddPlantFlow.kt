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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.model.PlantScanRecord
import com.agrosphere.app.data.repo.PlantIdRepository
import com.agrosphere.app.data.repo.PlantIdentification
import com.agrosphere.app.data.repo.PlantRepository
import java.io.File
import java.io.FileOutputStream
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

private enum class FlowStage { Name, Camera, Analyzing, Review }

@Composable
fun AddPlantFlow(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: PlantsViewModel = viewModel(factory = PlantsViewModel.Factory),
) {
    var stage          by remember { mutableStateOf(FlowStage.Name) }
    var nickname       by remember { mutableStateOf("") }
    var captured       by remember { mutableStateOf<Bitmap?>(null) }
    var identification by remember { mutableStateOf<PlantIdentification?>(null) }
    var idError        by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroPalette.BgDeep),
    ) {
        when (stage) {
            FlowStage.Name -> NameStage(
                nickname = nickname,
                onChange = { nickname = it },
                onBack   = onBack,
                onNext   = { stage = FlowStage.Camera },
            )

            FlowStage.Camera -> CameraStage(
                nickname     = nickname,
                onBack       = { stage = FlowStage.Name },
                onCaptured   = { bmp ->
                    captured = bmp
                    stage = FlowStage.Analyzing
                    scope.launch {
                        try {
                            identification = PlantIdRepository.identify(bmp)
                            stage = FlowStage.Review
                        } catch (e: Throwable) {
                            idError = e.message ?: "Couldn't identify the plant"
                            stage = FlowStage.Review
                        }
                    }
                },
            )

            FlowStage.Analyzing -> AnalyzingStage(nickname = nickname)

            FlowStage.Review -> {
                val ctx = LocalContext.current
                ReviewStage(
                    nickname        = nickname,
                    identification  = identification,
                    idError         = idError,
                    locationPresets = vm.locationPresets,
                    potSizePresets  = vm.potSizePresets,
                    accentPresets   = vm.accentPresets,
                    onBack          = { stage = FlowStage.Camera },
                    onRetake        = { captured = null; identification = null; idError = null; stage = FlowStage.Camera },
                    onSave          = { species, location, pot, sun, interval, accent ->
                        // Stable plant id so we can name the photo file after it.
                        val plantId = "p${System.currentTimeMillis()}"
                        val photoPath = captured?.let { savePlantPhoto(ctx, plantId, it) }

                        // Initial scan record from the AI identification — so the plant's
                        // profile has a history entry from the moment it's added.
                        val initialScan = identification?.takeIf { !it.unidentifiable }?.let { id ->
                            PlantScanRecord(
                                timestamp       = System.currentTimeMillis(),
                                verdict         = "Identified — ${id.commonName}",
                                healthScore     = 80,                       // healthy assumption at creation
                                riskLevel       = "healthy",
                                summary         = listOfNotNull(
                                    id.scientificName.takeIf { it.isNotBlank() },
                                    id.variety.takeIf { it.isNotBlank() },
                                ).joinToString(" · ").ifBlank { id.commonName },
                                recommendations = listOf(id.careNote).filter { it.isNotBlank() },
                                photoPath       = photoPath,
                            )
                        }

                        PlantRepository.addPlant(
                            name                 = nickname,
                            species              = species,
                            location             = location,
                            potSize              = pot,
                            sunlightNeed         = sun,
                            wateringIntervalDays = interval,
                            accent               = accent,
                            initialScan          = initialScan,
                            photoPath            = photoPath,
                        )
                        onSaved()
                    },
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Stage 1 — Nickname
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun NameStage(
    nickname: String,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        Row(modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            Text("Add a Plant", style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AgroPalette.Primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.LocalFlorist, null, tint = AgroPalette.Primary, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(20.dp))
            Text(
                "What do you call this plant?",
                style      = MaterialTheme.typography.headlineSmall,
                color      = AgroPalette.Ink,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Give it any nickname — \"Grandma's Rose\", \"Office Snake Plant\", anything you'll remember. Next, the camera will identify its species and care needs automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value          = nickname,
                onValueChange  = onChange,
                placeholder    = { Text("e.g. Grandma's Rose", color = AgroPalette.InkDim) },
                singleLine     = true,
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AgroPalette.Primary,
                    unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
                    focusedTextColor     = AgroPalette.Ink,
                    unfocusedTextColor   = AgroPalette.Ink,
                    cursorColor          = AgroPalette.Primary,
                    focusedContainerColor   = AgroPalette.SurfaceGlass,
                    unfocusedContainerColor = AgroPalette.SurfaceGlass,
                ),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.padding(20.dp)) {
            PrimaryButton(
                text    = "Continue — open camera",
                icon    = Icons.Rounded.CameraAlt,
                enabled = nickname.isNotBlank(),
                onClick = onNext,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Stage 2 — Camera
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraStage(
    nickname: String,
    onBack: () -> Unit,
    onCaptured: (Bitmap) -> Unit,
) {
    val context  = LocalContext.current
    val perm     = rememberPermissionState(android.Manifest.permission.CAMERA)
    var controller by remember { mutableStateOf<LifecycleCameraController?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            loadBitmap(context, uri)?.let(onCaptured)
        }
    }

    LaunchedEffect(Unit) { if (!perm.status.isGranted) perm.launchPermissionRequest() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (perm.status.isGranted) {
            CameraPreview(
                onControllerReady = { controller = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
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
                    "Point at your plant to identify it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Enable camera", onClick = { perm.launchPermissionRequest() })
            }
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
                Text(nickname.ifBlank { "New plant" }, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Aim at the whole plant", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
        }

        // Framing guides — a soft square in the center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        )

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gallery picker
            IconButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White)
            }

            // Shutter
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, AgroPalette.Primary, CircleShape)
                    .clickable {
                        controller?.let { c ->
                            capturePhoto(c, context) { bmp ->
                                bmp?.let(onCaptured)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFlorist, null, tint = AgroPalette.Primary, modifier = Modifier.size(32.dp))
            }

            // Spacer to balance row
            Box(modifier = Modifier.size(52.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Stage 3 — Analyzing
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnalyzingStage(nickname: String) {
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
                androidx.compose.ui.graphics.drawscope.rotate(rot) {
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
        Text(
            "Identifying ${nickname.ifBlank { "your plant" }}…",
            style      = MaterialTheme.typography.titleMedium,
            color      = AgroPalette.Ink,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Examining leaves, growth pattern and species cues",
            style = MaterialTheme.typography.bodySmall,
            color = AgroPalette.InkMuted,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Stage 4 — Review (auto-filled, user can adjust)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ReviewStage(
    nickname: String,
    identification: PlantIdentification?,
    idError: String?,
    locationPresets: List<String>,
    potSizePresets: List<String>,
    accentPresets: List<Color>,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onSave: (species: String, location: String, potSize: String, sunlight: String, intervalDays: Int, accent: Color) -> Unit,
) {
    val id = identification
    val unidentified = id?.unidentifiable == true || id == null

    var speciesName  by remember { mutableStateOf(id?.commonName ?: "") }
    var interval     by remember { mutableIntStateOf(id?.wateringIntervalDays ?: 7) }
    var sunlight     by remember { mutableStateOf(id?.sunlightNeed ?: "Partial Shade") }
    var location     by remember { mutableStateOf(locationPresets.first()) }
    var potSize      by remember { mutableStateOf(potSizePresets.first()) }
    var accent       by remember { mutableStateOf(accentPresets.first()) }
    val sunlightOptions = listOf("Full Sun", "Partial Shade", "Indoors", "Low Light")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            Text("Review", style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRetake) {
                Icon(Icons.Rounded.Refresh, "Retake", tint = AgroPalette.InkMuted)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Identification result ─────────────────────────────────────────
            item {
                GlassCard(
                    radius = 16.dp,
                    padding = 16.dp,
                    background = if (unidentified)
                        androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass)
                    else
                        Brush.linearGradient(listOf(AgroPalette.Primary.copy(alpha = 0.14f), AgroPalette.Primary.copy(alpha = 0.04f))),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (unidentified) Icons.Rounded.Edit else Icons.Rounded.AutoAwesome,
                                null, tint = AgroPalette.Primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (unidentified) "Couldn't identify automatically" else "Identified",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.InkMuted,
                            )
                            Text(
                                if (unidentified) "Type the species name" else (id?.commonName ?: ""),
                                style = MaterialTheme.typography.titleMedium,
                                color = AgroPalette.Ink,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (!unidentified) {
                                Text(
                                    listOfNotNull(
                                        id?.scientificName?.takeIf { it.isNotBlank() },
                                        id?.variety?.takeIf { it.isNotBlank() },
                                        id?.confidence?.let { "${it}% confidence" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgroPalette.InkMuted,
                                )
                            }
                        }
                    }
                    if (unidentified) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value         = speciesName,
                            onValueChange = { speciesName = it },
                            placeholder   = { Text("e.g. Rose, Money Plant", color = AgroPalette.InkDim) },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AgroPalette.Primary,
                                unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
                                focusedTextColor     = AgroPalette.Ink,
                                unfocusedTextColor   = AgroPalette.Ink,
                                cursorColor          = AgroPalette.Primary,
                                focusedContainerColor   = AgroPalette.SurfaceGlass,
                                unfocusedContainerColor = AgroPalette.SurfaceGlass,
                            ),
                        )
                    }
                    if (id?.careNote?.isNotBlank() == true) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AgroPalette.Primary.copy(alpha = 0.07f))
                                .padding(10.dp),
                        ) {
                            Text("💡 ${id.careNote}", style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink)
                        }
                    }
                }
            }

            // ── Auto-detected care profile (read-only display) ────────────────
            if (!unidentified && id != null) {
                item {
                    GlassCard(radius = 16.dp, padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Auto-detected care", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                            CareLine(Icons.Rounded.WaterDrop, "Water every ${id.wateringIntervalDays} day${if (id.wateringIntervalDays == 1) "" else "s"}", AgroPalette.Sky)
                            CareLine(Icons.Rounded.WbSunny, id.sunlightNeed, AgroPalette.Amber)
                            CareLine(Icons.Rounded.Grass, id.soilType, AgroPalette.Primary)
                            CareLine(Icons.Rounded.LocalFlorist, "Category — ${id.category}", AgroPalette.Iris)
                        }
                    }
                }
            }

            // ── User adjusts these ────────────────────────────────────────────
            item {
                SectionLabel("Where is it?")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(locationPresets) { loc ->
                        ChipBtn(label = loc, selected = loc == location) { location = loc }
                    }
                }
            }
            item {
                SectionLabel("Pot size")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(potSizePresets) { p ->
                        ChipBtn(label = p, selected = p == potSize) { potSize = p }
                    }
                }
            }
            // For unidentified plants, let user pick sunlight + interval
            if (unidentified) {
                item {
                    SectionLabel("Sunlight need")
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sunlightOptions) { s ->
                            ChipBtn(label = s, selected = s == sunlight) { sunlight = s }
                        }
                    }
                }
                item {
                    val suffix = if (interval == 1) "" else "s"
                    SectionLabel("Water every $interval day$suffix")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        StepBtn("-") { if (interval > 1) interval-- }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AgroPalette.SurfaceGlass)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text("$interval", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                        }
                        StepBtn("+") { if (interval < 30) interval++ }
                    }
                }
            }
            item {
                SectionLabel("Colour tag")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentPresets.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (c == accent) Modifier.border(2.dp, AgroPalette.Ink, CircleShape)
                                    else Modifier
                                )
                                .clickable { accent = c },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            PrimaryButton(
                text    = "Save plant",
                icon    = Icons.Rounded.CheckCircle,
                enabled = nickname.isNotBlank() && speciesName.isNotBlank(),
                onClick = {
                    onSave(speciesName, location, potSize, sunlight, interval, accent)
                },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Helpers
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ChipBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AgroPalette.Primary.copy(alpha = 0.18f) else AgroPalette.SurfaceGlass)
            .border(1.dp, if (selected) AgroPalette.Primary.copy(alpha = 0.45f) else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style     = MaterialTheme.typography.labelMedium,
            color     = if (selected) AgroPalette.Primary else AgroPalette.InkMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize  = 12.sp,
        )
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CareLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// CameraX glue
// ═════════════════════════════════════════════════════════════════════════════

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

/** Saves the plant photo as JPEG under filesDir/plants/{plantId}.jpg and returns the absolute path. */
internal fun savePlantPhoto(context: Context, plantId: String, bitmap: Bitmap): String? = runCatching {
    val dir = File(context.filesDir, "plants").apply { mkdirs() }
    val file = File(dir, "$plantId.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
    }
    file.absolutePath
}.getOrNull()

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
