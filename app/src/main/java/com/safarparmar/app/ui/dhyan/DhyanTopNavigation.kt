package com.safarparmar.app.ui.dhyan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.composables.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.isLightBackground

enum class DhyanTab {
    DHYAN,
    COURSES,
    LIVE,
}

@Composable
fun DhyanTopNavigation(
    selectedTab: DhyanTab,
    onTabSelected: (DhyanTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val isDark = !isLight

    val containerBg = DhyanFlatColors.PrimaryContainer.copy(alpha = if (isDark) 0.92f else 0.85f)
    val borderColor = DhyanFlatColors.BorderHairline

    Box(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(containerBg)
                .border(width = 0.5.dp, color = borderColor, shape = CircleShape)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DhyanTabItem(
                tab = DhyanTab.DHYAN,
                isSelected = selectedTab == DhyanTab.DHYAN,
                isDark = isDark,
                onClick = { onTabSelected(DhyanTab.DHYAN) },
            )
            DhyanTabItem(
                tab = DhyanTab.COURSES,
                isSelected = selectedTab == DhyanTab.COURSES,
                isDark = isDark,
                onClick = { onTabSelected(DhyanTab.COURSES) },
            )
            DhyanTabItem(
                tab = DhyanTab.LIVE,
                isSelected = selectedTab == DhyanTab.LIVE,
                isDark = isDark,
                onClick = { onTabSelected(DhyanTab.LIVE) },
            )
        }
    }
}

@Composable
private fun DhyanTabItem(
    tab: DhyanTab,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val activeBg = DhyanFlatColors.Primary
    val activeText = if (isDark) Color(0xFF27141E) else Color.White
    val inactiveText = DhyanFlatColors.Muted

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeBg else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tab_bg_${tab.name}",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) activeText else inactiveText,
        animationSpec = tween(durationMillis = 200),
        label = "tab_text_${tab.name}",
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(
                when (tab) {
                    DhyanTab.DHYAN -> R.string.dhyan_deep_breaths
                    DhyanTab.COURSES -> R.string.dhyan_courses_label
                    DhyanTab.LIVE -> R.string.dhyan_live_sessions_label
                }
            ),
            fontSize = 14.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}
