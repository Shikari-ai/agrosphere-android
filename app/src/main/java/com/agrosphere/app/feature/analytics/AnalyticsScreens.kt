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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Streak + Rank top row ─────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StreakCard(analytics = analytics, modifier = Modifier.weight(1f))
                    RankCard(analytics = analytics, modifier = Modifier.weight(1f))
                }
            }

            // ── Total Score card ──────────────────────────────────────────────
            item { TotalScoreCard(analytics = analytics) }

            // ── Activity Overview chart ───────────────────────────────────────
            item { ActivityOverviewCard(analytics = analytics) }

            // ── Per-metric stat tiles with mini sparklines ────────────────────
            item { StatTileStrip(analytics = analytics) }

            // ── Plant Health Trend + Rank Score Breakdown side-by-side ────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PlantHealthTrendCard(analytics = analytics, modifier = Modifier.weight(1f))
                    RankBreakdownCard(analytics = analytics, modifier = Modifier.weight(1f))
                }
            }

            // ── Milestones hex row + AI insights ──────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MilestonesCard(analytics = analytics, modifier = Modifier.weight(1f))
                    AIInsightsCard(analytics = analytics, modifier = Modifier.weight(1f))
                }
            }

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
// Streak card — 5-day weekly markers, current streak count, unlock hint
// ═════════════════════════════════════════════════════════════════════════════
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
private fun RankCard(analytics: PlantAnalytics, modifier: Modifier = Modifier) {
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

@Composable
private fun RankShield(rank: PlantRank, size: androidx.compose.ui.unit.Dp) {
    val tint = when (rank) {
        PlantRank.SEEDLING        -> AgroPalette.Sky
        PlantRank.SPROUT          -> AgroPalette.Primary
        PlantRank.SAPLING         -> AgroPalette.Primary
        PlantRank.GREEN_GROWER    -> AgroPalette.Primary
        PlantRank.PLANT_MASTER    -> AgroPalette.Iris
        PlantRank.BOTANIST_LEGEND -> AgroPalette.Amber
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 40))
            .background(tint.copy(alpha = 0.20f))
            .border(1.5.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(percent = 40)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            when (rank) {
                PlantRank.SEEDLING        -> Icons.Rounded.Spa
                PlantRank.SPROUT          -> Icons.Rounded.Eco
                PlantRank.SAPLING         -> Icons.Rounded.Grass
                PlantRank.GREEN_GROWER    -> Icons.Rounded.LocalFlorist
                PlantRank.PLANT_MASTER    -> Icons.Rounded.MilitaryTech
                PlantRank.BOTANIST_LEGEND -> Icons.Rounded.AutoAwesome
            },
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
        Spacer(Modifier.height(12.dp))
        // 4-stat row at the bottom
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ScoreSubStat(icon = Icons.Rounded.Search,      value = "${analytics.scansMonth}",   label = "Scans",          delta = analytics.scansDelta,        tint = AgroPalette.Amber,  modifier = Modifier.weight(1f))
            ScoreSubStat(icon = Icons.Rounded.WaterDrop,   value = "${analytics.watersMonth}",  label = "Waterings",      delta = analytics.watersDelta,       tint = AgroPalette.Sky,    modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ScoreSubStat(icon = Icons.Rounded.Eco,         value = "${analytics.careActionsMonth}", label = "Care Actions",  delta = analytics.careActionsDelta,  tint = AgroPalette.Primary, modifier = Modifier.weight(1f))
            ScoreSubStat(icon = Icons.Rounded.Favorite,    value = "${analytics.avgHealth}%",   label = "Avg Plant Health", delta = analytics.healthDelta,       tint = AgroPalette.Rose,   modifier = Modifier.weight(1f))
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
