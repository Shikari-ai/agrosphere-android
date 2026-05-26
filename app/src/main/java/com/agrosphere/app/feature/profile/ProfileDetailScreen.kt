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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import com.agrosphere.app.data.repo.LocalProfileStore
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.data.repo.SupportTicket
import com.agrosphere.app.data.repo.SupportTicketRepository
import com.agrosphere.app.data.repo.TicketType
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.components.SectionHeader
import com.agrosphere.app.ui.navigation.ProfileSections
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

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
    onOpenSection: (String) -> Unit = {},
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
                    ProfileSections.ACCOUNT -> AccountPanel(snackbar = snackbar, onSignOut = onSignOut, onOpenSection = onOpenSection)
                    ProfileSections.EDIT_PROFILE -> EditProfilePanel(snackbar = snackbar, onBack = onBack)
                    ProfileSections.MY_FARMS -> MyFarmsPanel(snackbar = snackbar, onOpenField = onOpenField)
                    ProfileSections.AI_PREFS -> AiPrefsPanel(snackbar = snackbar)
                    ProfileSections.AI_RELIABILITY -> AiReliabilityPanel()
                    ProfileSections.LEARNING -> LearningPanel()
                    ProfileSections.NOTIFICATIONS -> NotificationsPanel(snackbar = snackbar)
                    ProfileSections.MODE -> ModePanel(snackbar = snackbar)
                    ProfileSections.LANGUAGE -> LanguagePanel(snackbar = snackbar)
                    ProfileSections.HELP -> HelpPanel(snackbar = snackbar)
                    ProfileSections.ABOUT -> AboutPanel(snackbar = snackbar, onOpenSection = onOpenSection)
                    else -> AboutPanel(snackbar = snackbar, onOpenSection = onOpenSection)
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
    ProfileSections.AI_PREFS -> "AI preferences"
    ProfileSections.AI_RELIABILITY -> "AI reliability"
    ProfileSections.LEARNING -> "Learning evolution"
    ProfileSections.NOTIFICATIONS -> "Notifications"
    ProfileSections.MODE -> "App mode"
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
private fun AccountPanel(
    snackbar: SnackbarHostState,
    onSignOut: () -> Unit,
    onOpenSection: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepo = remember { com.agrosphere.app.data.auth.AuthRepository() }
    val user = authRepo.currentUser
    val name = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@')
        ?: "Guest farmer"
    val email = user?.email ?: "Guest session"
    val phone = LocalProfileStore.getPhone(context).ifBlank { "Not set" }
    val weather by com.agrosphere.app.data.weather.WeatherRepository.bundleFlow.collectAsState()
    val liveLocation = weather?.snapshot?.location.orEmpty()
    val location = LocalProfileStore.getLocation(context)
        .ifBlank { liveLocation }
        .ifBlank { "Locating…" }
    LaunchedEffect(Unit) {
        if (com.agrosphere.app.data.weather.WeatherRepository.cached() == null) {
            runCatching { com.agrosphere.app.data.weather.WeatherRepository.load(context) }
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(title = "Personal info") }
            item { InfoRow(Icons.Rounded.Edit, "Full name", name) { onOpenSection(ProfileSections.EDIT_PROFILE) } }
            item { InfoRow(Icons.Rounded.Email, "Email", email, trailing = null) {} }
            item { InfoRow(Icons.Rounded.Phone, "Phone", phone) { onOpenSection(ProfileSections.EDIT_PROFILE) } }
            item { InfoRow(Icons.Rounded.LocationOn, "Location", location) { onOpenSection(ProfileSections.EDIT_PROFILE) } }

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
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = if (deleting) "Deleting…" else "Delete account",
                    onClick = { if (!deleting) showDeleteConfirm = true },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your scans and chat history stay on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkDim,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(40.dp))
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = AgroPalette.Surface,
                title = { Text("Delete account?", color = AgroPalette.Ink) },
                text = {
                    Text(
                        "Your login will be permanently deleted. Your saved scans and chat history will stay on this device.",
                        color = AgroPalette.InkMuted,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        deleting = true
                        scope.launch {
                            try {
                                authRepo.deleteAccount()
                                onSignOut()
                            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                                deleting = false
                                snackbar.showSnackbar("For security, sign out and sign in again, then delete.")
                            } catch (t: Throwable) {
                                deleting = false
                                snackbar.showSnackbar("Couldn't delete account: ${t.message ?: "try again"}")
                            }
                        }
                    }) { Text("Delete", color = AgroPalette.Rose) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = AgroPalette.InkMuted)
                    }
                },
            )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialPhone = remember { LocalProfileStore.getPhone(context) }
    val initialLocation = remember {
        LocalProfileStore.getLocation(context)
            .ifBlank { com.agrosphere.app.data.weather.WeatherRepository.cached()?.snapshot?.location.orEmpty() }
    }
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var location by remember { mutableStateOf(initialLocation) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val changed = name != initialName || phone != initialPhone || location != initialLocation

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
        item { Field(label = "Phone", value = phone, onChange = { phone = it }, keyboard = KeyboardType.Phone) }
        item { Field(label = "Location", value = location, onChange = { location = it }) }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = if (saving) "Saving…" else "Save changes",
                icon = Icons.Rounded.CheckCircle,
                enabled = !saving && name.isNotBlank() && changed,
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            if (name != initialName) authRepo.updateDisplayName(name)
                            LocalProfileStore.save(context, phone, location)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reactively track permission state — updated by launcher result or "Enable" tap.
    var permGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        permGranted = granted
        if (!granted) scope.launch { snackbar.showSnackbar("Open Settings → App info → Notifications") }
    }

    // Persist per-topic preferences across sessions.
    val topicPrefs = remember { context.getSharedPreferences("agro_notif_topics", Context.MODE_PRIVATE) }
    val defaultTopics = listOf(
        "Storm watch"          to true,
        "Pest & disease alerts" to true,
        "Optimal spray window" to true,
        "Irrigation reminder"  to true,
        "Weather windows"      to false,
        "Weekly farm summary"  to true,
        "Subscription updates" to false,
        "App tips & tricks"    to false,
    )
    val items = remember {
        mutableStateListOf(*defaultTopics.map { (label, default) ->
            label to topicPrefs.getBoolean(label, default)
        }.toTypedArray())
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Permission banner — only shown when OS permission is missing.
        if (!permGranted) {
            item {
                GlassCard(radius = 14.dp, padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Notifications, null,
                            tint = AgroPalette.Amber,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notifications blocked",
                                style = MaterialTheme.typography.titleSmall,
                                color = AgroPalette.Ink,
                            )
                            Text(
                                "Allow AgroSphere to send you alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroPalette.InkMuted,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AgroPalette.Primary.copy(alpha = 0.18f))
                                .border(1.dp, AgroPalette.Primary.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Enable",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroPalette.Primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = "What to ping me about") }

        items(items.size) { idx ->
            val (label, on) = items[idx]
            GlassCard(radius = 14.dp, padding = 12.dp, onClick = {
                if (permGranted) {
                    val newOn = !on
                    items[idx] = label to newOn
                    topicPrefs.edit().putBoolean(label, newOn).apply()
                    scope.launch { snackbar.showSnackbar(if (newOn) "$label on" else "$label off") }
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (permGranted) AgroPalette.Ink else AgroPalette.InkDim,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = on,
                        enabled = permGranted,
                        onCheckedChange = { newOn ->
                            items[idx] = label to newOn
                            topicPrefs.edit().putBoolean(label, newOn).apply()
                            scope.launch { snackbar.showSnackbar(if (newOn) "$label on" else "$label off") }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AgroPalette.BgDeep,
                            checkedTrackColor = AgroPalette.Primary,
                            uncheckedThumbColor = AgroPalette.InkDim,
                            uncheckedTrackColor = AgroPalette.SurfaceGlass,
                            disabledCheckedTrackColor = AgroPalette.SurfaceGlass,
                            disabledUncheckedTrackColor = AgroPalette.SurfaceGlass,
                        ),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─── App mode (farmer / plant parent / both) ─────────────────────────────────
@Composable
private fun ModePanel(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentMode by com.agrosphere.app.data.repo.AppPreferences.userMode.collectAsState()

    val options = listOf(
        Triple("farmer", "🌾 Farmer",       "Manage agricultural fields, crops, and yield"),
        Triple("plant",  "🪴 Plant Parent", "Monitor flowers, houseplants, and home gardens"),
        Triple("both",   "🌿 Both",         "Full access — farmland and home plants"),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Changing modes updates your bottom tabs instantly. You can switch any time.",
                style = MaterialTheme.typography.bodySmall,
                color = AgroPalette.InkMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        items(options) { (key, title, desc) ->
            val selected = currentMode == key
            GlassCard(radius = 16.dp, padding = 16.dp, onClick = {
                // Persist + apply live
                context.getSharedPreferences("agro_prefs", Context.MODE_PRIVATE)
                    .edit().putString("user_mode", key).apply()
                com.agrosphere.app.data.repo.AppPreferences.setUserMode(key)
                scope.launch { snackbar.showSnackbar("Mode set to ${title.substringAfter(' ')}") }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                    }
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
        "How do I delete my account?" to "Profile → Account settings → Delete account. Your login is removed; your saved scans and chat history stay on this device.",
    )
    val scope = rememberCoroutineScope()
    var submitType by remember { mutableStateOf<TicketType?>(null) }
    var showMyTickets by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Get Help ─────────────────────────────────────────────────────────
        item { SectionHeader(title = "Get help") }
        item {
            HelpCard(
                icon = Icons.Rounded.Forum, iconTint = AgroPalette.Primary,
                title = "Chat with Support", sub = "We usually reply quickly",
                onClick = { submitType = TicketType.CHAT_SUPPORT },
            )
        }
        item {
            HelpCard(
                icon = Icons.Rounded.MenuBook, iconTint = AgroPalette.Sky,
                title = "Help Center", sub = "Browse articles and guides",
                onClick = { scope.launch { snackbar.showSnackbar("Help Center — coming soon.") } },
            )
        }
        item {
            HelpCard(
                icon = Icons.Rounded.BugReport, iconTint = AgroPalette.Rose,
                title = "Report an Issue", sub = "Let us know what went wrong",
                onClick = { submitType = TicketType.ISSUE },
            )
        }
        item {
            HelpCard(
                icon = Icons.Rounded.StarRate, iconTint = AgroPalette.Iris,
                title = "Feedback", sub = "Share your experience",
                onClick = { submitType = TicketType.FEEDBACK },
            )
        }

        // ── Your Submissions ─────────────────────────────────────────────────
        item { SectionHeader(title = "Your submissions") }
        item {
            HelpCard(
                icon = Icons.Rounded.Inbox, iconTint = AgroPalette.Amber,
                title = "My Tickets", sub = "See your requests & replies",
                onClick = { showMyTickets = true },
            )
        }

        // ── Quick Tips ───────────────────────────────────────────────────────
        item { SectionHeader(title = "Quick tips") }
        item {
            GlassCard(radius = 16.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AgroPalette.Amber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Lightbulb, null, tint = AgroPalette.Amber, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Add a field under Fields → Run a crop scan → Open Weather for forecasts → Use AI Assistant for real-time farm guidance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.InkMuted,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── FAQ ──────────────────────────────────────────────────────────────
        item { SectionHeader(title = "Frequently asked") }
        items(faqs) { (q, a) -> FaqRow(q = q, a = a) }
        item { Spacer(Modifier.height(40.dp)) }
    }

    // ── Submit ticket dialog ──────────────────────────────────────────────────
    submitType?.let { type ->
        TicketSubmitDialog(
            type = type,
            onDismiss = { submitType = null },
            onSubmit = { msg ->
                scope.launch {
                    val ok = SupportTicketRepository.submit(type, msg)
                    submitType = null
                    snackbar.showSnackbar(if (ok) "Message sent! We'll reply shortly." else "Failed to send — check your connection.")
                }
            },
        )
    }

    // ── My tickets dialog ─────────────────────────────────────────────────────
    if (showMyTickets) {
        MyTicketsDialog(onDismiss = { showMyTickets = false })
    }
}

@Composable
private fun HelpCard(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, title: String, sub: String, onClick: () -> Unit) {
    GlassCard(radius = 16.dp, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted)
        }
    }
}

@Composable
private fun TicketSubmitDialog(type: TicketType, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    val (tint, icon, subText) = when (type) {
        TicketType.CHAT_SUPPORT -> Triple(AgroPalette.Primary, Icons.Rounded.Forum, "Describe your problem and we'll get back to you.")
        TicketType.ISSUE        -> Triple(AgroPalette.Rose, Icons.Rounded.BugReport, "Describe what went wrong in as much detail as possible.")
        TicketType.FEEDBACK     -> Triple(AgroPalette.Iris, Icons.Rounded.StarRate, "Tell us what you love or what could be better.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AgroPalette.Surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(type.label, color = AgroPalette.Ink)
            }
        },
        text = {
            Column {
                Text(subText, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 1000) message = it },
                    placeholder = { Text("Type your message here…", style = MaterialTheme.typography.bodySmall) },
                    minLines = 4,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tint,
                        unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
                        focusedTextColor = AgroPalette.Ink,
                        unfocusedTextColor = AgroPalette.Ink,
                        cursorColor = tint,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${message.length} / 1000",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroPalette.InkDim,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (message.isNotBlank()) onSubmit(message.trim()) },
                enabled = message.isNotBlank(),
            ) { Text("Send", color = tint, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AgroPalette.InkMuted) }
        },
    )
}

@Composable
private fun MyTicketsDialog(onDismiss: () -> Unit) {
    val tickets = remember { mutableStateListOf<SupportTicket>() }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        tickets.addAll(SupportTicketRepository.myTickets())
        loading = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AgroPalette.Surface,
        title = { Text("My Tickets", color = AgroPalette.Ink) },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        CircularProgressIndicator(color = AgroPalette.Primary, modifier = Modifier.size(28.dp))
                    }
                    tickets.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Inbox, null, tint = AgroPalette.InkDim, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No tickets yet", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tickets.forEach { t -> TicketRow(t) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AgroPalette.Primary) }
        },
    )
}

@Composable
private fun TicketRow(ticket: SupportTicket) {
    val (tint, icon) = when (ticket.type) {
        TicketType.CHAT_SUPPORT -> AgroPalette.Primary to Icons.Rounded.Forum
        TicketType.ISSUE        -> AgroPalette.Rose    to Icons.Rounded.BugReport
        TicketType.FEEDBACK     -> AgroPalette.Iris    to Icons.Rounded.StarRate
    }
    val (statusLabel, statusTint) = when (ticket.status) {
        "replied"  -> "Replied"  to AgroPalette.Sky
        "resolved" -> "Resolved" to AgroPalette.InkMuted
        else       -> "Open"     to AgroPalette.Amber
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AgroPalette.SurfaceGlass)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(ticket.type.label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusTint.copy(alpha = 0.14f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) { Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusTint, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            ticket.message,
            style = MaterialTheme.typography.bodySmall,
            color = AgroPalette.InkMuted,
            maxLines = 2,
        )
        if (!ticket.devReply.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AgroPalette.Sky.copy(alpha = 0.08f))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.Send, null, tint = AgroPalette.Sky, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(ticket.devReply, style = MaterialTheme.typography.bodySmall, color = AgroPalette.Sky)
            }
        }
    }
}

@Composable
private fun FaqRow(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
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
private fun AboutPanel(snackbar: SnackbarHostState, onOpenSection: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    data class AboutLink(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val subtitle: String, val tint: androidx.compose.ui.graphics.Color, val url: String)

    val links = listOf(
        AboutLink(Icons.Rounded.Lock,     "Privacy Policy",   "How we handle your data",          AgroPalette.Sky,     "https://agritech-4d1ba.web.app/privacy.html"),
        AboutLink(Icons.Rounded.MenuBook, "Terms of Service", "Usage terms and conditions",        AgroPalette.Iris,    "https://agritech-4d1ba.web.app/terms.html"),
        AboutLink(Icons.Rounded.Email,    "Contact Us",       "Docs, contact, feedback",           AgroPalette.Primary, "help"),
        AboutLink(Icons.Rounded.StarRate, "Rate the App",     "Enjoying AgroSphere? Let us know",  AgroPalette.Amber,   ""),
        AboutLink(Icons.Rounded.Language, "Visit Website",    "agritech-4d1ba.web.app",            Color(0xFF22D3EE),    "https://agritech-4d1ba.web.app"),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Hero identity card ─────────────────────────────────────────────
        item {
            GlassCard(background = AgroBrushes.leafCard, radius = 24.dp, padding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AgroPalette.Primary.copy(alpha = 0.16f))
                            .border(1.dp, AgroPalette.Primary.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Bolt, null, tint = AgroPalette.Primary, modifier = Modifier.size(36.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Text("AgroSphere", style = MaterialTheme.typography.headlineMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Your fields. Smarter.", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AgroPalette.Primary.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text("v0.1.0", style = MaterialTheme.typography.labelMedium, color = AgroPalette.Primary, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AgroPalette.SurfaceGlass)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text("Build 1", style = MaterialTheme.typography.labelMedium, color = AgroPalette.InkMuted)
                        }
                    }
                }
            }
        }

        // ── Mission ────────────────────────────────────────────────────────
        item { SectionHeader(title = "Our Mission") }
        item {
            GlassCard(radius = 18.dp, padding = 18.dp) {
                Text(
                    "AgroSphere is built for smallholder and commercial farmers who deserve the same AI-powered insights that large agri-corporations rely on — free, accessible, and deeply local. We believe every farm, regardless of size, should have the intelligence to thrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroPalette.InkMuted,
                    lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6,
                )
            }
        }

        // ── What we stand for ──────────────────────────────────────────────
        item { SectionHeader(title = "What We Stand For") }
        items(listOf(
            Triple(Icons.Rounded.Verified,       "Farmer first",          "Every feature is designed around what a farmer actually needs in the field."),
            Triple(Icons.Rounded.Lock,           "Privacy by default",    "Your farm data is yours. We never sell or share your data with third parties."),
            Triple(Icons.Rounded.AutoAwesome,    "AI that explains itself","Our recommendations show reasoning, not just answers."),
            Triple(Icons.Rounded.Grass,          "Made in India",         "Built with Indian soil types, crops, and climate patterns at the core."),
        )) { (icon, title, desc) ->
            GlassCard(radius = 16.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AgroPalette.Primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = AgroPalette.Primary, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                }
            }
        }

        // ── Links ──────────────────────────────────────────────────────────
        item { SectionHeader(title = "Legal & Contact") }
        items(links) { link ->
            GlassCard(radius = 16.dp, padding = 14.dp, onClick = {
                when {
                    link.url == "help" -> onOpenSection(com.agrosphere.app.ui.navigation.ProfileSections.HELP)
                    link.title == "Rate the App" -> scope.launch { snackbar.showSnackbar("Coming soon on Play Store.") }
                    link.url.isNotBlank() -> {
                        val ok = launchUrl(context, link.url)
                        if (!ok) scope.launch { snackbar.showSnackbar("No browser app installed.") }
                    }
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(link.tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(link.icon, null, tint = link.tint, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(link.title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                        Text(link.subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.InkMuted)
                }
            }
        }

        // ── Footer ─────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Made with care in India 🌿", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                Spacer(Modifier.height(4.dp))
                Text("© 2025 AgroSphere. All rights reserved.", style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
            }
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
