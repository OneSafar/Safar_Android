package com.safar.app.ui.mehfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.ui.theme.Green500

@Composable
fun DmChatScreen(
    viewModel: MehfilViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dmState = uiState.dmState

    if (dmState !is DmState.Open) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler { onBack() }
    LaunchedEffect(dmState.messages.size) {
        if (dmState.messages.isNotEmpty()) {
            listState.animateScrollToItem(dmState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DmChatTopBar(
                peerName = dmState.peerName,
                onBack = onBack,
                onLeave = {
                    viewModel.leaveDmRoom()
                    onBack()
                },
            )
        },
        bottomBar = {
            DmMessageInput(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        viewModel.sendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
            )
        },
    ) { innerPadding ->
        if (dmState.messages.isEmpty()) {
            EmptyDmState(peerName = dmState.peerName, modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(
                    items = dmState.messages,
                    key = { index, msg -> "${index}:${msg.isMine}:${msg.text}" },
                    contentType = { _, _ -> "dmMessage" },
                ) { _, msg ->
                    DmMessageBubble(text = msg.text, isMine = msg.isMine)
                }
            }
        }
    }
}

@Composable
private fun DmChatTopBar(peerName: String, onBack: () -> Unit, onLeave: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                Text(peerName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(peerName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Green500))
                    Text("Ephemeral - Connected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(
                onClick = onLeave,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.5f)),
                modifier = Modifier.heightIn(min = 32.dp),
            ) {
                Text("Leave", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DmMessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Message...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                textStyle = TextStyle(fontSize = 14.sp),
            )
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(
                    if (value.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(0.12f),
                ),
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyDmState(peerName: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
            Text("Say hello to $peerName!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DmMessageBubble(text: String, isMine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp,
                    ),
                )
                .background(if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .widthIn(max = 280.dp),
        ) {
            Text(text, fontSize = 14.sp, color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
