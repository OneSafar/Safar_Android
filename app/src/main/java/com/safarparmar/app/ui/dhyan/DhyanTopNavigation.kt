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

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
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
fun DhyanTopNavigation(selectedTab: DhyanTab, onTabSelected: (DhyanTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(DhyanFlatColors.PrimaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DhyanTab.entries.forEach { tab ->
            val active = selectedTab == tab
            com.composables.ui.components.Button(onClick = { onTabSelected(tab) },
                style = com.composables.ui.components.ButtonStyle.Ghost,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .background(if (active) DhyanFlatColors.Primary else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .semantics { selected = active; role = androidx.compose.ui.semantics.Role.Tab },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                contentColor = if (active) { if (MaterialTheme.colorScheme.background.isLightBackground()) Color.White else Color(0xFF39132E) } else DhyanFlatColors.Muted,
            ) {
                Text(stringResource(when (tab) {
                    DhyanTab.DHYAN -> R.string.module_dhyan
                    DhyanTab.COURSES -> R.string.dhyan_courses_label
                    DhyanTab.LIVE -> R.string.dhyan_live_sessions_label
                }), fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}
