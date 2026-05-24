package com.agrosphere.app.feature.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.crops.CropData
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldSheet(
    crops: List<String> = emptyList(),   // kept for API compat but unused — CropData is the source
    stages: List<String>,
    accents: List<Color>,
    prefilledArea: Double = 0.0,
    onDismiss: () -> Unit,
    onSubmit: (name: String, crop: String, areaHa: Double, stage: String, accent: Color) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf(CropData.categories.first().crops.first().cropName) }
    var showCropPicker by remember { mutableStateOf(false) }
    var areaStr by remember { mutableStateOf(if (prefilledArea > 0.0) "%.2f".format(prefilledArea) else "") }
    var stage by remember { mutableStateOf(stages.first()) }
    var accent by remember { mutableStateOf(accents.first()) }

    val areaHa = areaStr.toDoubleOrNull()
    val canSubmit = name.isNotBlank() && areaHa != null && areaHa > 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AgroPalette.Surface,
        scrimColor = Color(0xAA000000),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, null, tint = accent) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("New field", style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    Text("Add a plot to start monitoring", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }

            // Name
            FormField(label = "Field name", value = name, onChange = { name = it }, placeholder = "e.g. North paddock")

            // Crop picker tile
            FormLabel("Crop")
            CropSelectorTile(value = crop, onClick = { showCropPicker = true })

            // Area
            FormField(
                label = "Area (ha)",
                value = areaStr,
                onChange = { v -> areaStr = v.filter { it.isDigit() || it == '.' }.take(6) },
                placeholder = "0.0",
                keyboard = KeyboardType.Decimal,
            )

            // Stage chips
            FormLabel("Stage")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stages) { s -> SelectChip(label = s, selected = s == stage) { stage = s } }
            }

            // Accent picker
            FormLabel("Accent")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accents.forEach { c ->
                    AccentSwatch(color = c, selected = c == accent) { accent = c }
                }
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = if (canSubmit) "Save field" else "Fill name + area to save",
                icon = Icons.Rounded.Check,
                enabled = canSubmit,
                onClick = {
                    onSubmit(name, crop, areaHa ?: 0.0, stage, accent)
                    onDismiss()
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showCropPicker) {
        CropPickerSheet(
            currentValue = crop,
            onDismiss = { showCropPicker = false },
            onSelect = { crop = it },
        )
    }
}

@Composable
private fun CropSelectorTile(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AgroPalette.Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Tap to browse all crops & varieties",
                style = MaterialTheme.typography.labelSmall,
                color = AgroPalette.InkMuted,
            )
        }
        Icon(Icons.Rounded.ExpandMore, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp, fontSize = 10.sp),
        color = AgroPalette.InkMuted,
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = AgroPalette.InkDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
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
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) AgroPalette.Ink else color.copy(alpha = 0.5f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
