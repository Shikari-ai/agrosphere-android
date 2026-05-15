package com.agrosphere.app.feature.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.StatChip
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun FieldDetailScreen(fieldId: String, onBack: () -> Unit) {
    val field = MockRepository.field(fieldId) ?: MockRepository.fields.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.size(4.dp))
            Text("Field", style = MaterialTheme.typography.titleMedium, color = AgroPalette.InkMuted)
        }
        Spacer(Modifier.height(8.dp))

        // Hero
        GlassCard(background = AgroBrushes.leafCard, radius = 26.dp, padding = 22.dp) {
            Column {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(field.accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Grass, contentDescription = null, tint = field.accent) }
                Spacer(Modifier.height(14.dp))
                Text(field.name, style = MaterialTheme.typography.displayMedium, color = AgroPalette.Ink)
                Text("${field.crop} · ${field.areaHa} ha", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip(Icons.Rounded.Bolt, "${field.healthScore}", "Health", field.accent, modifier = Modifier.weight(1f))
                    StatChip(Icons.Rounded.WaterDrop, "${field.moisturePct}%", "Soil", AgroPalette.Sky, modifier = Modifier.weight(1f))
                    StatChip(Icons.Rounded.CalendarMonth, "${field.sownDaysAgo}d", "Since sowing", AgroPalette.Amber, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Stage", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(6.dp))
        GlassCard(radius = 18.dp) {
            Text(field.stage, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent activity", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityRow("Irrigation", "Yesterday · 18mm", AgroPalette.Sky)
            ActivityRow("Foliar feed", "3 days ago · NPK 19-19-19", AgroPalette.Primary)
            ActivityRow("Scouting", "5 days ago · 2 hotspots flagged", AgroPalette.Amber)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActivityRow(title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color) {
    GlassCard(radius = 16.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}
