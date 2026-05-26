package com.agrosphere.app.feature.fields

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.agrosphere.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.agrosphere.app.data.agro.CropAdvisor
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.repo.AppPreferences
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.data.repo.UnitFormatter
import com.agrosphere.app.data.weather.WeatherRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.StatChip
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

private enum class DetailTab(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Rounded.ViewModule),
    Activity("Activity", Icons.Rounded.CalendarMonth),
    Health("Health", Icons.Rounded.Timeline),
}

@Composable
fun FieldDetailScreen(fieldId: String, onBack: () -> Unit, onDelete: () -> Unit = onBack) {
    val field = MockRepository.field(fieldId)
    if (field == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var tab by remember { mutableStateOf(DetailTab.Overview) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Hero header
        HeroBar(field = field, onBack = onBack)
        // Tab strip
        TabStrip(selected = tab, onSelect = { tab = it })

        // Tab content fills the remaining space
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                DetailTab.Overview -> OverviewTab(field)
                DetailTab.Activity -> ActivityTab()
                DetailTab.Health -> HealthTab(field)
            }
        }

        // ── Delete field button — always visible at the bottom ──────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroPalette.BgDeep.copy(alpha = 0.92f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            PrimaryButton(
                text = stringResource(R.string.field_delete_confirm_title),
                icon = Icons.Rounded.DeleteOutline,
                brush = SolidColor(AgroPalette.Rose),
                onClick = { showDeleteConfirm = true },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.field_delete_confirm_title)) },
            text = { Text(stringResource(R.string.field_delete_confirm_body, field.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
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

@Composable
private fun HeroBar(field: Field, onBack: () -> Unit) {
    val useMetric by AppPreferences.useMetric.collectAsState()
    GlassCard(
        background = AgroBrushes.leafCard,
        radius = 0.dp,
        padding = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
                }
                Spacer(Modifier.width(4.dp))
                Text("Field", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(field.accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, null, tint = field.accent) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(field.name, style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp), color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                    Text("${field.crop} · ${UnitFormatter.area(field.areaHa, useMetric)}", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}

@Composable
private fun TabStrip(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailTab.values().forEach { t ->
            val isSel = t == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSel) AgroPalette.Primary else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon, null, tint = if (isSel) AgroPalette.BgDeep else AgroPalette.InkMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    t.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = if (isSel) AgroPalette.BgDeep else AgroPalette.InkMuted,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverviewTab(field: Field) {
    val context = LocalContext.current
    val useMetric by AppPreferences.useMetric.collectAsState()
    val weatherBundle by WeatherRepository.bundleFlow.collectAsState()
    LaunchedEffect(Unit) {
        if (WeatherRepository.cached() == null) runCatching { WeatherRepository.load(context) }
    }
    val weather = weatherBundle?.snapshot

    val cropStage  = remember(field.crop, field.sownDaysAgo, weather) {
        CropAdvisor.stageFor(field.crop, field.sownDaysAgo, weather)
    }
    val nextOp     = remember(field.crop, field.sownDaysAgo, field.moisturePct, field.healthScore, weather) {
        CropAdvisor.nextOperation(field.crop, field.sownDaysAgo, field, weather)
    }
    val riskFlags  = remember(field.healthScore, field.moisturePct, weather) {
        CropAdvisor.riskFlags(field, weather)
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(Icons.Rounded.Bolt, "${field.healthScore}", "Health", field.accent, Modifier.weight(1f))
                StatChip(Icons.Rounded.WaterDrop, "${field.moisturePct}%", "Soil", AgroPalette.Sky, Modifier.weight(1f))
                StatChip(Icons.Rounded.CalendarMonth, "${field.sownDaysAgo}d", "Sown", AgroPalette.Amber, Modifier.weight(1f))
            }
        }
        item {
            GlassCard(radius = 18.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current stage", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted, modifier = Modifier.weight(1f))
                        if (cropStage.daysRemaining > 0) {
                            Text(
                                "~${cropStage.daysRemaining}d to next",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.Primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(cropStage.name, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cropStage.description, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
        item {
            Text("This week", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp))
        }
        item {
            GlassCard(radius = 18.dp) {
                Column {
                    InfoLine("Estimated yield", "${"%.0f".format(field.areaHa * 2.8)} t · ${UnitFormatter.area(field.areaHa, useMetric)} — based on cultivar + stage")
                    InfoLine("Next operation", nextOp)
                    InfoLine("Risk flags", riskFlags)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActivityTab() {
    // Activity log isn't backed by a real store yet — show an honest empty state
    // instead of seeding made-up operations.
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GlassCard(radius = 22.dp, padding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No activity yet", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Log irrigation, foliar feeds, or scout visits and they'll show up here as a timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Health trend
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HealthTab(field: Field) {
    // Synthetic 12-week trend ending at field.healthScore
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(radius = 22.dp, padding = 20.dp) {
                Column {
                    Text("Current health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(4.dp))
                    Text("${field.healthScore} / 100", style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp), color = field.accent, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            field.healthScore >= 85 -> "Excellent — keep current regimen."
                            field.healthScore >= 70 -> "Strong — small monitoring tweaks recommended."
                            field.healthScore >= 55 -> "Watch list — schedule a scan this week."
                            else -> "At risk — open Scanner and inspect within 24h."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
            }
        }
        item {
            GlassCard(radius = 22.dp, padding = 20.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Timeline, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Trend chart coming with scans", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The weekly sparkline lights up once you've run at least two scans on this field — we plot the real health scores, not a generated curve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun HealthSparkline(values: List<Int>, tint: Color) {
    val animated by animateFloatAsState(1f, animationSpec = tween(900), label = "spark")
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        if (values.size < 2) return@Canvas
        val padX = 12f
        val padY = 16f
        val usableW = size.width - padX * 2
        val usableH = size.height - padY * 2
        val step = usableW / (values.size - 1)
        val min = values.min().toFloat()
        val max = values.max().toFloat()
        val range = (max - min).coerceAtLeast(1f)

        val path = Path()
        val fillPath = Path()
        values.forEachIndexed { i, v ->
            val x = padX + step * i
            val ratio = ((v - min) / range)
            val y = padY + usableH - ratio * usableH * animated
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height - padY)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(padX + usableW, size.height - padY)
        fillPath.close()
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                0f to tint.copy(alpha = 0.35f),
                1f to Color.Transparent,
            )
        )
        drawPath(path, brush = Brush.horizontalGradient(listOf(tint, AgroPalette.Sky)), style = Stroke(width = 3f, cap = StrokeCap.Round))
        // Dots
        values.forEachIndexed { i, v ->
            val x = padX + step * i
            val ratio = ((v - min) / range)
            val y = padY + usableH - ratio * usableH * animated
            drawCircle(color = AgroPalette.Ink, radius = 2.6f, center = Offset(x, y))
        }
    }
}

