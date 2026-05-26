package com.agrosphere.app.feature.plants

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.R
import com.agrosphere.app.data.model.PlantEntry
import com.agrosphere.app.data.model.PlantScanRecord
import com.agrosphere.app.data.plants.PlantData
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.data.repo.WateringStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PlantDetailTab(val label: String) {
    Care("Care"), Activity("Activity"), Health("Health")
}

@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    onRescan: () -> Unit = {},
    onDelete: () -> Unit = onBack,
) {
    // Live subscribe to the repo — any change (watering, scan, stage) re-renders.
    val allPlants by PlantRepository.plants.collectAsState()
    val currentPlant = allPlants.firstOrNull { it.id == plantId }
    if (currentPlant == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var tab by remember { mutableStateOf(PlantDetailTab.Care) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        PlantHeroBar(plant = currentPlant, onBack = onBack)
        PlantTabStrip(selected = tab, onSelect = { tab = it })

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                PlantDetailTab.Care     -> CareTab(
                    plant = currentPlant,
                    onLogWatering = { PlantRepository.logWatering(currentPlant.id) },
                    onRescan      = onRescan,
                )
                PlantDetailTab.Activity -> ActivityTab(plant = currentPlant)
                PlantDetailTab.Health   -> HealthTab(plant = currentPlant)
            }
        }

        // ── Delete button ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroPalette.BgDeep.copy(alpha = 0.92f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            PrimaryButton(
                text  = stringResource(R.string.plant_delete_btn),
                icon  = Icons.Rounded.DeleteOutline,
                brush = androidx.compose.ui.graphics.SolidColor(AgroPalette.Rose),
                onClick = { showDeleteConfirm = true },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text(stringResource(R.string.plant_delete_confirm_title)) },
            text    = { Text(stringResource(R.string.plant_delete_confirm_body, currentPlant.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.common_delete), color = AgroPalette.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantHeroBar(plant: PlantEntry, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        plant.accent.copy(alpha = 0.22f),
                        plant.accent.copy(alpha = 0.06f),
                        AgroPalette.BgDeep,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "PLANT",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = AgroPalette.InkMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            // Mood is derived live from the plant's current state and reused by
            // both the emoji badge on the avatar and the caption next to the name.
            val mood = remember(plant) { plantMood(plant) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile avatar — uses the most recent scan photo for this plant
                // when available, otherwise falls back to the LocalFlorist glyph
                // tinted by the plant's accent colour. The mood emoji rides the
                // top-right corner as a constant indicator of how it's doing.
                Box(modifier = Modifier.size(64.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(plant.accent.copy(alpha = 0.28f))
                            .border(1.5.dp, plant.accent.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val photo = plant.photoPath?.takeIf { File(it).exists() }
                        if (photo != null) {
                            AsyncImage(
                                model = File(photo),
                                contentDescription = "${plant.name} photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Rounded.LocalFlorist, null, tint = plant.accent, modifier = Modifier.size(30.dp))
                        }
                    }
                    // Mood emoji badge — small circle hugging the top-right of the photo.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AgroPalette.BgDeep)
                            .border(2.dp, mood.tint.copy(alpha = 0.70f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(mood.emoji, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plant.species, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted, maxLines = 1)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(AgroPalette.InkDim),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(plant.location, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted, maxLines = 1)
                    }
                    // Live mood caption — reads out the same feeling the emoji shows.
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${mood.emoji} ${mood.caption}",
                        style = MaterialTheme.typography.labelSmall,
                        color = mood.tint,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        // Subtle bottom hairline so the hero reads as a distinct section
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(AgroPalette.SurfaceGlassBorder),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlantTabStrip(selected: PlantDetailTab, onSelect: (PlantDetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlantDetailTab.values().forEach { tab ->
            val sel = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) AgroPalette.Primary.copy(alpha = 0.18f) else AgroPalette.SurfaceGlass)
                    .border(1.dp, if (sel) AgroPalette.Primary.copy(alpha = 0.4f) else AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style     = MaterialTheme.typography.labelMedium,
                    color     = if (sel) AgroPalette.Primary else AgroPalette.InkMuted,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Care tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CareTab(
    plant: PlantEntry,
    onLogWatering: () -> Unit,
    onRescan: () -> Unit,
) {
    val status    = PlantRepository.wateringStatus(plant)
    val speciesInfo = PlantData.find(plant.species)
    val latest = plant.scanHistory.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Rescan card — shows last scan timestamp + a real Scan-now button ──
        item {
            GlassCard(
                radius = 16.dp,
                padding = 16.dp,
                background = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(AgroPalette.Primary.copy(alpha = 0.20f), AgroPalette.Iris.copy(alpha = 0.08f))
                ),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "LAST SCAN",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = AgroPalette.InkMuted,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (latest == null) "Never scanned"
                                else formatDateTime(latest.timestamp),
                                style = MaterialTheme.typography.titleSmall,
                                color = AgroPalette.Ink,
                                fontWeight = FontWeight.Bold,
                            )
                            if (latest != null) {
                                Text(
                                    "${relativeDays(latest.timestamp)} · health ${latest.healthScore}/100",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgroPalette.InkMuted,
                                )
                            } else {
                                Text(
                                    "Scan to populate health, stage and care tips.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgroPalette.InkMuted,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Prominent Scan-now button — distinct affordance, not just a clickable card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AgroPalette.Primary)
                            .clickable(onClick = onRescan)
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(18.dp))
                            Text(
                                if (latest == null) "Scan now" else "Scan again",
                                style = MaterialTheme.typography.labelLarge,
                                color = AgroPalette.BgDeep,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // ── Growth stage — auto-detected from scans, read-only ────────────────
        item {
            val stageTint = stageColor(plant.stage)
            val hasScanned = plant.lastScanMs > 0L
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(stageTint.copy(alpha = 0.18f))
                            .border(1.dp, stageTint.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(stageIcon(plant.stage), null, tint = stageTint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GROWTH STAGE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = AgroPalette.InkMuted, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(plant.stage, style = MaterialTheme.typography.titleLarge, color = stageTint, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (hasScanned) "Assessed from scan ${relativeDays(plant.lastScanMs)}"
                            else "Scan the plant to assess its current stage",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
            }
        }
        // ── Watering card ─────────────────────────────────────────────────────
        item {
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WaterDrop, null, tint = AgroPalette.Sky, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watering", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))

                    val (statusText, statusColor) = when (status) {
                        is WateringStatus.Overdue  -> {
                            val s = if (status.days == 1) "" else "s"
                            stringResource(R.string.plant_water_overdue, status.days, s) to AgroPalette.Rose
                        }
                        WateringStatus.DueToday    -> stringResource(R.string.plant_water_due_today) to AgroPalette.Amber
                        WateringStatus.NeverLogged -> stringResource(R.string.plant_water_never) to AgroPalette.InkDim
                        is WateringStatus.DueIn    -> {
                            val s = if (status.days == 1) "" else "s"
                            stringResource(R.string.plant_water_due_in, status.days, s) to AgroPalette.Sky
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.10f))
                            .padding(12.dp),
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (plant.lastWateredMs > 0L) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Last watered: ${formatDateTime(plant.lastWateredMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.InkMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    // Interval row — show the AI/species rationale so the cadence
                    // isn't read as an arbitrary default.
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.plant_care_interval),
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroPalette.InkMuted,
                            )
                            Text(
                                "Recommended for ${plant.species}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.InkDim,
                            )
                        }
                        Text(
                            stringResource(R.string.plant_care_days, plant.wateringIntervalDays),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.Sky,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AgroPalette.Sky.copy(alpha = 0.18f))
                            .border(1.dp, AgroPalette.Sky.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onLogWatering)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.WaterDrop, null, tint = AgroPalette.Sky, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.plant_water_log_btn), style = MaterialTheme.typography.labelLarge, color = AgroPalette.Sky, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Care guide card ───────────────────────────────────────────────────
        item {
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = AgroPalette.Amber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Care Guide", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                    CareIconRow(Icons.Rounded.WbSunny, stringResource(R.string.plant_care_sunlight), plant.sunlightNeed, AgroPalette.Amber)
                    // Prefer AI-identified soil/care over the static catalog — fall back to catalog only when AI didn't provide it.
                    val soilType = plant.soilType.ifBlank { speciesInfo?.soilType.orEmpty() }
                    val careNote = plant.careNote.ifBlank { speciesInfo?.careNote.orEmpty() }
                    if (soilType.isNotBlank()) {
                        CareIconRow(Icons.Rounded.Grass, stringResource(R.string.plant_care_soil), soilType, AgroPalette.Primary)
                    }
                    if (plant.scientificName.isNotBlank()) {
                        CareIconRow(Icons.Rounded.LocalFlorist, "Scientific name", plant.scientificName, AgroPalette.Iris)
                    }
                    if (careNote.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AgroPalette.Primary.copy(alpha = 0.07f))
                                .padding(10.dp),
                        ) {
                            Text("💡 $careNote", style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink)
                        }
                    }
                    CareInfoRow(label = "Pot", value = plant.potSize)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun CareInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CareIconRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity tab
// ─────────────────────────────────────────────────────────────────────────────

/** Activity tab — merged timeline of scan results + waterings, newest first. */
@Composable
private fun ActivityTab(plant: PlantEntry) {
    val timeline = remember(plant) {
        val scans   = plant.scanHistory.map { ActivityItem.Scan(it) }
        val waters  = plant.wateringLog.map { ActivityItem.Water(it) }
        (scans + waters).sortedByDescending { it.timestamp }
    }

    if (timeline.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.plant_activity_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Profile timeline",
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(timeline.size) { idx ->
                when (val item = timeline[idx]) {
                    is ActivityItem.Water -> WateringRow(item.timestamp)
                    is ActivityItem.Scan  -> ScanRow(item.record)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private sealed class ActivityItem(val timestamp: Long) {
    class Water(ts: Long) : ActivityItem(ts)
    class Scan(val record: PlantScanRecord) : ActivityItem(record.timestamp)
}

@Composable
private fun WateringRow(ts: Long) {
    GlassCard(radius = 12.dp, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(AgroPalette.Sky.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WaterDrop, null, tint = AgroPalette.Sky, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Watered", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                Text(formatDateTime(ts), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            }
        }
    }
}

@Composable
private fun ScanRow(record: PlantScanRecord) {
    val color = when (record.riskLevel.lowercase()) {
        "healthy" -> AgroPalette.Primary
        "low"     -> AgroPalette.Sky
        "medium"  -> AgroPalette.Amber
        else      -> AgroPalette.Rose
    }
    GlassCard(radius = 12.dp, padding = 12.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AutoAwesome, null, tint = color, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.verdict, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                    Text(formatDateTime(record.timestamp), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
                Text("${record.healthScore}", style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.ExtraBold)
            }
            if (record.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(record.summary, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            if (record.recommendations.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                record.recommendations.take(3).forEach { rec ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(rec, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Health tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HealthTab(plant: PlantEntry) {
    val score      = plant.healthScore
    val target     by animateFloatAsState(score / 100f, tween(800), label = "ring")
    val grade      = healthGrade(score)
    val ringColor  = gradeColor(grade)
    val latestScan = plant.scanHistory.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── Animated health ring + grade label ────────────────────────────────
        item {
            Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                    val inset  = 8.dp.toPx()
                    drawArc(AgroPalette.SurfaceGlass, -90f, 360f, false, style = stroke,
                        topLeft = Offset(inset, inset), size = GeomSize(size.width - inset * 2, size.height - inset * 2))
                    drawArc(ringColor, -90f, 360f * target, false, style = stroke,
                        topLeft = Offset(inset, inset), size = GeomSize(size.width - inset * 2, size.height - inset * 2))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", style = MaterialTheme.typography.displayMedium, color = ringColor, fontWeight = FontWeight.ExtraBold)
                    Text("of 100", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
            }
        }

        // Big grade label under the ring
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(grade.uppercase(), style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 2.sp), color = ringColor, fontWeight = FontWeight.ExtraBold)
                if (plant.lastScanMs > 0L) {
                    Text(
                        "Assessed ${relativeDays(plant.lastScanMs)} from a real scan",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkDim,
                    )
                } else {
                    Text(
                        "Default score — no scans yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkDim,
                    )
                }
            }
        }

        // ── 7-grade ladder (visual scale) ──────────────────────────────────────
        item { GradeLadder(currentGrade = grade) }

        // ── Description card — what this grade means ───────────────────────────
        item {
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(ringColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Lightbulb, null, tint = ringColor, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text("What \"$grade\" means", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        gradeDescription(grade),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroPalette.InkMuted,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // ── How to improve — combines latest scan recs + grade-based guidance ─
        item {
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(AgroPalette.Sky.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Sky, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text("How to improve", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                    val tips = buildImprovementTips(plant, grade, latestScan)
                    if (tips.isEmpty()) {
                        Text(
                            "Run a scan with the camera — AI will spot any issues and suggest specific fixes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroPalette.InkMuted,
                        )
                    } else {
                        tips.forEachIndexed { idx, tip ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(ringColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = ringColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(tip, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ── Latest scan summary (if any) ───────────────────────────────────────
        if (latestScan != null) {
            item {
                GlassCard(radius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("LAST AI ASSESSMENT", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = AgroPalette.InkMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(relativeDays(latestScan.timestamp), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                        }
                        Text(latestScan.verdict, style = MaterialTheme.typography.titleSmall, color = ringColor, fontWeight = FontWeight.Bold)
                        if (latestScan.summary.isNotBlank()) {
                            Text(latestScan.summary, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7-grade ladder visual — shows where this plant sits on the scale
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradeLadder(currentGrade: String) {
    // Worst → Bad → Okay → Moderate → Good → Great → Epic (ascending)
    val grades = listOf("Worst", "Bad", "Okay", "Moderate", "Good", "Great", "Epic")
    GlassCard(radius = 14.dp, padding = 12.dp) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                grades.forEach { g ->
                    val active = g == currentGrade
                    val color = gradeColor(g)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.dp)
                                .fillMaxWidth()
                                .height(if (active) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) color else color.copy(alpha = 0.18f)),
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            g,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (active) color else AgroPalette.InkDim,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grade scale: maps 0-100 → one of 7 grades; tint + description for each.
// ─────────────────────────────────────────────────────────────────────────────

private fun healthGrade(score: Int): String = when {
    score >= 95 -> "Epic"
    score >= 85 -> "Great"
    score >= 70 -> "Good"
    score >= 55 -> "Moderate"
    score >= 40 -> "Okay"
    score >= 25 -> "Bad"
    else        -> "Worst"
}

private fun gradeColor(grade: String): Color = when (grade) {
    "Epic"     -> Color(0xFF34D399)   // brightest emerald
    "Great"    -> AgroPalette.Primary
    "Good"     -> AgroPalette.Sky
    "Moderate" -> AgroPalette.Amber
    "Okay"     -> AgroPalette.Orange
    "Bad"      -> AgroPalette.Rose
    "Worst"    -> Color(0xFFC0223C)   // deep crimson
    else       -> AgroPalette.InkDim
}

private fun gradeDescription(grade: String): String = when (grade) {
    "Epic"     -> "Picture-perfect. Lush, vigorous, no visible stress anywhere. Whatever you're doing, keep doing it — this plant is thriving in its current conditions."
    "Great"    -> "Strong and healthy. Foliage is dense, colour is vibrant, growth is consistent. Minor cosmetic issues at most — stay on the same rhythm."
    "Good"     -> "Generally healthy with a few small areas to watch. No major problems but worth checking on weekly so anything new doesn't escalate."
    "Moderate" -> "Some signs of stress are showing. Could be early disease pressure, watering imbalance, or wrong light. Catch it now and recovery is quick."
    "Okay"     -> "Visible decline. Yellowing, drooping, or patchy growth. The plant is telling you something — adjust care and re-scan in a few days."
    "Bad"      -> "Significant damage or active disease. Needs intervention now — likely pest, fungal infection, root issues, or severe environmental mismatch."
    "Worst"    -> "Critical condition. Major dieback, heavy infestation, or near-failure. Quarantine from other plants, treat aggressively, prune affected parts."
    else       -> "—"
}

/** Builds the ranked improvement list — AI scan recommendations first (they're
 *  plant-specific and live data), grade-based generic tips after, and the
 *  species' own care note (from PlantData / AI ID) as a baseline reminder. */
private fun buildImprovementTips(plant: PlantEntry, grade: String, latestScan: PlantScanRecord?): List<String> {
    val out = mutableListOf<String>()
    // 1. Real AI recommendations from the most recent scan — top 3.
    latestScan?.recommendations?.take(3)?.forEach { out += it }
    // 2. Grade-based generic guidance — picked so it doesn't repeat scan advice tone.
    out += when (grade) {
        "Epic"     -> "Maintain the current watering and light setup — don't change a winning routine."
        "Great"    -> "Inspect new growth weekly for the first sign of pests or yellowing."
        "Good"     -> "Wipe leaves with a damp cloth monthly so dust doesn't block photosynthesis."
        "Moderate" -> "Check soil moisture with a finger — top 2 cm should dry between waterings."
        "Okay"     -> "Move closer to its preferred light, rotate weekly for even growth, hold back on fertiliser until it stabilises."
        "Bad"      -> "Re-scan now with a closer photo of the affected area to identify the exact issue."
        "Worst"    -> "Isolate from other plants immediately. Prune all severely affected parts with a sterile blade."
        else       -> ""
    }
    // 3. Species-specific tip (from AI ID or catalog) — only if non-empty.
    if (plant.careNote.isNotBlank() && out.none { it.equals(plant.careNote, ignoreCase = true) }) {
        out += plant.careNote
    }
    return out.filter { it.isNotBlank() }.distinct().take(5)
}

// ─────────────────────────────────────────────────────────────────────────────
// Date helpers
// ─────────────────────────────────────────────────────────────────────────────

private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

private fun formatDateTime(ms: Long): String = dateTimeFormat.format(Date(ms))

private fun relativeDays(ms: Long): String {
    val days = ((System.currentTimeMillis() - ms) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        days < 7  -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        else      -> "${days / 30}mo ago"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Live plant "mood" — derived from water status + health + stage. Drives both
// the emoji badge on the hero avatar and the caption next to it.
// ─────────────────────────────────────────────────────────────────────────────
private data class PlantMood(val emoji: String, val caption: String, val tint: Color)

private fun plantMood(plant: PlantEntry): PlantMood {
    val water  = PlantRepository.wateringStatus(plant)
    val h      = plant.healthScore
    val stage  = plant.stage

    // ── Critical states first — these override the stage-based moods ────────
    if (water is WateringStatus.Overdue && water.days >= 3) {
        return PlantMood("😵", "Severely thirsty", AgroPalette.Rose)
    }
    if (h < 25) {
        return PlantMood("🤒", "Needs urgent care", AgroPalette.Rose)
    }
    if (water is WateringStatus.Overdue) {
        return PlantMood("🥺", "Thirsty", AgroPalette.Rose)
    }

    // ── Stage-specific cheerful states ──────────────────────────────────────
    when (stage) {
        "Dormant"    -> return PlantMood("😴", "Resting", AgroPalette.InkMuted)
        "Recovering" -> return PlantMood("🤕", "Recovering", AgroPalette.Orange)
        "Flowering"  -> if (h >= 65) return PlantMood("🌸", "Blooming!",  AgroPalette.Rose)
        "Fruiting"   -> if (h >= 65) return PlantMood("🍅", "Fruiting!",  AgroPalette.Amber)
        "Seedling"   -> return PlantMood("🌱", "Growing up", AgroPalette.Primary)
    }

    // ── Water-due-today nudge before falling through to general mood ───────
    if (water is WateringStatus.DueToday) {
        return PlantMood("💧", "Could use water", AgroPalette.Sky)
    }

    // ── General mood, driven by health score ───────────────────────────────
    return when {
        h >= 90 -> PlantMood("🌟", "Thriving!",       AgroPalette.Primary)
        h >= 75 -> PlantMood("😊", "Happy",           AgroPalette.Primary)
        h >= 55 -> PlantMood("🙂", "Doing okay",      AgroPalette.Sky)
        else    -> PlantMood("😟", "Could be better", AgroPalette.Amber)
    }
}

/** Tint for each canonical growth stage — used by the stage card on the Care tab. */
private fun stageColor(stage: String): Color = when (stage) {
    "Seedling"   -> AgroPalette.Sky
    "Growing"    -> AgroPalette.Primary
    "Mature"     -> AgroPalette.Iris
    "Flowering"  -> AgroPalette.Rose
    "Fruiting"   -> AgroPalette.Amber
    "Dormant"    -> AgroPalette.InkMuted
    "Recovering" -> AgroPalette.Orange
    else         -> AgroPalette.InkDim
}

/** Icon for each canonical growth stage. */
private fun stageIcon(stage: String): androidx.compose.ui.graphics.vector.ImageVector = when (stage) {
    "Seedling"   -> Icons.Rounded.Spa
    "Growing"    -> Icons.Rounded.Grass
    "Mature"     -> Icons.Rounded.LocalFlorist
    "Flowering"  -> Icons.Rounded.LocalFlorist
    "Fruiting"   -> Icons.Rounded.AutoAwesome
    "Dormant"    -> Icons.Rounded.WaterDrop
    "Recovering" -> Icons.Rounded.Refresh
    else         -> Icons.Rounded.Spa
}
