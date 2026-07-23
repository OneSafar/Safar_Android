package com.safarparmar.app.ui.mehfil

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline

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
        containerColor = MehfilFlatColors.Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DmChatTopBar(
                peerName = dmState.peerName,
                peerAvatar = dmState.peerAvatar,
                connected = uiState.socketConnected,
                peerOnline = uiState.dmPeerOnline,
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
                    DmMessageBubble(
                        text = msg.text,
                        isMine = msg.isMine,
                        avatarUrl = if (msg.isMine) msg.senderAvatar else msg.senderAvatar ?: dmState.peerAvatar,
                        avatarName = if (msg.isMine) "You" else dmState.peerName,
                        state = msg.state,
                    )
                }
            }
        }
    }
}

@Composable
private fun DmChatTopBar(
    peerName: String,
    peerAvatar: String?,
    connected: Boolean,
    peerOnline: Boolean,
    onBack: () -> Unit,
    onLeave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MehfilFlatColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, MehfilFlatColors.Hairline, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MehfilFlatColors.Text,
                    modifier = Modifier.size(18.dp),
                )
            }
            DmAvatar(name = peerName, avatarUrl = peerAvatar, size = 34.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    peerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MehfilFlatColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (connected) MehfilFlatColors.Activity
                                else MehfilFlatColors.Like,
                            ),
                    )
                    Text(
                        when {
                            !connected -> "Connecting again…"
                            !peerOnline -> "Student is away"
                            else -> "Private chat · Messages are not saved"
                        },
                        fontSize = 11.sp,
                        color = MehfilFlatColors.Muted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MehfilFlatColors.Like.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onLeave)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Leave", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MehfilFlatColors.Like)
            }
        }
        PlanHairline()
    }
}

@Composable
private fun DmMessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val canSend = value.isNotBlank()
    Column(Modifier.fillMaxWidth().background(MehfilFlatColors.Bg)) {
        PlanHairline()
        Row(
            Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Message...", fontSize = 14.sp, color = MehfilFlatColors.Muted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = MehfilFlatColors.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MehfilFlatColors.Primary,
                    unfocusedBorderColor = MehfilFlatColors.Hairline,
                    focusedTextColor = MehfilFlatColors.Text,
                    unfocusedTextColor = MehfilFlatColors.Text,
                    cursorColor = MehfilFlatColors.Primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedPlaceholderColor = MehfilFlatColors.Muted,
                    unfocusedPlaceholderColor = MehfilFlatColors.Muted,
                ),
            )
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MehfilFlatColors.Primary
                        else MehfilFlatColors.Hairline.copy(alpha = 0.55f),
                    ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (canSend) Color.White else MehfilFlatColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun EmptyDmState(peerName: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MehfilFlatColors.Muted.copy(alpha = 0.45f),
            )
            Text("Say hello to $peerName!", fontSize = 14.sp, color = MehfilFlatColors.Muted)
        }
    }
}

@Composable
private fun DmMessageBubble(
    text: String,
    isMine: Boolean,
    avatarUrl: String?,
    avatarName: String,
    state: DmMessageState,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            DmAvatar(name = avatarName, avatarUrl = avatarUrl, size = 28.dp)
            Spacer(Modifier.size(6.dp))
        }
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
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
                    .then(
                        if (isMine) {
                            Modifier.background(MehfilFlatColors.Primary)
                        } else {
                            Modifier
                                .background(MehfilFlatColors.Hairline.copy(alpha = 0.28f))
                                .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 16.dp,
                                ))
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .widthIn(max = 280.dp),
            ) {
                Text(
                    text,
                    fontSize = 14.sp,
                    color = if (isMine) Color.White else MehfilFlatColors.Text,
                )
            }
            if (isMine && state != DmMessageState.SENT) {
                Text(
                    if (state == DmMessageState.SENDING) "Sending…" else "Not sent",
                    fontSize = 10.sp,
                    color = if (state == DmMessageState.FAILED) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                )
            }
        }
        if (isMine) {
            Spacer(Modifier.size(6.dp))
            DmAvatar(name = avatarName, avatarUrl = avatarUrl, size = 28.dp)
        }
    }
}

@Composable
private fun DmAvatar(name: String, avatarUrl: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MehfilFlatColors.Primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = MehfilFlatColors.Primary,
                fontSize = if (size.value >= 34f) 14.sp else 11.sp,
            )
        }
    }
}
