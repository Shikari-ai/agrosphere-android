package com.agrosphere.app.feature.analytics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.repo.AnalyticsRepository
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.LocalScanStore
import com.agrosphere.app.data.repo.PlantAnalytics
import com.agrosphere.app.data.repo.PlantRank
import com.agrosphere.app.data.repo.PlantRepository
import com.agrosphere.app.feature.home.AnalyticsStatChip
import com.agrosphere.app.feature.home.LegendDot
import com.agrosphere.app.feature.home.MicroStat
import com.agrosphere.app.feature.home.MilestoneRow
import com.agrosphere.app.feature.home.StackedBarChart
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.theme.AgroPalette

// ═════════════════════════════════════════════════════════════════════════════
// Plant Analytics — full-screen immersive dashboard. Shows the user's
// progress through a 6-tier rank ladder, with streaks, multi-series
// activity chart, health-trend line, score breakdown bars, hex milestone
// badges and an AI insights blurb. Same data layer as the home card; this
// is the deep-dive page.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PlantAnalyticsScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenRank: () -> Unit = {},
) {
    val plants by PlantRepository.plants.collectAsState()
    val analytics = remember(plants) { AnalyticsRepository.computePlantAnalytics(plants) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        AnalyticsHeaderBar(
            title = "Plant Analytics",
            subtitle = "Live stats — Last 30 days",
            emoji = "🪴",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── 1. Hero Streak + Rank combined card ───────────────────────────
            item { HeroStreakRankCard(analytics = analytics, onOpenRank = onOpenRank) }

            // ── 2. Total Score card — chart + ring + 4 inline stats ───────────
            item { TotalScoreImmersiveCard(analytics = analytics, onOpenRank = onOpenRank) }

            // ── 3. Achievements row ───────────────────────────────────────────
            item { AchievementsSection(analytics = analytics) }

            // ── 4. Rank progression timeline ──────────────────────────────────
            item { RankProgressTimeline(analytics = analytics, onOpenRank = onOpenRank) }

            // ── 5. This Month Highlights grid ─────────────────────────────────
            item { MonthlyHighlightsSection(analytics = analytics) }

            // ── 6. AI Tips banner ─────────────────────────────────────────────
            item { AITipsBanner(analytics = analytics) }

            // ── Per-plant breakdown (kept from the original) ──────────────────
            if (analytics.perPlant.isNotEmpty()) {
                item { SectionLabel("By plant") }
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

// ═════════════════════════════════════════════════════════════════════════════
// 1. Hero card — combines Streak (left) and Rank (right) into one elevated
//    glass surface with radial accent glow, matching the immersive mockup.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun HeroStreakRankCard(analytics: PlantAnalytics, onOpenRank: () -> Unit) {
    val rank = analytics.rank
    val xp = analytics.totalScore
    val cap = rank.nextThreshold
    val pct = if (cap > rank.minXp) ((xp - rank.minXp).toFloat() / (cap - rank.minXp)).coerceIn(0f, 1f) else 1f
    val animatedPct by animateFloatAsState(pct, tween(1400, easing = LinearOutSlowInEasing), label = "hero-rank")
    val tint = rankColor(rank)

    GlassCard(
        radius = 22.dp,
        padding = 16.dp,
        background = Brush.linearGradient(
            listOf(
                AgroPalette.Rose.copy(alpha = 0.10f),
                AgroPalette.BgDeep,
                tint.copy(alpha = 0.14f),
            ),
        ),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // ── LEFT half — Streak ────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(AgroPalette.Rose.copy(alpha = 0.50f), AgroPalette.Orange.copy(alpha = 0.10f), Color.Transparent),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = AgroPalette.Rose, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${analytics.careStreak}", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp), color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(4.dp))
                            Text("Day Streak", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            when {
                                analytics.careStreak == 0 -> "Start your streak today!"
                                analytics.careStreak < 3  -> "Off to a good start — keep it going daily."
                                analytics.careStreak < 7  -> "Keep it going!"
                                analytics.careStreak < 30 -> "Strong consistency."
                                else                     -> "You're a plant legend!"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Day progression dots — 7 dots, lit up to current streak (cap at 7)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val litCount = analytics.careStreak.coerceAtMost(7)
                    repeat(7) { i ->
                        val isLit = i < litCount
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isLit) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f))
                                .border(1.dp, if (isLit) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLit) Icon(Icons.Rounded.Check, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(11.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val nextMilestone = when {
                    analytics.careStreak < 7  -> 7
                    analytics.careStreak < 30 -> 30
                    else                      -> 100
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Until reward",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = AgroPalette.InkMuted,
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AgroPalette.Iris.copy(alpha = 0.18f))
                            .border(1.dp, AgroPalette.Iris.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💎", fontSize = 10.sp)
                            Spacer(Modifier.width(3.dp))
                            Text("${nextMilestone * 10}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AgroPalette.Iris, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            // Divider
            Box(modifier = Modifier.width(1.dp).height(120.dp).background(AgroPalette.SurfaceGlassBorder.copy(alpha = 0.5f)))
            Spacer(Modifier.width(12.dp))

            // ── RIGHT half — Rank ─────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onOpenRank),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Rank", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankShield(rank = rank, size = 40.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(rank.displayName, style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPct)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(AgroPalette.Primary, AgroPalette.Sky))),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatThousands(xp)} / ${formatThousands(cap)} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (rank.next != null) "Next: ${rank.next!!.displayName}" else "Top tier reached",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AgroPalette.InkMuted,
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. Total Score immersive card — score + multi-line chart + radial ring
//    + 4 inline stats. Single card consolidates everything score-related.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TotalScoreImmersiveCard(analytics: PlantAnalytics, onOpenRank: () -> Unit) {
    val ringTarget = (analytics.totalScore / 11000f).coerceIn(0f, 1f)
    val animatedRing by animateFloatAsState(ringTarget, tween(1600, easing = LinearOutSlowInEasing), label = "score-ring")

    GlassCard(radius = 22.dp, padding = 18.dp) {
        Column {
            // Header row — title + View Details pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total Score", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape).background(AgroPalette.SurfaceGlass),
                    contentAlignment = Alignment.Center,
                ) { Text("i", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted) }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AgroPalette.Primary.copy(alpha = 0.14f))
                        .border(1.dp, AgroPalette.Primary.copy(alpha = 0.40f), RoundedCornerShape(50))
                        .clickable(onClick = onOpenRank)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("View Details", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(10.dp))

            // Big score + delta
            Text(
                formatThousands(analytics.totalScore),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                color = AgroPalette.Ink,
                fontWeight = FontWeight.Black,
            )
            DeltaText(deltaPct = analytics.scoreDeltaPct, suffix = "vs last 30 days")
            Spacer(Modifier.height(14.dp))

            // Chart + ring + stats row
            Row(verticalAlignment = Alignment.Top) {
                // Left side — 4-series line chart with legend on top
                Column(modifier = Modifier.weight(1.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChartLegend(AgroPalette.Sky,     "Watering")
                        Spacer(Modifier.width(8.dp))
                        ChartLegend(AgroPalette.Amber,   "Scanning")
                        Spacer(Modifier.width(8.dp))
                        ChartLegend(AgroPalette.Primary, "Care")
                        Spacer(Modifier.width(8.dp))
                        ChartLegend(AgroPalette.Rose,    "Health")
                    }
                    Spacer(Modifier.height(8.dp))
                    MultiLineChart(
                        seriesWatering = analytics.watersByDay30,
                        seriesScanning = analytics.scansByDay30,
                        seriesCare     = analytics.careActionsByDay30,
                        seriesHealth   = analytics.healthTrend30.map { it / 4 },  // scale 0..100 → 0..25 to fit chart range
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("30d ago", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim, modifier = Modifier.weight(1f))
                        Text("today",   style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                    }
                }
                Spacer(Modifier.width(10.dp))
                // Right side — radial score ring + 4 stats stacked
                Column(modifier = Modifier.width(112.dp)) {
                    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val baseStroke = 6f
                            val gap = 4f
                            // Outer ring — Watering blue
                            drawScoreArc(animatedRing, AgroPalette.Sky,     0f,   baseStroke)
                            drawScoreArc(animatedRing, AgroPalette.Amber,   gap,  baseStroke)
                            drawScoreArc(animatedRing, AgroPalette.Primary, gap * 2, baseStroke)
                            drawScoreArc(animatedRing, AgroPalette.Rose,    gap * 3, baseStroke)
                        }
                        Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(34.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 4 inline stat tiles — single row at the bottom
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                InlineStatTile(
                    icon = Icons.Rounded.Search, tint = AgroPalette.Amber,
                    value = "${analytics.scansMonth}", label = "Scans",
                    delta = analytics.scansDelta, modifier = Modifier.weight(1f),
                )
                InlineStatTile(
                    icon = Icons.Rounded.WaterDrop, tint = AgroPalette.Sky,
                    value = "${analytics.watersMonth}", label = "Waterings",
                    delta = analytics.watersDelta, modifier = Modifier.weight(1f),
                )
                InlineStatTile(
                    icon = Icons.Rounded.Eco, tint = AgroPalette.Primary,
                    value = "${analytics.careActionsMonth}", label = "Care Actions",
                    delta = analytics.careActionsDelta, modifier = Modifier.weight(1f),
                )
                InlineStatTile(
                    icon = Icons.Rounded.Favorite, tint = AgroPalette.Rose,
                    value = "${analytics.avgHealth}%", label = "Avg Health",
                    delta = analytics.healthDelta, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
    }
}

@Composable
private fun InlineStatTile(icon: ImageVector, tint: Color, value: String, label: String, delta: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
            if (delta != 0) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.KeyboardArrowUp, null,
                    tint = if (delta > 0) AgroPalette.Primary else AgroPalette.Rose,
                    modifier = Modifier.size(11.dp))
                Text(
                    "${if (delta > 0) delta else -delta}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (delta > 0) AgroPalette.Primary else AgroPalette.Rose,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted, maxLines = 1)
    }
}

/** Concentric arc drawer used by the multi-color radial score ring. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScoreArc(
    progress: Float, color: Color, indentDp: Float, stroke: Float,
) {
    val inset = stroke / 2f + indentDp.dp.toPx()
    val arc   = size.minDimension - inset * 2
    drawArc(
        color = color.copy(alpha = 0.20f),
        startAngle = 135f, sweepAngle = 270f, useCenter = false,
        topLeft = Offset(inset, inset),
        size = GeomSize(arc, arc),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    if (progress > 0.01f) {
        drawArc(
            brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.6f), color, color.copy(alpha = 0.6f))),
            startAngle = 135f, sweepAngle = 270f * progress, useCenter = false,
            topLeft = Offset(inset, inset),
            size = GeomSize(arc, arc),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/** 4-series line chart — one stroke per metric, no fill, with faint baseline gridlines. */
@Composable
private fun MultiLineChart(
    seriesWatering: List<Int>,
    seriesScanning: List<Int>,
    seriesCare: List<Int>,
    seriesHealth: List<Int>,
    modifier: Modifier = Modifier,
) {
    val all = seriesWatering + seriesScanning + seriesCare + seriesHealth
    val maxVal = (all.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val n = seriesWatering.size
        if (n < 2) return@Canvas
        // Dashed gridlines
        listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
            val y = size.height * (1f - ratio)
            drawLine(
                color = AgroPalette.SurfaceGlassBorder.copy(alpha = 0.35f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
            )
        }
        val stepX = size.width / (n - 1)
        fun drawSeries(values: List<Int>, color: Color) {
            val path = Path().apply {
                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = size.height - (v.toFloat() / maxVal).coerceIn(0f, 1f) * size.height
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
        }
        drawSeries(seriesWatering, AgroPalette.Sky)
        drawSeries(seriesScanning, AgroPalette.Amber)
        drawSeries(seriesCare,     AgroPalette.Primary)
        drawSeries(seriesHealth,   AgroPalette.Rose)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. Achievements — horizontal row of shield-shaped badges, colourful for
//    unlocked / dimmed-with-lock for locked. 12 total achievements derived
//    from real plant state. "More Badges" placeholder closes the row.
// ═════════════════════════════════════════════════════════════════════════════
private data class Achievement(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val isUnlocked: Boolean,
    val lockedHint: String?,
)

private fun buildAchievements(a: PlantAnalytics): List<Achievement> = listOf(
    Achievement("First Plant Added",  Icons.Rounded.LocalFlorist,        AgroPalette.Primary, a.totalPlants >= 1,        null),
    Achievement("First Watering",     Icons.Rounded.WaterDrop,           AgroPalette.Sky,     a.watersMonth >= 1,        null),
    Achievement("First Scan",         Icons.Rounded.Search,              AgroPalette.Amber,   a.scansMonth >= 1,         null),
    Achievement("7-Day Streak",       Icons.Rounded.LocalFireDepartment, AgroPalette.Iris,    a.careStreak >= 7,         "7"),
    Achievement("30-Day Care Streak", Icons.Rounded.LocalFireDepartment, AgroPalette.Amber,   a.careStreak >= 30,        "30"),
    Achievement("Plant Saver",       Icons.Rounded.Favorite,            AgroPalette.Rose,    a.scansMonth >= 5 && a.avgHealth >= 80, null),
    Achievement("AI Explorer",       Icons.Rounded.AutoAwesome,         AgroPalette.Sky,     a.scansMonth >= 10,        null),
    Achievement("Watering Master",   Icons.Rounded.WaterDrop,           AgroPalette.Primary, a.watersMonth >= 20,       null),
    Achievement("Care Devotee",      Icons.Rounded.Eco,                 AgroPalette.Iris,    a.careActionsMonth >= 25,  null),
    Achievement("Perfect Health",    Icons.Rounded.Favorite,            AgroPalette.Primary, a.avgHealth >= 90,         null),
    Achievement("Plant Collector",   Icons.Rounded.Spa,                 AgroPalette.Amber,   a.totalPlants >= 5,        null),
    Achievement("Agro Legend",       Icons.Rounded.MilitaryTech,        AgroPalette.Amber,   a.rank == PlantRank.AGRO_LEGEND, null),
)

@Composable
private fun AchievementsSection(analytics: PlantAnalytics) {
    val achievements = remember(analytics) { buildAchievements(analytics) }
    val unlockedCount = achievements.count { it.isUnlocked }

    GlassCard(radius = 22.dp, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text("Achievements", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$unlockedCount", style = MaterialTheme.typography.labelLarge, color = AgroPalette.Primary, fontWeight = FontWeight.ExtraBold)
                    Text(" / ${achievements.size} Unlocked", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(achievements) { a -> AchievementShield(a) }
                item { MoreBadgesShield() }
            }
        }
    }
}

@Composable
private fun AchievementShield(a: Achievement) {
    val bg = if (a.isUnlocked)
        Brush.linearGradient(listOf(a.tint.copy(alpha = 0.35f), a.tint.copy(alpha = 0.10f), AgroPalette.BgDeep))
    else
        Brush.linearGradient(listOf(AgroPalette.SurfaceGlass, AgroPalette.SurfaceGlass.copy(alpha = 0.6f)))

    Box(
        modifier = Modifier
            .width(86.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                1.5.dp,
                if (a.isUnlocked) a.tint.copy(alpha = 0.7f) else AgroPalette.SurfaceGlassBorder,
                RoundedCornerShape(16.dp),
            )
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Shield top: lock icon for locked, or big tier icon for unlocked
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    if (a.isUnlocked) a.tint.copy(alpha = 0.20f) else AgroPalette.SurfaceGlassBorder.copy(alpha = 0.3f),
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (a.lockedHint != null && !a.isUnlocked) {
                    Text(a.lockedHint, style = MaterialTheme.typography.titleMedium, color = AgroPalette.InkMuted, fontWeight = FontWeight.ExtraBold)
                } else {
                    Icon(
                        a.icon, null,
                        tint = if (a.isUnlocked) a.tint else AgroPalette.InkMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                a.title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = if (a.isUnlocked) AgroPalette.Ink else AgroPalette.InkMuted,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!a.isUnlocked) Icon(Icons.Rounded.Lock, null, tint = AgroPalette.InkDim, modifier = Modifier.size(9.dp))
                Text(
                    if (a.isUnlocked) "Unlocked" else " Locked",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (a.isUnlocked) AgroPalette.Primary else AgroPalette.InkDim,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MoreBadgesShield() {
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                AgroPalette.SurfaceGlassBorder,
                RoundedCornerShape(16.dp),
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.InkDim, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text("More\nBadges", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp), color = AgroPalette.InkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. Rank Progress timeline — 5 emblems connected by a progress-filled line
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RankProgressTimeline(analytics: PlantAnalytics, onOpenRank: () -> Unit) {
    val ranks = PlantRank.values()
    val current = analytics.rank
    val progress = (current.ordinal.toFloat() / (ranks.size - 1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, tween(1500, easing = LinearOutSlowInEasing), label = "rank-line")

    GlassCard(
        radius = 22.dp,
        padding = 14.dp,
        onClick = onOpenRank,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌿", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text("Rank Progress", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape).background(AgroPalette.SurfaceGlass),
                    contentAlignment = Alignment.Center,
                ) { Text("i", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted) }
            }
            Spacer(Modifier.height(14.dp))
            // Timeline — Canvas line behind + Row of nodes on top
            Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopCenter)) {
                    val yCenter = 20.dp.toPx()
                    val leftPad = 24.dp.toPx()
                    val rightPad = 24.dp.toPx()
                    val xStart = leftPad
                    val xEnd   = size.width - rightPad
                    // Background line
                    drawLine(
                        color = AgroPalette.SurfaceGlassBorder.copy(alpha = 0.5f),
                        start = Offset(xStart, yCenter), end = Offset(xEnd, yCenter),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    )
                    // Filled progress portion
                    val fillEnd = xStart + (xEnd - xStart) * animatedProgress
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(AgroPalette.Primary, AgroPalette.Sky)),
                        start = Offset(xStart, yCenter), end = Offset(fillEnd, yCenter),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ranks.forEachIndexed { i, rank ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            val isAchieved = i <= current.ordinal
                            val isCurrent = i == current.ordinal
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .then(
                                        if (isCurrent)
                                            Modifier
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(rankColor(rank).copy(alpha = 0.4f), Color.Transparent),
                                                    ),
                                                    CircleShape,
                                                )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(modifier = Modifier.alpha(if (isAchieved) 1f else 0.4f)) {
                                    RankShield(rank = rank, size = 36.dp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                rank.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                                color = if (isCurrent) rankColor(rank) else if (isAchieved) AgroPalette.Ink else AgroPalette.InkMuted,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                            )
                            Text(
                                "${rank.minXp} XP",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                color = AgroPalette.InkDim,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. This Month Highlights — 4 cards in a 2x2 grid
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun MonthlyHighlightsSection(analytics: PlantAnalytics) {
    // Best day = day with the most combined activity (waters + scans)
    val bestDayIdx = analytics.careActionsByDay30.withIndex().maxByOrNull { it.value }?.index ?: 0
    val bestDayActions = analytics.careActionsByDay30.getOrNull(bestDayIdx) ?: 0
    val mostWatersSingleDay = analytics.watersByDay30.maxOrNull() ?: 0
    val healthImprovement = analytics.healthDelta
    val consistencyLabel = when {
        analytics.careStreak >= 14 -> "Excellent!"
        analytics.careStreak >= 7  -> "Great!"
        analytics.careStreak >= 3  -> "Good"
        analytics.careStreak >= 1  -> "Building"
        else                       -> "Start now"
    }

    GlassCard(radius = 22.dp, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text("This Month Highlights", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AgroPalette.Primary.copy(alpha = 0.18f))
                        .border(1.dp, AgroPalette.Primary.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text("Share", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AgroPalette.Primary, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HighlightCard(
                    icon = Icons.Rounded.KeyboardArrowUp, tint = AgroPalette.Primary,
                    headline = "Best Day",
                    sub = bestDayDate(bestDayIdx),
                    metric = "$bestDayActions",
                    metricLabel = "Actions",
                    modifier = Modifier.weight(1f),
                )
                HighlightCard(
                    icon = Icons.Rounded.WaterDrop, tint = AgroPalette.Sky,
                    headline = "Most Waterings",
                    sub = "in a single day",
                    metric = "$mostWatersSingleDay",
                    metricLabel = "times",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HighlightCard(
                    icon = Icons.Rounded.Favorite, tint = AgroPalette.Rose,
                    headline = "Health Improvement",
                    sub = "vs last 30 days",
                    metric = "${if (healthImprovement >= 0) "+" else ""}$healthImprovement%",
                    metricLabel = "",
                    metricTint = if (healthImprovement >= 0) AgroPalette.Primary else AgroPalette.Rose,
                    modifier = Modifier.weight(1f),
                )
                HighlightCard(
                    icon = Icons.Rounded.CalendarMonth, tint = AgroPalette.Iris,
                    headline = "Consistency",
                    sub = "${analytics.careStreak} day streak",
                    metric = consistencyLabel,
                    metricLabel = "",
                    metricTint = AgroPalette.Iris,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HighlightCard(
    icon: ImageVector, tint: Color,
    headline: String, sub: String,
    metric: String, metricLabel: String,
    metricTint: Color = AgroPalette.Ink,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.04f))))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp)) }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(headline, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = AgroPalette.Ink, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkMuted, maxLines = 1)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                metric,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = metricTint,
                fontWeight = FontWeight.ExtraBold,
            )
            if (metricLabel.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(metricLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AgroPalette.InkMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

private fun bestDayDate(offsetFromToday: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -(29 - offsetFromToday))
    val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return fmt.format(cal.time)
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. AI Tips banner — bot avatar + tip text + "View AI Tips" pill
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AITipsBanner(analytics: PlantAnalytics) {
    val inf = rememberInfiniteTransition(label = "ai-tip")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p")

    GlassCard(
        radius = 22.dp,
        padding = 14.dp,
        background = Brush.linearGradient(
            listOf(AgroPalette.Iris.copy(alpha = 0.18f), AgroPalette.Sky.copy(alpha = 0.08f), AgroPalette.BgDeep),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bot avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AgroPalette.Sky.copy(alpha = 0.50f * pulse),
                                AgroPalette.Iris.copy(alpha = 0.20f),
                                AgroPalette.BgDeep,
                            ),
                        ),
                    )
                    .border(1.5.dp, AgroPalette.Sky.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Sky, modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    aiTipHeadline(analytics),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    aiTipBody(analytics),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = AgroPalette.InkMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.Primary.copy(alpha = 0.20f))
                    .border(1.dp, AgroPalette.Primary.copy(alpha = 0.50f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 10.sp)
                    Spacer(Modifier.width(3.dp))
                    Text("View AI Tips", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun aiTipHeadline(a: PlantAnalytics): String = when {
    a.totalPlants == 0    -> "Welcome!"
    a.careStreak >= 7     -> "Great job! Your plants are healthy 🌿"
    a.avgHealth >= 85     -> "Your routine is working 🌟"
    a.healthDelta < -5    -> "Health is slipping ⚠️"
    a.watersDelta < 0     -> "Watering pace dropped 💧"
    else                  -> "On a steady rhythm"
}

private fun aiTipBody(a: PlantAnalytics): String = when {
    a.totalPlants == 0    -> "Add your first plant — the camera will identify it and build a care profile automatically."
    a.scansMonth < 5      -> "Try scanning more often to reach ${a.rank.next?.displayName ?: "the next tier"}."
    a.careStreak >= 7     -> "Keep scanning more often to reach ${a.rank.next?.displayName ?: "the next tier"}!"
    a.healthDelta < -5    -> "Re-scan the weakest plants — early diagnosis beats late treatment."
    a.watersDelta < 0     -> "Check the Plants tab for overdue plants."
    else                  -> "Scan more often to catch issues before they spread."
}
@Composable
private fun StreakCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    GlassCard(
        modifier = modifier,
        radius = 18.dp,
        padding = 14.dp,
        background = Brush.linearGradient(
            listOf(AgroPalette.Rose.copy(alpha = 0.22f), AgroPalette.Orange.copy(alpha = 0.10f), AgroPalette.BgDeep),
        ),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AgroPalette.Rose.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = AgroPalette.Rose, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("${analytics.careStreak} Day Streak", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    analytics.careStreak == 0 -> "Log a watering or scan today to start a streak."
                    analytics.careStreak < 3  -> "Off to a good start — keep it going daily."
                    analytics.careStreak < 7  -> "Strong rhythm! 7 days unlocks a milestone."
                    analytics.careStreak < 30 -> "Outstanding consistency."
                    else                     -> "Magnificent. You're a true plant parent."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
            Spacer(Modifier.height(10.dp))
            // Day-of-week markers — green check if active, dot otherwise.
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                analytics.weekActivity.forEachIndexed { i, active ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (active) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f))
                                .border(1.dp, if (active) AgroPalette.Primary else AgroPalette.SurfaceGlassBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) Icon(Icons.Rounded.Check, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(days[i], style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkDim)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val streakTarget = if (analytics.careStreak < 7) 7 else if (analytics.careStreak < 30) 30 else 100
            val daysToGo = streakTarget - analytics.careStreak
            Text(
                "Keep $daysToGo day streak to unlock 💎 ${streakTarget * 10}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = AgroPalette.InkMuted,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Rank card — current rank, next-rank progress bar
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RankCard(analytics: PlantAnalytics, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val rank = analytics.rank
    val xp = analytics.totalScore
    val cap = rank.nextThreshold
    val pct = if (cap > rank.minXp) ((xp - rank.minXp).toFloat() / (cap - rank.minXp)).coerceIn(0f, 1f) else 1f
    val animatedPct by animateFloatAsState(pct, tween(1400, easing = LinearOutSlowInEasing), label = "rank")

    GlassCard(
        modifier = modifier,
        radius = 18.dp,
        padding = 14.dp,
        background = Brush.linearGradient(
            listOf(AgroPalette.Primary.copy(alpha = 0.18f), AgroPalette.Iris.copy(alpha = 0.08f), AgroPalette.BgDeep),
        ),
        onClick = onClick,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your Rank", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankShield(rank = rank, size = 44.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(rank.displayName, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(
                        if (rank.next != null) "Next: ${rank.next!!.displayName}" else "Top tier reached",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AgroPalette.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPct)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(AgroPalette.Primary, AgroPalette.Sky))),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$xp / $cap XP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = AgroPalette.InkMuted,
            )
        }
    }
}

private fun rankColor(rank: PlantRank): Color = when (rank) {
    PlantRank.SEEDLING     -> AgroPalette.Primary
    PlantRank.GREEN_GROWER -> AgroPalette.Primary
    PlantRank.PLANT_EXPERT -> AgroPalette.Sky
    PlantRank.PLANT_MASTER -> AgroPalette.Iris
    PlantRank.AGRO_LEGEND  -> AgroPalette.Amber
}

private fun rankIcon(rank: PlantRank): ImageVector = when (rank) {
    PlantRank.SEEDLING     -> Icons.Rounded.Spa
    PlantRank.GREEN_GROWER -> Icons.Rounded.Eco
    PlantRank.PLANT_EXPERT -> Icons.Rounded.LocalFlorist
    PlantRank.PLANT_MASTER -> Icons.Rounded.MilitaryTech
    PlantRank.AGRO_LEGEND  -> Icons.Rounded.AutoAwesome
}

@Composable
private fun RankShield(rank: PlantRank, size: androidx.compose.ui.unit.Dp) {
    val tint = rankColor(rank)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 35))
            .background(
                Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.40f), tint.copy(alpha = 0.10f), Color.Transparent),
                ),
            )
            .border(1.8.dp, tint.copy(alpha = 0.70f), RoundedCornerShape(percent = 35)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            rankIcon(rank),
            null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Total Score card — big score + circular ring + 4 stat columns
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TotalScoreCard(analytics: PlantAnalytics) {
    val ringTarget = (analytics.totalScore / 11000f).coerceIn(0f, 1f)
    val animatedRing by animateFloatAsState(ringTarget, tween(1600, easing = LinearOutSlowInEasing), label = "score-ring")
    val delta = analytics.scoreDeltaPct

    GlassCard(radius = 18.dp, padding = 18.dp) {
        // GlassCard wraps its content in a Box — siblings would stack on top
        // of each other without an explicit Column wrapper here.
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Score", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier.size(14.dp).clip(CircleShape).background(AgroPalette.SurfaceGlass),
                            contentAlignment = Alignment.Center,
                        ) { Text("i", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatThousands(analytics.totalScore),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                    DeltaText(deltaPct = delta, suffix = "vs last 30 days")
                }
                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8f
                        val inset = stroke / 2f
                        val arc = size.minDimension - stroke
                        drawArc(
                            color = AgroPalette.SurfaceGlassBorder,
                            startAngle = 135f, sweepAngle = 270f, useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = GeomSize(arc, arc),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        if (animatedRing > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Primary)),
                                startAngle = 135f, sweepAngle = 270f * animatedRing, useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = GeomSize(arc, arc),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Icon(Icons.Rounded.Eco, null, tint = AgroPalette.Primary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            // 2×2 grid of sub-stats
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ScoreSubStat(icon = Icons.Rounded.Search,      value = "${analytics.scansMonth}",   label = "Scans",          delta = analytics.scansDelta,        tint = AgroPalette.Amber,  modifier = Modifier.weight(1f))
                ScoreSubStat(icon = Icons.Rounded.WaterDrop,   value = "${analytics.watersMonth}",  label = "Waterings",      delta = analytics.watersDelta,       tint = AgroPalette.Sky,    modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ScoreSubStat(icon = Icons.Rounded.Eco,         value = "${analytics.careActionsMonth}", label = "Care Actions",  delta = analytics.careActionsDelta,  tint = AgroPalette.Primary, modifier = Modifier.weight(1f))
                ScoreSubStat(icon = Icons.Rounded.Favorite,    value = "${analytics.avgHealth}%",   label = "Avg Plant Health", delta = analytics.healthDelta,       tint = AgroPalette.Rose,   modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreSubStat(icon: ImageVector, value: String, label: String, delta: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                if (delta != 0) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = if (delta > 0) AgroPalette.Primary else AgroPalette.Rose,
                        modifier = Modifier.size(11.dp))
                    Text("${if (delta > 0) delta else -delta}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (delta > 0) AgroPalette.Primary else AgroPalette.Rose, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Activity Overview — 3-series stacked bar over 30 days
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ActivityOverviewCard(analytics: PlantAnalytics) {
    GlassCard(radius = 18.dp, padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activity Overview", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                LegendDot(AgroPalette.Sky, "Watering")
                Spacer(Modifier.width(8.dp))
                LegendDot(AgroPalette.Amber, "Scan")
                Spacer(Modifier.width(8.dp))
                LegendDot(AgroPalette.Primary, "Care")
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AgroPalette.SurfaceGlass)
                        .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("30 Days", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink)
                    Icon(Icons.Rounded.ArrowDropDown, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            ThreeSeriesBarChart(
                series1 = analytics.watersByDay30,   // sky
                series2 = analytics.scansByDay30,    // amber
                series3 = analytics.careActionsByDay30, // primary green
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("30 Days Ago", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkDim, modifier = Modifier.weight(1f))
                Text("Today",       style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkDim)
            }
        }
    }
}

/** Three slim parallel bars per day rather than stacked, so each series stays
 *  readable instead of compressing into a stack. */
@Composable
private fun ThreeSeriesBarChart(
    series1: List<Int>, series2: List<Int>, series3: List<Int>,
    modifier: Modifier = Modifier,
) {
    val n = series1.size
    val maxVal = listOf(series1, series2, series3).flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
    Canvas(modifier = modifier) {
        if (n == 0) return@Canvas
        val groupGap = 3.dp.toPx()
        val groupWidth = (size.width - groupGap * (n - 1)) / n
        val barGap = 1.dp.toPx()
        val barW = (groupWidth - barGap * 2) / 3
        val corner = CornerRadius(barW / 2.5f)
        for (i in 0 until n) {
            val x0 = i * (groupWidth + groupGap)
            val s1 = series1[i]
            val s2 = series2[i]
            val s3 = series3[i]
            val h1 = size.height * (s1.toFloat() / maxVal)
            val h2 = size.height * (s2.toFloat() / maxVal)
            val h3 = size.height * (s3.toFloat() / maxVal)
            if (h1 > 0.5f) drawRoundRect(AgroPalette.Sky,     Offset(x0,                       size.height - h1), GeomSize(barW, h1), corner)
            if (h2 > 0.5f) drawRoundRect(AgroPalette.Amber,   Offset(x0 + barW + barGap,        size.height - h2), GeomSize(barW, h2), corner)
            if (h3 > 0.5f) drawRoundRect(AgroPalette.Primary, Offset(x0 + 2 * (barW + barGap),  size.height - h3), GeomSize(barW, h3), corner)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4 stat tiles with mini area-sparkline at the bottom
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun StatTileStrip(analytics: PlantAnalytics) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTileWithSparkline(
            icon = Icons.Rounded.WaterDrop, tint = AgroPalette.Sky,
            value = "${analytics.watersMonth}", label = "Waterings",
            delta = analytics.watersDelta, series = analytics.watersByDay30,
            modifier = Modifier.weight(1f),
        )
        StatTileWithSparkline(
            icon = Icons.Rounded.Search, tint = AgroPalette.Amber,
            value = "${analytics.scansMonth}", label = "Scans",
            delta = analytics.scansDelta, series = analytics.scansByDay30,
            modifier = Modifier.weight(1f),
        )
        StatTileWithSparkline(
            icon = Icons.Rounded.Eco, tint = AgroPalette.Primary,
            value = "${analytics.careActionsMonth}", label = "Care Actions",
            delta = analytics.careActionsDelta, series = analytics.careActionsByDay30,
            modifier = Modifier.weight(1f),
        )
        StatTileWithSparkline(
            icon = Icons.Rounded.Favorite, tint = AgroPalette.Rose,
            value = "${analytics.avgHealth}%", label = "Avg Plant Health",
            delta = analytics.healthDelta, series = analytics.healthTrend30,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTileWithSparkline(
    icon: ImageVector, tint: Color,
    value: String, label: String,
    delta: Int, series: List<Int>,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, radius = 14.dp, padding = 10.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(tint.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp)) }
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.InkMuted, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            DeltaText(deltaPct = delta, suffix = "vs last 30 days", small = true)
            Spacer(Modifier.height(6.dp))
            // Mini area sparkline
            MiniSparkline(values = series, tint = tint, modifier = Modifier.fillMaxWidth().height(22.dp))
        }
    }
}

/** Area sparkline — smooth line + soft fill below. */
@Composable
private fun MiniSparkline(values: List<Int>, tint: Color, modifier: Modifier = Modifier) {
    val maxVal = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val n = values.size
        if (n < 2) return@Canvas
        val stepX = size.width / (n - 1)
        val path = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v.toFloat() / maxVal) * size.height
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        // Fill (area) — close down to baseline
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.05f))),
        )
        drawPath(path, color = tint, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Plant Health Trend — single-line area chart with % axis labels
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlantHealthTrendCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Plant Health Trend", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(AgroPalette.SurfaceGlass),
                    contentAlignment = Alignment.Center,
                ) { Text("i", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkMuted) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${analytics.avgHealth}%",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.width(6.dp))
                DeltaText(deltaPct = analytics.healthDelta, suffix = "vs last 30 days", small = true)
            }
            Spacer(Modifier.height(8.dp))
            // Chart with axis labels
            Row {
                Column(modifier = Modifier.padding(end = 4.dp)) {
                    Text("100%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                    Spacer(Modifier.height(12.dp))
                    Text("75%",  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                    Spacer(Modifier.height(12.dp))
                    Text("50%",  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                    Spacer(Modifier.height(12.dp))
                    Text("25%",  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                    Spacer(Modifier.height(10.dp))
                    Text("0%",   style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
                }
                HealthTrendLineChart(
                    values = analytics.healthTrend30,
                    modifier = Modifier.fillMaxWidth().height(86.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("30 Days Ago", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim, modifier = Modifier.weight(1f))
                Text("Today",       style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkDim)
            }
        }
    }
}

@Composable
private fun HealthTrendLineChart(values: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val n = values.size
        if (n < 2) return@Canvas
        val stepX = size.width / (n - 1)
        // Y axis: 0..100
        val path = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v / 100f).coerceIn(0f, 1f) * size.height
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        // Faint gridlines at 25/50/75
        listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
            val y = size.height * (1f - ratio)
            drawLine(
                color = AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 0.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
            )
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(AgroPalette.Primary.copy(alpha = 0.35f), AgroPalette.Primary.copy(alpha = 0.04f))))
        drawPath(path, color = AgroPalette.Primary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Rank Score Breakdown — 4 progress bars
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RankBreakdownCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rank Score Breakdown", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(AgroPalette.SurfaceGlass),
                    contentAlignment = Alignment.Center,
                ) { Text("i", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.InkMuted) }
            }
            Spacer(Modifier.height(10.dp))
            BreakdownRow(Icons.Rounded.Search,    "Scanning",     analytics.scanningPoints,    3000, AgroPalette.Amber)
            Spacer(Modifier.height(8.dp))
            BreakdownRow(Icons.Rounded.WaterDrop, "Watering",     analytics.wateringPoints,    3000, AgroPalette.Sky)
            Spacer(Modifier.height(8.dp))
            BreakdownRow(Icons.Rounded.Favorite,  "Plant Health", analytics.plantHealthPoints, 3000, AgroPalette.Primary)
            Spacer(Modifier.height(8.dp))
            BreakdownRow(Icons.Rounded.Spa,       "Care Actions", analytics.careActionPoints,  2000, AgroPalette.Iris)
        }
    }
}

@Composable
private fun BreakdownRow(icon: ImageVector, label: String, value: Int, cap: Int, tint: Color) {
    val pct = (value.toFloat() / cap).coerceIn(0f, 1f)
    val animated by animateFloatAsState(pct, tween(1200, easing = LinearOutSlowInEasing), label = "br-$label")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(tint.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = AgroPalette.Ink, modifier = Modifier.weight(1f))
            Text(
                "${formatThousands(value)} / ${formatThousands(cap)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = AgroPalette.InkMuted,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.7f), tint))),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Milestones — hex-shaped badges with locked/unlocked states
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun MilestonesCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
    val milestones = listOf(
        Milestone("First plant",  analytics.totalPlants >= 1,    Icons.Rounded.LocalFlorist, null),
        Milestone("First scan",   analytics.scansMonth >= 1,     Icons.Rounded.Search,       null),
        Milestone("First water",  analytics.watersMonth >= 1,    Icons.Rounded.WaterDrop,    null),
        Milestone("7d streak",    analytics.careStreak >= 7,     Icons.Rounded.LocalFireDepartment, "7"),
        Milestone("30d streak",   analytics.careStreak >= 30,    Icons.Rounded.LocalFireDepartment, "30"),
    )
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Milestones", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("View all", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                milestones.forEach { m -> HexBadge(milestone = m, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private data class Milestone(val label: String, val achieved: Boolean, val icon: ImageVector, val numericLabel: String?)

@Composable
private fun HexBadge(milestone: Milestone, modifier: Modifier = Modifier) {
    val tint = if (milestone.achieved) AgroPalette.Primary else AgroPalette.InkDim
    val border = if (milestone.achieved) AgroPalette.Primary.copy(alpha = 0.7f) else AgroPalette.SurfaceGlassBorder
    val fill = if (milestone.achieved) AgroPalette.Primary.copy(alpha = 0.20f) else AgroPalette.SurfaceGlass

    Box(
        modifier = modifier
            .height(54.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) / 2f
            val cx = w / 2f
            val cy = h / 2f
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 30.0)  // flat-top
                    val x = cx + r * kotlin.math.cos(angle).toFloat()
                    val y = cy + r * kotlin.math.sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(hex, color = fill)
            drawPath(hex, color = border, style = Stroke(width = 1.5.dp.toPx()))
        }
        if (milestone.numericLabel != null && !milestone.achieved) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(milestone.numericLabel, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = tint, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(1.dp))
                Icon(Icons.Rounded.Lock, null, tint = tint, modifier = Modifier.size(9.dp))
            }
        } else {
            Icon(
                milestone.icon, null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// AI Insights — generative blurb based on current state
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AIInsightsCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "ai-glow")
    val pulse by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p")

    val message = aiInsightFor(analytics)
    GlassCard(
        modifier = modifier,
        radius = 18.dp,
        padding = 14.dp,
        background = Brush.linearGradient(
            listOf(AgroPalette.Iris.copy(alpha = 0.18f), AgroPalette.Primary.copy(alpha = 0.08f), AgroPalette.BgDeep),
        ),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Insights", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AgroPalette.Primary.copy(alpha = 0.18f * pulse))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("New", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = AgroPalette.Ink,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AgroPalette.Sky.copy(alpha = 0.45f * pulse), AgroPalette.Iris.copy(alpha = 0.15f), Color.Transparent),
                            ),
                        )
                        .border(1.dp, AgroPalette.Sky.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Sky, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

private fun aiInsightFor(a: PlantAnalytics): String = when {
    a.totalPlants == 0     -> "Add your first plant to start tracking. The camera will identify it and build a care profile."
    a.scansMonth == 0      -> "You haven't scanned anything in 30 days. A quick scan tells the AI how each plant is doing."
    a.careStreak >= 7      -> "Your plants are loving the care! Streak strong — keep watering on schedule and scan weekly."
    a.avgHealth >= 85      -> "Healthy garden! Your routine is working — stay consistent and inspect new growth weekly."
    a.watersDelta < 0      -> "Watering is slipping vs last 30 days. Check the Plants tab for overdue plants."
    a.healthDelta < -5     -> "Average health is dropping. Re-scan the weakest plants — early diagnosis beats late treatment."
    else                   -> "You're on a steady rhythm. Try scanning more often to catch issues before they spread."
}

// ═════════════════════════════════════════════════════════════════════════════
// Shared bits — header, delta-arrow text, section label, helpers
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnalyticsHeaderBar(title: String, subtitle: String, emoji: String?, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = AgroPalette.Ink)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                if (emoji != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(emoji, fontSize = 18.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live stats — ", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                Text("Last 30 days", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.ArrowDropDown, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp))
            }
        }
        Icon(Icons.Rounded.CalendarMonth, null, tint = AgroPalette.Primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
}

@Composable
private fun DeltaText(deltaPct: Int, suffix: String, small: Boolean = false) {
    val tint = when {
        deltaPct > 0  -> AgroPalette.Primary
        deltaPct < 0  -> AgroPalette.Rose
        else          -> AgroPalette.InkMuted
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (deltaPct != 0) {
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                null,
                tint = tint,
                modifier = Modifier.size(if (small) 10.dp else 14.dp),
            )
        }
        Text(
            "${if (deltaPct > 0) "+" else if (deltaPct < 0) "" else ""}${deltaPct}% $suffix",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (small) 9.sp else 11.sp),
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatThousands(n: Int): String {
    if (n < 1000) return n.toString()
    val s = n.toString().reversed().chunked(3).joinToString(",").reversed()
    return s
}

// ═════════════════════════════════════════════════════════════════════════════
// Rank Progress — dedicated screen showing the full 6-tier ladder, current
// position, XP to next rank, and what each tier unlocks. Opened from the
// Rank card on the Plant Analytics dashboard.
// ═════════════════════════════════════════════════════════════════════════════

private data class RankPerk(val title: String, val description: String)

private fun perksFor(rank: PlantRank): List<RankPerk> = when (rank) {
    PlantRank.SEEDLING -> listOf(
        RankPerk("Plant tracking",        "Add plants, log waterings, store care notes."),
        RankPerk("Camera identification", "Snap a photo, get a species ID with care profile."),
        RankPerk("Daily streak tracker",  "Earn streaks for consistent care."),
    )
    PlantRank.GREEN_GROWER -> listOf(
        RankPerk("Watering reminders",    "Notification when each plant is due."),
        RankPerk("Growth stage tracker",  "Plants auto-tag Seedling / Growing / Flowering / etc."),
        RankPerk("Pest predictions",      "Weather-driven threat list per plant species."),
    )
    PlantRank.PLANT_EXPERT -> listOf(
        RankPerk("Scan history",          "Every AI scan saved with verdict + recommendations."),
        RankPerk("Health trend chart",    "Per-plant and overall health over time."),
        RankPerk("Advanced analytics",    "Full activity breakdown by week and month."),
    )
    PlantRank.PLANT_MASTER -> listOf(
        RankPerk("Master badge",          "Exclusive badge displayed on your Profile."),
        RankPerk("Care notes export",     "Download your full plant journal as a file."),
        RankPerk("Custom care plans",     "Override AI-recommended intervals per plant."),
    )
    PlantRank.AGRO_LEGEND -> listOf(
        RankPerk("Legendary status",      "Top 1% — Agro Legend badge unlocked."),
        RankPerk("Early access",          "Try new AgroSphere features before everyone else."),
        RankPerk("Annual plant report",   "Year-in-review summary with achievements."),
    )
}

@Composable
fun RankProgressScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val plants by PlantRepository.plants.collectAsState()
    val analytics = remember(plants) { AnalyticsRepository.computePlantAnalytics(plants) }
    val current = analytics.rank
    val xp = analytics.totalScore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        AnalyticsHeaderBar(
            title = "Rank Progress",
            subtitle = "${current.displayName} · $xp XP",
            emoji = "🏆",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Top hero banner with current rank + progress ──────────────────
            item {
                GlassCard(
                    radius = 18.dp,
                    padding = 18.dp,
                    background = Brush.linearGradient(
                        listOf(AgroPalette.Primary.copy(alpha = 0.22f), AgroPalette.Iris.copy(alpha = 0.10f), AgroPalette.BgDeep),
                    ),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RankShield(rank = current, size = 72.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CURRENT RANK", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = AgroPalette.InkMuted, fontWeight = FontWeight.SemiBold)
                                Text(current.displayName, style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Primary, fontWeight = FontWeight.ExtraBold)
                                Text("$xp Earned XP", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                            }
                        }
                        if (current.next != null) {
                            val cap = current.nextThreshold
                            val pct = ((xp - current.minXp).toFloat() / (cap - current.minXp)).coerceIn(0f, 1f)
                            val animated by animateFloatAsState(pct, tween(1400, easing = LinearOutSlowInEasing), label = "hero-rank")
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Next: ", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                                Text(current.next!!.displayName, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Sky, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("${cap - xp} XP to go", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(AgroPalette.SurfaceGlassBorder.copy(alpha = 0.4f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animated)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(listOf(AgroPalette.Primary, AgroPalette.Sky))),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("$xp / $cap XP", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Text("You've reached the top tier. Congratulations!", style = MaterialTheme.typography.bodySmall, color = AgroPalette.Amber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Section label ─────────────────────────────────────────────────
            item {
                Text(
                    "RANK LADDER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = AgroPalette.InkMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }

            // ── All 6 tier cards ──────────────────────────────────────────────
            items(PlantRank.values().toList()) { tier ->
                RankTierCard(
                    tier = tier,
                    currentRank = current,
                    currentXp = xp,
                )
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun RankTierCard(
    tier: PlantRank,
    currentRank: PlantRank,
    currentXp: Int,
) {
    val isCurrent  = tier == currentRank
    val isAchieved = tier.ordinal <= currentRank.ordinal
    val isLocked   = !isAchieved
    val tint = rankColor(tier)
    val perks = perksFor(tier)

    GlassCard(
        radius = 18.dp,
        padding = 16.dp,
        background = if (isCurrent)
            Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.04f), AgroPalette.BgDeep))
        else if (isLocked)
            androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass.copy(alpha = 0.5f))
        else
            androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
        border = if (isCurrent)
            androidx.compose.foundation.BorderStroke(1.5.dp, tint.copy(alpha = 0.7f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, AgroPalette.SurfaceGlassBorder),
    ) {
        Column {
            // Header row — shield + name + XP threshold + status badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.alpha(if (isLocked) 0.45f else 1f)) {
                    RankShield(rank = tier, size = 52.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tier.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isLocked) AgroPalette.InkMuted else tint,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.width(8.dp))
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(tint.copy(alpha = 0.20f))
                                    .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("CURRENT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp), color = tint, fontWeight = FontWeight.Bold) }
                        } else if (isLocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, null, tint = AgroPalette.InkDim, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "${tier.minXp - currentXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AgroPalette.InkDim,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, null, tint = AgroPalette.Primary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("CLEARED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp), color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        "Requires ${formatThousands(tier.minXp)} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgroPalette.InkDim,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Perks list
            Text(
                "UNLOCKS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontSize = 9.sp),
                color = AgroPalette.InkMuted,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            perks.forEach { perk ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) AgroPalette.InkDim else tint),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            perk.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLocked) AgroPalette.InkMuted else AgroPalette.Ink,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            perk.description,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                            color = AgroPalette.InkMuted,
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Field Analytics — kept simpler (mirrors original) since the user's design
// focus was the plant page. Same data engine, lighter layout.
// ═════════════════════════════════════════════════════════════════════════════

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
        AnalyticsHeaderBar(
            title = "Field Analytics",
            subtitle = "Live stats — Last 30 days",
            emoji = "🌾",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStatChip("${analytics.totalFields}", "fields",     AgroPalette.Primary, modifier = Modifier.weight(1f))
                    AnalyticsStatChip("%.1f".format(analytics.totalAreaHa), "ha", AgroPalette.Iris, modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.avgHealth}",   "avg health", AgroPalette.Sky,     modifier = Modifier.weight(1f))
                    AnalyticsStatChip("${analytics.avgMoisture}", "avg moist",  AgroPalette.Amber,   modifier = Modifier.weight(1f))
                }
            }
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
            if (analytics.perField.isNotEmpty()) {
                item { SectionLabel("By field") }
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
