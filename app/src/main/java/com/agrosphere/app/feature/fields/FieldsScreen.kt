package com.agrosphere.app.feature.fields

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.R
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch

private enum class FilterChip(@StringRes val labelRes: Int) {
    All(R.string.fields_filter_all),
    Cereals(R.string.fields_filter_cereals),
    Pulses(R.string.fields_filter_pulses),
    Rice(R.string.fields_filter_rice),
    Healthy(R.string.fields_filter_healthy),
    Watch(R.string.fields_filter_attention),
}
private enum class SortOrder(@StringRes val labelRes: Int) {
    Health(R.string.fields_sort_health),
    Area(R.string.fields_sort_area),
    Sown(R.string.fields_sort_recent),
    Name(R.string.fields_sort_name),
}

@Composable
fun FieldsScreen(
    padding: PaddingValues,
    onOpenField: (String) -> Unit,
    onOpenMap: () -> Unit = {},
    onOpenMapPicker: () -> Unit = {},
    vm: FieldsViewModel = viewModel(factory = FieldsViewModel.Factory),
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FilterChip.All) }
    var sort by remember { mutableStateOf(SortOrder.Health) }
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val all by vm.fields.collectAsState()
    val filtered = remember(all, query, filter, sort) {
        all.asSequence()
            .filter { f ->
                val q = query.trim().lowercase()
                q.isEmpty() || f.name.lowercase().contains(q) || f.crop.lowercase().contains(q)
            }
            .filter { f ->
                when (filter) {
                    FilterChip.All -> true
                    FilterChip.Cereals -> f.crop in setOf("Wheat", "Maize")
                    FilterChip.Pulses -> f.crop in setOf("Soybean")
                    FilterChip.Rice -> f.crop == "Rice"
                    FilterChip.Healthy -> f.healthScore >= 80
                    FilterChip.Watch -> f.healthScore < 70
                }
            }
            .sortedWith(
                when (sort) {
                    SortOrder.Health -> compareByDescending { it.healthScore }
                    SortOrder.Area -> compareByDescending { it.areaHa }
                    SortOrder.Sown -> compareBy { it.sownDaysAgo }
                    SortOrder.Name -> compareBy { it.name }
                }
            )
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        TopBlock(allCount = all.size, allArea = all.sumOf { it.areaHa }, avgHealth = all.map { it.healthScore }.average().toInt())
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AgroPalette.SurfaceGlass)
                            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                            .clickable(onClick = onOpenMap)
                            .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Map, null, tint = AgroPalette.Sky, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.fields_map_chip), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                SearchBar(value = query, onChange = { query = it })
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterChip.values()) { c ->
                        FilterPill(label = stringResource(c.labelRes), selected = c == filter) { filter = c }
                    }
                }
            }
            item {
                SortRow(
                    sort = sort, onSortChange = { sort = it },
                    resultCount = filtered.size, totalCount = all.size,
                )
            }
            items(filtered, key = { it.id }) { field ->
                FieldRow(
                    field = field,
                    onClick = { onOpenField(field.id) },
                    onDelete = {
                        vm.deleteField(field.id)
                        scope.launch { snackbar.showSnackbar(context.getString(R.string.field_deleted_snackbar, field.name)) }
                    },
                )
            }
            if (all.isEmpty()) {
                item { FirstFieldHero(onAdd = { showAddSheet = true }, onDrawOnMap = onOpenMapPicker) }
            } else if (filtered.isEmpty()) {
                item { FilteredEmptyState() }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        // Dual FAB cluster — small "Draw on map" chip + primary "Add field" plus.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.SurfaceGlass)
                    .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                    .clickable(onClick = onOpenMapPicker)
                    .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Map, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fields_draw_on_map_chip), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
            }
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = AgroPalette.Primary,
                contentColor = AgroPalette.BgDeep,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add field")
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
    }

    if (showAddSheet) {
        AddFieldSheet(
            crops = vm.cropPresets,
            stages = vm.stagePresets,
            accents = vm.accentPresets,
            onDismiss = { showAddSheet = false },
            onSubmit = { name, crop, area, stage, moisture, accent ->
                vm.addField(name, crop, area, stage, moisture, accent)
                scope.launch { snackbar.showSnackbar("$name added.") }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBlock(allCount: Int, allArea: Double, avgHealth: Int) {
    Column {
        ScreenTitle(eyebrow = stringResource(R.string.fields_eyebrow), title = stringResource(R.string.fields_title))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MiniMetric(stringResource(R.string.fields_metric_fields), "$allCount", AgroPalette.Primary, Modifier.weight(1f))
            MiniMetric(stringResource(R.string.fields_metric_area), "%.1f ha".format(allArea), AgroPalette.Sky, Modifier.weight(1f))
            MiniMetric(stringResource(R.string.fields_metric_avg_health), "$avgHealth", AgroPalette.Iris, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted) },
        placeholder = { Text(stringResource(R.string.fields_search_placeholder), color = AgroPalette.InkDim) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroPalette.Primary,
            unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
            focusedTextColor = AgroPalette.Ink,
            unfocusedTextColor = AgroPalette.Ink,
            cursorColor = AgroPalette.Primary,
            focusedContainerColor = AgroPalette.SurfaceGlass,
            unfocusedContainerColor = AgroPalette.SurfaceGlass,
        ),
    )
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "pill-$label")
    val glow  by inf.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1400), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "g",
    )
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .drawBehind {
                if (selected) {
                    // Subtle radial glow behind selected pill
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to AgroPalette.Primary.copy(alpha = 0.30f * glow),
                            1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.65f,
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                }
            }
            .background(if (selected) AgroPalette.Primary else AgroPalette.SurfaceGlass)
            .border(1.dp, if (selected) AgroPalette.Primary.copy(alpha = 0.8f) else AgroPalette.SurfaceGlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = if (selected) AgroPalette.BgDeep else AgroPalette.InkMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SortRow(sort: SortOrder, onSortChange: (SortOrder) -> Unit, resultCount: Int, totalCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.fields_results_count, resultCount, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = AgroPalette.InkMuted,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AgroPalette.SurfaceGlass)
                .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                .clickable {
                    val next = SortOrder.values().let { it[(it.indexOf(sort) + 1) % it.size] }
                    onSortChange(next)
                }
                .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Sort, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.fields_sort_label, stringResource(sort.labelRes)), style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FieldRow(field: Field, onClick: () -> Unit, onDelete: () -> Unit = {}) {
    var showConfirm by remember { mutableStateOf(false) }

    // Shimmer for the health bar
    val inf    = rememberInfiniteTransition(label = "field-bar-${field.id}")
    val shimX  by inf.animateFloat(
        -0.5f, 1.5f,
        infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sx",
    )

    GlassCard(
        radius = 20.dp, padding = 16.dp, onClick = onClick,
        border = BorderStroke(1.dp, field.accent.copy(alpha = 0.28f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(field.accent.copy(alpha = 0.18f))
                    .border(1.dp, field.accent.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Grass, null, tint = field.accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(field.name, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Text(
                    "${field.crop} · ${field.areaHa} ha · ${field.stage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.height(8.dp))
                // Animated health bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AgroPalette.SurfaceGlassBorder),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(field.healthScore / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    0f to field.accent.copy(alpha = 0.55f),
                                    1f to field.accent,
                                )
                            )
                            .drawWithContent {
                                drawContent()
                                // Shimmer sweep
                                val bH = size.width * 0.28f
                                val cx = shimX * size.width
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f   to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.45f),
                                        1f   to Color.Transparent,
                                        startX = cx - bH, endX = cx + bH,
                                    ),
                                    topLeft = Offset.Zero,
                                    size    = Size(size.width, size.height),
                                )
                            },
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${field.healthScore}", style = MaterialTheme.typography.headlineSmall, color = field.accent, fontWeight = FontWeight.Black)
                Text("health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { showConfirm = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = AgroPalette.Rose.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.field_delete_confirm_title)) },
            text = { Text(stringResource(R.string.field_delete_confirm_body, field.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.common_delete), color = AgroPalette.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun FilteredEmptyState() {
    GlassCard(radius = 22.dp, padding = 28.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.fields_empty_filter_title), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.fields_empty_filter_body),
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun FirstFieldHero(onAdd: () -> Unit, onDrawOnMap: () -> Unit = {}) {
    GlassCard(
        background = com.agrosphere.app.ui.theme.AgroBrushes.leafCard,
        radius = 26.dp,
        padding = 28.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(AgroPalette.PrimaryDim),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Primary, modifier = Modifier.size(36.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.empty_first_field_title), style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.empty_first_field_body),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            com.agrosphere.app.ui.components.PrimaryButton(
                text = stringResource(R.string.action_add_field),
                icon = Icons.Rounded.Add,
                onClick = onAdd,
            )
            Spacer(Modifier.height(8.dp))
            com.agrosphere.app.ui.components.GhostButton(
                text = stringResource(R.string.fields_first_or_draw),
                onClick = onDrawOnMap,
            )
        }
    }
}
