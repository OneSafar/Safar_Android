package com.safarparmar.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import com.safarparmar.app.R
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.theme.SafarSemanticColors

private data class LanguageOption(
    val title: String,
    val subtitle: String,
    val tag: String
)

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    val activeTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
    val dialogTitle = if (activeTag == "hi") "भाषा चुनें" else "Select Language"
    
    val options = listOf(
        LanguageOption("English", "Default language", "en"),
        LanguageOption("हिन्दी (Hindi)", "हिन्दी भाषा", "hi"),
        LanguageOption("Hinglish", "Hindi in English script", "hi-Latn")
    )

    val dialogBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFE5E8EC)
    val brandPurple = SafarSemanticColors.brandPurple()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = dialogBg,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(brandPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = brandPurple,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = dialogTitle,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1C1D36),
                                    lineHeight = 24.sp
                                )

                            }
                        }

                        com.composables.ui.components.IconButton(
                            style = com.composables.ui.components.ButtonStyle.Ghost,
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).offset(x = 4.dp, y = (-4).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.common_close),
                                tint = if (isDark) Color(0xFFA0AAB0) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    // Options list
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.forEach { option ->
                            val isSelected = option.tag == activeTag
                            
                            val unselectedBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF3F5F7)
                            val selectedBg = if (isDark) brandPurple.copy(alpha = 0.15f) else brandPurple.copy(alpha = 0.08f)
                            
                            val animatedBg by animateColorAsState(
                                targetValue = if (isSelected) selectedBg else unselectedBg,
                                animationSpec = tween(200), label = ""
                            )
                            
                            val borderColor = if (isSelected) brandPurple else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(animatedBg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .clickable {
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(option.tag)
                                        )
                                        onDismiss()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    // Custom Radio Button
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) brandPurple else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 0.dp else 2.dp,
                                                color = if (isSelected) Color.Transparent else if (isDark) Color(0xFF6B7280) else Color(0xFF94A3B8),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = option.title,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) brandPurple else if (isDark) Color(0xFFE5E7EB) else Color(0xFF1E293B),
                                        )
                                        Text(
                                            text = option.subtitle,
                                            fontSize = 13.5.sp,
                                            color = if (isSelected) brandPurple.copy(alpha = 0.7f) else if (isDark) Color(0xFFA0AAB0) else Color(0xFF64748B),
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = brandPurple.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = "Active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandPurple,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
