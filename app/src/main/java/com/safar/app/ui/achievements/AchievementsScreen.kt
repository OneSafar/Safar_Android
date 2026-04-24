package com.safar.app.ui.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.safar.app.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safar.app.BuildConfig
import com.safar.app.domain.model.Achievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
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
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered) { achievement ->
                    AchievementCard(achievement)
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

private val achievementImages: Map<String, String> = mapOf(
    "G001" to "/Achievments/Badges/Badge (1).webp",
    "G002" to "/Achievments/Badges/Badge (2).webp",
    "G003" to "/Achievments/Badges/Badge (3).webp",
    "G004" to "/Achievments/Badges/Badge (4).webp",
    "F001" to "/Achievments/Badges/Special_Badge (2).webp",
    "F002" to "/Achievments/Badges/Special_Badge (5).webp",
    "F003" to "/Achievments/Badges/Special_Badge (4).webp",
    "F004" to "/Achievments/Badges/Badge (6).webp",
    "F005" to "/Achievments/Badges/Badge (7).webp",
    "S001" to "/Achievments/Badges/Badge (8).webp",
    "S002" to "/Achievments/Badges/Special_Badge (1).webp",
    "ET006" to "/Achievments/Badges/Special_Badge (3).webp",
    "T005" to "/Achievments/Titles/Title (5).webp",
    "T006" to "/Achievments/Titles/Title (3).webp",
    "T007" to "/Achievments/Titles/Title (7).webp",
    "T008" to "/Achievments/Titles/Title (6).webp",
    "T001" to "/Achievments/Titles/Title (8).webp",
    "T002" to "/Achievments/Titles/Title (2).webp",
    "T003" to "/Achievments/Titles/Title (1).webp",
    "T004" to "/Achievments/Titles/Title (4).webp",
    "ET001" to "/Achievments/Titles/Special_Title (2).webp",
    "ET002" to "/Achievments/Titles/Special_Title (1).webp",
    "ET003" to "/Achievments/Titles/Special_Title (5).webp",
    "ET004" to "/Achievments/Titles/Special_Title (3).webp",
    "ET005" to "/Achievments/Titles/Special_Title (4).webp",
    "T009" to "/Achievments/svgviewer-output.svg",
)

@Composable
private fun AchievementCard(achievement: Achievement) {
    val primary = MaterialTheme.colorScheme.primary
    val isEarned = achievement.earned

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imagePath = achievementImages[achievement.id]
            if (imagePath != null) {
                AsyncImage(
                    model = run {
                        val origin = BuildConfig.BASE_URL.trimEnd('/').let {
                            val uri = android.net.Uri.parse(it)
                            "${uri.scheme}://${uri.host}"
                        }
                        "$origin$imagePath"
                    },
                    contentDescription = achievement.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isEarned) primary.copy(0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isEarned) primary.copy(0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (achievement.type == "title") "👑" else "🏅",
                        fontSize = 24.sp,
                        color = if (!isEarned) Color.Unspecified.copy(alpha = 0.4f) else Color.Unspecified,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        achievement.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    LinearProgressIndicator(
                        progress = { (achievement.currentValue.toFloat() / achievement.targetValue).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.15f),
                    )
                    Text("${achievement.currentValue} / ${achievement.targetValue}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                }
                if (isEarned) {
                    Text("✓ Earned", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = primary)
                }
            }

            if (achievement.holderCount > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${achievement.holderCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("holders", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
