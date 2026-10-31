package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.adaptive.isSpaciousLandscape
import com.safarparmar.app.ui.adaptive.rememberSafarAdaptiveState
import com.safarparmar.app.ui.tutorial.tutorialTarget

/** Panels are actions over the current destination, but still need selected feedback. */
internal fun ekagraNavigationSelection(tab: EkagraNavTab, musicOpen: Boolean, themeOpen: Boolean): EkagraNavTab = when {
    themeOpen -> EkagraNavTab.THEME
    musicOpen -> EkagraNavTab.MUSIC
    else -> tab
}

@Composable
internal fun EkagraBottomNav(
    selectedTab: EkagraNavTab,
    onSelect: (EkagraNavTab) -> Unit,
    isOnVideo: Boolean,
    isDarkTheme: Boolean,
    selectedTheme: VisualTheme? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotionPolicy.current
    val haptic = LocalHapticFeedback.current
    val floating = rememberSafarAdaptiveState().isSpaciousLandscape
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    // Read the animated value only in the layer: frames do not recompose the destinations.
    val position = animateFloatAsState(
        targetValue = selectedTab.ordinal.toFloat(),
        animationSpec = if (motion.animationsEnabled) spring(dampingRatio = 1f, stiffness = 650f) else snap(),
        label = "ekagraNavIndicator",
    )
    val shape = RoundedCornerShape(22.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        val dockWidth = if (floating) ekagraTabletChromeWidth(maxWidth) else maxWidth
        Box(
            Modifier.width(dockWidth).padding(horizontal = if (floating) 0.dp else 8.dp, vertical = 6.dp)
                .clip(shape).background(scheme.surface.copy(alpha = 0.98f))
                .border(1.dp, scheme.outlineVariant, shape).padding(4.dp)
                .testTag("ekagra.navigation"),
        ) {
            // One highlight for all five actions, rather than five independent popping backgrounds.
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer {
                        translationX = direction * position.value * size.width / EkagraNavTab.entries.size
                    }
                    .fillMaxWidth(),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(1f / EkagraNavTab.entries.size)
                    .clip(RoundedCornerShape(17.dp)).background(scheme.primaryContainer))
            }
            Row(Modifier.fillMaxWidth().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                EkagraNavTab.entries.forEach { tab ->
                    val active = tab == selectedTab
                    val color = if (active) scheme.primary else scheme.onSurfaceVariant
                    Button(
                        onClick = {
                            if (tab != selectedTab) {
                                onSelect(tab)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        style = ButtonStyle.Ghost,
                        shape = RoundedCornerShape(17.dp),
                        contentColor = color,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 9.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                            .semantics { selected = active; role = Role.Tab }
                            .testTag("ekagra.nav.${tab.name}")
                            .then(if (tab == EkagraNavTab.DURATION) Modifier.tutorialTarget("ekagra.settingsEntry") else Modifier)
                            .then(if (tab == EkagraNavTab.MUSIC)
                                Modifier.tutorialTarget("ekagra.personalizeEntry") else Modifier),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
                            Text(stringResource(tab.labelRes), fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
