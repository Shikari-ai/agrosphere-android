package com.agrosphere.app.feature.assistant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agrosphere.app.data.model.ChatMessage
import com.agrosphere.app.data.repo.MockRepository
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun AssistantScreen(padding: PaddingValues) {
    val messages = remember { mutableStateListOf<ChatMessage>().also { it.addAll(MockRepository.starterChat) } }
    var draft by remember { mutableStateOf("") }
    var nextId by remember { mutableStateOf(2L) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding())
            .imePadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AgroPalette.PrimaryDim),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AgroPalette.Primary) }
                Spacer(Modifier.size(12.dp))
                ScreenTitle(eyebrow = "AI", title = "AgroAI")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { msg -> Bubble(msg) }
        }

        // Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Spacer(Modifier.size(8.dp))
            IconButton(
                onClick = {
                    val prompt = draft.trim()
                    if (prompt.isEmpty()) return@IconButton
                    messages += ChatMessage(nextId++, true, prompt)
                    draft = ""
                    scope.launch {
                        delay(500)
                        messages += ChatMessage(nextId++, false, MockRepository.mockReply(prompt))
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AgroPalette.Primary),
            ) { Icon(Icons.Rounded.Send, null, tint = AgroPalette.BgDeep) }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.fromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = if (isUser) 6.dp else 20.dp,
                        bottomStart = if (isUser) 20.dp else 6.dp,
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
