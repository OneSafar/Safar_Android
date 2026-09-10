package com.safarparmar.app.ui.dhyan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.isLightBackground

enum class DhyanTab(val label: String) {
    DHYAN("Dhyan"),
    COURSES("Courses"),
    LIVE("Dhyan Live"),
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
        modifier = modifier,
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
                icon = Icons.Default.Spa,
                isSelected = selectedTab == DhyanTab.DHYAN,
                isDark = isDark,
                onClick = { onTabSelected(DhyanTab.DHYAN) },
            )
            DhyanTabItem(
                tab = DhyanTab.COURSES,
                icon = Icons.Default.School,
                isSelected = selectedTab == DhyanTab.COURSES,
                isDark = isDark,
                onClick = { onTabSelected(DhyanTab.COURSES) },
            )
            DhyanTabItem(
                tab = DhyanTab.LIVE,
                icon = Icons.Default.LiveTv,
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
    icon: ImageVector,
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
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = tab.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}
