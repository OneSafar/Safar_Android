package com.example.liquidglass

/**
 * LIQUID GLASS — DEMO SCREEN
 * ---------------------------------------------------------------------------
 * A single screen that exercises every piece of the system: backdrop, top
 * bar, labels, search field, chips, a card with buttons + a switch, a list
 * of glass list items with badges, a FAB, a bottom sheet, and an overlay
 * with a dialog card.
 *
 * Use this file as a map of "how the pieces fit together" — copy whichever
 * parts you need into your own screens.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassDemoScreen() {
    var sheetVisible by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }
    var switchOn by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Drifting blurred background
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize())

        // 2. Screen content
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Liquid Glass",
                actions = {
                    GlassIconButton(icon = Icons.Rounded.Notifications, onClick = { dialogVisible = true })
                },
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column {
                        GlassLabel("Good evening", variant = GlassLabelVariant.Subtitle)
                        GlassLabel("Design system preview", variant = GlassLabelVariant.Title)
                    }
                }

                item {
                    GlassTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = "Search",
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        },
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("All", "Cards", "Lists", "Buttons").forEachIndexed { index, label ->
                            GlassChip(text = label, selected = index == selectedChip, onClick = { selectedChip = index })
                        }
                    }
                }

                item {
                    GlassCard {
                        GlassLabel("Buttons", variant = GlassLabelVariant.Subtitle)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton(text = "Primary", onClick = {}, style = GlassButtonStyle.Primary)
                            GlassButton(text = "Secondary", onClick = {}, style = GlassButtonStyle.Secondary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            GlassLabel("Enable notifications", variant = GlassLabelVariant.Body)
                            GlassSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
                        }
                    }
                }

                item {
                    GlassLabel("Recent activity", variant = GlassLabelVariant.Subtitle)
                }

                items(sampleListItems) { entry ->
                    GlassListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .liquidGlass(shape = CircleShape, surfaceTint = entry.color, tintAlpha = 0.5f, elevation = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(entry.icon, contentDescription = null, tint = Color.White)
                            }
                        },
                        trailing = { GlassBadge(count = entry.badge) },
                        onClick = { sheetVisible = true },
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // 3. Floating action button
        GlassFAB(
            icon = Icons.Rounded.Add,
            onClick = { sheetVisible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
        )

        // 4. Bottom sheet
        if (sheetVisible) {
            GlassBottomSheet(onDismissRequest = { sheetVisible = false }, sheetState = sheetState) {
                GlassLabel("Quick actions", variant = GlassLabelVariant.Title)
                Spacer(Modifier.height(16.dp))
                GlassListItem(
                    title = "Share",
                    leading = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White) },
                )
                Spacer(Modifier.height(8.dp))
                GlassDivider()
                Spacer(Modifier.height(8.dp))
                GlassListItem(
                    title = "Archive",
                    leading = { Icon(Icons.Rounded.Archive, contentDescription = null, tint = Color.White) },
                )
                Spacer(Modifier.height(16.dp))
                GlassButton(
                    text = "Close",
                    onClick = { sheetVisible = false },
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 5. Full-screen overlay + dialog card
        GlassOverlay(visible = dialogVisible, onDismiss = { dialogVisible = false }) {
            GlassDialogCard(
                title = "New update available",
                message = "A fresh coat of glass has been applied to your app. Reload to see the changes.",
                confirmText = "Reload",
                dismissText = "Later",
                onConfirm = { dialogVisible = false },
                onDismiss = { dialogVisible = false },
            )
        }
    }
}

private data class DemoEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val badge: Int,
)

private val sampleListItems = listOf(
    DemoEntry("Design review", "Today · 3:00 PM", Icons.Rounded.Palette, GlassPalette.Violet, 2),
    DemoEntry("Team sync", "Tomorrow · 10:00 AM", Icons.Rounded.Groups, GlassPalette.Cyan, 0),
    DemoEntry("Ship v2.0", "Friday · 5:00 PM", Icons.Rounded.RocketLaunch, GlassPalette.Coral, 5),
)

/**
 * Example entry point. Point your own Activity's setContent at
 * LiquidGlassDemoScreen() (or better, at your own screen composables built
 * from the pieces above) wrapped in LiquidGlassTheme.
 */
class GlassDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiquidGlassTheme {
                LiquidGlassDemoScreen()
            }
        }
    }
}
