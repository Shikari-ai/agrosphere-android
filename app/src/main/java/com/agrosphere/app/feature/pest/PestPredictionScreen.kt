package com.agrosphere.app.feature.pest

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.R
import com.agrosphere.app.data.repo.NeighbourAlert
import com.agrosphere.app.data.repo.PestScenario
import com.agrosphere.app.data.repo.PestSource
import com.agrosphere.app.data.repo.RegionalPest
import com.agrosphere.app.data.repo.VerifiedPrediction
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun PestPredictionScreen(padding: PaddingValues, onBack: () -> Unit) {
    val vm: PestPredictionViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.pest_back), tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitle(eyebrow = stringResource(R.string.pest_screen_eyebrow), title = stringResource(R.string.pest_screen_title))
            }
            IconButton(onClick = vm::refresh) {
                Icon(Icons.Rounded.Refresh, stringResource(R.string.pest_refresh), tint = AgroPalette.InkMuted)
            }
        }

        when {
            state.loading -> LoadingBlock()
            !state.hasFields -> EmptyBlock()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.neighbourAlert?.let { alert ->
                    item { CrossBorderBanner(alert) }
                }

                item { SummaryCard(state.highCount, state.medCount, state.lowCount, state.region) }

                if (state.regionalPests.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.pest_regional_title)) }
                    item { RegionalCard(state.regionalPests, state.sampledCells, state.region) }
                }

                if (state.scenarios.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.pest_section_scenarios)) }
                    item { ScenariosCard(state.scenarios) }
                }

                item { SectionLabel(stringResource(R.string.pest_section_verified)) }
                item {
                    VerifyCard(
                        verifying = state.verifying,
                        result = state.verifyResult,
                        error = state.verifyError,
                        onRun = vm::runVerification,
                        onOpen = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                    )
                }

                item { SectionLabel(stringResource(R.string.pest_section_field_risk)) }
                items(state.risks) { RiskCard(it) }
            }
        }
    }
}

// ─── Building blocks ───────────────────────────────────────────────────────────

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.HIGH -> AgroPalette.Rose
    RiskLevel.MEDIUM -> AgroPalette.Amber
    RiskLevel.LOW -> AgroPalette.Primary
}

private fun riskWordRes(level: RiskLevel): Int = when (level) {
    RiskLevel.HIGH -> R.string.pest_risk_high
    RiskLevel.MEDIUM -> R.string.pest_risk_moderate
    RiskLevel.LOW -> R.string.pest_risk_low
}

private fun regionalRiskColor(level: String): Color = when (level) {
    "high" -> AgroPalette.Rose
    "medium" -> AgroPalette.Amber
    else -> AgroPalette.Primary
}

@Composable
private fun CrossBorderBanner(alert: NeighbourAlert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AgroPalette.Rose.copy(alpha = 0.12f))
            .border(1.dp, AgroPalette.Rose.copy(alpha = 0.34f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = AgroPalette.Rose, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.pest_xborder_title),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.Rose,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${alert.label} — ${stringResource(R.string.pest_xborder_body)} ${alert.confidencePct}%",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun RegionalCard(pests: List<RegionalPest>, sampledCells: Int, region: String) {
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Column {
            if (region.isNotBlank()) {
                Text(
                    region,
                    style = MaterialTheme.typography.labelMedium,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                stringResource(R.string.pest_regional_note),
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
            Spacer(Modifier.height(8.dp))
            pests.take(8).forEach { p -> RegionalRow(p) }
            Spacer(Modifier.height(8.dp))
            Text(
                "$sampledCells ${stringResource(R.string.pest_areas_sampled)}",
                style = MaterialTheme.typography.labelSmall,
                color = AgroPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun RegionalRow(p: RegionalPest) {
    val tint = regionalRiskColor(p.riskLevel)
    val where = when {
        p.inCenter && p.inNeighbours -> stringResource(R.string.pest_here_and_nearby)
        p.inCenter -> stringResource(R.string.pest_in_your_area)
        else -> stringResource(R.string.pest_in_nearby_districts)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.BugReport, null, tint = tint, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.label, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
            Text(
                "$where · ${p.totalCount} ${stringResource(R.string.pest_reports_word)}",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
        }
        Text("${p.confidencePct}%", style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = AgroPalette.InkMuted,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SummaryCard(high: Int, med: Int, low: Int, region: String) {
    GlassCard(radius = 22.dp, padding = 18.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = AgroPalette.Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.pest_today_pressure),
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (region.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(region, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCol(high, stringResource(R.string.pest_risk_high), AgroPalette.Rose)
                StatCol(med, stringResource(R.string.pest_risk_moderate), AgroPalette.Amber)
                StatCol(low, stringResource(R.string.pest_risk_low), AgroPalette.Primary)
            }
        }
    }
}

@Composable
private fun StatCol(count: Int, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.headlineMedium, color = tint, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
    }
}

@Composable
private fun RiskCard(risk: FieldPestRisk) {
    val tint = riskColor(risk.level)
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // Plant cards get the florist glyph; field cards keep the
                    // bug-report icon so the entity type is obvious at a glance.
                    Icon(
                        if (risk.isPlant) Icons.Rounded.LocalFlorist else Icons.Rounded.BugReport,
                        null,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(risk.fieldName, style = MaterialTheme.typography.bodyLarge, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (risk.isPlant) "${risk.crop} · plant" else risk.crop,
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                    )
                }
                RiskBadge(risk.level)
            }

            Spacer(Modifier.height(12.dp))
            // pressure bar
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(AgroPalette.SurfaceGlassBorder)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((risk.pressurePct / 100f).coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tint),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.pest_pressure_line, risk.pressurePct, risk.factors),
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )

            if (risk.threats.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    risk.threats.forEach { ThreatChip(it, tint) }
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(level: RiskLevel) {
    val tint = riskColor(level)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(stringResource(riskWordRes(level)), style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThreatChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = AgroPalette.Ink)
    }
}

@Composable
private fun ScenariosCard(scenarios: List<PestScenario>) {
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            scenarios.forEach { s -> ScenarioRow(s) }
        }
    }
}

@Composable
private fun ScenarioRow(s: PestScenario) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.title, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Medium)
            Text("${s.likelihoodPct}%", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(AgroPalette.SurfaceGlassBorder)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((s.likelihoodPct / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.Primary),
            )
        }
        if (s.detail.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(s.detail, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
        }
    }
}

@Composable
private fun VerifyCard(
    verifying: Boolean,
    result: VerifiedPrediction?,
    error: String?,
    onRun: () -> Unit,
    onOpen: (String) -> Unit,
) {
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Column {
            Text(
                stringResource(R.string.pest_verify_blurb),
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
            )
            Spacer(Modifier.height(12.dp))

            // Run button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (verifying) AgroPalette.SurfaceGlass else AgroPalette.Primary)
                    .clickable(enabled = !verifying, onClick = onRun)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (verifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AgroPalette.Primary)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.pest_verify_loading), style = MaterialTheme.typography.labelLarge, color = AgroPalette.InkMuted)
                    } else {
                        Icon(Icons.Rounded.Search, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pest_verify_run), style = MaterialTheme.typography.labelLarge, color = AgroPalette.BgDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = AgroPalette.Amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pest_verify_unavailable), style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }

            if (result != null) {
                Spacer(Modifier.height(16.dp))
                VerifyResult(result, onOpen)
            }
        }
    }
}

@Composable
private fun VerifyResult(r: VerifiedPrediction, onOpen: (String) -> Unit) {
    val confTint = when {
        r.confidencePct >= 70 -> AgroPalette.Primary
        r.confidencePct >= 45 -> AgroPalette.Amber
        else -> AgroPalette.InkMuted
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.species, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (r.verified) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(AgroPalette.Primary.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Verified, null, tint = AgroPalette.Primary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.pest_verified_badge), style = MaterialTheme.typography.labelSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.pest_confidence, r.confidencePct), style = MaterialTheme.typography.labelMedium, color = confTint, fontWeight = FontWeight.Bold)

        if (r.summary.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(r.summary, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink)
        }

        if (r.scenarios.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            r.scenarios.forEach { s ->
                ScenarioRow(s)
                Spacer(Modifier.height(10.dp))
            }
        }

        if (r.sources.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.pest_sources).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = AgroPalette.InkMuted,
            )
            Spacer(Modifier.height(6.dp))
            r.sources.forEach { src -> SourceRow(src, onOpen) }
        }
    }
}

@Composable
private fun SourceRow(src: PestSource, onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onOpen(src.url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.OpenInNew, null, tint = AgroPalette.Sky, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(src.title, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Sky, modifier = Modifier.weight(1f), maxLines = 2)
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AgroPalette.Primary)
    }
}

@Composable
private fun EmptyBlock() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.BugReport, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.pest_empty_title), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.pest_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
        }
    }
}
