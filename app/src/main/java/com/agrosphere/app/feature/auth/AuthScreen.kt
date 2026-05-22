package com.agrosphere.app.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.R
import com.agrosphere.app.ui.components.GoogleLogo
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Unwrap a Compose [Context] down to the hosting [Activity] (Credential Manager needs it). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    var tab by remember { mutableStateOf(0) } // 0 = Sign in, 1 = Sign up
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val state by vm.state.collectAsState()
    val isLoading = state is AuthUiState.Loading
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val snackbar = remember { SnackbarHostState() }

    // Surface auth errors via snackbar; route on success.
    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.Success -> onAuthenticated()
            is AuthUiState.Error -> {
                snackbar.showSnackbar(message = s.message, duration = SnackbarDuration.Long)
                vm.dismissError()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AuthBackgroundBrush())) {
        // Animated background particle field
        ParticleField()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            LogoBlock()

            Spacer(Modifier.height(12.dp))
            Text(
                buildBrand(),
                style = MaterialTheme.typography.displayLarge,
                color = AgroPalette.Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "YOUR FIELDS · SMARTER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                color = AgroPalette.InkMuted,
            )

            Spacer(Modifier.height(28.dp))

            GlassAuthCard {
                TabStrip(
                    selected = tab,
                    onSelect = { tab = it },
                )
                Spacer(Modifier.height(22.dp))

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally { -it * dir } + fadeOut(tween(220)))
                    },
                    label = "auth-form",
                ) { current ->
                    if (current == 0) {
                        SignInForm(
                            email = email,
                            password = password,
                            showPassword = showPassword,
                            isLoading = isLoading,
                            onEmail = { email = it },
                            onPassword = { password = it },
                            onTogglePassword = { showPassword = !showPassword },
                            onSubmit = { vm.signIn(email, password) },
                        )
                    } else {
                        SignUpForm(
                            name = name,
                            email = email,
                            password = password,
                            showPassword = showPassword,
                            isLoading = isLoading,
                            onName = { name = it },
                            onEmail = { email = it },
                            onPassword = { password = it },
                            onTogglePassword = { showPassword = !showPassword },
                            onSubmit = { vm.signUp(name, email, password) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                DividerWithText("or continue with")
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SocialButton(
                        glyph = null, label = "Google",
                        accent = AgroPalette.Ink.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        googleLogo = true,
                    ) { vm.signInGoogle(context.findActivity() ?: context, webClientId) }
                    SocialButton(
                        glyph = null, label = "Phone",
                        accent = AgroPalette.Iris,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Phone,
                    ) {
                        // Phone OTP not yet wired — fall back to guest for now.
                        vm.signInAsGuest()
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            GuestEntry(onClick = { vm.signInAsGuest() })
            Spacer(Modifier.height(16.dp))
        }

        // Snackbar host overlays the bottom of the screen.
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Bottom)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
        )

        // Centered loader overlay while a request is in flight.
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AgroPalette.Primary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Brand title with accent
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun buildBrand(): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        append("Agro")
        pushStyle(androidx.compose.ui.text.SpanStyle(color = AgroPalette.Primary, fontWeight = FontWeight.Black))
        append("Sphere")
        pop()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Logo block — pulsing emerald disc + two counter-rotating dashed orbits
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LogoBlock() {
    val transition = rememberInfiniteTransition(label = "logo")
    val rot1 by transition.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "rot1",
    )
    val rot2 by transition.animateFloat(
        0f, -360f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "rot2",
    )
    val pulse by transition.animateFloat(
        0.35f, 0.9f,
        animationSpec = infiniteRepeatable(tween(2400), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer soft glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.25f * pulse),
                        0.6f to AgroPalette.Primary.copy(alpha = 0.05f * pulse),
                        1f to Color.Transparent,
                    ),
                    shape = CircleShape,
                )
        )
        // Outer dashed orbit (slow, reversed)
        Canvas(modifier = Modifier.size(104.dp).rotate(rot2)) {
            drawCircle(
                color = AgroPalette.Primary.copy(alpha = 0.20f),
                radius = size.minDimension / 2,
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)),
            )
        }
        // Inner dashed orbit (faster, forward)
        Canvas(modifier = Modifier.size(92.dp).rotate(rot1)) {
            drawCircle(
                color = AgroPalette.Primary.copy(alpha = 0.45f),
                radius = size.minDimension / 2,
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)),
            )
        }
        // Inner disc
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(AgroPalette.Primary.copy(alpha = 0.10f), CircleShape)
                .border(1.dp, AgroPalette.Primary.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Spa,
                contentDescription = null,
                tint = AgroPalette.Primary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Particle field — drifting emerald dots
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ParticleField(count: Int = 14) {
    val transition = rememberInfiniteTransition(label = "particles")
    // We animate a single 0..1 progress and use per-particle offsets via index.
    val t by transition.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "t",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        repeat(count) { i ->
            val seed = (i * 137 + 7) % 1000 / 1000f
            val localT = ((t + seed) % 1f)
            val x = (w * ((i * 53) % 100) / 100f) + sin((localT + i) * PI.toFloat() * 2f) * 24f
            val y = h - (h + 80f) * localT
            val radius = 1.2f + (i % 3) * 0.7f
            val alpha = when {
                localT < 0.15f -> localT / 0.15f
                localT > 0.85f -> (1f - localT) / 0.15f
                else -> 1f
            } * 0.6f
            drawCircle(
                color = AgroPalette.Primary.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glass card shell with top emerald hairline + bottom-right glow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlassAuthCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xCC0A1118), shape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, shape)
            .drawBehind {
                // top emerald hairline
                val mid = size.width / 2f
                val len = size.width * 0.55f
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, AgroPalette.Primary.copy(alpha = 0.55f), Color.Transparent),
                        startX = mid - len / 2, endX = mid + len / 2,
                    ),
                    start = Offset(mid - len / 2, 0f),
                    end = Offset(mid + len / 2, 0f),
                    strokeWidth = 1.4f,
                )
                // bottom-right radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                        center = Offset(size.width + 40f, size.height + 40f),
                        radius = 220f,
                    ),
                    radius = 220f,
                    center = Offset(size.width + 40f, size.height + 40f),
                )
            }
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Column { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab strip with sliding neon indicator
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TabStrip(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("SIGN IN", "SIGN UP")
    val indicator by animateFloatAsState(targetValue = selected.toFloat(), label = "indicator")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                        color = if (selected == i) AgroPalette.Ink else AgroPalette.InkDim,
                        fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
        // Underline track + indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AgroPalette.SurfaceGlassBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(2.dp)
                    .slideIndicator(indicator)
                    .background(
                        Brush.horizontalGradient(listOf(AgroPalette.Primary, AgroPalette.Sky))
                    )
            )
        }
    }
}

/** Slides a half-width indicator across its parent. fraction: 0f (left) .. 1f (right). */
private fun Modifier.slideIndicator(fraction: Float): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = ((constraints.maxWidth - placeable.width) * fraction.coerceIn(0f, 1f)).toInt()
    layout(placeable.width, placeable.height) {
        placeable.place(shift, 0)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Forms
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignInForm(
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, color = AgroPalette.Ink)
        Text("Sign in to your fields", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(18.dp))

        GlowField(
            value = email,
            onValueChange = onEmail,
            label = "Email",
            leading = Icons.Rounded.AlternateEmail,
            keyboard = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        GlowField(
            value = password,
            onValueChange = onPassword,
            label = "Password",
            leading = Icons.Rounded.Lock,
            trailing = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingClick = onTogglePassword,
            keyboard = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Forgot password?",
            style = MaterialTheme.typography.labelMedium,
            color = AgroPalette.Primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { /* TODO */ }
                .padding(6.dp),
        )
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = if (isLoading) "Signing in…" else "Continue",
            icon = Icons.Rounded.ArrowForward,
            enabled = !isLoading && email.isNotBlank() && password.length >= 4,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun SignUpForm(
    name: String,
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column {
        Text("Create your farm", style = MaterialTheme.typography.headlineMedium, color = AgroPalette.Ink)
        Text("Start your digital twin in 30 seconds", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(18.dp))

        GlowField(
            value = name,
            onValueChange = onName,
            label = "Your name",
            leading = Icons.Rounded.Person,
        )
        Spacer(Modifier.height(12.dp))
        GlowField(
            value = email,
            onValueChange = onEmail,
            label = "Email",
            leading = Icons.Rounded.AlternateEmail,
            keyboard = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        GlowField(
            value = password,
            onValueChange = onPassword,
            label = "Password",
            leading = Icons.Rounded.Lock,
            trailing = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingClick = onTogglePassword,
            keyboard = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = if (isLoading) "Creating…" else "Create account",
            icon = Icons.Rounded.ArrowForward,
            enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && password.length >= 6,
            onClick = onSubmit,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glow-on-focus text field
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leading, contentDescription = null, tint = AgroPalette.Primary) },
        trailingIcon = if (trailing != null) {
            {
                IconButton(onClick = { onTrailingClick?.invoke() }) {
                    Icon(trailing, contentDescription = null, tint = AgroPalette.InkMuted)
                }
            }
        } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroPalette.Primary,
            unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
            focusedTextColor = AgroPalette.Ink,
            unfocusedTextColor = AgroPalette.Ink,
            cursorColor = AgroPalette.Primary,
            focusedLabelColor = AgroPalette.Primary,
            unfocusedLabelColor = AgroPalette.InkMuted,
            focusedContainerColor = Color(0x33000000),
            unfocusedContainerColor = Color(0x22000000),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Divider with center text
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DividerWithText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AgroPalette.SurfaceGlassBorder)
        )
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = AgroPalette.InkDim,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AgroPalette.SurfaceGlassBorder)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Social button — outlined emerald-tinted
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SocialButton(
    glyph: String?,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    googleLogo: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22000000), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        when {
            googleLogo -> Image(
                imageVector = GoogleLogo,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            icon != null -> Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            glyph != null -> Text(glyph, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = AgroPalette.Ink)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Guest entry — subtle text CTA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GuestEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Just exploring? ",
            style = MaterialTheme.typography.bodyMedium,
            color = AgroPalette.InkMuted,
        )
        Text(
            "Continue as guest →",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AgroPalette.Primary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background — vertical gradient with subtle radial accents
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AuthBackgroundBrush(): Brush = Brush.verticalGradient(
    0f to Color(0xFF020617),
    0.35f to AgroPalette.BgFarm,
    1f to AgroPalette.BgDeep,
)
