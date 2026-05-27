package com.agrosphere.app.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.repo.AnalyticsRepository
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.LocalScanStore
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.feature.home.AnalyticsStatChip
import com.agrosphere.app.feature.home.LegendDot
import com.agrosphere.app.feature.home.MicroStat
import com.agrosphere.app.feature.home.MilestoneRow
import com.agrosphere.app.feature.home.StackedBarChart
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.theme.AgroPalette

// ═════════════════════════════════════════════════════════════════════════════
// Dedicated analytics screens — opened when the user taps a Plant Analytics
// or Field Analytics card on the home dashboard. Same content the bottom-sheet
// used to show, but now lives at its own route so it feels like a real page
// instead of a popup (and shows up in the back stack, can be deep-linked, etc.).
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PlantAnalyticsScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val plants by PlantRepository.plants.collectAsState()
    val analytics = remember(plants) { AnalyticsRepository.computePlantAnalytics(plants) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        AnalyticsTopBar(
            title = "Plant Analytics",
            subtitle = "Live stats — last 30 days",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Streak banner ─────────────────────────────────────────────────
            item {
                GlassCard(
                    radius = 18.dp,
                    padding = 16.dp,
                    background = Brush.linearGradient(
                        listOf(AgroPalette.Rose.copy(alpha = 0.20f), AgroPalette.Orange.copy(alpha = 0.10f))
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(AgroPalette.Rose.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = AgroPalette.Rose, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${analytics.careStreak} day streak", style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                when {
                                    analytics.careStreak == 0 -> "Log a watering or scan today to start a streak."
                                    analytics.careStreak < 3  -> "Off to a good start — keep it going daily for best growth."
                                    analytics.careStreak < 7  -> "Nice rhythm! 7 days unlocks your first milestone."
                                    analytics.careStreak < 30 -> "Strong consistency — your plants are reaping the benefits."
                                    else                     -> "Magnificent. You're a true plant parent."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroPalette.InkMuted,
                            )
                        }
                    }
                }
            }

            // ── 30-day stacked bar chart ──────────────────────────────────────
            item {
                GlassCard(radius = 16.dp, padding = 16.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Activity — 30 days", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(AgroPalette.Sky, "water")
                                LegendDot(AgroPalette.Amber, "scan")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        StackedBarChart(
                            waters = analytics.watersByDay30,
                            scans  = analytics.scansByDay30,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("30d ago", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim, modifier = Modifier.weight(1f))
                            Text("today", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                        }
                    }
                }
            }

            // ── Totals strip ──────────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStatChip("${analytics.watersMonth}", "waterings",  AgroPalette.Sky,     modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.scansMonth}",  "scans",      AgroPalette.Amber,   modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.avgHealth}",   "avg health", AgroPalette.Primary, modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.totalPlants}", "plants",     AgroPalette.Iris,    modifier = Modifier.weight(1f))
                }
            }

            // ── Milestones ────────────────────────────────────────────────────
            item {
                GlassCard(radius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Milestones", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        MilestoneRow("First plant added", analytics.totalPlants >= 1)
                        MilestoneRow("First scan logged", analytics.scansMonth >= 1)
                        MilestoneRow("First watering logged", analytics.watersMonth >= 1)
                        MilestoneRow("7-day care streak", analytics.careStreak >= 7)
                        MilestoneRow("10 scans this month", analytics.scansMonth >= 10)
                        MilestoneRow("30-day care streak", analytics.careStreak >= 30)
                    }
                }
            }

            // ── Per-plant breakdown ───────────────────────────────────────────
            if (analytics.perPlant.isNotEmpty()) {
                item {
                    Text("By plant", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                }
                items(analytics.perPlant) { stat ->
                    GlassCard(radius = 14.dp, padding = 14.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(stat.plant.accent.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.LocalFlorist, null, tint = stat.plant.accent, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stat.plant.name, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                                    Text("${stat.plant.species} · health ${stat.plant.healthScore}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                                }
                                if (stat.plantStreak > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = AgroPalette.Rose, modifier = Modifier.size(14.dp))
                                        Text("${stat.plantStreak}d", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Rose, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MicroStat("💧", "${stat.watersMonth}", "waters", AgroPalette.Sky, Modifier.weight(1f))
                                MicroStat("✨", "${stat.scansMonth}",  "scans",  AgroPalette.Amber, Modifier.weight(1f))
                                MicroStat("🌱", stat.plant.stage,        "stage",  AgroPalette.Primary, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun FieldAnalyticsScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val fields by FieldRepository.fields.collectAsState()
    val scans = remember(fields) { LocalScanStore.load(context) }
    val analytics = remember(fields, scans) { AnalyticsRepository.computeFieldAnalytics(fields, scans) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        AnalyticsTopBar(
            title = "Field Analytics",
            subtitle = "Live stats — last 30 days",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Scan-cadence banner ───────────────────────────────────────────
            item {
                GlassCard(
                    radius = 18.dp,
                    padding = 16.dp,
                    background = Brush.linearGradient(
                        listOf(AgroPalette.Amber.copy(alpha = 0.20f), AgroPalette.Primary.copy(alpha = 0.10f))
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(AgroPalette.Amber.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.BarChart, null, tint = AgroPalette.Amber, modifier = Modifier.size(26.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${analytics.scansMonth} scans this month", style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                when {
                                    analytics.scansMonth == 0 -> "Scout one of your fields with the camera to start tracking."
                                    analytics.scansMonth < 4  -> "Light scouting cadence — aim for a weekly pass per field."
                                    analytics.scansMonth < 12 -> "Solid scouting — your fields are well watched."
                                    else                     -> "Top-tier scouting cadence. Catching issues early."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroPalette.InkMuted,
                            )
                        }
                    }
                }
            }

            // ── 30-day chart ──────────────────────────────────────────────────
            item {
                GlassCard(radius = 16.dp, padding = 16.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Scans — 30 days", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            LegendDot(AgroPalette.Amber, "scan")
                        }
                        Spacer(Modifier.height(10.dp))
                        StackedBarChart(
                            waters = List(analytics.scansByDay30.size) { 0 },
                            scans  = analytics.scansByDay30,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("30d ago", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim, modifier = Modifier.weight(1f))
                            Text("today",  style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                        }
                    }
                }
            }

            // ── Totals strip ──────────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStatChip("${analytics.totalFields}", "fields",     AgroPalette.Primary, modifier = Modifier.weight(1f))
                    AnalyticsStatChip("%.1f".format(analytics.totalAreaHa), "ha", AgroPalette.Iris, modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.avgHealth}",   "avg health", AgroPalette.Sky,     modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.avgMoisture}", "avg moist",  AgroPalette.Amber,   modifier = Modifier.weight(1f))
                }
            }

            // ── Milestones ────────────────────────────────────────────────────
            item {
                GlassCard(radius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Milestones", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        MilestoneRow("First field added",  analytics.totalFields >= 1)
                        MilestoneRow("First scan logged",  analytics.scansMonth >= 1)
                        MilestoneRow("Multiple fields (3+)", analytics.totalFields >= 3)
                        MilestoneRow("10 scans this month", analytics.scansMonth >= 10)
                        MilestoneRow("1+ hectare under management", analytics.totalAreaHa >= 1.0)
                        MilestoneRow("Average health 80+",  analytics.avgHealth >= 80)
                    }
                }
            }

            // ── Per-field breakdown ───────────────────────────────────────────
            if (analytics.perField.isNotEmpty()) {
                item {
                    Text("By field", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                }
                items(analytics.perField) { f ->
                    GlassCard(radius = 14.dp, padding = 14.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(f.accent.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Grass, null, tint = f.accent, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.name, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                                    Text("${f.crop} · ${"%.1f".format(f.areaHa)} ha · ${f.stage}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MicroStat("💚", "${f.healthScore}",  "health",   AgroPalette.Primary, Modifier.weight(1f))
                                MicroStat("💧", "${f.moisturePct}%", "moisture", AgroPalette.Sky,     Modifier.weight(1f))
                                MicroStat("📅", "${f.sownDaysAgo}d", "sown ago", AgroPalette.Amber,   Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun AnalyticsTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
        }
        Icon(Icons.Rounded.BarChart, null, tint = AgroPalette.Primary.copy(alpha = 0.6f))
    }
}
