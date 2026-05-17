package com.agrosphere.app.feature.profile

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.navigation.ProfileSections
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

// ═════════════════════════════════════════════════════════════════════════════
// ProfileScreen — Hero (avatar + identity), Quick actions, Farm Intelligence
// score gauge, Stats grid (2×2), grouped menu sections, prominent sign-out.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSection: (String) -> Unit = {},
    onOpenRegional: () -> Unit = {},
    onOpenDeveloper: () -> Unit = {},
    vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by vm.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileBackdrop()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TopBar(onBack = onBack) }
            item { Hero(state = state) }
            item { QuickActionsRow(onOpenSection = onOpenSection) }
            item { ScoreCard(score = 86, label = "Excellent — top 15%") }
            item { StatsGrid(onOpen = { onOpenSection(ProfileSections.FARM_OVERVIEW) }) }

            item { GroupLabel("Farm") }
            item {
                MenuItem(Icons.Rounded.Grass, "Farm overview", "Fields, crops, totals", AgroPalette.Primary,
                    onClick = { onOpenSection(ProfileSections.FARM_OVERVIEW) })
            }
            item {
                MenuItem(Icons.Rounded.Group, "My farms", "Switch or invite collaborators", AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.MY_FARMS) })
            }
            item {
                MenuItem(Icons.Rounded.WorkspacePremium, "Subscription", "Free plan · upgrade to Pro", AgroPalette.Amber,
                    trailing = { ProPill() },
                    onClick = { onOpenSection(ProfileSections.SUBSCRIPTION) })
            }

            item { GroupLabel("Intelligence") }
            item {
                MenuItem(Icons.Rounded.Psychology, "AI preferences", "Tone, units, recommendations", AgroPalette.Iris,
                    onClick = { onOpenSection(ProfileSections.AI_PREFS) })
            }
            item {
                MenuItem(Icons.Rounded.Shield, "AI reliability", "Data freshness · model health", Color(0xFF67D4F0),
                    onClick = { onOpenSection(ProfileSections.AI_RELIABILITY) })
            }
            item {
                MenuItem(Icons.Rounded.History, "Learning evolution", "What AgroSphere is learning", AgroPalette.Iris,
                    onClick = { onOpenSection(ProfileSections.LEARNING) })
            }
            item {
                MenuItem(Icons.Rounded.Public, "Regional intelligence", "Anonymous network — 23 farms nearby", Color(0xFF22D3EE),
                    onClick = onOpenRegional)
            }

            item { GroupLabel("Account") }
            item {
                MenuItem(Icons.Rounded.Person, "Account settings", "Name, email, phone, security", AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.ACCOUNT) })
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.Notifications, title = "Notifications",
                    subtitle = if (state.notificationsOn) "Storm + irrigation alerts on" else "All quiet",
                    tint = AgroPalette.Primary,
                    checked = state.notificationsOn, onCheckedChange = vm::toggleNotifications,
                    onRowClick = { onOpenSection(ProfileSections.NOTIFICATIONS) },
                )
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.Straighten, title = "Units",
                    subtitle = if (state.useMetric) "Metric (°C, mm, ha, km/h)" else "Imperial (°F, in, ac, mph)",
                    tint = AgroPalette.Sky,
                    checked = state.useMetric, onCheckedChange = vm::toggleUnits,
                )
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.DarkMode, title = "Dark theme",
                    subtitle = if (state.darkTheme) "Always dark" else "Follow system",
                    tint = AgroPalette.Iris,
                    checked = state.darkTheme, onCheckedChange = vm::toggleTheme,
                )
            }
            item {
                MenuItem(
                    icon = Icons.Rounded.Language,
                    title = androidx.compose.ui.res.stringResource(com.agrosphere.app.R.string.profile_menu_language),
                    subtitle = com.agrosphere.app.data.i18n.LocaleManager.currentDisplayLabel(),
                    tint = AgroPalette.Amber,
                    onClick = { onOpenSection(ProfileSections.LANGUAGE) },
                )
            }

            item { GroupLabel("Support") }
            item {
                MenuItem(Icons.Rounded.SupportAgent, "Help & support", "Docs, contact, feedback", AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.HELP) })
            }
            item {
                MenuItem(Icons.Rounded.Info, "About AgroSphere", "v0.1.0 · build notes", AgroPalette.InkMuted,
                    onClick = { onOpenSection(ProfileSections.ABOUT) })
            }
            item {
                MenuItem(Icons.Rounded.Code, "Developer panel", "Logs, flags, test injection", AgroPalette.Iris,
                    onClick = onOpenDeveloper)
            }

            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "Sign out",
                    icon = Icons.Rounded.Logout,
                    brush = SolidColor(AgroPalette.Rose),
                    onClick = {
                        vm.signOut()
                        onSignOut()
                    },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Made with care · AgroSphere 0.1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkDim,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
        }
        Spacer(Modifier.width(4.dp))
        Text("Profile", style = MaterialTheme.typography.titleMedium, color = AgroPalette.InkMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Hero(state: ProfileUiState) {
    GlassCard(background = AgroBrushes.leafCard, radius = 28.dp, padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(photoUrl = state.photoUrl, displayName = state.displayName, isAnonymous = state.isAnonymous)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AgroPalette.Ink,
                        maxLines = 1,
                    )
                    if (state.isVerified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.VerifiedUser, null, tint = AgroPalette.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                RoleBadge(isAnonymous = state.isAnonymous)
                if (state.email.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Email, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(state.email, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nashik, Maharashtra", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}

@Composable
private fun Avatar(photoUrl: String?, displayName: String, isAnonymous: Boolean) {
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        // Ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris,
                            AgroPalette.Primary,
                        )
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(AgroPalette.SurfaceElev),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(76.dp).clip(CircleShape),
                )
            } else {
                Text(
                    text = initialsOf(displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        // Camera badge bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(AgroPalette.Primary)
                .border(2.dp, AgroPalette.SurfaceElev, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CameraAlt, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(14.dp))
        }
        if (isAnonymous) {
            // Guest dot at top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Amber)
                    .border(2.dp, AgroPalette.SurfaceElev, CircleShape)
            )
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }
    return when (parts.size) {
        0 -> "?"
        1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Composable
private fun RoleBadge(isAnonymous: Boolean) {
    val (label, tint) = if (isAnonymous) "GUEST MODE" to AgroPalette.Amber
        else "FARM OWNER" to AgroPalette.Primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontSize = 9.sp),
            color = tint, fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick actions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsRow(onOpenSection: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction("Edit", Icons.Rounded.Person, Modifier.weight(1f)) { onOpenSection(ProfileSections.EDIT_PROFILE) }
        QuickAction("Invite", Icons.Rounded.Group, Modifier.weight(1f)) { onOpenSection(ProfileSections.MY_FARMS) }
        QuickAction("Share", Icons.Rounded.Share, Modifier.weight(1f)) { onOpenSection(ProfileSections.ABOUT) }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, radius = 16.dp, padding = 12.dp, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Farm Intelligence Score
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScoreCard(score: Int, label: String) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
        label = "score",
    )
    GlassCard(background = AgroBrushes.leafCard, radius = 24.dp, padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreGauge(progress = animated, score = score, modifier = Modifier.size(96.dp))
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "FARM INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = AgroPalette.Primary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Farm Intelligence Score", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(8.dp))
                StarsRow(rating = (score / 20f))
            }
        }
    }
}

@Composable
private fun ScoreGauge(progress: Float, score: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8f
            val arc = size.minDimension - stroke
            val inset = stroke / 2f
            drawArc(
                color = AgroPalette.SurfaceGlassBorder,
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(arc, arc),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Primary)
                ),
                startAngle = 135f, sweepAngle = 270f * progress, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(arc, arc),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp), color = AgroPalette.Ink, fontWeight = FontWeight.Black)
            Text("of 100", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

@Composable
private fun StarsRow(rating: Float) {
    Row {
        repeat(5) { i ->
            val filled = (i + 1) <= rating.toInt()
            Icon(
                Icons.Rounded.Star, null,
                tint = if (filled) AgroPalette.Amber else AgroPalette.SurfaceGlassBorder,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats grid (2×2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsGrid(onOpen: () -> Unit) {
    val fields by com.agrosphere.app.data.repo.FieldRepository.fields.collectAsState()
    val stats = listOf(
        Triple("Farms", "${fields.size}", AgroPalette.Primary),
        Triple(
            "Area",
            if (fields.isEmpty()) "—" else "%.1f ha".format(fields.sumOf { it.areaHa }),
            AgroPalette.Sky,
        ),
        Triple("Crops", if (fields.isEmpty()) "—" else "${fields.map { it.crop }.distinct().size} types", AgroPalette.Amber),
        Triple(
            "Avg health",
            if (fields.isEmpty()) "—" else "${fields.map { it.healthScore }.average().toInt()}",
            AgroPalette.Iris,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value, tint) ->
                    StatTile(label = label, value = value, tint = tint, modifier = Modifier.weight(1f), onClick = onOpen)
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp, onClick = onClick) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = tint, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Menu items
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = AgroPalette.InkDim,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    GlassCard(radius = 18.dp, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            if (trailing != null) trailing() else Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted)
        }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
) {
    GlassCard(radius = 18.dp, padding = 14.dp, onClick = onRowClick ?: { onCheckedChange(!checked) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, maxLines = 1)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AgroPalette.BgDeep,
                    checkedTrackColor = AgroPalette.Primary,
                    checkedBorderColor = AgroPalette.Primary,
                    uncheckedThumbColor = AgroPalette.InkDim,
                    uncheckedTrackColor = AgroPalette.SurfaceGlass,
                    uncheckedBorderColor = AgroPalette.SurfaceGlassBorder,
                ),
            )
        }
    }
}

@Composable
private fun ProPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Amber.copy(alpha = 0.18f))
            .border(1.dp, AgroPalette.Amber.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = AgroPalette.Amber, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                "PRO",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.sp),
                color = AgroPalette.Amber, fontWeight = FontWeight.Black,
            )
        }
    }
}
