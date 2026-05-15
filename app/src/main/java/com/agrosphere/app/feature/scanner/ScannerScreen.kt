package com.agrosphere.app.feature.scanner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun ScannerScreen(padding: PaddingValues) {
    var scanned by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ScreenTitle(eyebrow = "Diagnostics", title = "Leaf scanner")
        Spacer(Modifier.height(16.dp))

        // Viewfinder placeholder — wire CameraX preview here when permission flow is enabled
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(AgroPalette.SurfaceElev),
            contentAlignment = Alignment.Center,
        ) {
            if (scanned) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = AgroPalette.Primary,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Scan complete", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AgroPalette.PrimaryDim),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.CameraAlt, null, tint = AgroPalette.Primary, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text("Aim at a leaf or crop area", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                    Text("Hold steady for 2 seconds", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
            // Corner brackets
            Corners()
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton(text = "Gallery", onClick = { scanned = true }, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = if (scanned) "Re-scan" else "Capture",
                icon = Icons.Rounded.CameraAlt,
                onClick = { scanned = !scanned },
                modifier = Modifier.weight(1f),
            )
        }

        if (scanned) {
            Spacer(Modifier.height(20.dp))
            Text("Findings", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MockRepository.sampleScan.forEach { f ->
                    GlassCard(radius = 18.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                                Text(
                                    "${(f.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AgroPalette.Primary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Severity: ${f.severity}", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Amber)
                            Spacer(Modifier.height(8.dp))
                            Text(f.advice, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Corners() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val len = 28.dp
        val w = 3.dp
        // top-left
        Box(Modifier.size(len, w).background(AgroPalette.Primary).align(Alignment.TopStart))
        Box(Modifier.size(w, len).background(AgroPalette.Primary).align(Alignment.TopStart))
        // top-right
        Box(Modifier.size(len, w).background(AgroPalette.Primary).align(Alignment.TopEnd))
        Box(Modifier.size(w, len).background(AgroPalette.Primary).align(Alignment.TopEnd))
        // bottom-left
        Box(Modifier.size(len, w).background(AgroPalette.Primary).align(Alignment.BottomStart))
        Box(Modifier.size(w, len).background(AgroPalette.Primary).align(Alignment.BottomStart))
        // bottom-right
        Box(Modifier.size(len, w).background(AgroPalette.Primary).align(Alignment.BottomEnd))
        Box(Modifier.size(w, len).background(AgroPalette.Primary).align(Alignment.BottomEnd))
    }
}
