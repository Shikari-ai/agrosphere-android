package com.agrosphere.app.feature.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.ui.components.AgroSphereEmblem
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay

// ─── Local palette ────────────────────────────────────────────────────────────
private val BgBlack      = Color(0xFF000000)
private val BgTealTint   = Color(0xFF051A10)   // very dark green/teal — bottom glow
private val BgInput      = Color(0xFF1C1C1C)
private val BgUserBubble = Color(0xFF1A2820)
private val TxtPrimary   = Color(0xFFE8EAED)
private val TxtMuted     = Color(0xFF9AA0A6)
private val TxtDim       = Color(0xFF5F6368)

private val farmingPrompts = listOf(
    "What's on your farm today?",
    "How are your crops looking?",
    "Any pest risks nearby?",
    "Need irrigation advice?",
    "What should I spray this week?",
    "How's the weather for farming?",
    "Any new ideas to explore?",
)

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantScreen(padding: PaddingValues) {
    val vm: AssistantViewModel = viewModel()
    val context       = LocalContext.current
    val messages      by vm.messages.collectAsState()
    val typing        by vm.isTyping.collectAsState()
    val provider      by vm.provider.collectAsState()
    val selProvider   by vm.selectedProvider.collectAsState()
    var draft         by remember { mutableStateOf("") }
    val listState     = rememberLazyListState()
    val isEmpty       = messages.size <= 1 && !typing  // only greeting → show welcome

    LaunchedEffect(messages.size, typing) {
        val target = messages.size - 1 + if (typing) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target.coerceAtLeast(0))
    }

    fun send(text: String) {
        val q = text.trim(); if (q.isEmpty()) return
        vm.send(q); draft = ""
    }
    fun handlePicked(uri: Uri?) {
        uri?.let { decodeBitmap(context, it)?.let { bmp -> vm.sendImage(bmp, it.toString(), draft); draft = "" } }
    }
    val galleryLauncher    = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())    { handlePicked(it) }
    val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())         { handlePicked(it) }
    fun pickImage() {
        runCatching { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure { runCatching { getContentLauncher.launch("image/*") } }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?.let { if (draft.isBlank()) draft = it else draft += " $it" }
    }
    fun startVoice() {
        runCatching {
            voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your question")
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f   to BgBlack,
                    0.6f to BgBlack,
                    1f   to BgTealTint,
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            TopBar(
                selectedProvider = selProvider,
                answeredProvider = provider,
                onSelectProvider = { vm.selectProvider(it) },
                onNewChat        = { vm.clearChat() },
            )

            // ── Body ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (isEmpty) {
                    // Welcome — centred like Gemini
                    WelcomeCenter(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        state          = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageRow(msg)
                            Spacer(Modifier.height(if (msg.fromUser) 6.dp else 20.dp))
                        }
                        if (typing) item { ThinkingRow() }
                    }
                    // Scrim above input
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, BgBlack)))
                    )
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            InputBar(
                draft         = draft,
                onDraftChange = { draft = it },
                onSend        = { send(draft) },
                onImage       = { pickImage() },
                onVoice       = { startVoice() },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — Gemini style: ≡   AgroAI · Model ▾   ✏
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    selectedProvider: String?,
    answeredProvider: String,
    onSelectProvider: (String?) -> Unit,
    onNewChat:        () -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val modelLabel = when (selectedProvider) {
        "gemini" -> "Gemini 2.5"
        "groq"   -> "Llama 3.3"
        "github" -> "GitHub AI"
        else     -> answeredProvider.takeIf { it != "ai" }
            ?.replaceFirstChar { it.uppercaseChar() } ?: "AgroAI"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hamburger
        IconButton(onClick = { /* drawer / sidebar */ }) {
            Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = TxtPrimary)
        }

        Spacer(Modifier.weight(1f))

        // Center model pill — exactly like "Gemini Flash ▾"
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { showSheet = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "AgroAI  ·  $modelLabel",
                style      = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                color      = TxtPrimary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = TxtMuted, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.weight(1f))

        // New chat / edit icon
        IconButton(onClick = onNewChat) {
            Icon(Icons.Rounded.Create, contentDescription = "New chat", tint = TxtPrimary)
        }
    }

    // Model picker sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1A1A1A),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text("Model", style = MaterialTheme.typography.titleMedium, color = TxtPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Pin a model or let AgroAI choose", style = MaterialTheme.typography.bodySmall, color = TxtMuted)
                Spacer(Modifier.height(20.dp))
                ProviderOption("Auto",              "Best available for each query",       Icons.Rounded.AutoAwesome, AgroPalette.Primary, selectedProvider == null)     { onSelectProvider(null);     showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("Gemini 2.5 Flash",  "Google · fast & multimodal",          Icons.Rounded.Star,        AgroPalette.Sky,     selectedProvider == "gemini") { onSelectProvider("gemini"); showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("Groq · Llama 3.3",  "Blazing-fast open-source inference",  Icons.Rounded.Bolt,        AgroPalette.Orange,  selectedProvider == "groq")   { onSelectProvider("groq");   showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("GitHub · Llama 3.3","GitHub Models",                        Icons.Rounded.Code,        AgroPalette.Iris,    selectedProvider == "github") { onSelectProvider("github"); showSheet = false }
            }
        }
    }
}

@Composable
private fun ProviderOption(
    title: String, subtitle: String,
    icon: ImageVector, accent: Color,
    selected: Boolean, onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.10f) else Color(0xFF252525))
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else Color(0xFF333333), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, color = TxtPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = TxtMuted)
        }
        if (selected) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(13.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Welcome centre — AgroSphereEmblem + dynamic rotating prompt
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeCenter(modifier: Modifier = Modifier) {
    // Slowly animate the orbit rings
    val inf = rememberInfiniteTransition(label = "emblem")
    val ring by inf.animateFloat(
        initialValue  = 0.70f,
        targetValue   = 1.00f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "ring",
    )

    // Cycle through farming prompts every 3 s
    var promptIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_200)
            promptIdx = (promptIdx + 1) % farmingPrompts.size
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // AgroSphere emblem — replaces Gemini's 4-colour star
            AgroSphereEmblem(
                modifier     = Modifier.size(72.dp),
                ringProgress = ring,
                leafScale    = 1f,
            )

            Spacer(Modifier.height(28.dp))

            // Dynamic prompt with crossfade — like Gemini's cycling text
            AnimatedContent(
                targetState  = promptIdx,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                },
                label = "prompt",
            ) { idx ->
                Text(
                    text      = farmingPrompts[idx],
                    style     = MaterialTheme.typography.headlineMedium.copy(
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 36.sp,
                    ),
                    color     = TxtPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Messages
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageRow(msg: ChatMessage) {
    if (msg.fromUser) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .background(BgUserBubble)
                    .border(1.dp, Color(0xFF2A3830), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (msg.imageUri != null) {
                    AsyncImage(
                        model = msg.imageUri, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (msg.text.isNotBlank())
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = TxtPrimary)
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            // Small emblem avatar
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AgroSphereEmblem(
                    modifier     = Modifier.size(20.dp),
                    ringProgress = 1f,
                    leafScale    = 1f,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (msg.imageUri != null) {
                    AsyncImage(
                        model = msg.imageUri, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(10.dp))
                }
                if (msg.text.isNotBlank())
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 15.sp),
                        color = TxtPrimary,
                    )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Thinking indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThinkingRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            AgroSphereEmblem(modifier = Modifier.size(20.dp), ringProgress = 1f, leafScale = 1f)
        }
        Spacer(Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            ThinkingDot(0f); ThinkingDot(0.2f); ThinkingDot(0.4f)
        }
    }
}

@Composable
private fun ThinkingDot(delayFraction: Float) {
    val inf   = rememberInfiniteTransition(label = "dot")
    val scale by inf.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(600, delayMillis = (delayFraction * 600).toInt(), easing = LinearEasing), RepeatMode.Reverse),
        label = "s",
    )
    Box(modifier = Modifier.size((7 * scale).dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.5f + 0.5f * scale)))
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    draft: String, onDraftChange: (String) -> Unit,
    onSend: () -> Unit, onImage: () -> Unit, onVoice: () -> Unit,
) {
    val canSend = draft.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(BgInput)
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(30.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // "+" attachment
        IconButton(
            onClick  = onImage,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
        ) { Icon(Icons.Rounded.Add, null, tint = TxtMuted, modifier = Modifier.size(20.dp)) }

        Spacer(Modifier.width(4.dp))

        // Text field
        Box(
            modifier           = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment   = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text("Ask AgroAI", style = TextStyle(fontSize = 15.sp, color = TxtDim, lineHeight = 22.sp))
            }
            BasicTextField(
                value         = draft,
                onValueChange = onDraftChange,
                textStyle     = TextStyle(fontSize = 15.sp, color = TxtPrimary, lineHeight = 22.sp),
                cursorBrush   = SolidColor(AgroPalette.Primary),
                maxLines      = 6,
                modifier      = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(4.dp))

        // Mic always visible when empty, send when text exists
        if (!canSend) {
            IconButton(
                onClick  = onVoice,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
            ) { Icon(Icons.Rounded.Mic, null, tint = TxtMuted, modifier = Modifier.size(20.dp)) }
        } else {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Primary)
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Image decode
// ─────────────────────────────────────────────────────────────────────────────

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
} catch (_: Throwable) { null }
