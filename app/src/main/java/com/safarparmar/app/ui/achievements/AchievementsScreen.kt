package com.safarparmar.app.ui.achievements

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.ui.components.*
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Achievement
import com.safarparmar.app.ui.theme.*

private data class AchievementPalette(val canvas: Color, val paper: Color, val ink: Color, val muted: Color,
    val plum: Color, val soft: Color, val peach: Color, val line: Color)

@Composable
private fun achievementPalette(): AchievementPalette {
    val light = MaterialTheme.colorScheme.background.isLightBackground()
    return if (light) AchievementPalette(Color(0xFFFFF5F0), Color(0xFFFFFDFC), Color(0xFF39132E), Color(0xFF75616F),
        Color(0xFF491438), Color(0xFFEDE0F4), Color(0xFFFFDFD5), Color(0xFFEADFE6))
    else AchievementPalette(Color(0xFF211921), Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB),
        Color(0xFFF0BEDC), Color(0xFF513551), Color(0xFF684047), Color(0xFF654D66))
}

@Composable
fun AchievementsScreen(
    achievements: List<Achievement>, selectedAchievementId: String = "",
    onSelectAchievement: (String?) -> Unit = {}, onBack: () -> Unit,
) {
    val c = achievementPalette()
    val scheme = MaterialTheme.colorScheme.copy(background = c.canvas, surface = c.paper, onSurface = c.ink,
        onBackground = c.ink, onSurfaceVariant = c.muted, primary = c.plum, outlineVariant = c.line,
        secondaryContainer = c.soft, onSecondaryContainer = c.ink)
    var selectedFilter by rememberSaveable { mutableStateOf("all") }
    val filters = listOf("all", "earned", "badge", "title")
    val filtered = remember(achievements, selectedFilter) {
        when (selectedFilter) {
            "earned" -> achievements.filter { it.earned }
            "badge" -> achievements.filter { it.type == "badge" }
            "title" -> achievements.filter { it.type == "title" }
            else -> achievements
        }
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSafarComposablesAccent provides c.plum) {
            SafarComposablesTheme {
                Column(Modifier.fillMaxSize().background(c.canvas).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    // Equal side slots keep the title at the actual horizontal center.
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = c.plum)
                        }
                        Text(stringResource(R.string.achievements_title), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            fontFamily = PoppinsFontFamily, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { com.safarparmar.app.ui.tutorial.TitliGuideButton() }
                    }
                    Column(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 900.dp).weight(1f)) {
                        val earned = achievements.count { it.earned }
                        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip(stringResource(R.string.achievements_earned_count, earned), c.soft, c.plum)
                            StatChip(stringResource(R.string.achievements_locked_count, achievements.size - earned), c.peach, c.ink)
                            StatChip(stringResource(R.string.achievements_total_count, achievements.size), c.paper, c.muted)
                        }
                        Tabs(selectedTab = selectedFilter, onSelectedTabChange = { selectedFilter = it }, orderedTabs = filters) {
                            TabList(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), dividerColor = c.line) {
                                Row(Modifier.fillMaxWidth()) {
                                    filters.forEach { filter ->
                                        Tab(key = filter, modifier = Modifier.weight(1f).heightIn(min = 48.dp), text = {
                                            Text(stringResource(when (filter) {
                                                "earned" -> R.string.achievements_filter_earned
                                                "badge" -> R.string.achievements_filter_badges
                                                "title" -> R.string.achievements_filter_titles
                                                else -> R.string.common_all
                                            }), fontSize = 12.sp)
                                        })
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.achievements_empty), color = c.muted, textAlign = TextAlign.Center)
                        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered, key = { it.id }) { achievement ->
                                AchievementCard(achievement, achievement.id == selectedAchievementId, onSelectAchievement, c)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, background: Color, ink: Color) {
    Text(label, modifier = Modifier.background(background, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ink)
}

@Composable
private fun AchievementCard(achievement: Achievement, selected: Boolean, onSelect: (String?) -> Unit, c: AchievementPalette) {
    val shape = RoundedCornerShape(22.dp)
    var artworkLoaded by remember(achievement.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(shape).background(c.paper)
        .border(if (selected) 2.dp else 1.dp, if (selected) c.plum else c.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(64.dp)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(if (achievement.earned) c.soft else c.peach), contentAlignment = Alignment.Center) {
                    if (!artworkLoaded) Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = c.plum, modifier = Modifier.size(30.dp))
                    AchievementImages.urlFor(achievement.id)?.let { url ->
                        AsyncImage(model = url, contentDescription = achievement.name, contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(6.dp), onSuccess = { artworkLoaded = true }, onError = { artworkLoaded = false })
                    }
                }
                AchievementLockState(achievement.earned, c, Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(achievement.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink, fontFamily = PoppinsFontFamily)
                achievement.rarity?.let { rarity ->
                    StatChip(rarity.replaceFirstChar { it.uppercase() }, if (rarity.equals("special", true)) c.peach else c.soft, c.plum)
                }
            }
        }
        achievement.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = c.ink, fontSize = 13.sp, lineHeight = 19.sp) }
        Text(achievement.requirement, color = c.muted, fontSize = 12.sp, lineHeight = 18.sp)
        if (!achievement.earned && achievement.targetValue > 0) {
            val target = (achievement.currentValue / achievement.targetValue).toFloat().coerceIn(0f, 1f)
            val progress by animateFloatAsState(target, tween(220), label = "Achievement progress")
            ProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), indicatorColor = c.plum, trackColor = c.soft, height = 5.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.achievements_locked), fontSize = 11.sp, color = c.muted, modifier = Modifier.weight(1f))
                Text("${achievement.currentValue.formatAchievementValue()} / ${achievement.targetValue.formatAchievementValue()}", fontSize = 11.sp, color = c.muted)
            }
        } else if (!achievement.earned) Text(stringResource(R.string.achievements_locked), color = c.muted, fontSize = 11.sp)
        if (achievement.earned) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.achievements_earned_mark), modifier = Modifier.weight(1f), color = c.plum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Button(onClick = { if (!selected) onSelect(achievement.id) }, enabled = !selected,
                    style = if (selected) ButtonStyle.Secondary else ButtonStyle.Outlined, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (selected) R.string.achievements_active else R.string.achievements_set_active), fontSize = 12.sp)
                }
            }
        }
    }
}

/** A finite state transition, with no looping animation or disabled overlay on the card. */
@Composable
private fun AchievementLockState(earned: Boolean, c: AchievementPalette, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = earned, modifier = modifier, label = "Achievement lock state",
        transitionSpec = { (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .85f)) togetherWith
            (fadeOut(tween(100)) + scaleOut(tween(100), targetScale = .9f)) }) { unlocked ->
        Box(Modifier.size(28.dp).background(if (unlocked) c.plum else c.peach, CircleShape)
            .border(2.dp, c.paper, CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (unlocked) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = stringResource(if (unlocked) R.string.achievements_earned_mark else R.string.achievements_locked),
                tint = if (unlocked) c.canvas else c.ink, modifier = Modifier.size(14.dp))
        }
    }
}

private fun Double.formatAchievementValue() = if (this % 1 == 0.0) toInt().toString() else toString()
