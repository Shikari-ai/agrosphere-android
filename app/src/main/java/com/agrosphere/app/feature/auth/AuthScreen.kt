package com.agrosphere.app.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// AgroSphere — Auth screen
// "Living field at dawn": aurora mesh background, silhouette hills,
// pulsing hex emblem, rotating tagline, glass card wrapped in a slowly
// rotating conic gradient stroke, sliding-pill tabs, refined forms.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var tab by remember { mutableStateOf(0) } // 0 = sign-in, 1 = sign-up
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DawnGradient())) {
        AuroraBackground()
        HillSilhouette()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            HexEmblem()
            Spacer(Modifier.height(18.dp))
            Text(buildBrand(), style = MaterialTheme.typography.displayLarge, color = AgroPalette.Ink)
            Spacer(Modifier.height(8.dp))
            RotatingTagline()

            Spacer(Modifier.height(28.dp))

            ConicBorderCard {
                PillTabs(selected = tab, onSelect = { tab = it })
                Spacer(Modifier.height(22.dp))

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(animationSpec = tween(320)) { it * dir } + fadeIn(tween(260)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { -it * dir } + fadeOut(tween(200)))
                    },
                    label = "form-swap",
                ) { current ->
                    if (current == 0) {
                        SignInForm(
                            email = email,
                            password = password,
                            showPassword = showPassword,
                            onEmail = { email = it },
                            onPassword = { password = it },
                            onTogglePassword = { showPassword = !showPassword },
                            onSubmit = onAuthenticated,
                        )
                    } else {
                        SignUpForm(
                            name = name,
                            email = email,
                            password = password,
                            showPassword = showPassword,
                            onName = { name = it },
                            onEmail = { email = it },
                            onPassword = { password = it },
                            onTogglePassword = { showPassword = !showPassword },
                            onSubmit = onAuthenticated,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                DividerWithText("or continue with")
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialChip("G", "Google", AgroPalette.Sky, Modifier.weight(1f)) { onAuthenticated() }
                    SocialChip(null, "Phone", AgroPalette.Iris, Modifier.weight(1f), icon = Icons.Rounded.Phone) { onAuthenticated() }
                }
            }

            Spacer(Modifier.height(22.dp))
            GuestEntry(onClick = onAuthenticated)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Background
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun DawnGradient(): Brush = Brush.verticalGradient(
    0f to Color(0xFF020617),
    0.45f to Color(0xFF06140E),
    1f to AgroPalette.BgDeep,
)

/** Three slowly drifting radial orbs in emerald/sky/iris. Plus sparse stars. */
@Composable
private fun AuroraBackground() {
    val tr = rememberInfiniteTransition(label = "aurora")
    val t by tr.animateFloat(
        0f, (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "t",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Orb A — emerald
        val ax = w * 0.5f + sin(t) * w * 0.25f
        val ay = h * 0.20f + cos(t * 0.7f) * h * 0.08f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Primary.copy(alpha = 0.45f),
                0.55f to AgroPalette.Primary.copy(alpha = 0.10f),
                1f to Color.Transparent,
                center = Offset(ax, ay),
                radius = w * 0.85f,
            ),
            radius = w * 0.85f,
            center = Offset(ax, ay),
        )
        // Orb B — sky
        val bx = w * 0.15f + cos(t * 0.8f + 1f) * w * 0.30f
        val by = h * 0.55f + sin(t * 0.5f + 2f) * h * 0.10f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Sky.copy(alpha = 0.28f),
                0.55f to AgroPalette.Sky.copy(alpha = 0.06f),
                1f to Color.Transparent,
                center = Offset(bx, by),
                radius = w * 0.7f,
            ),
            radius = w * 0.7f,
            center = Offset(bx, by),
        )
        // Orb C — iris
        val cx = w * 0.8f + sin(t * 0.6f + 3f) * w * 0.20f
        val cy = h * 0.40f + cos(t * 0.9f) * h * 0.10f
        drawCircle(
            brush = Brush.radialGradient(
                0f to AgroPalette.Iris.copy(alpha = 0.22f),
                0.6f to AgroPalette.Iris.copy(alpha = 0.04f),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
            center = Offset(cx, cy),
        )

        // Star dust — 30 tiny points at fixed positions, twinkling via t
        repeat(30) { i ->
            val seed = (i * 197 + 13) % 1000 / 1000f
            val sx = w * ((i * 47 + 3) % 100) / 100f
            val sy = h * ((i * 31 + 11) % 95) / 100f
            val twinkle = 0.35f + 0.65f * (sin(t * 1.4f + seed * 6.28f) * 0.5f + 0.5f)
            drawCircle(
                color = AgroPalette.Ink.copy(alpha = 0.10f + 0.30f * twinkle),
                radius = 0.8f + (i % 3) * 0.5f,
                center = Offset(sx, sy),
            )
        }
    }
}

/** Two layers of sin-wave hill silhouettes at the bottom of the screen. */
@Composable
private fun HillSilhouette() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        fun makeHills(baselineFrac: Float, amplitude: Float, freq: Float, phase: Float, color: Color) {
            val path = Path()
            val baseline = h * baselineFrac
            path.moveTo(0f, baseline)
            var x = 0f
            while (x <= w) {
                val y = baseline + sin(x * freq + phase) * amplitude
                path.lineTo(x, y)
                x += 6f
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            drawPath(path, color = color)
        }

        // Far hills — lighter, higher
        makeHills(0.74f, 18f, 0.011f, 0.4f, Color(0xFF0C2417).copy(alpha = 0.85f))
        // Near hills — darker, lower
        makeHills(0.84f, 26f, 0.014f, 1.6f, Color(0xFF06140C))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Hex emblem with pulse + rotating energy ring
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun HexEmblem() {
    val tr = rememberInfiniteTransition(label = "hex")
    val rot by tr.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "rot",
    )
    val pulse by tr.animateFloat(
        0.45f, 1f,
        animationSpec = infiniteRepeatable(tween(2_200), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )

    Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(
                    Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.30f * pulse),
                        0.6f to AgroPalette.Primary.copy(alpha = 0.06f * pulse),
                        1f to Color.Transparent,
                    ),
                    shape = CircleShape,
                )
        )
        // Rotating sweep ring
        Canvas(modifier = Modifier.size(112.dp)) {
            withTransform({ rotate(rot) }) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            AgroPalette.Primary.copy(alpha = 0f),
                            AgroPalette.Primary.copy(alpha = 0.9f),
                            AgroPalette.Sky.copy(alpha = 0.6f),
                            AgroPalette.Primary.copy(alpha = 0f),
                        ),
                        center = center,
                    ),
                    radius = size.minDimension / 2 - 1f,
                    style = Stroke(width = 2f),
                )
            }
        }
        // Hex with leaf icon
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(HexShape)
                .background(Color(0xFF0A1A12))
                .border(1.dp, AgroPalette.Primary.copy(alpha = 0.55f), HexShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Spa,
                contentDescription = null,
                tint = AgroPalette.Primary,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

/** Regular hexagon shape (point-up). */
private val HexShape = GenericShape { s: Size, _ ->
    val cx = s.width / 2f
    val cy = s.height / 2f
    val r = s.minDimension / 2f
    val angleOffset = -PI.toFloat() / 2f
    for (i in 0..5) {
        val a = angleOffset + i * (PI.toFloat() / 3f)
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ═════════════════════════════════════════════════════════════════════════════
// Brand title + rotating tagline
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun buildBrand(): AnnotatedString = buildAnnotatedString {
    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold))
    append("Agro")
    pop()
    pushStyle(SpanStyle(color = AgroPalette.Primary, fontWeight = FontWeight.Black))
    append("Sphere")
    pop()
}

@Composable
private fun RotatingTagline() {
    val phrases = remember {
        listOf(
            "Grow smarter.",
            "See every leaf.",
            "Sense the weather.",
            "Talk to your farm.",
        )
    }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            index = (index + 1) % phrases.size
        }
    }
    Crossfade(targetState = index, animationSpec = tween(600), label = "tagline") { i ->
        Text(
            phrases[i],
            style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 2.4.sp),
            color = AgroPalette.InkMuted,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Glass card wrapped in a slowly rotating conic gradient stroke
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ConicBorderCard(content: @Composable () -> Unit) {
    val tr = rememberInfiniteTransition(label = "border")
    val angle by tr.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "angle",
    )
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xE60A1118), shape)
            .drawBehind {
                val cr = CornerRadius(28.dp.toPx())
                val stroke = 1.6.dp.toPx()
                withTransform({ rotate(angle) }) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                AgroPalette.Primary.copy(alpha = 0.0f),
                                AgroPalette.Primary.copy(alpha = 0.85f),
                                AgroPalette.Sky.copy(alpha = 0.55f),
                                AgroPalette.Iris.copy(alpha = 0.40f),
                                AgroPalette.Primary.copy(alpha = 0.0f),
                            ),
                            center = center,
                        ),
                        cornerRadius = cr,
                        style = Stroke(width = stroke),
                    )
                }
                // Subtle bottom-right glow inside the card
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.16f),
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

// ═════════════════════════════════════════════════════════════════════════════
// Pill segmented tabs
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PillTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Sign in", "Sign up")
    val pos by animateFloatAsState(targetValue = selected.toFloat(), label = "pill")
    val trackShape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(trackShape)
            .background(Color(0x55000000), trackShape)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, trackShape)
            .padding(4.dp),
    ) {
        // Sliding pill
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(38.dp)
                .slidePill(pos)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(AgroPalette.Primary.copy(alpha = 0.85f), AgroPalette.Sky.copy(alpha = 0.75f))
                    )
                )
        )
        Row(modifier = Modifier.fillMaxWidth().height(38.dp)) {
            tabs.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected == i) AgroPalette.BgDeep else AgroPalette.InkMuted,
                        fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Slides a half-width child across its parent. fraction: 0..1. */
private fun Modifier.slidePill(fraction: Float): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = ((constraints.maxWidth - placeable.width) * fraction.coerceIn(0f, 1f)).toInt()
    layout(placeable.width, placeable.height) { placeable.place(shift, 0) }
}

// ═════════════════════════════════════════════════════════════════════════════
// Forms
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun SignInForm(
    email: String,
    password: String,
    showPassword: Boolean,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column {
        Text("Welcome back.", style = MaterialTheme.typography.headlineLarge, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
        Text("Step into your fields.", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(20.dp))

        GlowField(
            value = email, onValueChange = onEmail,
            label = "Email", leading = Icons.Rounded.AlternateEmail,
            keyboard = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        GlowField(
            value = password, onValueChange = onPassword,
            label = "Password", leading = Icons.Rounded.Lock,
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
            text = "Continue",
            icon = Icons.Rounded.ArrowForward,
            enabled = email.isNotBlank() && password.length >= 4,
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
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column {
        Text("Plant your roots.", style = MaterialTheme.typography.headlineLarge, color = AgroPalette.Ink, fontWeight = FontWeight.ExtraBold)
        Text("Set up your digital twin in 30 seconds.", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
        Spacer(Modifier.height(20.dp))

        GlowField(value = name, onValueChange = onName, label = "Your name", leading = Icons.Rounded.Person)
        Spacer(Modifier.height(12.dp))
        GlowField(value = email, onValueChange = onEmail, label = "Email", leading = Icons.Rounded.AlternateEmail, keyboard = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        GlowField(
            value = password, onValueChange = onPassword,
            label = "Password", leading = Icons.Rounded.Lock,
            trailing = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingClick = onTogglePassword,
            keyboard = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "Create account",
            icon = Icons.Rounded.ArrowForward,
            enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6,
            onClick = onSubmit,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Input + utilities
// ═════════════════════════════════════════════════════════════════════════════
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
            { IconButton(onClick = { onTrailingClick?.invoke() }) {
                Icon(trailing, contentDescription = null, tint = AgroPalette.InkMuted)
            } }
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

@Composable
private fun DividerWithText(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AgroPalette.SurfaceGlassBorder))
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = AgroPalette.InkDim,
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AgroPalette.SurfaceGlassBorder))
    }
}

@Composable
private fun SocialChip(
    glyph: String?,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
            icon != null -> Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            glyph != null -> Text(glyph, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = AgroPalette.Ink)
    }
}

@Composable
private fun GuestEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Just exploring? ", style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
        Text(
            "Continue as guest →",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AgroPalette.Primary,
        )
    }
}
