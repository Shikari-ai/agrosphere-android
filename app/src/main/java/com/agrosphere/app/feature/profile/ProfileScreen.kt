package com.agrosphere.app.feature.profile

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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun ProfileScreen(onBack: () -> Unit, onSignOut: () -> Unit) {
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
            Text("Profile", style = MaterialTheme.typography.titleMedium, color = AgroPalette.InkMuted)
        }
        Spacer(Modifier.height(8.dp))

        // Avatar card
        GlassCard(background = AgroBrushes.leafCard, radius = 26.dp, padding = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Person, null, tint = AgroPalette.Primary, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Guest farmer", style = MaterialTheme.typography.headlineSmall, color = AgroPalette.Ink)
                    Text("Demo mode · 4 fields", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Account", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Item(Icons.Rounded.Notifications, "Notifications", "Storm + irrigation alerts")
            Item(Icons.Rounded.Language, "Language", "English")
            Item(Icons.Rounded.Shield, "Privacy", "Data + permissions")
            Item(Icons.Rounded.SupportAgent, "Support", "Help & feedback")
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = "Sign out",
            icon = Icons.Rounded.Logout,
            brush = androidx.compose.ui.graphics.SolidColor(AgroPalette.Rose),
            onClick = onSignOut,
        )
        Spacer(Modifier.height(24.dp))
        Text("AgroSphere v0.1.0", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Item(icon: ImageVector, title: String, subtitle: String) {
    GlassCard(radius = 16.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.PrimaryDim),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = AgroPalette.Primary) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted)
        }
    }
}
