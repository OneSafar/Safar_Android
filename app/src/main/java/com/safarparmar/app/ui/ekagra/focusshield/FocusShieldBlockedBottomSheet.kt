package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusShieldBlockedBottomSheet(
    prompt: FocusShieldBlockPrompt,
    isEkagraTimerRunning: Boolean,
    onDismiss: () -> Unit,
    onQuickUnlock: (minutes: Int, pauseTimer: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blue = Color(0xFF0A56D9)
    var showQuickUnlockDialog by remember { mutableStateOf(false) }

    if (showQuickUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showQuickUnlockDialog = false },
            title = { Text("Quick unlock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Unlock ${prompt.appName} for a short break. Your Ekagra timer will pause automatically.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                showQuickUnlockDialog = false
                                onQuickUnlock(1, true)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("1 minute")
                        }
                        Button(
                            onClick = {
                                showQuickUnlockDialog = false
                                onQuickUnlock(5, true)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("5 minutes")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickUnlockDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = blue,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.72f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "${prompt.appName} is blocked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (prompt.alwaysOn) {
                    "Always On is working. Open KAVACH and turn it off when you want to use this app."
                } else if (prompt.strict) {
                    "Beast Mode is on. You cannot open this app until your study timer ends."
                } else {
                    "This app is blocked while you study. Tap Quick unlock if you need it for a few minutes."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.82f),
            )

            Spacer(Modifier.height(22.dp))

            if (!prompt.strict && !prompt.alwaysOn) {
                Button(
                    onClick = { showQuickUnlockDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = blue,
                    ),
                ) {
                    Text(
                        text = "Quick unlock",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
            ) {
                Text("I'll Control Myself.", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}
