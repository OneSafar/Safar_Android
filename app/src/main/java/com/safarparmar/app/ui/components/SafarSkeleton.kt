package com.safarparmar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns a high-performance linear gradient Brush that sweeps diagonally across skeleton elements,
 * creating an authentic, professional shimmer loading animation.
 */
@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1400f,
    durationMillis: Int = 1200,
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isPlannerDark = runCatching { com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme.current }.getOrDefault(false)
    val isMaterialDark = MaterialTheme.colorScheme.background.run { (red + green + blue) / 3f < 0.5f }
    val isDark = (isPlannerDark == true) || isMaterialDark

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1E1E24),
            Color(0xFF2E2E38),
            Color(0xFF454554),
            Color(0xFF2E2E38),
            Color(0xFF1E1E24),
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9),
            Color(0xFFFFFFFF),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
        )
    }

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 450f, translateAnim - 450f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun SafarSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

@Composable
fun SafarSkeletonBar(
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SafarSkeletonBox(
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SafarSkeletonBar(fraction = 0.5f, height = 14.dp)
                SafarSkeletonBar(fraction = 0.35f, height = 12.dp)
            }
        }
        SafarSkeletonBar(height = 48.dp, cornerRadius = 12.dp)
        SafarSkeletonBar(fraction = 0.7f, height = 14.dp)
    }
}

@Composable
fun PlanCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SafarSkeletonBar(fraction = 0.4f, height = 20.dp)
        SafarSkeletonBar(height = 36.dp, cornerRadius = 12.dp)
        SafarSkeletonBar(fraction = 0.7f, height = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SafarSkeletonBar(modifier = Modifier.weight(1f), height = 8.dp, cornerRadius = 4.dp)
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
fun GoalRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SafarSkeletonBox(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SafarSkeletonBar(fraction = 0.65f, height = 16.dp)
            SafarSkeletonBar(fraction = 0.4f, height = 12.dp)
        }
    }
}

@Composable
fun StatCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SafarSkeletonBar(fraction = 0.35f, height = 12.dp)
        SafarSkeletonBar(fraction = 0.5f, height = 28.dp)
    }
}

@Composable
fun SyllabusRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SafarSkeletonBar(fraction = 0.55f, height = 16.dp)
            SafarSkeletonBar(fraction = 0.35f, height = 12.dp)
        }
        SafarSkeletonBox(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun StudyCircleSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SafarSkeletonBar(height = 110.dp, cornerRadius = 18.dp)
        SafarSkeletonBar(fraction = 0.4f, height = 18.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SafarSkeletonBar(modifier = Modifier.weight(1f), height = 90.dp, cornerRadius = 14.dp)
            SafarSkeletonBar(modifier = Modifier.weight(1f), height = 90.dp, cornerRadius = 14.dp)
        }
        SafarSkeletonBar(fraction = 0.5f, height = 18.dp)
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SafarSkeletonBox(modifier = Modifier.size(42.dp), shape = CircleShape)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SafarSkeletonBar(fraction = 0.6f, height = 14.dp)
                    SafarSkeletonBar(fraction = 0.4f, height = 12.dp)
                }
                SafarSkeletonBox(modifier = Modifier.size(24.dp), shape = RoundedCornerShape(6.dp))
            }
        }
    }
}

@Composable
fun NishthaAnalyticsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            SafarSkeletonBox(modifier = Modifier.size(120.dp), shape = CircleShape)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                SafarSkeletonBar(modifier = Modifier.weight(1f), height = 74.dp, cornerRadius = 14.dp)
            }
        }
        SafarSkeletonBar(height = 160.dp, cornerRadius = 16.dp)
    }
}

@Composable
fun MehfilRoomSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SafarSkeletonBox(modifier = Modifier.size(44.dp), shape = CircleShape)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SafarSkeletonBar(fraction = 0.55f, height = 15.dp)
                SafarSkeletonBar(fraction = 0.35f, height = 12.dp)
            }
            SafarSkeletonBox(modifier = Modifier.size(60.dp, 28.dp), shape = RoundedCornerShape(14.dp))
        }
        SafarSkeletonBar(fraction = 0.85f, height = 14.dp)
    }
}

@Composable
fun JournalEntrySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            SafarSkeletonBar(fraction = 0.4f, height = 14.dp)
            SafarSkeletonBar(fraction = 0.2f, height = 12.dp)
        }
        SafarSkeletonBar(fraction = 0.75f, height = 16.dp)
        SafarSkeletonBar(fraction = 0.9f, height = 14.dp)
    }
}

@Composable
fun AnnouncementRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafarSkeletonBox(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SafarSkeletonBar(fraction = 0.6f, height = 14.dp)
            SafarSkeletonBar(fraction = 0.85f, height = 12.dp)
        }
    }
}
