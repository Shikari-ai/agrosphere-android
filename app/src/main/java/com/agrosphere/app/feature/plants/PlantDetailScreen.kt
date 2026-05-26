package com.agrosphere.app.feature.plants

import androidx.compose.animation.core.animateFloatAsState
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(plant.accent.copy(alpha = 0.28f))
                        .border(1.5.dp, plant.accent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.LocalFlorist, null, tint = plant.accent, modifier = Modifier.size(30.dp))
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
        // ── Rescan card — primary action ──────────────────────────────────────
        item {
            GlassCard(
                radius = 16.dp,
                padding = 16.dp,
                background = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(AgroPalette.Primary.copy(alpha = 0.20f), AgroPalette.Iris.copy(alpha = 0.08f))
                ),
                onClick = onRescan,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan ${plant.name}", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        Text(
                            if (latest == null) "First scan — update its health profile"
                            else "Last scanned ${relativeDays(latest.timestamp)} · update health & history",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroPalette.InkMuted,
                        )
                    }
                    Icon(Icons.Rounded.Refresh, null, tint = AgroPalette.Primary)
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.10f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (plant.lastWateredMs > 0L) {
                            Text(
                                "Last: ${formatDate(plant.lastWateredMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.InkMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    CareInfoRow(label = stringResource(R.string.plant_care_interval), value = stringResource(R.string.plant_care_days, plant.wateringIntervalDays))
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
    val score   = plant.healthScore
    val target  by animateFloatAsState(score / 100f, tween(800), label = "ring")
    val ringColor = when {
        score >= 75 -> AgroPalette.Primary
        score >= 50 -> AgroPalette.Amber
        else        -> AgroPalette.Rose
    }
    val verdict = when {
        score >= 85 -> "Excellent"
        score >= 70 -> "Good"
        score >= 55 -> "Watch"
        else        -> "At risk"
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            // Health ring
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
                    val inset  = 8.dp.toPx()
                    drawArc(AgroPalette.SurfaceGlass, -90f, 360f, false, style = stroke,
                        topLeft = Offset(inset, inset), size = GeomSize(size.width - inset * 2, size.height - inset * 2))
                    drawArc(ringColor, -90f, 360f * target, false, style = stroke,
                        topLeft = Offset(inset, inset), size = GeomSize(size.width - inset * 2, size.height - inset * 2))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", style = MaterialTheme.typography.displaySmall, color = ringColor, fontWeight = FontWeight.ExtraBold)
                    Text(verdict, style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                }
            }
        }
        item {
            Text(
                "Health score is derived from scan history. Run a scan with the camera to update it.",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date helpers
// ─────────────────────────────────────────────────────────────────────────────

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

private fun formatDate(ms: Long): String = dateFormat.format(Date(ms))
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
