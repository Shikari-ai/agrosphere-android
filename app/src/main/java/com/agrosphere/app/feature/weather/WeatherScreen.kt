package com.agrosphere.app.feature.weather

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.model.WeatherAlert
import com.agrosphere.app.data.model.WeatherDay
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun WeatherScreen(padding: PaddingValues) {
    val today = MockRepository.weekForecast.first()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(eyebrow = "Forecast", title = "Weather") }

        item {
            GlassCard(background = AgroBrushes.coolCard, radius = 26.dp, padding = 22.dp) {
                Column {
                    Text("RIGHT NOW", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Sky)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${today.tempC}°", style = MaterialTheme.typography.displayLarge, color = AgroPalette.Ink, fontWeight = FontWeight.Black)
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(today.condition, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                            Text("Low ${today.tempLowC}°", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniMetric("Rain", "${today.rainMm} mm", AgroPalette.Sky, Modifier.weight(1f))
                        MiniMetric("Humidity", "${today.humidityPct}%", AgroPalette.Primary, Modifier.weight(1f))
                        MiniMetric("Wind", "9 km/h", AgroPalette.Iris, Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionHeader(title = "7-day outlook") }
        items(MockRepository.weekForecast.drop(0)) { day -> DayRow(day) }

        item { SectionHeader(title = "Agronomy alerts") }
        items(MockRepository.weatherAlerts) { alert -> AlertCard(alert) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = tint)
        }
    }
}

@Composable
private fun DayRow(day: WeatherDay) {
    val (icon, tint) = when {
        "thunder" in day.condition.lowercase() -> Icons.Rounded.Thunderstorm to AgroPalette.Iris
        "shower" in day.condition.lowercase() || day.rainMm > 0 -> Icons.Rounded.WaterDrop to AgroPalette.Sky
        "cloud" in day.condition.lowercase() -> Icons.Rounded.Cloud to AgroPalette.InkMuted
        else -> Icons.Rounded.WbSunny to AgroPalette.Amber
    }
    GlassCard(radius = 16.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(day.label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(day.condition, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${day.tempC}° / ${day.tempLowC}°", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text("${day.rainMm} mm", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Sky)
            }
        }
    }
}

@Composable
private fun AlertCard(alert: WeatherAlert) {
    val tint = when (alert.severity) {
        WeatherAlert.Severity.Info -> AgroPalette.Sky
        WeatherAlert.Severity.Watch -> AgroPalette.Amber
        WeatherAlert.Severity.Warning -> AgroPalette.Rose
    }
    GlassCard(radius = 18.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Bolt, null, tint = tint) }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Spacer(Modifier.height(2.dp))
                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}
