package com.agrosphere.app.feature.fields

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.crops.CropCategory
import com.agrosphere.app.data.crops.CropData
import com.agrosphere.app.data.crops.CropVariety
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.theme.AgroPalette
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CropPickerSheet(
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CropData.categories.first()) }
    val expandedCrops = remember { mutableStateOf(setOf<String>()) }

    val searchResults = remember(searchQuery) { CropData.search(searchQuery) }
    val isSearching = searchQuery.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AgroPalette.Surface,
        scrimColor = Color(0xAA000000),
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Select crop",
                        style = MaterialTheme.typography.titleLarge,
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.Bold,
                    )
                    if (currentValue.isNotBlank()) {
                        Text(
                            "Current: $currentValue",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = AgroPalette.InkMuted)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Search bar ───────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                placeholder = {
                    Text(
                        "Search any crop or variety…",
                        color = AgroPalette.InkDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
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

            Spacer(Modifier.height(12.dp))

            if (isSearching) {
                // ── Search results ────────────────────────────────────────────
                SearchResultsList(
                    results = searchResults,
                    currentValue = currentValue,
                    onSelect = { onSelect(it); onDismiss() },
                )
            } else {
                // ── Browse: category tabs + crop list ─────────────────────────
                CategoryTabRow(
                    categories = CropData.categories,
                    selected = selectedCategory,
                    onSelect = {
                        selectedCategory = it
                        expandedCrops.value = emptySet()
                    },
                )
                Spacer(Modifier.height(8.dp))
                CropList(
                    category = selectedCategory,
                    currentValue = currentValue,
                    expandedCrops = expandedCrops.value,
                    onToggleExpand = { cropName ->
                        expandedCrops.value = if (cropName in expandedCrops.value)
                            expandedCrops.value - cropName
                        else
                            expandedCrops.value + cropName
                    },
                    onSelectCrop = { onSelect(it); onDismiss() },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category tab row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryTabRow(
    categories: List<CropCategory>,
    selected: CropCategory,
    onSelect: (CropCategory) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        items(categories) { cat ->
            val isSel = cat == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSel) AgroPalette.Primary else AgroPalette.SurfaceGlass)
                    .border(1.dp, if (isSel) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (isSel) AgroPalette.BgDeep else AgroPalette.InkMuted,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Crop list for the selected category
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CropList(
    category: CropCategory,
    currentValue: String,
    expandedCrops: Set<String>,
    onToggleExpand: (String) -> Unit,
    onSelectCrop: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 4.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(category.crops, key = { it.cropName }) { crop ->
            CropRow(
                crop = crop,
                currentValue = currentValue,
                isExpanded = crop.cropName in expandedCrops,
                onToggleExpand = { onToggleExpand(crop.cropName) },
                onSelectCrop = onSelectCrop,
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CropRow(
    crop: CropVariety,
    currentValue: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectCrop: (String) -> Unit,
) {
    val isCropSelected = currentValue == crop.cropName
    val hasVarieties = crop.varieties.isNotEmpty()

    Column {
        // ── Crop header row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(if (isExpanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RoundedCornerShape(16.dp))
                .background(
                    when {
                        isCropSelected -> AgroPalette.Primary.copy(alpha = 0.15f)
                        isExpanded -> AgroPalette.SurfaceGlass
                        else -> AgroPalette.SurfaceGlass
                    }
                )
                .border(
                    1.dp,
                    if (isCropSelected) AgroPalette.Primary.copy(alpha = 0.5f) else AgroPalette.SurfaceGlassBorder,
                    if (isExpanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RoundedCornerShape(16.dp),
                )
                .clickable {
                    if (hasVarieties) onToggleExpand()
                    else onSelectCrop(crop.cropName)
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Crop icon dot
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCropSelected) AgroPalette.Primary.copy(alpha = 0.2f) else AgroPalette.SurfaceGlassBorder),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Grass,
                    null,
                    tint = if (isCropSelected) AgroPalette.Primary else AgroPalette.InkMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    crop.cropName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCropSelected) AgroPalette.Primary else AgroPalette.Ink,
                    fontWeight = if (isCropSelected) FontWeight.Bold else FontWeight.Medium,
                )
                if (hasVarieties) {
                    Text(
                        "${crop.varieties.size} varieties",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            // Right-side: checkmark if selected, or expand chevron if has varieties
            if (isCropSelected && !isExpanded) {
                Icon(Icons.Rounded.Check, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp))
            } else if (hasVarieties) {
                Icon(
                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = AgroPalette.InkMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Expanded varieties ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded && hasVarieties,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(AgroPalette.SurfaceGlass.copy(alpha = 0.7f))
                    .border(
                        1.dp,
                        AgroPalette.SurfaceGlassBorder,
                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    )
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
            ) {
                // "Just this crop" option at top
                VarietyChip(
                    label = crop.cropName,
                    sublabel = "no specific variety",
                    selected = currentValue == crop.cropName,
                    onSelect = { onSelectCrop(crop.cropName) },
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    crop.varieties.forEach { variety ->
                        val displayVal = "${crop.cropName} · $variety"
                        VarietyChip(
                            label = variety,
                            selected = currentValue == displayVal,
                            onSelect = { onSelectCrop(displayVal) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VarietyChip(
    label: String,
    sublabel: String? = null,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AgroPalette.Primary else AgroPalette.BgDeep.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (selected) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = if (selected) AgroPalette.BgDeep else AgroPalette.Ink,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (sublabel != null) {
                Text(
                    sublabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (selected) AgroPalette.BgDeep.copy(alpha = 0.7f) else AgroPalette.InkDim,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search results list
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchResultsList(
    results: List<CropData.CropEntry>,
    currentValue: String,
    onSelect: (String) -> Unit,
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Search, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text("No crops found", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text("Try a different name or variety", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(results, key = { it.displayName }) { entry ->
            val isSelected = currentValue == entry.displayName
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) AgroPalette.Primary.copy(alpha = 0.15f) else AgroPalette.SurfaceGlass)
                    .border(
                        1.dp,
                        if (isSelected) AgroPalette.Primary.copy(alpha = 0.5f) else AgroPalette.SurfaceGlassBorder,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(entry.displayName) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AgroPalette.Primary.copy(alpha = 0.2f) else AgroPalette.SurfaceGlassBorder),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Grass, null, tint = if (isSelected) AgroPalette.Primary else AgroPalette.InkMuted, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) AgroPalette.Primary else AgroPalette.Ink,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        entry.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkMuted,
                    )
                }
                if (isSelected) {
                    Icon(Icons.Rounded.Check, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
