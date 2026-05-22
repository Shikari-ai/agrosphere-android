package com.agrosphere.app.feature.scanner

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.R
import com.agrosphere.app.data.repo.VisionDiagnosis
import com.agrosphere.app.data.repo.VisionTreatment
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.theme.AgroPalette

internal fun riskTint(level: String): Color = when (level.lowercase()) {
    "high" -> AgroPalette.Rose
    "medium" -> AgroPalette.Amber
    else -> AgroPalette.Primary
}

@StringRes
internal fun riskLabelRes(level: String): Int = when (level.lowercase()) {
    "healthy" -> R.string.scan_level_healthy
    "high" -> R.string.scan_level_high
    "medium" -> R.string.scan_level_medium
    else -> R.string.scan_level_low
}

/** Full diagnosis detail card — header, risk pill, confidence bar, narrative,
 *  recommendations and treatments. Shared by the scanner result + history. */
@Composable
internal fun DiagnosisCard(d: VisionDiagnosis) {
    val tint = riskTint(d.riskLevel)
    Column {
        GlassCard(radius = 18.dp, padding = 16.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (d.riskLevel == "healthy") Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                            null, tint = tint, modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(d.diseaseName, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        if (d.plantType.isNotBlank()) {
                            Text(d.plantType, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                        }
                    }
                    Text("${d.confidence}%", style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text(stringResource(riskLabelRes(d.riskLevel)), style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
                    }
                    if (d.scientificName.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(d.scientificName, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(AgroPalette.SurfaceGlassBorder)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((d.confidence / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(tint),
                    )
                }

                val body = d.narrative.ifBlank { d.summary }
                if (body.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
                }
            }
        }

        if (d.recommendations.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.scan_recommended).uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp), color = AgroPalette.InkMuted)
            Spacer(Modifier.height(8.dp))
            GlassCard(radius = 16.dp, padding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.recommendations.forEach { r ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(r, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Ink)
                        }
                    }
                }
            }
        }

        if (d.treatments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.scan_treatments).uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp), color = AgroPalette.InkMuted)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.treatments.forEach { t -> TreatmentRow(t) }
            }
        }
    }
}

@Composable
internal fun TreatmentRow(t: VisionTreatment) {
    val tint = when (t.type) {
        "chemical" -> AgroPalette.Rose
        "organic" -> AgroPalette.Primary
        "fertilizer" -> AgroPalette.Sky
        else -> AgroPalette.Amber
    }
    GlassCard(radius = 14.dp, padding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(t.type.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = tint, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                if (t.usage.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(t.usage, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}
