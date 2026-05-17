package com.agrosphere.app.feature.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(padding: PaddingValues) {
    val messages = remember { mutableStateListOf<ChatMessage>().also { it.addAll(MockRepository.starterChat) } }
    var draft by remember { mutableStateOf("") }
    var nextId by remember { mutableStateOf(2L) }
    var typing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, typing) {
        val target = messages.size - 1 + if (typing) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target.coerceAtLeast(0))
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return
        messages += ChatMessage(nextId++, true, prompt)
        draft = ""
        typing = true
        scope.launch {
            delay(900)
            messages += ChatMessage(nextId++, false, MockRepository.demoReply(prompt))
            typing = false
        }
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
            OnlineDot()
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
                onClick = { /* future: attach */ },
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
                onClick = { if (canSend) send(draft) },
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

@Composable
private fun OnlineDot() {
    // Honest about being stubbed — no LLM is wired in yet.
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AgroPalette.Amber.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AgroPalette.Amber))
        Spacer(Modifier.width(6.dp))
        Text("DEMO MODE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp), color = AgroPalette.Amber, fontWeight = FontWeight.Bold)
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
        Box(
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
            Text(
                msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) AgroPalette.BgDeep else AgroPalette.Ink,
            )
        }
    }
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
        AvatarMini()
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 6.dp))
                .background(AgroPalette.SurfaceGlass)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypingDot(0)
                Spacer(Modifier.width(4.dp))
                TypingDot(150)
                Spacer(Modifier.width(4.dp))
                TypingDot(300)
            }
        }
    }
}

@Composable
private fun TypingDot(delayMs: Int) {
    val tr = rememberInfiniteTransition(label = "dot")
    val alpha by tr.animateFloat(
        0.3f, 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = delayMs, easing = LinearEasing)),
        label = "a",
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(AgroPalette.Primary.copy(alpha = alpha))
    )
}
