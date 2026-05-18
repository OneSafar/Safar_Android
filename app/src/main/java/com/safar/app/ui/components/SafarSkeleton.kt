package com.safar.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun rememberSkeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )
    return alpha
}

@Composable
fun SafarSkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    val alpha = rememberSkeletonAlpha()
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
}

@Composable
fun SafarSkeletonBar(
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val alpha = rememberSkeletonAlpha()
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
}

@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
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
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
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
    val alpha = rememberSkeletonAlpha()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
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
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SafarSkeletonBar(fraction = 0.35f, height = 12.dp)
        SafarSkeletonBar(fraction = 0.5f, height = 28.dp)
    }
}

@Composable
fun SyllabusRowSkeleton(modifier: Modifier = Modifier) {
    val alpha = rememberSkeletonAlpha()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
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
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        )
    }
}
