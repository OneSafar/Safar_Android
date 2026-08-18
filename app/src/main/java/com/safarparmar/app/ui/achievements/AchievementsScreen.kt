package com.safarparmar.app.ui.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.safarparmar.app.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.domain.model.Achievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
    selectedAchievementId: String = "",
    onSelectAchievement: (String?) -> Unit = {},
    onBack: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf("all") }
    val filters = listOf("all", "earned", "badge", "title")

    val filtered = when (selectedFilter) {
        "earned" -> achievements.filter { it.earned }
        "badge"  -> achievements.filter { it.type == "badge" }
        "title"  -> achievements.filter { it.type == "title" }
        else     -> achievements
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        com.safarparmar.app.ui.ekagra.focusshield.KavachCircularBackButton(onClick = onBack)
                    }
                },
                title = {
                    Column {
                        Text("Achievements & Titles", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val earned = achievements.count { it.earned }
            val total  = achievements.size

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatChip("$earned Earned", MaterialTheme.colorScheme.primary)
                StatChip("${total - earned} Locked", MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                StatChip("$total Total", MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
            }

            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp,
            ) {
                filters.forEachIndexed { i, f ->
                    Tab(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        text = { Text(f.replaceFirstChar { it.uppercase() }, fontSize = 13.sp) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No achievements currently",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(filtered) { index, achievement ->
                        StaggeredAchievementEntranceBox(index = index) {
                            AchievementCard(
                                achievement = achievement,
                                isSelected = achievement.id == selectedAchievementId,
                                onSelectAchievement = onSelectAchievement,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    isSelected: Boolean,
    onSelectAchievement: (String?) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val isEarned = achievement.earned

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            val imageUrl = AchievementImages.urlFor(achievement.id)
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = achievement.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(primary.copy(0.15f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(primary.copy(0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (achievement.type == "title") "👑" else "🏅",
                        fontSize = 24.sp,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        achievement.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                    )
                    if (achievement.rarity != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(rarityColor(achievement.rarity).copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(achievement.rarity.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = rarityColor(achievement.rarity))
                        }
                    }
                }
                achievement.description?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                }
                Text(achievement.requirement, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (!isEarned && achievement.targetValue > 0) {
                    Spacer(Modifier.height(2.dp))
                    val targetProgress = (achievement.currentValue / achievement.targetValue).toFloat().coerceIn(0f, 1f)
                    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "achievementProgress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.15f),
                    )
                    val formattedCurrent = if (achievement.currentValue % 1 == 0.0) achievement.currentValue.toInt().toString() else achievement.currentValue.toString()
                    val formattedTarget = if (achievement.targetValue % 1 == 0.0) achievement.targetValue.toInt().toString() else achievement.targetValue.toString()
                    Text("$formattedCurrent / $formattedTarget", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                }
                if (isEarned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("✓ Earned", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = primary)
                        AssistChip(
                            onClick = { if (!isSelected) onSelectAchievement(achievement.id) },
                            enabled = !isSelected,
                            label = { Text(if (isSelected) "Active" else "Set active", fontSize = 10.sp) },
                        )
                    }
                }
            }


        }

        if (!isEarned) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        }
    }
}

private fun rarityColor(rarity: String): Color = when (rarity.lowercase()) {
    "legendary" -> Amber400
    "epic"      -> Purple500
    "rare"      -> Blue500
    "special"   -> Emerald400
    else        -> Slate400
}

@Composable
private fun StaggeredAchievementEntranceBox(
    index: Int,
    content: @Composable () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else (18 + index * 10).dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300,
            delayMillis = index * 35,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "achievementStaggeredOffset",
    )
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 260,
            delayMillis = index * 35,
        ),
        label = "achievementStaggeredAlpha",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}
