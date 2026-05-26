package com.agrosphere.app.feature.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agrosphere.app.R
import com.agrosphere.app.data.weather.WeatherRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.navigation.ProfileSections
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat

// ═════════════════════════════════════════════════════════════════════════════
// ProfileScreen — immersive hero band, live score + breakdown bars,
// animated stat tiles, activity summary, tinted section dividers,
// subscription banner, DataStore-persisted toggles.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSection: (String) -> Unit = {},
    onOpenRegional: () -> Unit = {},
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
    val state by vm.state.collectAsState()
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { vm.updatePhoto(it) }
    }
    val notifPermLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        vm.toggleNotifications(granted)
        if (!granted) scope.launch { snackbar.showSnackbar("Open Profile → Notifications to enable in Settings") }
    }

    val weather by WeatherRepository.bundleFlow.collectAsState()
    LaunchedEffect(Unit) {
        if (WeatherRepository.cached() == null) runCatching { WeatherRepository.load(context) }
    }
    val locationLabel = weather?.snapshot?.location.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileBackdrop()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Immersive hero band (back + avatar + identity + quick actions) ─
            item {
                ProfileHeroBand(
                    state = state,
                    location = locationLabel,
                    onBack = onBack,
                    onOpenSection = onOpenSection,
                    onComingSoon = { scope.launch { snackbar.showSnackbar("Coming soon") } },
                    onCameraClick = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                )
            }

            // ── Farm Intelligence Score with breakdown bars ────────────────────
            item { ScoreCard(state = state) }

            // ── Stats 2×2 with animated counters ─────────────────────────────
            item { StatsGrid(onOpen = { onOpenSection(ProfileSections.FARM_OVERVIEW) }) }

            // ── Activity summary ──────────────────────────────────────────────
            item { ActivitySummaryCard(fieldCount = com.agrosphere.app.data.repo.FieldRepository.fields.collectAsState().value.size) }

            // ── Farm section ──────────────────────────────────────────────────
            item { SectionDivider(stringResource(R.string.profile_section_farm), AgroPalette.Primary) }
            item {
                MenuItem(Icons.Rounded.Grass, stringResource(R.string.profile_menu_farm_overview), stringResource(R.string.profile_sub_farm_overview), AgroPalette.Primary,
                    onClick = { onOpenSection(ProfileSections.FARM_OVERVIEW) })
            }
            item {
                MenuItem(Icons.Rounded.Group, stringResource(R.string.profile_menu_my_farms), stringResource(R.string.profile_sub_my_farms), AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.MY_FARMS) })
            }

            // ── Account section ───────────────────────────────────────────────
            item { SectionDivider(stringResource(R.string.profile_section_account), AgroPalette.Sky) }
            item {
                MenuItem(Icons.Rounded.Person, stringResource(R.string.profile_menu_account_settings), stringResource(R.string.profile_sub_account), AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.ACCOUNT) })
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.profile_menu_notifications),
                    subtitle = if (state.notificationsOn) stringResource(R.string.profile_sub_notif_on) else stringResource(R.string.profile_sub_notif_off),
                    tint = AgroPalette.Primary,
                    checked = state.notificationsOn,
                    onCheckedChange = { value ->
                        if (!value) {
                            vm.toggleNotifications(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) vm.toggleNotifications(true)
                            else notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.toggleNotifications(true)
                        }
                    },
                    onRowClick = { onOpenSection(ProfileSections.NOTIFICATIONS) },
                )
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.Straighten,
                    title = stringResource(R.string.profile_toggle_units),
                    subtitle = if (state.useMetric) stringResource(R.string.profile_sub_units_metric) else stringResource(R.string.profile_sub_units_imperial),
                    tint = AgroPalette.Sky,
                    checked = state.useMetric,
                    onCheckedChange = vm::toggleUnits,
                )
            }
            item {
                ToggleItem(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.profile_toggle_dark),
                    subtitle = if (state.darkTheme) stringResource(R.string.profile_sub_dark_on) else stringResource(R.string.profile_sub_dark_off),
                    tint = AgroPalette.Iris,
                    checked = state.darkTheme,
                    onCheckedChange = vm::toggleTheme,
                )
            }
            item {
                val userMode by com.agrosphere.app.data.repo.AppPreferences.userMode.collectAsState()
                MenuItem(
                    icon = Icons.Rounded.Eco,
                    title = stringResource(R.string.profile_menu_mode),
                    subtitle = when (userMode) {
                        "farmer" -> stringResource(R.string.profile_sub_mode_farmer)
                        "plant"  -> stringResource(R.string.profile_sub_mode_plant)
                        else     -> stringResource(R.string.profile_sub_mode_both)
                    },
                    tint = AgroPalette.Primary,
                    onClick = { onOpenSection(ProfileSections.MODE) },
                )
            }
            item {
                MenuItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.profile_menu_language),
                    subtitle = com.agrosphere.app.data.i18n.LocaleManager.currentDisplayLabel(),
                    tint = AgroPalette.Amber,
                    onClick = { onOpenSection(ProfileSections.LANGUAGE) },
                )
            }

            // ── Intelligence section ───────────────────────────────────────────
            item { SectionDivider(stringResource(R.string.profile_section_intelligence), AgroPalette.Iris) }
            item {
                MenuItem(Icons.Rounded.Psychology, stringResource(R.string.profile_menu_ai_prefs), stringResource(R.string.profile_sub_ai_prefs), AgroPalette.Iris,
                    onClick = { onOpenSection(ProfileSections.AI_PREFS) })
            }
            item {
                MenuItem(Icons.Rounded.Shield, stringResource(R.string.profile_menu_ai_reliability), stringResource(R.string.profile_sub_ai_reliability), Color(0xFF67D4F0),
                    onClick = { onOpenSection(ProfileSections.AI_RELIABILITY) })
            }
            item {
                MenuItem(Icons.Rounded.History, stringResource(R.string.profile_menu_learning), stringResource(R.string.profile_sub_learning), AgroPalette.Iris,
                    onClick = { onOpenSection(ProfileSections.LEARNING) })
            }
            item {
                MenuItem(Icons.Rounded.Public, stringResource(R.string.profile_menu_regional), stringResource(R.string.profile_sub_regional), Color(0xFF22D3EE),
                    onClick = onOpenRegional)
            }

            // ── Support section ───────────────────────────────────────────────
            item { SectionDivider(stringResource(R.string.profile_section_support), AgroPalette.InkMuted) }
            item {
                MenuItem(Icons.Rounded.SupportAgent, stringResource(R.string.profile_menu_help), stringResource(R.string.profile_sub_help), AgroPalette.Sky,
                    onClick = { onOpenSection(ProfileSections.HELP) })
            }
            item {
                MenuItem(Icons.Rounded.Info, stringResource(R.string.profile_menu_about), stringResource(R.string.profile_sub_about), AgroPalette.InkMuted,
                    onClick = { onOpenSection(ProfileSections.ABOUT) })
            }

            // ── Sign out + footer ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    text = stringResource(R.string.profile_sign_out),
                    icon = Icons.Rounded.Logout,
                    brush = SolidColor(AgroPalette.Rose),
                    onClick = { showSignOutConfirm = true },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.profile_made_in_india),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        androidx.compose.material3.SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
        )

        if (showSignOutConfirm) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                containerColor = AgroPalette.Surface,
                title = { Text(stringResource(R.string.sign_out_confirm_title), color = AgroPalette.Ink) },
                text  = { Text(stringResource(R.string.sign_out_confirm_body), color = AgroPalette.InkMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutConfirm = false
                        vm.signOut()
                        onSignOut()
                    }) { Text(stringResource(R.string.profile_sign_out), color = AgroPalette.Rose) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) {
                        Text(stringResource(R.string.sign_out_confirm_cancel), color = AgroPalette.InkMuted)
                    }
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProfileHeroBand — full-bleed cinematic hero: back button, animated avatar
// ring, identity info, quick action row — all in one premium GlassCard.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeroBand(
    state: ProfileUiState,
    location: String,
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit,
    onComingSoon: () -> Unit = {},
    onCameraClick: () -> Unit = {},
) {
    val inf = rememberInfiniteTransition(label = "hero-profile")
    val orbAngle by inf.animateFloat(
        0f, (PI * 2).toFloat(),
        infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "orb",
    )
    val glowPulse by inf.animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "glow",
    )
    val ringAngle by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "ring",
    )

    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AgroBrushes.leafCard, shape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, shape),
    ) {
        // Canvas premium layer — neon hairline + orbiting particles + ambient glow
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height

            // Neon top hairline
            val mid = w / 2f; val halfLen = w * 0.46f
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, AgroPalette.Primary.copy(alpha = 0.35f), AgroPalette.Primary.copy(alpha = 0.70f * glowPulse), AgroPalette.Primary.copy(alpha = 0.35f), Color.Transparent),
                    startX = mid - halfLen, endX = mid + halfLen,
                ),
                start = Offset(mid - halfLen, 1f), end = Offset(mid + halfLen, 1f),
                strokeWidth = 1.5f,
            )

            // Orbiting particle cluster (top-right area)
            val orbCx = w * 0.82f; val orbCy = h * 0.22f; val orbR = w * 0.12f
            repeat(7) { i ->
                val trailAng = orbAngle - i * 0.30f
                val tr = (0.70f - i * 0.10f).coerceAtLeast(0f) * glowPulse
                drawCircle(
                    color  = AgroPalette.Primary.copy(alpha = tr),
                    radius = (5.5f - i * 0.7f).coerceAtLeast(1f),
                    center = Offset(orbCx + cos(trailAng) * orbR, orbCy + sin(trailAng) * orbR),
                )
            }
            val px = orbCx + cos(orbAngle) * orbR; val py = orbCy + sin(orbAngle) * orbR
            drawCircle(
                brush = Brush.radialGradient(0f to AgroPalette.Primary.copy(alpha = 0.95f), 1f to Color.Transparent, center = Offset(px, py), radius = 8f),
                radius = 8f, center = Offset(px, py),
            )

            // Ambient glow behind avatar (left side)
            drawCircle(
                brush = Brush.radialGradient(0f to AgroPalette.Primary.copy(alpha = 0.16f * glowPulse), 1f to Color.Transparent, center = Offset(w * 0.22f, h * 0.48f), radius = w * 0.45f),
                radius = w * 0.45f, center = Offset(w * 0.22f, h * 0.48f),
            )
        }

        Column(modifier = Modifier.padding(18.dp)) {
            // Row 1: back button + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = AgroPalette.Ink, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.weight(1f))
                if (location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = AgroPalette.Sky, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(location, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Row 2: avatar + identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedAvatarRing(
                    photoUrl = state.photoUrl,
                    displayName = state.displayName,
                    isAnonymous = state.isAnonymous,
                    ringAngle = ringAngle,
                    glowPulse = glowPulse,
                    onCameraClick = onCameraClick,
                )
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.displayName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                            color = AgroPalette.Ink,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        if (state.isVerified) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.VerifiedUser, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    RoleBadge(isAnonymous = state.isAnonymous)
                    if (state.email.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Email, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(state.email, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Row 3: quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroQuickAction(stringResource(R.string.common_edit), Icons.Rounded.Edit, AgroPalette.Primary, Modifier.weight(1f)) { onOpenSection(ProfileSections.EDIT_PROFILE) }
                HeroQuickAction(stringResource(R.string.profile_quick_share), Icons.Rounded.Share, AgroPalette.Sky, Modifier.weight(1f), onClick = onComingSoon)
                HeroQuickAction(stringResource(R.string.profile_quick_invite), Icons.Rounded.Group, AgroPalette.Iris, Modifier.weight(1f), onClick = onComingSoon)
            }
        }
    }
}

@Composable
private fun AnimatedAvatarRing(
    photoUrl: String?,
    displayName: String,
    isAnonymous: Boolean,
    ringAngle: Float,
    glowPulse: Float,
    onCameraClick: () -> Unit = {},
) {
    Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        // Animated rotating gradient ring
        Canvas(modifier = Modifier.size(84.dp)) {
            rotate(ringAngle) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Amber, AgroPalette.Primary)
                    ),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(3f, 3f),
                    size = androidx.compose.ui.geometry.Size(size.width - 6f, size.height - 6f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
            }
            // Outer glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.7f to AgroPalette.Primary.copy(alpha = 0.18f * glowPulse),
                    1f to AgroPalette.Primary.copy(alpha = 0.35f * glowPulse),
                ),
                radius = size.minDimension / 2f,
            )
        }
        // Avatar content
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(AgroPalette.SurfaceElev),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(76.dp).clip(CircleShape))
            } else {
                Text(
                    text = initialsOf(displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AgroPalette.Primary,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        // Camera badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(AgroPalette.Primary)
                .border(2.dp, AgroPalette.SurfaceElev, CircleShape)
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.CameraAlt, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(14.dp)) }
        // Guest dot
        if (isAnonymous) {
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

@Composable
private fun HeroQuickAction(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
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
    val (label, tint) = if (isAnonymous) stringResource(R.string.profile_role_guest) to AgroPalette.Amber
        else stringResource(R.string.profile_role_owner) to AgroPalette.Primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontSize = 9.sp), color = tint, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Farm Intelligence Score — live score + breakdown bars
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScoreCard(state: ProfileUiState) {
    val animated by animateFloatAsState(
        targetValue = state.farmIntelScore / 100f,
        animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
        label = "score",
    )
    val cropAnim by animateFloatAsState(state.cropHealthFraction, tween(1200, easing = LinearOutSlowInEasing), label = "ch")
    val diversityAnim by animateFloatAsState(state.fieldDiversityFraction, tween(1400, easing = LinearOutSlowInEasing), label = "div")

    GlassCard(background = AgroBrushes.leafCard, radius = 24.dp, padding = 20.dp) {
        Row(verticalAlignment = Alignment.Top) {
            ScoreGauge(progress = animated, score = state.farmIntelScore, modifier = Modifier.size(100.dp))
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.profile_score_eyebrow),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = AgroPalette.Primary,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.profile_score_title), style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
                if (state.scoreLabel.isNotBlank()) {
                    Text(state.scoreLabel, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                }
                Spacer(Modifier.height(6.dp))
                StarsRow(rating = state.farmIntelScore / 20f)
                Spacer(Modifier.height(10.dp))

                // Breakdown micro-bars
                BreakdownBar("Crop health", cropAnim,          "${(cropAnim * 100).toInt()}%",      AgroPalette.Primary)
                Spacer(Modifier.height(6.dp))
                BreakdownBar("Field diversity", diversityAnim,  "${(diversityAnim * 100).toInt()}%", AgroPalette.Sky)
                Spacer(Modifier.height(6.dp))
                BreakdownBar("Data freshness", 0.92f,          "92%",                               AgroPalette.Amber)
            }
        }
    }
}

@Composable
private fun BreakdownBar(label: String, fraction: Float, valueText: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted, modifier = Modifier.width(90.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AgroPalette.SurfaceGlass),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.7f), tint))),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(valueText, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
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
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Primary)),
                    startAngle = 135f, sweepAngle = 270f * progress, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(arc, arc),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (score == 0) "—" else "$score",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                color = AgroPalette.Ink, fontWeight = FontWeight.Black,
            )
            Text(stringResource(R.string.profile_score_of_100), style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
        }
    }
}

@Composable
private fun StarsRow(rating: Float) {
    Row {
        repeat(5) { i ->
            Icon(
                Icons.Rounded.Star, null,
                tint = if ((i + 1) <= rating.toInt()) AgroPalette.Amber else AgroPalette.SurfaceGlassBorder,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats grid — 2×2 with animated counters
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsGrid(onOpen: () -> Unit) {
    val fields by com.agrosphere.app.data.repo.FieldRepository.fields.collectAsState()
    val stats = listOf(
        Triple(stringResource(R.string.profile_stats_farms),      fields.size,                                                         AgroPalette.Primary),
        Triple(stringResource(R.string.profile_stats_area),        if (fields.isEmpty()) 0 else fields.sumOf { it.areaHa }.toInt(),    AgroPalette.Sky),
        Triple(stringResource(R.string.profile_stats_crops),       if (fields.isEmpty()) 0 else fields.map { it.crop }.distinct().size, AgroPalette.Amber),
        Triple(stringResource(R.string.profile_stats_avg_health),  if (fields.isEmpty()) 0 else fields.map { it.healthScore }.average().toInt(), AgroPalette.Iris),
    )
    val suffixes = listOf("", " ha", "", "")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { colIdx, (label, value, tint) ->
                    val globalIdx = rowIdx * 2 + colIdx
                    AnimatedStatTile(
                        label = label,
                        targetValue = value,
                        suffix = suffixes[globalIdx],
                        tint = tint,
                        modifier = Modifier.weight(1f),
                        onClick = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStatTile(label: String, targetValue: Int, suffix: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    var start by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) { start = targetValue }
    val animated by animateIntAsState(start, tween(900, easing = LinearOutSlowInEasing), label = "counter")

    GlassCard(modifier = modifier, radius = 18.dp, padding = 14.dp, onClick = onClick) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                if (targetValue == 0) "—" else "$animated$suffix",
                style = MaterialTheme.typography.headlineSmall,
                color = tint,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity summary card — usage hooks (mock values, wired to field count)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActivitySummaryCard(fieldCount: Int) {
    GlassCard(radius = 20.dp, padding = 16.dp) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("View all", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityPill(Icons.Rounded.CameraAlt,      "${(fieldCount * 3).coerceAtLeast(1)}", "Scans",       AgroPalette.Amber)
                ActivityDivider()
                ActivityPill(Icons.Rounded.AutoAwesome,    "12",                                   "AI Chats",    AgroPalette.Iris)
                ActivityDivider()
                ActivityPill(Icons.Rounded.CalendarMonth,  "7",                                    "Days Active", AgroPalette.Primary)
            }
        }
    }
}

@Composable
private fun ActivityPill(icon: ImageVector, value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkMuted)
    }
}

@Composable
private fun ActivityDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(AgroPalette.SurfaceGlassBorder)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section divider with tinted gradient accent line
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionDivider(text: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = AgroPalette.InkDim,
            modifier = Modifier.padding(start = 4.dp, end = 10.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(tint.copy(alpha = 0.40f), Color.Transparent))
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subscription upgrade banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SubscriptionBanner(onClick: () -> Unit) {
    GlassCard(radius = 20.dp, padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Amber.copy(alpha = 0.18f))
                    .border(1.dp, AgroPalette.Amber.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = AgroPalette.Amber, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Upgrade to Pro", style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.SemiBold)
                Text("Unlock AI insights & priority support", style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AgroPalette.Amber.copy(alpha = 0.18f))
                    .border(1.dp, AgroPalette.Amber.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("Get Pro →", style = MaterialTheme.typography.labelSmall, color = AgroPalette.Amber, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Menu item components
// ─────────────────────────────────────────────────────────────────────────────
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
            Text("PRO", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.sp), color = AgroPalette.Amber, fontWeight = FontWeight.Black)
        }
    }
}
