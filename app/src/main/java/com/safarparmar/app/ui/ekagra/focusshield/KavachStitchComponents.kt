package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.glassSurface

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun StudyPlannerCircularBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.nav_previous),
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFFC2410C) else Color(0xFFE0654B)

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun KavachCircularBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.nav_previous),
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF3B0764) else Color(0xFF581C87)

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachStitchBackHeader(
    onBack: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    backTint: Color = Color.White,
) {
    if (title == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            KavachCircularBackButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    } else {
        TopAppBar(
            modifier = modifier.statusBarsPadding(),
            title = {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.TextMain,
                )
            },
            navigationIcon = {
                KavachCircularBackButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = KavachDesign.Background),
        )
    }
}

/** Primary CTA — macOS translucent coloured glass (GlassButton recipe). */
@Composable
fun KavachStitchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = KavachDesign.isDark
    GlassButton(
        onClick = onClick,
        accentColor = KavachDesign.Primary,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        isDarkTheme = isDark,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.65f),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Secondary / dismiss CTA — neutral macOS glass panel. */
@Composable
fun KavachStitchSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = KavachDesign.isDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .glassSurface(shape = RoundedCornerShape(16.dp), isDarkTheme = isDark)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = KavachDesign.HubTextMuted.copy(alpha = if (enabled) 1f else 0.5f),
        )
    }
}

/** Compact allow CTA used in permission rows. */
@Composable
fun KavachStitchAllowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = KavachDesign.isDark
    GlassButton(
        onClick = onClick,
        accentColor = KavachDesign.Primary,
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(999.dp),
        isDarkTheme = isDark,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun KavachStitchStatusChip(
    granted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (granted) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KavachDesign.SuccessBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = KavachDesign.SuccessText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.kavach_permission_granted),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = KavachDesign.SuccessText,
            )
        }
    } else {
        Text(
            text = stringResource(R.string.kavach_permission_required),
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KavachDesign.Primary)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
fun KavachStitchPillToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 2f,
        label = "pill_thumb",
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) KavachDesign.Primary else KavachDesign.SearchFieldBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color.Black.copy(alpha = 0.04f), CircleShape),
        )
    }
}

@Composable
fun KavachStitchSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(KavachDesign.Surface),
    ) {
        content()
    }
}
