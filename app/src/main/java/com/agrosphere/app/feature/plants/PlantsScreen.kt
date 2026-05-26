package com.agrosphere.app.feature.plants

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.R
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.data.repo.WateringStatus
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch

private enum class PlantFilter(@StringRes val labelRes: Int) {
    All(R.string.plants_filter_all),
    Flowering(R.string.plants_filter_flowering),
    Indoor(R.string.plants_filter_indoor),
    Succulent(R.string.plants_filter_succulent),
    Herb(R.string.plants_filter_herb),
    Overdue(R.string.plants_filter_overdue),
}

@Composable
fun PlantsScreen(
    padding: PaddingValues,
    onOpenPlant: (String) -> Unit,
    onAddPlant: () -> Unit,
    onScanPlant: (String) -> Unit = {},
    vm: PlantsViewModel = viewModel(factory = PlantsViewModel.Factory),
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var query  by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(PlantFilter.All) }

    val all by vm.plants.collectAsState()

    val filtered = remember(all, query, filter) {
        all.asSequence()
            .filter { p ->
                val q = query.trim().lowercase()
                q.isEmpty() || p.name.lowercase().contains(q) || p.species.lowercase().contains(q)
            }
            .filter { p ->
                when (filter) {
                    PlantFilter.All      -> true
                    PlantFilter.Flowering -> p.species in floweringSpecies
                    PlantFilter.Indoor    -> p.species in indoorSpecies
                    PlantFilter.Succulent -> p.species in succulentSpecies
                    PlantFilter.Herb      -> p.species in herbSpecies
                    PlantFilter.Overdue   -> PlantRepository.isDueOrOverdue(p)
                }
            }
            .sortedWith(compareBy<PlantEntry> {
                when (PlantRepository.wateringStatus(it)) {
                    is WateringStatus.Overdue    -> 0
                    WateringStatus.DueToday      -> 1
                    WateringStatus.NeverLogged   -> 2
                    else                         -> 3
                }
            }.thenByDescending { it.healthScore })
            .toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = padding.calculateBottomPadding()),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PlantTopBlock(
                    allCount     = all.size,
                    dueCount     = all.count { PlantRepository.isDueOrOverdue(it) },
                    healthyCount = all.count { it.healthScore >= 75 },
                )
            }
            item {
                PlantSearchBar(value = query, onChange = { query = it })
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlantFilter.values()) { f ->
                        PlantFilterPill(
                            label    = stringResource(f.labelRes),
                            selected = f == filter,
                            onClick  = { filter = f },
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.plants_results_count, filtered.size, all.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkMuted,
                )
            }
            items(filtered, key = { it.id }) { plant ->
                PlantCard(
                    plant    = plant,
                    onClick  = { onOpenPlant(plant.id) },
                    onScan   = { onScanPlant(plant.id) },
                    onDelete = {
                        vm.deletePlant(plant.id)
                        scope.launch {
                            snackbar.showSnackbar(context.getString(R.string.plant_deleted_snackbar, plant.name))
                        }
                    },
                )
            }
            if (all.isEmpty()) {
                item { PlantEmptyHero(onAdd = onAddPlant) }
            } else if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.plants_empty_filter_title), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                        Text(stringResource(R.string.plants_empty_filter_body), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onAddPlant,
            containerColor = AgroPalette.Primary,
            contentColor   = AgroPalette.BgDeep,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add plant")
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
    }

}

// ─────────────────────────────────────────────────────────────────────────────
// Top metrics block
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantTopBlock(allCount: Int, dueCount: Int, healthyCount: Int) {
    Column {
        ScreenTitle(
            eyebrow = stringResource(R.string.plants_eyebrow),
            title   = stringResource(R.string.plants_title),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PlantMiniMetric(stringResource(R.string.plants_metric_plants),  "$allCount",    AgroPalette.Primary, Modifier.weight(1f))
            PlantMiniMetric(stringResource(R.string.plants_metric_due),     "$dueCount",    AgroPalette.Rose,    Modifier.weight(1f))
            PlantMiniMetric(stringResource(R.string.plants_metric_healthy), "$healthyCount",AgroPalette.Iris,    Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlantMiniMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantSearchBar(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted) },
        placeholder = { Text(stringResource(R.string.plants_search_placeholder), color = AgroPalette.InkDim) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
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

// ─────────────────────────────────────────────────────────────────────────────
// Filter pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "pill-$label")
    val glow by inf.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1400), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "g",
    )
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .drawBehind {
                if (selected) drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.30f * glow),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.65f,
                    )
                )
            }
            .background(
                if (selected) AgroPalette.Primary.copy(alpha = 0.14f)
                else AgroPalette.SurfaceGlass,
                shape,
            )
            .border(
                1.dp,
                if (selected) AgroPalette.Primary.copy(alpha = 0.5f) else AgroPalette.SurfaceGlassBorder,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style     = MaterialTheme.typography.labelMedium,
            color     = if (selected) AgroPalette.Primary else AgroPalette.InkMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Plant card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantCard(plant: PlantEntry, onClick: () -> Unit, onScan: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val status = PlantRepository.wateringStatus(plant)

    val waterColor = when (status) {
        is WateringStatus.Overdue -> AgroPalette.Rose
        WateringStatus.DueToday   -> AgroPalette.Amber
        WateringStatus.NeverLogged -> AgroPalette.InkDim
        else                      -> AgroPalette.Sky
    }
    val waterLabel = when (status) {
        is WateringStatus.Overdue  -> "Overdue ${status.days}d"
        WateringStatus.DueToday    -> "Due today"
        WateringStatus.NeverLogged -> "Not logged"
        is WateringStatus.DueIn    -> "In ${status.days}d"
    }

    GlassCard(radius = 16.dp, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar — shows the plant's most recent scan photo if available,
            // falls back to the LocalFlorist glyph tinted by accent otherwise.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(plant.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                val photo = plant.photoPath?.takeIf { java.io.File(it).exists() }
                if (photo != null) {
                    coil.compose.AsyncImage(
                        model = java.io.File(photo),
                        contentDescription = "${plant.name} photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Rounded.LocalFlorist, null, tint = plant.accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            // Name + species + location
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.name, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                Text(plant.species, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LocationChip(plant.location)
                    WaterChip(label = waterLabel, color = waterColor)
                }
            }
            // Scan button — quick rescan to update health stats without opening detail.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onScan() }
                    .background(AgroPalette.Sky.copy(alpha = 0.16f))
                    .border(1.dp, AgroPalette.Sky.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CameraAlt, "Scan plant", tint = AgroPalette.Sky, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            // Health score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${plant.healthScore}",
                    style     = MaterialTheme.typography.titleSmall,
                    color     = healthColor(plant.healthScore),
                    fontWeight = FontWeight.ExtraBold,
                )
                Text("health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
            Spacer(Modifier.width(8.dp))
            // Delete button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { showDelete = true }
                    .background(AgroPalette.Rose.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = AgroPalette.Rose, modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title  = { Text(stringResource(R.string.plant_delete_confirm_title)) },
            text   = { Text(stringResource(R.string.plant_delete_confirm_body, plant.name)) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) {
                    Text(stringResource(R.string.common_delete), color = AgroPalette.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun LocationChip(location: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(location, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
    }
}

@Composable
private fun WaterChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantEmptyHero(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AgroPalette.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.LocalFlorist, null, tint = AgroPalette.Primary, modifier = Modifier.size(36.dp))
        }
        Text(stringResource(R.string.plants_empty_title), style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.plants_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = AgroPalette.InkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AgroPalette.Primary)
                .clickable(onClick = onAdd)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Add my first plant", style = MaterialTheme.typography.labelLarge, color = AgroPalette.BgDeep, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun healthColor(score: Int): Color = when {
    score >= 80 -> AgroPalette.Primary
    score >= 60 -> AgroPalette.Amber
    else        -> AgroPalette.Rose
}

// Category membership sets for filter logic
private val floweringSpecies = setOf(
    "Rose","Marigold","Hibiscus","Jasmine","Sunflower","Dahlia","Lavender",
    "Petunia","Bougainvillea","Chrysanthemum","Ixora","Adenium","Plumeria",
)
private val indoorSpecies = setOf(
    "Money Plant (Pothos)","Peace Lily","Snake Plant","ZZ Plant","Spider Plant",
    "Rubber Plant","Philodendron","Chinese Evergreen","Dracaena","Fiddle Leaf Fig",
    "Boston Fern","Monstera","Areca Palm","Anthurium",
)
private val succulentSpecies = setOf(
    "Aloe Vera","Jade Plant","Echeveria","Haworthia","Barrel Cactus",
    "Christmas Cactus","Sedum","Kalanchoe",
)
private val herbSpecies = setOf(
    "Basil","Mint","Tulsi","Coriander","Curry Leaf","Lemongrass",
    "Tomato","Chilli","Spinach","Methi (Fenugreek)",
)
