package com.safarparmar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared clock, with animation reads deferred until drawing. */
@Composable
private fun Modifier.skeletonFill(): Modifier {
    val phase = com.safarparmar.app.performance.rememberDecorationPhase(1200)
    val enabled = com.safarparmar.app.performance.decorativeMotionEnabled()
    val colorScheme = MaterialTheme.colorScheme
    val colors = androidx.compose.runtime.remember(colorScheme) {
        listOf(colorScheme.surfaceContainerHighest, colorScheme.surfaceContainerHigh, colorScheme.surfaceContainerHighest)
    }
    return this.then(Modifier.drawBehind {
        if (!enabled) drawRect(colors.first()) else {
            val position = phase() * (size.width + size.height + 450f)
            drawRect(Brush.linearGradient(colors, Offset(position - 450f, 0f), Offset(position, size.height)))
        }
    })
}

@Composable
fun SafarSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .skeletonFill(),
    )
}

@Composable
fun SafarSkeletonBar(
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .skeletonFill(),
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
