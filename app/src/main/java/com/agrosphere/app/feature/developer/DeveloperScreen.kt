package com.agrosphere.app.feature.developer

import android.os.Build
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// DeveloperScreen — internal panel for testing + diagnostics. Not for end
// users; reachable from Profile → About → "Developer panel" easter egg.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<LogLine>() }

    LaunchedEffect(Unit) {
        // Seed a few fake log lines
        logs += LogLine("INFO", "WeatherRepository", "load() succeeded — Nashik, 31°C")
        logs += LogLine("INFO", "AuthRepository", "userFlow emitted — uid=guest-anon-7d8e2c")
        logs += LogLine("WARN", "ScannerVM", "Inference latency 312ms (threshold 250ms)")
        logs += LogLine("INFO", "MockRepository", "fields hydrated — 4 fields, 15.3 ha")
    }

    // Feature flags
    val flags = remember {
        mutableStateListOf(
            FeatureFlag("scanner.batch_mode", true),
            FeatureFlag("ai.copilot.voice", true),
            FeatureFlag("weather.use_imd_proxy", false),
            FeatureFlag("home.parallax_header", true),
            FeatureFlag("profile.theme_toggle", true),
            FeatureFlag("regional.opt_in_share", false),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ScreenTitle(eyebrow = "Internal", title = "Developer")
                }
                EnvironmentChip()
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Environment", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp)) }
                item { EnvironmentCard() }

                item { Text("Feature flags", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
                items(flags.size) { i ->
                    FlagRow(flag = flags[i], onChange = {
                        flags[i] = flags[i].copy(enabled = it)
                        scope.launch {
                            snackbar.showSnackbar("${flags[i].key} → ${if (it) "ON" else "OFF"}")
                            logs.add(0, LogLine("INFO", "FeatureFlag", "${flags[i].key} = $it"))
                        }
                    })
                }

                item { Text("Test injection", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TestButton("Mock scan", AgroPalette.Primary, Modifier.weight(1f)) {
                            scope.launch {
                                snackbar.showSnackbar("Injected scan: Wheat leaf rust, 87% conf")
                                logs.add(0, LogLine("INFO", "ScannerVM", "TEST INJECT — scan(Wheat leaf rust)"))
                            }
                        }
                        TestButton("Storm alert", AgroPalette.Amber, Modifier.weight(1f)) {
                            scope.launch {
                                snackbar.showSnackbar("Pushed storm watch — Sunday 26mm")
                                logs.add(0, LogLine("WARN", "AlertsBus", "TEST INJECT — storm watch Sunday"))
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TestButton("Crash test", AgroPalette.Rose, Modifier.weight(1f)) {
                            scope.launch {
                                snackbar.showSnackbar("Stubbed — non-fatal exception logged.")
                                logs.add(0, LogLine("ERROR", "Crashlytics", "TEST NON_FATAL — synthetic"))
                            }
                        }
                        TestButton("Clear cache", AgroPalette.Sky, Modifier.weight(1f)) {
                            scope.launch {
                                snackbar.showSnackbar("Cleared 1.4 MB of cached weather.")
                                logs.add(0, LogLine("INFO", "Cache", "Cleared weather cache (1.4 MB)"))
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Logs", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        SmallChip(icon = Icons.Rounded.Refresh, label = "Clear") {
                            logs.clear()
                            logs += LogLine("INFO", "Developer", "Log buffer cleared.")
                        }
                        Spacer(Modifier.width(6.dp))
                        SmallChip(icon = Icons.Rounded.ContentCopy, label = "Copy") {
                            scope.launch { snackbar.showSnackbar("Logs copied to clipboard.") }
                        }
                    }
                }
                items(logs) { line -> LogRow(line) }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
        )
    }
}

private data class FeatureFlag(val key: String, val enabled: Boolean)
private data class LogLine(val level: String, val tag: String, val message: String) {
    val time: String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}

@Composable
private fun EnvironmentChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Iris.copy(alpha = 0.16f))
            .border(1.dp, AgroPalette.Iris.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text("DEBUG", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp), color = AgroPalette.Iris, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EnvironmentCard() {
    GlassCard(radius = 18.dp) {
        Column {
            InfoLine("App", "AgroSphere 0.1.0 · debug")
            InfoLine("Build SDK", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            InfoLine("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
            InfoLine("Locale", Locale.getDefault().toLanguageTag())
            InfoLine("Firebase project", "agritech-4d1ba")
            InfoLine("Weather backend", "open-meteo.com")
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FlagRow(flag: FeatureFlag, onChange: (Boolean) -> Unit) {
    GlassCard(radius = 14.dp, padding = 12.dp, onClick = { onChange(!flag.enabled) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Code, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                flag.key,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                color = AgroPalette.Ink,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = flag.enabled,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AgroPalette.BgDeep,
                    checkedTrackColor = AgroPalette.Primary,
                    uncheckedThumbColor = AgroPalette.InkDim,
                    uncheckedTrackColor = AgroPalette.SurfaceGlass,
                ),
            )
        }
    }
}

@Composable
private fun TestButton(label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Science, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink)
    }
}

@Composable
private fun LogRow(line: LogLine) {
    val (tint, icon) = when (line.level) {
        "ERROR" -> AgroPalette.Rose to Icons.Rounded.BugReport
        "WARN" -> AgroPalette.Amber to Icons.Rounded.Warning
        else -> AgroPalette.Primary to Icons.Rounded.Terminal
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AgroPalette.SurfaceGlass)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(line.time, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = AgroPalette.InkDim)
                Spacer(Modifier.width(8.dp))
                Text(line.level, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp), color = tint, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(line.tag, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = AgroPalette.InkMuted)
            }
            Spacer(Modifier.height(2.dp))
            Text(line.message, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = AgroPalette.Ink)
        }
    }
}
