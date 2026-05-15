package com.agrosphere.app.feature.fields

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.model.Field
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun FieldsScreen(
    padding: PaddingValues,
    onOpenField: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(eyebrow = "Plots", title = "Your fields")
            Spacer(Modifier.height(4.dp))
            Text(
                "${MockRepository.fields.size} fields · ${"%.1f".format(MockRepository.fields.sumOf { it.areaHa })} ha total",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(MockRepository.fields) { field ->
            FieldRow(field = field, onClick = { onOpenField(field.id) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun FieldRow(field: Field, onClick: () -> Unit) {
    GlassCard(radius = 20.dp, padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(field.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Grass, contentDescription = null, tint = field.accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(field.name, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Text(
                    "${field.crop} · ${field.areaHa} ha · ${field.stage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.height(8.dp))
                ProgressBar(field.healthScore, field.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${field.healthScore}", style = MaterialTheme.typography.headlineSmall, color = field.accent)
                Text("health", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
        }
    }
}

@Composable
private fun ProgressBar(value: Int, tint: androidx.compose.ui.graphics.Color) {
    val pct = (value.coerceIn(0, 100)) / 100f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(AgroPalette.SurfaceGlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tint)
        )
    }
}
