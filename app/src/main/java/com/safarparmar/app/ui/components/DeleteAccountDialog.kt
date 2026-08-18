package com.safarparmar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.SafarSemanticColors

private val DestructiveRed = Color(0xFFDC2626)

@Composable
fun DeleteAccountDialog(
    userEmail: String,
    isDeleting: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmDelete: (password: String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SafarSemanticColors.plannerBackground())
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(18.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title
                Text(
                    text = "Delete Account",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextDark,
                )

                // Honest explanation paragraph with email highlight
                val textColor = PlannerFlatColors.TextDark
                val explanation = remember(userEmail, textColor) {
                    buildAnnotatedString {
                        append("This will permanently delete ")
                        if (userEmail.isNotBlank()) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = textColor)) {
                                append(userEmail)
                            }
                            append(" and ")
                        }
                        append("all associated study data including focus sessions, streaks, and study plans. This action cannot be undone.")
                    }
                }

                Text(
                    text = explanation,
                    fontSize = 13.5.sp,
                    lineHeight = 19.5.sp,
                    color = PlannerFlatColors.TextMuted,
                )

                // Password Confirmation Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Confirm password",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PlannerFlatColors.TextMuted,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isDeleting,
                        placeholder = {
                            Text(
                                "Your account password",
                                fontSize = 13.5.sp,
                                color = PlannerFlatColors.TextMuted.copy(alpha = 0.7f),
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (password.isNotBlank() && !isDeleting) {
                                    onConfirmDelete(password)
                                }
                            },
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = PlannerFlatColors.TextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DestructiveRed,
                            unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                            cursorColor = DestructiveRed,
                        ),
                    )
                }

                // Error message
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.5.sp,
                        color = DestructiveRed,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(10.dp))
                            .clickable(enabled = !isDeleting) { onDismiss() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextDark,
                        )
                    }

                    // Delete Action
                    val canSubmit = password.isNotBlank() && !isDeleting
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canSubmit) DestructiveRed else DestructiveRed.copy(alpha = 0.4f))
                            .clickable(enabled = canSubmit) { onConfirmDelete(password) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDeleting) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Deleting…",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        } else {
                            Text(
                                text = "Delete Account",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
