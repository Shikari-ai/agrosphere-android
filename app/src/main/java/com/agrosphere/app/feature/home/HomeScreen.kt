package com.agrosphere.app.feature.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.components.StatChip
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun HomeScreen(
    padding: PaddingValues,
    onOpenProfile: () -> Unit,
    onOpenField: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(eyebrow = "Today", title = "Good morning")
                IconButton(onClick = onOpenProfile) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AgroPalette.PrimaryDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = AgroPalette.Primary)
                    }
                }
            }
        }

        // Hero — farm pulse
        item {
            GlassCard(
                background = AgroBrushes.leafCard,
                radius = 26.dp,
                padding = 22.dp,
            ) {
                Column {
                    Text("FARM PULSE", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Primary)
                    Spacer(Modifier.height(6.dp))
                    Text("All systems green", style = MaterialTheme.typography.headlineLarge, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "4 fields · 15.3 ha · avg health 76",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroPalette.InkMuted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(Icons.Rounded.WbSunny, "31°", "Today", AgroPalette.Amber, modifier = Modifier.weight(1f))
                        StatChip(Icons.Rounded.WaterDrop, "54%", "Humidity", AgroPalette.Sky, modifier = Modifier.weight(1f))
                        StatChip(Icons.Rounded.Bolt, "12", "Alerts", AgroPalette.Iris, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick actions
        item {
            SectionHeader(title = "Quick actions")
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Scan a leaf", Icons.Rounded.CameraAlt, AgroPalette.Primary, Modifier.weight(1f), onOpenScanner)
                QuickAction("Ask AgroAI", Icons.Rounded.AutoAwesome, AgroPalette.Iris, Modifier.weight(1f), onOpenAssistant)
            }
        }

        // Alerts
        item {
            SectionHeader(title = "Today's alerts", trailing = "See all")
        }
        items(MockRepository.homeAlerts) { alert ->
            GlassCard(radius = 18.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(alert.tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = alert.tint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(alert.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Text(alert.subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
        }

        // Field carousel
        item {
            SectionHeader(title = "Your fields", trailing = "Manage")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(MockRepository.fields) { field ->
                    GlassCard(
                        modifier = Modifier.width(220.dp),
                        background = SolidColor(AgroPalette.SurfaceGlass),
                        radius = 22.dp,
                        padding = 16.dp,
                        onClick = { onOpenField(field.id) },
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(field.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Grass, contentDescription = null, tint = field.accent)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(field.name, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                            Text("${field.crop} · ${field.areaHa} ha", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                            Spacer(Modifier.height(14.dp))
                            Text("Health ${field.healthScore}", style = MaterialTheme.typography.labelMedium, color = field.accent)
                            HealthBar(field.healthScore, field.accent)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier,
        radius = 20.dp,
        padding = 16.dp,
        onClick = onClick,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = tint) }
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
        }
    }
}

@Composable
private fun HealthBar(score: Int, tint: androidx.compose.ui.graphics.Color) {
    val pct = (score.coerceIn(0, 100)) / 100f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
            .background(AgroPalette.SurfaceGlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                .background(tint)
        )
    }
}

