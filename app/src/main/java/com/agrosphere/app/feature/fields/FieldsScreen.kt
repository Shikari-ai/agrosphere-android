package com.agrosphere.app.feature.fields

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
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch

private enum class FilterChip(val label: String) { All("All"), Cereals("Cereals"), Pulses("Pulses"), Rice("Rice"), Healthy("Healthy 80+"), Watch("Needs attention") }
private enum class SortOrder(val label: String) { Health("Health"), Area("Area"), Sown("Recent"), Name("Name") }

@Composable
fun FieldsScreen(
    padding: PaddingValues,
    onOpenField: (String) -> Unit,
    onOpenMap: () -> Unit = {},
    vm: FieldsViewModel = viewModel(factory = FieldsViewModel.Factory),
) {
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
                            Text("Map", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
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
                        FilterPill(label = c.label, selected = c == filter) { filter = c }
                    }
                }
            }
            item {
                SortRow(
                    sort = sort, onSortChange = { sort = it },
                    resultCount = filtered.size, totalCount = all.size,
                )
            }
            items(filtered, key = { it.id }) { field -> FieldRow(field = field, onClick = { onOpenField(field.id) }) }
            if (filtered.isEmpty()) {
                item { EmptyState() }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
            containerColor = AgroPalette.Primary,
            contentColor = AgroPalette.BgDeep,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add field")
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
        ScreenTitle(eyebrow = "Plots", title = "Your fields")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MiniMetric("Fields", "$allCount", AgroPalette.Primary, Modifier.weight(1f))
            MiniMetric("Area", "%.1f ha".format(allArea), AgroPalette.Sky, Modifier.weight(1f))
            MiniMetric("Avg health", "$avgHealth", AgroPalette.Iris, Modifier.weight(1f))
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
        placeholder = { Text("Search fields or crops…", color = AgroPalette.InkDim) },
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
            "$resultCount of $totalCount fields",
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
            Text("Sort: ${sort.label}", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FieldRow(field: Field, onClick: () -> Unit) {
    GlassCard(radius = 20.dp, padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(field.accent.copy(alpha = 0.18f)),
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
                            .background(field.accent),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${field.healthScore}", style = MaterialTheme.typography.headlineSmall, color = field.accent, fontWeight = FontWeight.Black)
                Text("health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    GlassCard(radius = 22.dp, padding = 28.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text("No fields match", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "Try clearing the search or switching filters.",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
        }
    }
}
