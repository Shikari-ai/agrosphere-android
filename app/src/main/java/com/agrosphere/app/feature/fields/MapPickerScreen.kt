package com.agrosphere.app.feature.fields

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.weather.LocationProvider
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════
// MapPickerScreen — draw a polygon on a satellite map, area computes itself,
// then save it straight as a new field. Uses the same FieldRepository as the
// manual sheet, so the result populates Home / Map / Profile reactively.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun MapPickerScreen(
    onBack: () -> Unit,
    vm: FieldsViewModel = viewModel(factory = FieldsViewModel.Factory),
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Polygon vertices, in tap order.
    val vertices = remember { mutableStateListOf<LatLng>() }

    // Form state — we ship a complete field from this screen.
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf(vm.cropPresets.first()) }
    var stage by remember { mutableStateOf(vm.stagePresets.first()) }
    val accent = AgroPalette.Primary

    // Spherical-correct area in square metres → hectares (rounded to 2 dp).
    val areaHa by remember {
        derivedStateOf {
            if (vertices.size < 3) 0.0
            else SphericalUtil.computeArea(vertices).let { sqM -> (sqM / 10_000.0) }
        }
    }

    // Centre the camera on the device's last-known location.
    val defaultCamera = remember { CameraPosition.fromLatLngZoom(LatLng(19.9975, 73.7898), 14f) }
    val cameraPosition = rememberCameraPositionState { position = defaultCamera }
    LaunchedEffect(Unit) {
        val place = LocationProvider.fastCurrent(context)
        cameraPosition.position = CameraPosition.fromLatLngZoom(LatLng(place.latitude, place.longitude), 16.5f)
    }

    Box(modifier = Modifier.fillMaxSize().background(AgroPalette.BgDeep)) {
        // ─── Satellite map fills the screen ───
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPosition,
            properties = MapProperties(mapType = MapType.HYBRID, isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                compassEnabled = true, myLocationButtonEnabled = false,
                zoomControlsEnabled = false, mapToolbarEnabled = false,
            ),
            onMapClick = { latLng -> vertices += latLng },
        ) {
            // Vertex markers
            vertices.forEachIndexed { i, v ->
                Marker(state = MarkerState(position = v), title = "Vertex ${i + 1}")
            }
            // Open polyline before we have 3+ points (closing edge faked by Polygon)
            if (vertices.size in 2..2) {
                Polyline(
                    points = vertices.toList(),
                    color = AgroPalette.Primary,
                    width = 5f,
                )
            }
            // Closed polygon once we have enough vertices
            if (vertices.size >= 3) {
                Polygon(
                    points = vertices.toList(),
                    fillColor = Color(0x4010B981),
                    strokeColor = AgroPalette.Primary,
                    strokeWidth = 6f,
                )
            }
        }

        // ─── Top bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.ArrowBack, onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC0A1118))
                    .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    when {
                        vertices.isEmpty() -> "Tap the map to add the first corner"
                        vertices.size < 3 -> "${vertices.size}/3+ vertices — tap to add more"
                        else -> "${vertices.size} vertices · area ${"%.2f".format(areaHa)} ha"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ─── Right-side controls ───
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleIconButton(Icons.Rounded.Undo, enabled = vertices.isNotEmpty()) {
                vertices.removeAt(vertices.lastIndex)
            }
            CircleIconButton(Icons.Rounded.DeleteOutline, enabled = vertices.isNotEmpty()) {
                vertices.clear()
            }
        }

        // ─── Bottom sheet with form ───
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            GlassCard(radius = 24.dp, padding = 16.dp) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Area display row
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (vertices.size >= 3) "%.2f".format(areaHa) else "—",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                            color = AgroPalette.Primary,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "ha",
                            style = MaterialTheme.typography.labelMedium,
                            color = AgroPalette.InkMuted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (vertices.size >= 3) "computed live" else "draw 3+ corners",
                            style = MaterialTheme.typography.labelSmall,
                            color = AgroPalette.InkDim,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Field name") },
                        placeholder = { Text("e.g. North paddock", color = AgroPalette.InkDim) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgroPalette.Primary,
                            unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
                            focusedTextColor = AgroPalette.Ink,
                            unfocusedTextColor = AgroPalette.Ink,
                            cursorColor = AgroPalette.Primary,
                            focusedLabelColor = AgroPalette.Primary,
                            unfocusedLabelColor = AgroPalette.InkMuted,
                            focusedContainerColor = AgroPalette.SurfaceGlass,
                            unfocusedContainerColor = AgroPalette.SurfaceGlass,
                        ),
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "CROP",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp, fontSize = 10.sp),
                        color = AgroPalette.InkMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.cropPresets) { c -> PickerChip(c, c == crop) { crop = c } }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "STAGE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp, fontSize = 10.sp),
                        color = AgroPalette.InkMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.stagePresets) { s -> PickerChip(s, s == stage) { stage = s } }
                    }

                    Spacer(Modifier.height(14.dp))
                    val canSave = vertices.size >= 3 && name.isNotBlank() && areaHa > 0.0
                    PrimaryButton(
                        text = if (canSave) "Save ${"%.2f".format(areaHa)} ha field" else "Add ≥3 vertices + a name",
                        icon = Icons.Rounded.Check,
                        enabled = canSave,
                        onClick = {
                            vm.addField(
                                name = name,
                                crop = crop,
                                areaHa = areaHa,
                                stage = stage,
                                moisturePct = 60,
                                accent = accent,
                            )
                            scope.launch {
                                snackbar.showSnackbar("$name saved · ${"%.2f".format(areaHa)} ha")
                            }
                            onBack()
                        },
                    )
                    AnimatedVisibility(visible = vertices.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            GhostButton(
                                text = "Clear and start over",
                                onClick = { vertices.clear() },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
        )
    }
}

@Composable
private fun PickerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AgroPalette.Primary else AgroPalette.SurfaceGlass)
            .border(1.dp, if (selected) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AgroPalette.BgDeep else AgroPalette.InkMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC0A1118))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null,
            tint = if (enabled) AgroPalette.Ink else AgroPalette.InkDim,
            modifier = Modifier.size(20.dp),
        )
    }
}
