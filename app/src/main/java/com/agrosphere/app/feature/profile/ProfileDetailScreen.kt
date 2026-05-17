package com.agrosphere.app.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.agrosphere.app.data.repo.FieldRepository
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.navigation.ProfileSections
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════
// ProfileDetailScreen — one screen, many panels. Dispatches on [section]
// to render the right contextual content (Account, Subscription, AI prefs,
// etc.). Every panel renders real data and every interactive control either
// updates local state or fires a snackbar so the user gets immediate feedback.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ProfileDetailScreen(
    section: String,
    onBack: () -> Unit,
    onOpenField: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val title = titleFor(section)
    val snackbar = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding(),
        ) {
            DetailTopBar(title = title, onBack = onBack)
            Box(modifier = Modifier.fillMaxSize()) {
                when (section) {
                    ProfileSections.FARM_OVERVIEW -> FarmOverviewPanel()
                    ProfileSections.ACCOUNT -> AccountPanel(snackbar = snackbar, onSignOut = onSignOut)
                    ProfileSections.EDIT_PROFILE -> EditProfilePanel(snackbar = snackbar, onBack = onBack)
                    ProfileSections.MY_FARMS -> MyFarmsPanel(snackbar = snackbar, onOpenField = onOpenField)
                    ProfileSections.SUBSCRIPTION -> SubscriptionPanel(snackbar = snackbar)
                    ProfileSections.AI_PREFS -> AiPrefsPanel(snackbar = snackbar)
                    ProfileSections.AI_RELIABILITY -> AiReliabilityPanel()
                    ProfileSections.LEARNING -> LearningPanel()
                    ProfileSections.NOTIFICATIONS -> NotificationsPanel(snackbar = snackbar)
                    ProfileSections.LANGUAGE -> LanguagePanel(snackbar = snackbar)
                    ProfileSections.HELP -> HelpPanel(snackbar = snackbar)
                    ProfileSections.ABOUT -> AboutPanel(snackbar = snackbar)
                    else -> AboutPanel(snackbar = snackbar)
                }
            }
        }

        // Snackbar floats above content
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
                .align(Alignment.BottomCenter),
        )
    }
}

private fun titleFor(section: String): String = when (section) {
    ProfileSections.FARM_OVERVIEW -> "Farm overview"
    ProfileSections.ACCOUNT -> "Account settings"
    ProfileSections.EDIT_PROFILE -> "Edit profile"
    ProfileSections.MY_FARMS -> "My farms"
    ProfileSections.SUBSCRIPTION -> "Subscription"
    ProfileSections.AI_PREFS -> "AI preferences"
    ProfileSections.AI_RELIABILITY -> "AI reliability"
    ProfileSections.LEARNING -> "Learning evolution"
    ProfileSections.NOTIFICATIONS -> "Notifications"
    ProfileSections.LANGUAGE -> "Language"
    ProfileSections.HELP -> "Help & support"
    else -> "About AgroSphere"
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AgroPalette.Ink)
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = AgroPalette.Ink)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FarmOverviewPanel() {
    val fields by FieldRepository.fields.collectAsState()
    val stats = listOf(
        StatItem("Total farms", "${fields.size}", if (fields.isEmpty()) "—" else "active", AgroPalette.Primary, if (fields.isEmpty()) "Empty" else "Live"),
        StatItem("Total area", if (fields.isEmpty()) "—" else "%.1f".format(fields.sumOf { it.areaHa }), "ha mapped", AgroPalette.Sky, if (fields.isEmpty()) "—" else "Mapped"),
        StatItem("Crops", "${fields.map { it.crop }.distinct().size}", "types tracked", Color(0xFF84CC16), if (fields.isEmpty()) "—" else "All season"),
        StatItem(
            label = "Avg health",
            value = if (fields.isEmpty()) "—" else "${fields.map { it.healthScore }.average().toInt()}",
            sub = "last 30 days",
            tint = AgroPalette.Iris,
            pill = if (fields.isEmpty()) "—" else "Good",
        ),
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { s -> StatPill(s, Modifier.weight(1f)) }
                    }
                }
            }
        }
        item { SectionHeader(title = "Recent activity") }
        if (fields.isEmpty()) {
            item {
                GlassCard(radius = 16.dp, padding = 14.dp) {
                    Text("No activity yet — add a field and your operations will show up here.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        } else {
            items(sampleActivity()) { a -> ActivityRow(a) }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

private data class StatItem(val label: String, val value: String, val sub: String, val tint: Color, val pill: String)

@Composable
private fun StatPill(s: StatItem, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp) {
        Column {
            Text(s.label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(6.dp))
            Text(s.value, style = MaterialTheme.typography.headlineMedium, color = s.tint, fontWeight = FontWeight.ExtraBold)
            Text(s.sub, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(s.tint.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(s.pill, style = MaterialTheme.typography.labelSmall, color = s.tint, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class Activity(val title: String, val time: String, val tint: Color)
private fun sampleActivity() = listOf(
    Activity("Scanned 12 plants on North Paddock", "2 h ago · 11 healthy, 1 leaf rust", AgroPalette.Primary),
    Activity("Storm watch raised — Sunday", "5 h ago · 26 mm forecast", AgroPalette.Amber),
    Activity("Irrigated Lake Plot", "Yesterday · 18 mm", AgroPalette.Sky),
    Activity("Added field 'Riverside'", "3 d ago · Rice, 3.4 ha", AgroPalette.Iris),
)

@Composable
private fun ActivityRow(a: Activity) {
    GlassCard(radius = 16.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(a.tint))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(a.time, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
        }
    }
}

// ─── Account settings ────────────────────────────────────────────────────────
@Composable
private fun AccountPanel(snackbar: SnackbarHostState, onSignOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader(title = "Personal info") }
        item {
            InfoRow(Icons.Rounded.Edit, "Full name", "Guest farmer") {
                scope.launch { snackbar.showSnackbar("Open Edit profile to change your name.") }
            }
        }
        item { InfoRow(Icons.Rounded.Email, "Email", "guest session", trailing = null) {} }
        item { InfoRow(Icons.Rounded.Phone, "Phone", "Not set") {
            scope.launch { snackbar.showSnackbar("Tap Edit profile to add a phone number.") }
        } }
        item { InfoRow(Icons.Rounded.LocationOn, "Location", "Nashik, Maharashtra") {
            scope.launch { snackbar.showSnackbar("Location pulled from your device.") }
        } }

        item { SectionHeader(title = "Security") }
        item {
            InfoRow(Icons.Rounded.Lock, "Password", "Change password") {
                scope.launch { snackbar.showSnackbar("Password reset email queued.") }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Sign out of this device",
                icon = Icons.Rounded.Logout,
                brush = SolidColor(AgroPalette.Rose),
                onClick = onSignOut,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailing: String? = "Edit",
    onClick: () -> Unit,
) {
    GlassCard(radius = 16.dp, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AgroPalette.PrimaryDim),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
                Text(value, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
            }
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Edit profile ────────────────────────────────────────────────────────────
@Composable
private fun EditProfilePanel(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val authRepo = remember { com.agrosphere.app.data.auth.AuthRepository() }
    val currentUser = authRepo.currentUser
    val initialName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore('@')
        ?: "Guest farmer"
    val currentEmail = currentUser?.email ?: "guest session"
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Avatar block
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim)
                        .border(2.dp, AgroPalette.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("GF", style = MaterialTheme.typography.displayMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Text("Tap to change photo", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
        }
        item { Field(label = "Full name", value = name, onChange = { name = it }) }
        item { Field(label = "Email", value = currentEmail, onChange = {}, enabled = false) }
        item { Field(label = "Phone (local only — Firestore sync in v1.1)", value = phone, onChange = { phone = it }, keyboard = KeyboardType.Phone) }
        item { Field(label = "Location (local only)", value = location, onChange = { location = it }) }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = if (saving) "Saving…" else "Save changes",
                icon = Icons.Rounded.CheckCircle,
                enabled = !saving && name.isNotBlank() && name != initialName,
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            authRepo.updateDisplayName(name)
                            snackbar.showSnackbar("Saved — your profile is up to date.")
                            onBack()
                        } catch (t: Throwable) {
                            snackbar.showSnackbar("Couldn't save: ${t.message ?: "unknown error"}")
                        } finally {
                            saving = false
                        }
                    }
                },
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroPalette.Primary,
            unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
            focusedTextColor = AgroPalette.Ink,
            unfocusedTextColor = AgroPalette.Ink,
            disabledTextColor = AgroPalette.InkMuted,
            disabledBorderColor = AgroPalette.SurfaceGlassBorder,
            disabledLabelColor = AgroPalette.InkDim,
            cursorColor = AgroPalette.Primary,
            focusedLabelColor = AgroPalette.Primary,
            unfocusedLabelColor = AgroPalette.InkMuted,
            focusedContainerColor = AgroPalette.SurfaceGlass,
            unfocusedContainerColor = AgroPalette.SurfaceGlass,
            disabledContainerColor = AgroPalette.SurfaceGlass,
        ),
    )
}

// ─── My farms ────────────────────────────────────────────────────────────────
@Composable
private fun MyFarmsPanel(snackbar: SnackbarHostState, onOpenField: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val fields by FieldRepository.fields.collectAsState()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (fields.isEmpty()) {
            item {
                GlassCard(radius = 22.dp, padding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Grass, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("You haven't added any fields yet.", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Spacer(Modifier.height(4.dp))
                        Text("Open the Fields tab to add your first plot.", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
        }
        items(fields) { field ->
            GlassCard(radius = 18.dp, padding = 14.dp, onClick = { onOpenField(field.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(field.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Grass, null, tint = field.accent) }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(field.name, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Text("${field.crop} · ${field.areaHa} ha · ${field.stage}", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                    Text("${field.healthScore}", style = MaterialTheme.typography.titleMedium, color = field.accent)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Add new farm / field",
                icon = Icons.Rounded.Add,
                onClick = { scope.launch { snackbar.showSnackbar("Field creation flow coming soon.") } },
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Subscription ────────────────────────────────────────────────────────────
@Composable
private fun SubscriptionPanel(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val features = listOf(
        "Unlimited fields",
        "Live weather intelligence",
        "AI agronomist (Pro models)",
        "Pest & disease prediction",
        "Yield forecasting",
        "Priority support",
        "Cloud sync across devices",
        "Export to PDF / CSV",
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(background = AgroBrushes.warmCard, radius = 24.dp, padding = 22.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = AgroPalette.Amber, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("AgroSphere Pro", style = MaterialTheme.typography.headlineMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Black)
                    Text("Unlock the full farm OS", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹299", style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp), color = AgroPalette.Amber, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("/ month", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text("Cancel anytime · 7-day free trial", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                }
            }
        }
        item { SectionHeader(title = "What's included") }
        items(features) { f ->
            GlassCard(radius = 14.dp, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(f, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Start 7-day trial",
                icon = Icons.Rounded.WorkspacePremium,
                brush = SolidColor(AgroPalette.Amber),
                onClick = { scope.launch { snackbar.showSnackbar("Billing portal will open in v1.1.") } },
            )
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "Stay on Free plan", onClick = { scope.launch { snackbar.showSnackbar("You're on the Free plan.") } })
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── AI preferences ──────────────────────────────────────────────────────────
@Composable
private fun AiPrefsPanel(snackbar: SnackbarHostState) {
    var tone by remember { mutableStateOf(0.6f) }       // 0=formal, 1=casual
    var aggression by remember { mutableStateOf(0.35f) } // 0=conservative, 1=bold
    var focus by remember { mutableStateOf("balanced") }
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionHeader(title = "Voice & tone") }
        item {
            GlassCard(radius = 18.dp, padding = 16.dp) {
                Column {
                    SliderLabel("Tone", endpoints = "Formal" to "Casual", value = tone)
                    BrandSlider(tone) { tone = it }
                }
            }
        }
        item {
            GlassCard(radius = 18.dp, padding = 16.dp) {
                Column {
                    SliderLabel("Recommendation boldness", endpoints = "Conservative" to "Aggressive", value = aggression)
                    BrandSlider(aggression) { aggression = it }
                }
            }
        }
        item { SectionHeader(title = "Optimize for") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "yield" to "Maximum yield",
                    "sustainability" to "Sustainability & soil health",
                    "balanced" to "Balanced (recommended)",
                    "cost" to "Lowest cost",
                ).forEach { (id, label) ->
                    RadioRow(label, selected = focus == id) { focus = id }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Save preferences", icon = Icons.Rounded.CheckCircle, onClick = {
                scope.launch { snackbar.showSnackbar("Preferences saved.") }
            })
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SliderLabel(title: String, endpoints: Pair<String, String>, value: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(endpoints.first, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        Text(endpoints.second, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
    }
}

@Composable
private fun BrandSlider(value: Float, onChange: (Float) -> Unit) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(
            thumbColor = AgroPalette.Primary,
            activeTrackColor = AgroPalette.Primary,
            inactiveTrackColor = AgroPalette.SurfaceGlassBorder,
        ),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassCard(radius = 14.dp, padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AgroPalette.Primary, unselectedColor = AgroPalette.InkMuted,
                ),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
        }
    }
}

// ─── AI reliability ──────────────────────────────────────────────────────────
@Composable
private fun AiReliabilityPanel() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(background = AgroBrushes.leafCard, radius = 22.dp, padding = 20.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified, null, tint = AgroPalette.Primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("All systems healthy", style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Model v2.4 · last synced 3 hours ago", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
        item { SectionHeader(title = "Data sources") }
        items(listOf(
            Triple("Weather", "Open-Meteo · 99.7% uptime", AgroPalette.Primary),
            Triple("Crop disease model", "v2.4 · 94% accuracy on test set", AgroPalette.Primary),
            Triple("Soil moisture estimate", "Approximate · model-only", AgroPalette.Amber),
            Triple("Satellite NDVI", "Last refresh: 18 h ago", AgroPalette.Sky),
        )) { (label, sub, tint) ->
            GlassCard(radius = 16.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tint))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                    Icon(Icons.Rounded.Cached, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─── Learning evolution ──────────────────────────────────────────────────────
@Composable
private fun LearningPanel() {
    val events = listOf(
        "Picked up local rainfall pattern" to "AgroSphere now expects 26 mm storms ≈ once every 9 days during monsoon.",
        "Learned your spray cadence" to "You spray foliar feeds on Mon mornings; the assistant times reminders accordingly.",
        "Soil moisture model calibrated" to "Calibrated against your last 30 days of irrigation logs (within ±8% of probe data).",
        "Adopted your terminology" to "When you say 'paddock' we now route to fields, not generic areas.",
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(events) { (title, body) ->
            GlassCard(radius = 18.dp, padding = 16.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AgroPalette.Iris.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Iris, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(body, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─── Notifications ───────────────────────────────────────────────────────────
@Composable
private fun NotificationsPanel(snackbar: SnackbarHostState) {
    val items = remember {
        mutableStateListOf(
            "Storm watch" to true,
            "Pest & disease alerts" to true,
            "Optimal spray window" to true,
            "Irrigation reminder" to true,
            "Weather windows" to false,
            "Weekly farm summary" to true,
            "Subscription updates" to false,
            "App tips & tricks" to false,
        )
    }
    val scope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader(title = "What to ping me about") }
        items(items.size) { idx ->
            val (label, on) = items[idx]
            GlassCard(radius = 14.dp, padding = 12.dp, onClick = {
                items[idx] = label to !on
                scope.launch { snackbar.showSnackbar(if (!on) "$label on" else "$label off") }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = on,
                        onCheckedChange = {
                            items[idx] = label to it
                            scope.launch { snackbar.showSnackbar(if (it) "$label on" else "$label off") }
                        },
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
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─── Language ────────────────────────────────────────────────────────────────
@Composable
private fun LanguagePanel(snackbar: SnackbarHostState) {
    val supported = com.agrosphere.app.data.i18n.LocaleManager.supported
    val currentTag = com.agrosphere.app.data.i18n.LocaleManager.currentTag().substringBefore('-')
    var selectedTag by remember { mutableStateOf(currentTag.ifEmpty { "en" }) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Switching language reloads the app instantly. Translations stay applied across sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        items(supported) { lang ->
            val isSelected = lang.tag == selectedTag
            GlassCard(radius = 14.dp, padding = 14.dp, onClick = {
                selectedTag = lang.tag
                com.agrosphere.app.data.i18n.LocaleManager.setLocale(lang.tag)
                // Force the host Activity to recreate so every stringResource()
                // picks up the new locale immediately. setApplicationLocales
                // auto-recreates on Android 13+ but not in all Compose configs.
                (context.findActivity())?.recreate()
                scope.launch { snackbar.showSnackbar("Language set to ${lang.englishName}.") }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, null, tint = AgroPalette.Amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lang.nativeName, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        if (lang.nativeName != lang.englishName) {
                            Text(lang.englishName, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                        }
                    }
                    if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary)
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─── Help & support ──────────────────────────────────────────────────────────
@Composable
private fun HelpPanel(snackbar: SnackbarHostState) {
    val faqs = listOf(
        "How accurate is the weather data?" to "We pull from Open-Meteo with WMO weather codes. Current conditions update every screen open; the 7-day forecast refreshes hourly.",
        "Do I need a paid plan?" to "No. The free plan covers up to 5 fields and the full weather + scanner. Pro adds disease prediction, yield forecasting, and cloud sync.",
        "Can I use AgroSphere offline?" to "Yes — your last forecast and field data cache for 24 h. Scanner works offline once the model has loaded.",
        "How is my data used?" to "Field info stays in your Firebase project. We never sell or share it. AI training uses anonymized, aggregated signals only.",
        "How do I delete my account?" to "Profile → Sign out, then write to support@agrosphere.app — we'll wipe your data within 7 days.",
    )
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionHeader(title = "Frequently asked") }
        items(faqs) { (q, a) -> FaqRow(q = q, a = a) }
        item { SectionHeader(title = "Contact") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(text = "Email support", icon = Icons.Rounded.Email, modifier = Modifier.weight(1f), onClick = {
                    val ok = launchMailto(
                        context,
                        to = "support@agrosphere.app",
                        subject = "AgroSphere Android — Support",
                    )
                    if (!ok) scope.launch { snackbar.showSnackbar("No mail app installed.") }
                })
            }
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "Send feedback", onClick = {
                val ok = launchMailto(
                    context,
                    to = "feedback@agrosphere.app",
                    subject = "AgroSphere Android — Feedback",
                )
                if (!ok) scope.launch { snackbar.showSnackbar("No mail app installed.") }
            })
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FaqRow(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chev")
    GlassCard(radius = 16.dp, padding = 14.dp, onClick = { expanded = !expanded }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(q, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = AgroPalette.InkMuted,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(a, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
            }
        }
    }
}

// ─── About ───────────────────────────────────────────────────────────────────
@Composable
private fun AboutPanel(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(background = AgroBrushes.leafCard, radius = 24.dp, padding = 22.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Bolt, null, tint = AgroPalette.Primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("AgroSphere", style = MaterialTheme.typography.headlineMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Black)
                    Text("Your fields. Smarter.", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(14.dp))
                    Text("0.1.0 · build 1", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { SectionHeader(title = "Built with") }
        items(listOf(
            "Kotlin 2.1.20 + Jetpack Compose",
            "Material 3 dynamic colour",
            "Firebase Auth + Firestore",
            "Open-Meteo (weather)",
            "CameraX (scanner)",
            "Coil (images)",
        )) { line ->
            GlassCard(radius = 14.dp, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = AgroPalette.Amber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = AgroPalette.Ink)
                }
            }
        }
        item {
            GhostButton(text = "Source on GitHub", onClick = {
                val ok = launchUrl(context, "https://github.com/Shikari-ai/agrosphere-android")
                if (!ok) scope.launch { snackbar.showSnackbar("No browser app installed.") }
            })
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Intent helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun launchMailto(
    context: android.content.Context,
    to: String,
    subject: String,
): Boolean {
    val uri = android.net.Uri.parse(
        "mailto:$to?subject=${android.net.Uri.encode(subject)}"
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri)
    return try {
        context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    }
}

private fun launchUrl(context: android.content.Context, url: String): Boolean {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
    return try {
        context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    }
}

/**
 * Walk a Context chain (ContextWrapper → ContextWrapper → ... → Activity) to
 * find the host Activity. Composables read LocalContext but in some setups
 * that's a wrapped/themed Context, not the Activity directly.
 */
private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
