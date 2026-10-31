package com.safarparmar.app.ui.nishtha

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.nishtha.journal.JournalMockupFont

/** Full-width, bottom-attached surface. Native navigation insets extend the plum
 * surface beneath the gesture area while keeping every tab safely above it. */
@Composable
internal fun AttachedNishthaNavigation(selected: NishthaTab, onSelect: (NishthaTab) -> Unit) {
    val plum = Color(0xFF542347)
    val blush = Color(0xFFFFF5F0)
    val mint = Color(0xFFC9EFE0)
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = plum,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            containerColor = plum,
            contentColor = blush,
            tonalElevation = 0.dp,
        ) {
            NishthaTab.entries.filter { it != NishthaTab.ANALYTICS }.forEach { tab ->
                val active = selected == tab
                NavigationBarItem(
                    selected = active,
                    onClick = {
                        if (!active) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onSelect(tab)
                    },
                    icon = {
                        Icon(if (active) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = null, modifier = Modifier.size(25.dp))
                    },
                    label = {
                        Text(stringResource(tab.labelRes), fontFamily = JournalMockupFont,
                            fontSize = 11.sp, lineHeight = 15.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = plum,
                        selectedTextColor = mint,
                        indicatorColor = mint,
                        unselectedIconColor = blush,
                        unselectedTextColor = blush,
                    ),
                )
            }
        }
    }
}
