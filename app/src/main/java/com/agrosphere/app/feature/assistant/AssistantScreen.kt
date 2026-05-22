package com.agrosphere.app.feature.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import coil.compose.AsyncImage
import com.agrosphere.app.data.model.ChatMessage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun AssistantScreen(padding: PaddingValues) {
    val vm: AssistantViewModel = viewModel()
    val context = LocalContext.current
    val messages by vm.messages.collectAsState()
    val typing by vm.isTyping.collectAsState()
    val provider by vm.provider.collectAsState()
    val selectedProvider by vm.selectedProvider.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, typing) {
        val target = messages.size - 1 + if (typing) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target.coerceAtLeast(0))
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return
        vm.send(prompt)
        draft = ""
    }

    // Image upload → vision analysis
    fun handlePicked(uri: Uri?) {
        if (uri != null) decodeBitmap(context, uri)?.let { bmp ->
            vm.sendImage(bmp, uri.toString(), draft)
            draft = ""
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> handlePicked(uri) }
    // Fallback for devices/emulators without the system photo picker.
    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> handlePicked(uri) }

    fun pickImage() {
        runCatching {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure {
            runCatching { getContentLauncher.launch("image/*") }
        }
    }

    // Voice typing → fills the draft (system speech UI handles the mic permission)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            draft = if (draft.isBlank()) spoken else "$draft $spoken"
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your question")
        }
        runCatching { voiceLauncher.launch(intent) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.PrimaryDim)
                    .border(1.dp, AgroPalette.Primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitle(eyebrow = "AI", title = "AgroAI")
            }
            ModelSelector(
                selected = selectedProvider,
                answered = provider,
                onSelect = { vm.selectProvider(it) },
            )
        }

        // Suggested prompts (only when conversation is short)
        AnimatedVisibility(messages.size <= 1, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Text(
                    "TRY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = AgroPalette.InkMuted,
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(suggestedPrompts()) { p ->
                        SuggestedChip(p) { send(p) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { msg -> Bubble(msg) }
            if (typing) item { TypingBubble() }
        }

        // Composer
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { pickImage() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.SurfaceGlass),
            ) { Icon(Icons.Rounded.Add, null, tint = AgroPalette.InkMuted) }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Ask about pests, weather, irrigation…", color = AgroPalette.InkDim) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgroPalette.Primary,
                    unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
                    focusedTextColor = AgroPalette.Ink,
                    unfocusedTextColor = AgroPalette.Ink,
                    cursorColor = AgroPalette.Primary,
                    focusedContainerColor = AgroPalette.SurfaceGlass,
                    unfocusedContainerColor = AgroPalette.SurfaceGlass,
                ),
                singleLine = false,
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank()
            IconButton(
                onClick = { if (canSend) send(draft) else startVoice() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) AgroPalette.Primary else AgroPalette.SurfaceGlass),
            ) {
                Icon(
                    if (canSend) Icons.Rounded.Send else Icons.Rounded.Mic,
                    null,
                    tint = if (canSend) AgroPalette.BgDeep else AgroPalette.InkMuted,
                )
            }
        }
    }
}

/** Tappable badge that opens a bottom sheet to pin the AI model (or Auto). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    selected: String?,      // null = Auto
    answered: String,       // provider that replied last (for Auto label)
    onSelect: (String?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // On Auto, show whoever answered last; otherwise show the pinned choice.
    val label = when (selected) {
        "gemini" -> "GEMINI"
        "groq"   -> "GROQ"
        "github" -> "GITHUB"
        else     -> "AUTO" + (answered.takeIf { it != "ai" }?.let { " · ${it.uppercase()}" } ?: "")
    }

    // Trigger badge in the header
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Primary.copy(alpha = 0.16f))
            .clickable { showSheet = true }
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AgroPalette.Primary))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
            color = AgroPalette.Primary,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown, null,
            tint = AgroPalette.Primary,
            modifier = Modifier.size(14.dp),
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = AgroPalette.BgDeep,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    "Choose AI model",
                    style = MaterialTheme.typography.titleMedium,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Pin a provider or let AgroAI pick the best one",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                )
                Spacer(Modifier.height(16.dp))

                ProviderOption(
                    "Auto", "Smart pick — always the best available",
                    Icons.Rounded.AutoAwesome, AgroPalette.Primary, selected == null,
                ) { onSelect(null); showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption(
                    "Gemini 2.5 Flash", "Google · fast & capable",
                    Icons.Rounded.Star, AgroPalette.Sky, selected == "gemini",
                ) { onSelect("gemini"); showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption(
                    "Groq · Llama 3.3 70B", "Blazing-fast inference",
                    Icons.Rounded.Bolt, AgroPalette.Orange, selected == "groq",
                ) { onSelect("groq"); showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption(
                    "GitHub · Llama 3.3 70B", "GitHub Models",
                    Icons.Rounded.Code, AgroPalette.Iris, selected == "github",
                ) { onSelect("github"); showSheet = false }
            }
        }
    }
}

@Composable
private fun ProviderOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else AgroPalette.SurfaceGlass)
            .border(
                1.5.dp,
                if (selected) accent else AgroPalette.SurfaceGlassBorder,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
        }
        if (selected) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun SuggestedChip(text: String, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.SurfaceGlass)
            .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = AgroPalette.Ink)
    }
}

private fun suggestedPrompts(): List<String> = listOf(
    "When can I spray?",
    "Which field needs water?",
    "What's the storm risk?",
    "Best fertilizer this week?",
    "How is my crop health trending?",
)

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.fromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            AvatarMini()
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomEnd = if (isUser) 6.dp else 22.dp,
                        bottomStart = if (isUser) 22.dp else 6.dp,
                    )
                )
                .background(if (isUser) AgroPalette.Primary else AgroPalette.SurfaceGlass)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (msg.imageUri != null) {
                AsyncImage(
                    model = msg.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                if (msg.text.isNotBlank()) Spacer(Modifier.height(8.dp))
            }
            if (msg.text.isNotBlank()) {
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) AgroPalette.BgDeep else AgroPalette.Ink,
                )
            }
        }
    }
}

// Decode a picked image Uri into a downsampled software Bitmap.
// Catches Throwable (not just Exception) so a full-res photo that would OOM
// returns null instead of crashing the app.
private fun decodeBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val max = maxOf(info.size.width, info.size.height)
            if (max > 1600) {
                val scale = 1600f / max
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
} catch (_: Throwable) {
    null
}

@Composable
private fun AvatarMini() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(AgroPalette.PrimaryDim),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp)) }
}

@Composable
private fun TypingBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        AvatarMiniPulsing()
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 6.dp))
                .drawBehind {
                    // Neon plasma top hairline
                    drawLine(
                        brush = Brush.horizontalGradient(
                            0f   to Color.Transparent,
                            0.2f to AgroPalette.Primary.copy(alpha = 0.25f),
                            0.5f to AgroPalette.Primary.copy(alpha = 0.65f),
                            0.8f to AgroPalette.Primary.copy(alpha = 0.25f),
                            1f   to Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(size.width, 0f),
                        strokeWidth = 1.4f,
                    )
                }
                .background(AgroPalette.SurfaceGlass)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            NeuralTypingIndicator()
        }
    }
}

/** Three plasma nodes connected by a travelling sin-wave arc. */
@Composable
private fun NeuralTypingIndicator() {
    val inf  = rememberInfiniteTransition(label = "neural")
    val wave by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label         = "wave",
    )

    Canvas(Modifier.size(width = 74.dp, height = 28.dp)) {
        val w      = size.width
        val h      = size.height
        val cy     = h / 2f
        val nodeR  = h * 0.215f
        val xs     = floatArrayOf(w * 0.15f, w * 0.50f, w * 0.85f)
        val phases = floatArrayOf(0f, 0.333f, 0.667f)

        // ── Travelling sin-wave arc between nodes ──────────────────────────
        val arcPath = Path()
        val steps   = 80
        for (i in 0..steps) {
            val t  = i / steps.toFloat()
            val x  = w * t
            // Envelope: amplitude peaks between the nodes, fades to 0 at edges
            val env = sin((t * PI).toFloat()).coerceIn(0f, 1f)
            val y   = cy + nodeR * 0.85f * env *
                    sin(((t * 2.8f - wave * 2.8f) * PI * 2).toDouble()).toFloat()
            if (i == 0) arcPath.moveTo(x, y) else arcPath.lineTo(x, y)
        }
        drawPath(
            path  = arcPath,
            brush = Brush.horizontalGradient(
                0f   to AgroPalette.Primary.copy(alpha = 0.00f),
                0.15f to AgroPalette.Primary.copy(alpha = 0.38f),
                0.50f to AgroPalette.Primary.copy(alpha = 0.60f),
                0.85f to AgroPalette.Primary.copy(alpha = 0.38f),
                1f   to AgroPalette.Primary.copy(alpha = 0.00f),
            ),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round),
        )

        // ── Plasma nodes ────────────────────────────────────────────────────
        xs.forEachIndexed { idx, x ->
            val t     = ((wave + phases[idx]) % 1f)
            val pulse = sin((t * PI * 2).toDouble()).toFloat()
            val frac  = (pulse + 1f) * 0.5f          // 0..1
            val sc    = 0.68f + 0.32f * frac
            val al    = 0.42f + 0.58f * frac
            val r     = nodeR * sc
            val np    = Offset(x, cy)

            // Soft outer corona
            drawCircle(
                brush = Brush.radialGradient(
                    0f   to AgroPalette.Primary.copy(alpha = 0.50f * al),
                    0.45f to AgroPalette.Primary.copy(alpha = 0.20f * al),
                    1f   to Color.Transparent,
                    center = np, radius = r * 3.4f,
                ),
                radius = r * 3.4f, center = np,
            )
            // Solid core
            drawCircle(AgroPalette.Primary.copy(alpha = al), radius = r, center = np)
            // Specular highlight
            drawCircle(
                Color.White.copy(alpha = 0.60f * al),
                radius = r * 0.36f,
                center = Offset(x - r * 0.20f, cy - r * 0.20f),
            )

            // Tiny orbiting satellite dot
            val orbitAngle = (wave * 2f * PI.toFloat() + idx * 2.0944f) // 2π/3 stagger
            val orbitR     = r * 2.2f
            val satX       = x  + orbitR * cos(orbitAngle.toDouble()).toFloat()
            val satY       = cy + orbitR * sin(orbitAngle.toDouble()).toFloat()
            drawCircle(
                AgroPalette.Primary.copy(alpha = 0.75f * al),
                radius = r * 0.30f,
                center = Offset(satX, satY),
            )
        }
    }
}

/** AvatarMini with a soft pulsing glow ring to match the neural typing indicator. */
@Composable
private fun AvatarMiniPulsing() {
    val inf   = rememberInfiniteTransition(label = "av-glow")
    val glow  by inf.animateFloat(
        0.25f, 0.80f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "g",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .drawBehind {
                val c  = Offset(size.width / 2f, size.height / 2f)
                val r  = size.minDimension * 0.56f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = 0.55f * glow),
                        1f to Color.Transparent,
                        center = c, radius = r * 2f,
                    ),
                    radius = r * 2f, center = c,
                )
            }
            .clip(CircleShape)
            .background(AgroPalette.PrimaryDim),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(14.dp)) }
}
