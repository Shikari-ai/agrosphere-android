package com.agrosphere.app.feature.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agrosphere.app.data.i18n.LocaleManager
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.ui.components.AgroSphereEmblem
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgBlack      = Color(0xFF000000)
private val BgTealTint   = Color(0xFF051A10)
private val BgInput      = Color(0xFF1C1C1C)
private val BgDrawer     = Color(0xFF111111)
private val BgUserBubble = Color(0xFF1A2820)
private val TxtPrimary   = Color(0xFFE8EAED)
private val TxtMuted     = Color(0xFF9AA0A6)
private val TxtDim       = Color(0xFF5F6368)

/** Rotating welcome prompts translated into every supported app language. */
private val farmingPromptsByLang: Map<String, List<String>> = mapOf(
    "en" to listOf(
        "What's on your farm today?",
        "How are your crops looking?",
        "Any pest risks nearby?",
        "Need irrigation advice?",
        "What should I spray this week?",
        "How's the weather for farming?",
        "Any new ideas to explore?",
    ),
    "hi" to listOf(
        "आज आपके खेत में क्या हो रहा है?",
        "आपकी फसलें कैसी दिख रही हैं?",
        "पास में कोई कीट जोखिम है?",
        "सिंचाई की सलाह चाहिए?",
        "इस हफ्ते क्या छिड़काव करूं?",
        "खेती के लिए मौसम कैसा है?",
        "कोई नए विचार खोजने हैं?",
    ),
    "mr" to listOf(
        "आज तुमच्या शेतात काय चालले आहे?",
        "तुमच्या पिकांची स्थिती कशी आहे?",
        "जवळपास कोणता कीड धोका आहे?",
        "सिंचन सल्ला हवा आहे?",
        "या आठवड्यात काय फवारणी करावी?",
        "शेतीसाठी हवामान कसे आहे?",
        "काही नवीन कल्पना शोधायच्या आहेत?",
    ),
    "ta" to listOf(
        "இன்று உங்கள் பண்ணையில் என்ன நடக்கிறது?",
        "உங்கள் பயிர்கள் எப்படி இருக்கின்றன?",
        "அருகில் ஏதேனும் பூச்சி அபாயம் உள்ளதா?",
        "நீர்ப்பாசன ஆலோசனை தேவையா?",
        "இந்த வாரம் என்ன தெளிக்க வேண்டும்?",
        "விவசாயத்திற்கு வானிலை எப்படி உள்ளது?",
        "புதிய யோசனைகள் ஆராய வேண்டுமா?",
    ),
    "te" to listOf(
        "ఈరోజు మీ పొలంలో ఏమి జరుగుతుందో?",
        "మీ పంటలు ఎలా ఉన్నాయి?",
        "దగ్గర్లో ఏమైనా చీడపురుగు ప్రమాదం ఉందా?",
        "నీటిపారుదల సలహా కావాలా?",
        "ఈ వారం ఏమి పిచికారీ చేయాలి?",
        "వ్యవసాయానికి వాతావరణం ఎలా ఉంది?",
        "ఏదైనా కొత్త ఆలోచనలు అన్వేషించాలా?",
    ),
    "bn" to listOf(
        "আজ আপনার খামারে কী হচ্ছে?",
        "আপনার ফসল কেমন দেখাচ্ছে?",
        "কাছাকাছি কি কোনো কীটপতঙ্গের ঝুঁকি আছে?",
        "সেচের পরামর্শ দরকার?",
        "এই সপ্তাহে কী স্প্রে করব?",
        "কৃষির জন্য আবহাওয়া কেমন?",
        "কোনো নতুন ধারণা খুঁজে দেখতে হবে?",
    ),
    "gu" to listOf(
        "આજે તમારા ખેતરમાં શું ચાલી રહ્યું છે?",
        "તમારા પાક કેવા દેખાઈ રહ્યા છે?",
        "નજીકમાં કોઈ જીવાત જોખમ છે?",
        "સિંચાઈ સલાહ જોઈએ?",
        "આ અઠવાડિયે શું છાંટવું?",
        "ખેતી માટે હવામાન કેવું છે?",
        "કોઈ નવા વિચારો શોધવા છે?",
    ),
    "hne" to listOf(
        "आज तोर खेत मा का होत हे?",
        "तोर फसल कइसन दिखत हे?",
        "लगे कोनो कीड़ा खतरा हे का?",
        "सिंचाई के सलाह चाही?",
        "ए हफ्ता मा का छिड़काव करंव?",
        "खेती बर मौसम कइसन हे?",
        "कोनो नवा बात खोजे हे?",
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// Keyboard height — reads directly from the View tree, bypassing Compose's
// inset-consumption chain (which blocks imePadding() inside ModalNavigationDrawer
// when the parent Scaffold has already consumed the IME insets).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberImeHeightDp(): androidx.compose.runtime.State<androidx.compose.ui.unit.Dp> {
    val view    = LocalView.current
    val density = LocalDensity.current
    val state   = remember { mutableStateOf(0.dp) }
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rawInsets = ViewCompat.getRootWindowInsets(view)
            val imeBottom: Int = rawInsets
                ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            state.value = with(density) { imeBottom.toDp() }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return state
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantScreen(padding: PaddingValues) {
    val vm: AssistantViewModel = viewModel()
    val context       = LocalContext.current
    val messages      by vm.messages.collectAsState()
    val typing        by vm.isTyping.collectAsState()
    val provider      by vm.provider.collectAsState()
    val selProvider   by vm.selectedProvider.collectAsState()
    val sessions      by vm.sessions.collectAsState()
    val callMode      by vm.callMode.collectAsState()
    val callStatus    by vm.callStatus.collectAsState()
    var draft         by remember { mutableStateOf("") }
    // Raw IME height read directly from the view tree — works inside ModalNavigationDrawer
    val imeHeightDp   by rememberImeHeightDp()
    // Use the larger of scaffold bottom padding (nav bar) and actual keyboard height
    val bottomPadding = maxOf(padding.calculateBottomPadding(), imeHeightDp)
    val listState     = rememberLazyListState()
    val drawerState   = rememberDrawerState(DrawerValue.Closed)
    val scope         = rememberCoroutineScope()
    val isEmpty       = messages.size <= 1 && !typing

    LaunchedEffect(messages.size, typing) {
        val target = messages.size - 1 + if (typing) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target.coerceAtLeast(0))
    }

    // ── Launchers ─────────────────────────────────────────────────────────────
    fun send(text: String) { val q = text.trim(); if (q.isEmpty()) return; vm.send(q); draft = "" }
    fun handlePicked(uri: Uri?) {
        uri?.let { decodeBitmap(context, it)?.let { bmp -> vm.sendImage(bmp, it.toString(), draft); draft = "" } }
    }
    val galleryLauncher    = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())  { handlePicked(it) }
    val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())       { handlePicked(it) }
    fun pickImage() {
        runCatching { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure  { runCatching { getContentLauncher.launch("image/*") } }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?.let { recognised ->
                if (callMode != CallMode.NONE) {
                    // In call mode: send immediately
                    vm.send(recognised)
                } else {
                    if (draft.isBlank()) draft = recognised else draft += " $recognised"
                }
            }
    }
    fun startVoice() {
        runCatching {
            voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask AgroAI")
            })
        }
    }

    // ── Drawer wraps everything ───────────────────────────────────────────────
    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            HistoryDrawer(
                sessions  = sessions,
                onNewChat = { vm.clearChat(); scope.launch { drawerState.close() } },
                onSession = { session -> vm.loadSession(session); scope.launch { drawerState.close() } },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to BgBlack, 0.6f to BgBlack, 1f to BgTealTint))
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // ── Main chat column ──────────────────────────────────────────────
            // bottomPadding = max(navBar, actual keyboard height) — read directly
            // from the view tree so it works even inside ModalNavigationDrawer.
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)) {
                TopBar(
                    selectedProvider = selProvider,
                    answeredProvider = provider,
                    onHamburger      = { scope.launch { drawerState.open() } },
                    onSelectProvider = { vm.selectProvider(it) },
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (isEmpty) {
                        WelcomeCenter(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize(),
                            state               = listState,
                            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                MessageRow(msg)
                                Spacer(Modifier.height(if (msg.fromUser) 6.dp else 20.dp))
                            }
                            if (typing) item { ThinkingRow() }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, BgBlack)))
                        )
                    }
                }
                InputBar(
                    draft         = draft,
                    onDraftChange = { draft = it },
                    onSend        = { send(draft) },
                    onImage       = { pickImage() },
                    onVoice       = { startVoice() },
                    onVoiceCall   = { vm.startVoiceCall() },
                    onVideoCall   = { vm.startVideoCall() },
                )
            }

            // ── Voice call overlay ────────────────────────────────────────────
            AnimatedVisibility(
                visible = callMode == CallMode.VOICE,
                enter   = fadeIn() + slideInVertically { it },
                exit    = fadeOut() + slideOutVertically { it },
            ) {
                VoiceCallOverlay(
                    messages    = messages,
                    callStatus  = callStatus,
                    modelLabel  = modelLabel(selProvider, provider),
                    ttsFinished = vm.ttsFinished,
                    onSend      = { vm.send(it) },
                    onListenStart = { vm.onListeningStarted() },
                    onListenError = { vm.onListeningError() },
                    onEndCall   = { vm.endCall() },
                )
            }

            // ── Video call overlay ────────────────────────────────────────────
            AnimatedVisibility(
                visible = callMode == CallMode.VIDEO,
                enter   = fadeIn() + slideInVertically { it },
                exit    = fadeOut() + slideOutVertically { it },
            ) {
                VideoCallOverlay(
                    messages    = messages,
                    callStatus  = callStatus,
                    modelLabel  = modelLabel(selProvider, provider),
                    ttsFinished = vm.ttsFinished,
                    onSend      = { vm.send(it) },
                    onListenStart = { vm.onListeningStarted() },
                    onListenError = { vm.onListeningError() },
                    onEndCall   = { vm.endCall() },
                )
            }
        }
    }
}

private fun modelLabel(sel: String?, answered: String): String = when (sel) {
    "gemini" -> "Gemini 2.5"
    "groq"   -> "Llama 3.3"
    "github" -> "GitHub AI"
    else     -> answered.takeIf { it != "ai" }?.replaceFirstChar { it.uppercaseChar() } ?: "AgroAI"
}

// ─────────────────────────────────────────────────────────────────────────────
// History drawer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryDrawer(
    sessions: List<ChatSession>,
    onNewChat: () -> Unit,
    onSession: (ChatSession) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDrawer)
            .padding(top = 56.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier            = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                AgroSphereEmblem(modifier = Modifier.size(32.dp), ringProgress = 1f, leafScale = 1f)
                Spacer(Modifier.width(12.dp))
                Text(
                    "AgroSphere AI",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = TxtPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // New chat
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Edit, null, tint = AgroPalette.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text("New chat", style = MaterialTheme.typography.bodyMedium, color = TxtPrimary, fontWeight = FontWeight.Medium)
            }

            if (sessions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recent",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TxtDim,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                LazyColumn {
                    items(sessions) { session ->
                        SessionItem(session = session, onClick = { onSession(session) })
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))
                Text(
                    "No history yet",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TxtDim,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionItem(session: ChatSession, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF222222)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.preview,
                style     = MaterialTheme.typography.bodyMedium,
                color     = TxtPrimary,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(session.dateLabel, style = MaterialTheme.typography.labelSmall, color = TxtDim)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — Gemini style: ≡   AgroAI · Model ▾   (no pencil)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    selectedProvider: String?,
    answeredProvider: String,
    onHamburger:      () -> Unit,
    onSelectProvider: (String?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val label = modelLabel(selectedProvider, answeredProvider)

    Row(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        // Hamburger — opens history drawer
        IconButton(onClick = onHamburger) {
            Icon(Icons.Rounded.Menu, contentDescription = "History", tint = TxtPrimary)
        }

        Spacer(Modifier.weight(1f))

        // Model pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { showSheet = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "AgroAI  ·  $label",
                style      = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                color      = TxtPrimary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = TxtMuted, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.weight(1f))

        // Placeholder spacer to keep pill centred (same width as hamburger)
        Spacer(Modifier.size(48.dp))
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1A1A1A),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            ) {
                Text("Choose model", style = MaterialTheme.typography.titleMedium, color = TxtPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Pin a model or let AgroAI choose", style = MaterialTheme.typography.bodySmall, color = TxtMuted)
                Spacer(Modifier.height(20.dp))
                ProviderOption("Auto",             "Best available for each query",      Icons.Rounded.AutoAwesome, AgroPalette.Primary, selectedProvider == null)     { onSelectProvider(null);     showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("Gemini 2.5 Flash", "Google · fast & multimodal",         Icons.Rounded.Star,        AgroPalette.Sky,     selectedProvider == "gemini") { onSelectProvider("gemini"); showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("Groq · Llama 3.3", "Blazing-fast open-source inference", Icons.Rounded.Bolt,        AgroPalette.Orange,  selectedProvider == "groq")   { onSelectProvider("groq");   showSheet = false }
                Spacer(Modifier.height(10.dp))
                ProviderOption("GitHub · Llama 3.3","GitHub Models",                     Icons.Rounded.Code,        AgroPalette.Iris,    selectedProvider == "github") { onSelectProvider("github"); showSheet = false }
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
            modifier         = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, color = TxtPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = TxtMuted)
        }
        if (selected) {
            Box(
                modifier         = Modifier.size(22.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(13.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Welcome centre
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeCenter(modifier: Modifier = Modifier) {
    val prompts = farmingPromptsByLang[LocaleManager.activeLanguageTag()]
        ?: farmingPromptsByLang["en"]!!

    val inf = rememberInfiniteTransition(label = "emblem")
    val ring by inf.animateFloat(
        initialValue  = 0.70f,
        targetValue   = 1.00f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "ring",
    )
    var promptIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(prompts) { promptIdx = 0 }
    LaunchedEffect(prompts, promptIdx) { delay(3_200); promptIdx = (promptIdx + 1) % prompts.size }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            AgroSphereEmblem(modifier = Modifier.size(72.dp), ringProgress = ring, leafScale = 1f)
            Spacer(Modifier.height(28.dp))
            AnimatedContent(
                targetState  = promptIdx,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                label        = "prompt",
            ) { idx ->
                Text(
                    prompts[idx],
                    style     = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Normal, lineHeight = 36.sp),
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
                        model = msg.imageUri, contentDescription = null, contentScale = ContentScale.Crop,
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
            Box(
                modifier         = Modifier.padding(top = 2.dp).size(26.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { AgroSphereEmblem(modifier = Modifier.size(20.dp), ringProgress = 1f, leafScale = 1f) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (msg.imageUri != null) {
                    AsyncImage(
                        model = msg.imageUri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(10.dp))
                }
                if (msg.text.isNotBlank())
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 15.sp), color = TxtPrimary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Thinking dots
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThinkingRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier         = Modifier.size(26.dp).clip(CircleShape).background(AgroPalette.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { AgroSphereEmblem(modifier = Modifier.size(20.dp), ringProgress = 1f, leafScale = 1f) }
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
// Input bar  — "+" opens plus-menu sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    draft: String, onDraftChange: (String) -> Unit,
    onSend: () -> Unit, onImage: () -> Unit, onVoice: () -> Unit,
    onVoiceCall: () -> Unit, onVideoCall: () -> Unit,
) {
    var showPlus by remember { mutableStateOf(false) }
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
        // "+" — opens plus menu
        IconButton(
            onClick  = { showPlus = true },
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
        ) { Icon(Icons.Rounded.Image, null, tint = TxtMuted, modifier = Modifier.size(20.dp)) }

        Spacer(Modifier.width(4.dp))

        Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
            if (draft.isEmpty()) Text("Ask AgroAI", style = TextStyle(fontSize = 15.sp, color = TxtDim, lineHeight = 22.sp))
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

        if (!canSend) {
            IconButton(
                onClick  = onVoice,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
            ) { Icon(Icons.Rounded.Mic, null, tint = TxtMuted, modifier = Modifier.size(20.dp)) }
        } else {
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(AgroPalette.Primary).clickable(onClick = onSend),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
        }
    }

    // Plus menu sheet
    if (showPlus) {
        ModalBottomSheet(
            onDismissRequest = { showPlus = false },
            sheetState       = rememberModalBottomSheetState(),
            containerColor   = Color(0xFF151515),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
                Text("Add to conversation", style = MaterialTheme.typography.titleSmall, color = TxtMuted, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                PlusOption(Icons.Rounded.Image,     "Photo",      AgroPalette.Sky)    { showPlus = false; onImage() }
                Spacer(Modifier.height(12.dp))
                PlusOption(Icons.Rounded.Mic,       "Voice call", AgroPalette.Primary) { showPlus = false; onVoiceCall() }
                Spacer(Modifier.height(12.dp))
                PlusOption(Icons.Rounded.VideoCall, "Video call", AgroPalette.Orange)  { showPlus = false; onVideoCall() }
            }
        }
    }
}

@Composable
private fun PlusOption(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TxtPrimary, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared call recognizer logic
// Auto-loop: listen → user speaks → send → TTS speaks → ttsFinished → listen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun rememberCallRecognizer(
    onSend:       (String) -> Unit,
    onListenStart: () -> Unit,
    onListenError: () -> Unit,
): Pair<SpeechRecognizer, () -> Unit> {
    val context    = LocalContext.current
    val micPerm    = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val startListening: () -> Unit = remember {
        {
            if (!micPerm.status.isGranted) {
                micPerm.launchPermissionRequest()
            } else {
                runCatching {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    }
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { onListenStart() }
                        override fun onBeginningOfSpeech()             {}
                        override fun onRmsChanged(rmsdB: Float)        {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech()                   {}
                        override fun onPartialResults(partial: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                        override fun onResults(results: Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                            if (!text.isNullOrBlank()) onSend(text)
                            // NOTE: next listen cycle is triggered by ttsFinished in the overlay
                        }
                        override fun onError(error: Int) { onListenError() }
                    })
                    recognizer.startListening(intent)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { recognizer.cancel(); recognizer.destroy() } }
    }

    return Pair(recognizer, startListening)
}

// ─────────────────────────────────────────────────────────────────────────────
// Voice call overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceCallOverlay(
    messages:     List<ChatMessage>,
    callStatus:   String,
    modelLabel:   String,
    ttsFinished:  kotlinx.coroutines.flow.SharedFlow<Unit>,
    onSend:       (String) -> Unit,
    onListenStart: () -> Unit,
    onListenError: () -> Unit,
    onEndCall:    () -> Unit,
) {
    val (_, startListening) = rememberCallRecognizer(onSend, onListenStart, onListenError)

    // Start listening immediately when overlay appears
    LaunchedEffect(Unit) { startListening() }

    // Auto-loop: restart listening every time TTS finishes
    LaunchedEffect(ttsFinished) { ttsFinished.collect { startListening() } }

    val inf  = rememberInfiniteTransition(label = "vcall")
    val ring by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "r",
    )
    val isSpeaking = callStatus.startsWith("Speaking")
    val lastAi = messages.lastOrNull { !it.fromUser }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505)), contentAlignment = Alignment.Center) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(modelLabel, style = MaterialTheme.typography.titleMedium, color = TxtMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    callStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSpeaking) AgroPalette.Sky.copy(alpha = 0.9f) else AgroPalette.Primary.copy(alpha = 0.9f),
                )
            }

            AgroSphereEmblem(modifier = Modifier.size(120.dp), ringProgress = ring, leafScale = 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false).padding(top = 32.dp, bottom = 16.dp),
            ) {
                if (lastAi != null) {
                    Text(
                        lastAi.text.take(220),
                        style     = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 15.sp),
                        color     = TxtPrimary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines  = 7,
                        overflow  = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Manual mic tap (re-triggers if user wants to speak while AI is talking)
                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape)
                        .background(AgroPalette.Primary.copy(alpha = 0.15f))
                        .border(2.dp, AgroPalette.Primary.copy(alpha = 0.4f), CircleShape)
                        .clickable { startListening() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, null, tint = AgroPalette.Primary, modifier = Modifier.size(28.dp)) }

                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(0xFFB00020))
                        .clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video call overlay  (front camera background + auto-loop voice)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoCallOverlay(
    messages:     List<ChatMessage>,
    callStatus:   String,
    modelLabel:   String,
    ttsFinished:  kotlinx.coroutines.flow.SharedFlow<Unit>,
    onSend:       (String) -> Unit,
    onListenStart: () -> Unit,
    onListenError: () -> Unit,
    onEndCall:    () -> Unit,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller     = remember { LifecycleCameraController(context) }
    val previewView    = remember {
        PreviewView(context).apply {
            this.controller = controller
            scaleType       = PreviewView.ScaleType.FILL_CENTER
        }
    }
    LaunchedEffect(Unit) {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        controller.bindToLifecycle(lifecycleOwner)
    }

    val (_, startListening) = rememberCallRecognizer(onSend, onListenStart, onListenError)

    LaunchedEffect(Unit)         { startListening() }
    LaunchedEffect(ttsFinished)  { ttsFinished.collect { startListening() } }

    val inf  = rememberInfiniteTransition(label = "video")
    val ring by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "r",
    )
    val isSpeaking = callStatus.startsWith("Speaking")
    val lastAi = messages.lastOrNull { !it.fromUser }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(modelLabel, style = MaterialTheme.typography.titleMedium, color = TxtMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    callStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSpeaking) AgroPalette.Sky.copy(alpha = 0.9f) else AgroPalette.Primary.copy(alpha = 0.9f),
                )
            }

            AgroSphereEmblem(modifier = Modifier.size(110.dp), ringProgress = ring, leafScale = 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false).padding(top = 28.dp, bottom = 16.dp),
            ) {
                if (lastAi != null) {
                    Text(
                        lastAi.text.take(220),
                        style     = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 15.sp),
                        color     = TxtPrimary,
                        textAlign = TextAlign.Center,
                        maxLines  = 7,
                        overflow  = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape)
                        .background(AgroPalette.Primary.copy(alpha = 0.15f))
                        .border(2.dp, AgroPalette.Primary.copy(alpha = 0.4f), CircleShape)
                        .clickable { startListening() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, null, tint = AgroPalette.Primary, modifier = Modifier.size(28.dp)) }

                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(0xFFB00020))
                        .clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
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
