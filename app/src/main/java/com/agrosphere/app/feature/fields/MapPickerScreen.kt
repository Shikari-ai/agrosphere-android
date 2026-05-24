package com.agrosphere.app.feature.fields

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.geocoding.GeocodingApi
import com.agrosphere.app.data.geocoding.NominatimPlace
import com.agrosphere.app.data.weather.LocationProvider
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// MapPickerScreen — draw a polygon on a satellite map, area computes itself,
// then save it as a real field.
//
// Uses Esri WorldImagery raster tiles via osmdroid — the same provider drone
// mission-planning ground stations use as their default satellite layer.
// No API key, no Cloud Console setup. NavIC positioning works automatically
// on supported devices (Android 11+) via FusedLocationProvider.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun MapPickerScreen(
    onBack: () -> Unit,
    vm: FieldsViewModel = viewModel(factory = FieldsViewModel.Factory),
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val vertices = remember { mutableStateListOf<GeoPoint>() }
    var layerStyle by remember { mutableStateOf(MapLayer.Satellite) }

    // Place search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<NominatimPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var resultsExpanded by remember { mutableStateOf(false) }

    // Live area (square metres → hectares) via spherical-excess formula.
    val areaHa by remember {
        derivedStateOf {
            if (vertices.size < 3) 0.0 else sphericalAreaSquareMetres(vertices) / 10_000.0
        }
    }

    // Show AddFieldSheet details form after the user has set up the map.
    var showDetailsSheet by remember { mutableStateOf(false) }

    // We hold a reference to the MapView so right-side controls can pan/zoom it,
    // plus a typed handle to the overlays we mutate from update().
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val overlaysState = remember { mutableStateOf<MapOverlays?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
    }

    // Centre the map on the device's location once we know it.
    LaunchedEffect(mapViewState.value) {
        val map = mapViewState.value ?: return@LaunchedEffect
        val place = LocationProvider.fastCurrent(context)
        map.controller.setZoom(16.5)
        map.controller.setCenter(GeoPoint(place.latitude, place.longitude))
    }

    Box(modifier = Modifier.fillMaxSize().background(AgroPalette.BgDeep)) {

        // ─── Tile-based map fills the screen ───
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mv.setTileSource(layerStyle.tileSource())
                    mv.setMultiTouchControls(true)
                    mv.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    mv.isVerticalMapRepetitionEnabled = false
                    mv.isHorizontalMapRepetitionEnabled = true
                    mv.minZoomLevel = 3.0
                    mv.maxZoomLevel = 19.0

                    val polygon = Polygon().apply {
                        fillPaint.color = Color(0x4010B981).toArgb()
                        outlinePaint.color = AgroPalette.Primary.toArgb()
                        outlinePaint.strokeWidth = 6f
                    }
                    val previewLine = Polyline().apply {
                        outlinePaint.color = AgroPalette.Primary.toArgb()
                        outlinePaint.strokeWidth = 5f
                    }
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let { vertices += it }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?) = false
                    })

                    // Order matters: events under markers, polygon under markers.
                    mv.overlays.add(eventsOverlay)
                    mv.overlays.add(polygon)
                    mv.overlays.add(previewLine)

                    overlaysState.value = MapOverlays(polygon, previewLine, mutableListOf())
                    mapViewState.value = mv
                    mv.onResume()
                }
            },
            update = { mv ->
                mv.setTileSource(layerStyle.tileSource())

                val o = overlaysState.value ?: return@AndroidView
                o.polygon.points = if (vertices.size >= 3) vertices.toList() else emptyList()
                o.previewLine.setPoints(if (vertices.size == 2) vertices.toList() else emptyList())

                // Drop old markers + re-create. Cheap because count is tiny.
                o.markers.forEach { mv.overlays.remove(it) }
                o.markers.clear()
                vertices.forEachIndexed { i, gp ->
                    val m = Marker(mv).apply {
                        position = gp
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Vertex ${i + 1}"
                    }
                    mv.overlays.add(m)
                    o.markers.add(m)
                }
                mv.invalidate()
            },
        )

        DisposableEffect(Unit) {
            onDispose {
                mapViewState.value?.onPause()
                mapViewState.value?.onDetach()
                mapViewState.value = null
            }
        }

        // ─── Top: back button + search bar (+ floating results list) ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Rounded.ArrowBack, onClick = onBack)
                Spacer(Modifier.width(8.dp))
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { newValue ->
                        searchQuery = newValue
                        if (newValue.isBlank()) {
                            searchResults = emptyList()
                            resultsExpanded = false
                        }
                    },
                    searching = searching,
                    onSearch = {
                        val q = searchQuery.trim()
                        if (q.isEmpty()) return@SearchBar
                        searching = true
                        resultsExpanded = true
                        scope.launch {
                            searchResults = GeocodingApi.search(q)
                            searching = false
                            if (searchResults.isEmpty()) {
                                snackbar.showSnackbar("No matches for \"$q\".")
                                resultsExpanded = false
                            }
                        }
                    },
                    onClear = {
                        searchQuery = ""
                        searchResults = emptyList()
                        resultsExpanded = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Floating results list — only when we have results and the user
            // hasn't dismissed them.
            AnimatedVisibility(visible = resultsExpanded && searchResults.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, top = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xEE0A1118))
                        .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(18.dp)),
                ) {
                    LazyColumn(modifier = Modifier.height((searchResults.size * 64).dp.coerceAtMost(280.dp))) {
                        items(searchResults, key = { it.place_id ?: it.display_name.hashCode().toLong() }) { p ->
                            SearchResultRow(p) {
                                val target = GeoPoint(p.latitude, p.longitude)
                                mapViewState.value?.controller?.animateTo(target, 15.5, 600L)
                                resultsExpanded = false
                                searchQuery = p.shortLabel
                            }
                        }
                    }
                }
            }

            // Status pill — kept underneath the search bar so the vertex count
            // info still shows. Hidden while results are open.
            if (!resultsExpanded) {
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.padding(start = 52.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xCC0A1118))
                            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            when {
                                vertices.isEmpty() -> "Tap the map to add the first corner"
                                vertices.size < 3 -> "${vertices.size}/3+ vertices — keep tapping"
                                else -> "${vertices.size} vertices · ${"%.2f".format(areaHa)} ha"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = AgroPalette.Ink,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // ─── Right-side controls ───
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleIconButton(Icons.Rounded.Layers) {
                layerStyle = if (layerStyle == MapLayer.Satellite) MapLayer.Street else MapLayer.Satellite
                scope.launch { snackbar.showSnackbar("Layer: ${layerStyle.label}") }
            }
            CircleIconButton(Icons.Rounded.MyLocation) {
                scope.launch {
                    val place = LocationProvider.fastCurrent(context)
                    mapViewState.value?.controller?.animateTo(
                        GeoPoint(place.latitude, place.longitude), 17.0, 600L,
                    )
                }
            }
            CircleIconButton(Icons.Rounded.Undo, enabled = vertices.isNotEmpty()) {
                vertices.removeAt(vertices.lastIndex)
            }
            CircleIconButton(Icons.Rounded.DeleteOutline, enabled = vertices.isNotEmpty()) {
                vertices.clear()
            }
        }

        // ─── Bottom bar: area summary + Next button ───
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF00A1118))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (vertices.size >= 3) "${"%.2f".format(areaHa)} ha" else "No area drawn yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (vertices.size >= 3) AgroPalette.Primary else AgroPalette.InkMuted,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (vertices.size >= 3) "${vertices.size} vertices · tap Next to continue"
                        else "Tap map to draw polygon, or tap Next to skip",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkDim,
                    )
                }
                Spacer(Modifier.width(12.dp))
                PrimaryButton(
                    text = "Next",
                    icon = Icons.Rounded.Check,
                    onClick = { showDetailsSheet = true },
                )
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

    if (showDetailsSheet) {
        AddFieldSheet(
            stages = vm.stagePresets,
            accents = vm.accentPresets,
            prefilledArea = areaHa,
            onDismiss = { showDetailsSheet = false },
            onSubmit = { name, crop, area, stage, accent ->
                vm.addField(name, crop, area, stage, 60, accent)
                onBack()
            },
        )
    }
}

@Composable
private fun MapCropSelectorTile(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1A14))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            Icons.Rounded.Check,
            null,
            tint = AgroPalette.Primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
        androidx.compose.material3.Icon(
            Icons.Rounded.Close,
            contentDescription = "Change crop",
            tint = AgroPalette.InkMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer selector
// ─────────────────────────────────────────────────────────────────────────────
private enum class MapLayer(val label: String) {
    Satellite("Satellite"),
    Street("Street");

    fun tileSource(): OnlineTileSourceBase = when (this) {
        Satellite -> EsriWorldImagery
        Street -> TileSourceFactory.MAPNIK
    }
}

/** Esri's free World Imagery tile service. Mission Planner's default satellite layer. */
private val EsriWorldImagery: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "Esri WorldImagery",
    0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Powered by Esri",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

// ─────────────────────────────────────────────────────────────────────────────
// Spherical-excess area in m² (treats Earth as a sphere; accurate to <0.5% at
// field scale — same approach Mission Planner and Survey software use).
// ─────────────────────────────────────────────────────────────────────────────
private fun sphericalAreaSquareMetres(points: SnapshotStateList<GeoPoint>): Double {
    if (points.size < 3) return 0.0
    val r = 6_378_137.0 // mean Earth radius in metres
    var total = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        total += dLon * (2 + sin(Math.toRadians(p1.latitude)) + sin(Math.toRadians(p2.latitude)))
    }
    return abs(total * r * r / 2.0)
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable small widgets
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PickerLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp, fontSize = 10.sp),
        color = AgroPalette.InkMuted,
    )
}

@Composable
private fun PickerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AgroPalette.Primary else Color(0xFF0E1A14))
            .border(1.dp, if (selected) AgroPalette.Primary else Color(0x33FFFFFF), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AgroPalette.BgDeep else AgroPalette.Ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xEE0A1118))
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(start = 12.dp)) {
            if (searching) {
                CircularProgressIndicator(
                    strokeWidth = 1.6.dp,
                    color = AgroPalette.Primary,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(18.dp))
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search places — Nashik, Lake Plot…", color = AgroPalette.InkDim, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = AgroPalette.Ink,
                unfocusedTextColor = AgroPalette.Ink,
                cursorColor = AgroPalette.Primary,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(6.dp),
            ) {
                Icon(Icons.Rounded.Close, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SearchResultRow(place: NominatimPlace, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                place.shortLabel,
                style = MaterialTheme.typography.titleSmall,
                color = AgroPalette.Ink,
                maxLines = 1,
            )
            Text(
                place.display_name,
                style = MaterialTheme.typography.labelSmall,
                color = AgroPalette.InkMuted,
                maxLines = 1,
            )
        }
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
            .background(Color(0xF00A1118))
            .border(1.dp, Color(0x44FFFFFF), CircleShape)
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

/** Typed handle to the mutable osmdroid overlays we sync from Compose state. */
private data class MapOverlays(
    val polygon: Polygon,
    val previewLine: Polyline,
    val markers: MutableList<Marker>,
)
