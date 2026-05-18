package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safar.app.R

@Composable
fun FocusShieldConsentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            color = scheme.surface,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                KavachConsentHeader(scheme = scheme)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.kavach_consent_description),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider(color = scheme.surfaceVariant)

                    KavachConsentInfoRow(
                        icon = {
                            Icon(
                                Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = scheme.outline,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = stringResource(R.string.kavach_consent_how_label),
                        body = stringResource(R.string.kavach_consent_how_body),
                        labelColor = scheme.onSurface,
                        bodyColor = scheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.secondaryContainer)
                            .border(
                                1.dp,
                                scheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.kavach_consent_privacy_label),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.6.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.kavach_consent_privacy_body),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = scheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surface)
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = scheme.surfaceVariant,
                    )
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.kavach_consent_agree),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                        )
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.kavach_consent_cancel),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = scheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KavachConsentHeader(scheme: androidx.compose.material3.ColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .height(128.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer.copy(alpha = 0.35f))
                    .border(1.dp, scheme.primaryContainer.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.kavach_consent_title),
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun KavachConsentInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    body: String,
    labelColor: Color,
    bodyColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 4.dp)) {
            icon()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = bodyColor,
            )
        }
    }
}

@Composable
fun NotificationConsentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Notifications", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Allow notifications so SAFAR can show:",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BulletItem("Timer progress")
                BulletItem("Active Kavach status")
                Text(
                    "Notifications are optional.\nKavach and study timers still work without them.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow Notifications", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun AccessibilityConsentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Accessibility Permission", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Kavach uses Accessibility only to notice when a selected app opens during a focus session.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Kavach can:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BulletItem("Notice opened app names")
                    BulletItem("Show a block screen for apps you selected")
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Kavach does not:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BulletItem("Read messages, passwords, photos, or typed text")
                    BulletItem("See what is inside other apps")
                    BulletItem("Change phone settings")
                }

                Text(
                    "This is optional. Without it, Kavach blocking stays off, and the rest of SAFAR continues to work.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("I understand, open settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}

@Composable
private fun BulletItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = text },
    ) {
        Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
