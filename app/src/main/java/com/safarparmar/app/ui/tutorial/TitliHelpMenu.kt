package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon as UiIcon
import com.composables.ui.components.Text as UiText
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.popups.SafarDialog

/** A bounded feature directory: native app surfaces, readable rows, always-visible support actions. */
@Composable
internal fun TitliHelpMenu(name: String?, onFeature: (TitliFeature) -> Unit, onQueries: () -> Unit, onDismiss: () -> Unit) {
    TutorialVisualStyle { TitliHelpMenuContent(name, onFeature, onQueries, onDismiss) }
}

@Composable
private fun TitliHelpMenuContent(name: String?, onFeature: (TitliFeature) -> Unit, onQueries: () -> Unit, onDismiss: () -> Unit) {
    val displayName = name?.trim()?.takeIf { it.isNotBlank() }
    val scheme = MaterialTheme.colorScheme
    SafarDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp), contentAlignment = Alignment.Center) {
            val compactHeight = maxHeight < 420.dp
            Surface(Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight * 0.9f),
                shape = RoundedCornerShape(32.dp), color = scheme.background, contentColor = scheme.onSurface) {
                Column(Modifier.then(if (compactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth().drawBehind {
                        drawCircle(scheme.primaryContainer.copy(alpha = 0.35f), 40.dp.toPx(), Offset(size.width * 0.92f, 5.dp.toPx()))
                    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(56.dp).background(scheme.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                            TitliIcon(Modifier.size(52.dp), animate = true)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.titli_name), fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.titli_menu_tutorials), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("titli.menu.close"),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = scheme.primaryContainer.copy(alpha = 0.5f), contentColor = scheme.onSurface)) {
                            Icon(Icons.Default.Close, stringResource(R.string.common_close))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (displayName == null) stringResource(R.string.titli_greeting) else stringResource(R.string.titli_greeting_name, displayName),
                            fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.titli_menu_choose), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    Column(Modifier.then(if (compactHeight) Modifier else Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TitliFeature.entries.forEach { feature ->
                            val (label, icon) = feature.menuPresentation()
                            UiButton(onClick = { onFeature(feature) }, style = ButtonStyle.Ghost,
                                shape = RoundedCornerShape(24.dp), contentColor = scheme.onSurface,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                                    .background(scheme.surface, RoundedCornerShape(24.dp)).testTag("titli.menu.${feature.name}")) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(44.dp).background(scheme.primaryContainer, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                        UiIcon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(23.dp))
                                    }
                                    UiText(stringResource(label), color = scheme.onSurface, fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    UiIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = scheme.primaryContainer)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UiButton(onClick = onQueries, style = ButtonStyle.Ghost, shape = RoundedCornerShape(20.dp), contentColor = scheme.onPrimary,
                            modifier = Modifier.weight(0.35f).heightIn(min = 64.dp).background(scheme.primary, RoundedCornerShape(20.dp)).testTag("titli.menu.other"), contentPadding = PaddingValues(12.dp)) {
                            UiIcon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                            UiText(stringResource(R.string.titli_other), color = scheme.onPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                        UiButton(onClick = onQueries, style = ButtonStyle.Ghost, shape = RoundedCornerShape(20.dp), contentColor = scheme.onSecondaryContainer,
                            modifier = Modifier.weight(0.65f).heightIn(min = 64.dp).background(scheme.secondaryContainer, RoundedCornerShape(20.dp)).testTag("titli.menu.queries"), contentPadding = PaddingValues(12.dp)) {
                            UiIcon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            UiText(stringResource(R.string.titli_your_queries), color = scheme.onSecondaryContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun TitliFeature.menuPresentation(): Pair<Int, ImageVector> = when (this) {
    TitliFeature.EKAGRA -> R.string.titli_help_ekagra to Icons.Default.Timer
    TitliFeature.GOALS -> R.string.titli_help_goals to Icons.Default.Flag
    TitliFeature.YOUTUBE -> R.string.titli_help_youtube to Icons.Default.PlayCircle
    TitliFeature.HABITS -> R.string.titli_help_habits to Icons.Default.CheckCircle
    TitliFeature.TOPPERS_BATCH -> R.string.practice_batch to Icons.Default.School
    TitliFeature.MEHFIL -> R.string.practice_mehfil to Icons.Default.Forum
    TitliFeature.DHYAN -> R.string.practice_dhyan to Icons.Default.SelfImprovement
    TitliFeature.STUDY_CIRCLE -> R.string.practice_next_circle to Icons.Default.Groups
    TitliFeature.CHECK_IN -> R.string.practice_next_checkin to Icons.Default.Mood
    TitliFeature.JOURNAL -> R.string.practice_next_journal to Icons.AutoMirrored.Filled.MenuBook
    TitliFeature.STREAKS -> R.string.practice_next_streaks to Icons.Default.LocalFireDepartment
    TitliFeature.ANALYTICS -> R.string.practice_next_analytics to Icons.Default.BarChart
    TitliFeature.EXAM_PLANNER -> R.string.practice_next_planner to Icons.Default.CalendarMonth
}
