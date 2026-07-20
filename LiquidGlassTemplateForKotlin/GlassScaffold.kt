package com.example.liquidglass

/**
 * LIQUID GLASS — SCAFFOLD PIECES
 * ---------------------------------------------------------------------------
 * Screen-level components: floating top bar, floating bottom navigation,
 * a floating action button, a glass bottom sheet, a full-screen scrim
 * overlay, and a dialog card meant to be shown inside that overlay.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────
// TOP BAR — a floating glass pill instead of a docked opaque bar
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .liquidGlass(shape = RoundedCornerShape(24.dp), tintAlpha = 0.12f, elevation = 10.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GlassIconButton(icon = Icons.Rounded.ArrowBack, onClick = onBack)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

// ─────────────────────────────────────────────────────────────────────────
// BOTTOM NAVIGATION — floating glass pill bar with tab items
// ─────────────────────────────────────────────────────────────────────────

data class GlassNavItem(val label: String, val icon: ImageVector)

@Composable
fun GlassBottomNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .liquidGlass(shape = RoundedCornerShape(28.dp), tintAlpha = 0.14f, elevation = 14.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) GlassPalette.Cyan else Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// FLOATING ACTION BUTTON
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .liquidGlass(shape = CircleShape, surfaceTint = GlassPalette.Violet, tintAlpha = 0.5f, elevation = 20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// BOTTOM SHEET
// ─────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 24.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(
                        topStart = 32.dp, topEnd = 32.dp, bottomStart = 28.dp, bottomEnd = 28.dp,
                    ),
                    tintAlpha = 0.20f,
                    elevation = 24.dp,
                )
                .padding(20.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// FULL-SCREEN OVERLAY (scrim + optional centered content, e.g. a dialog)
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    dismissOnScrimClick: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .then(
                    if (dismissOnScrimClick) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismiss() }
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// DIALOG CARD — meant to be placed inside a GlassOverlay
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassDialogCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(28.dp), tintAlpha = 0.24f, elevation = 30.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* consume clicks so they don't fall through to the scrim */ }
            .padding(24.dp),
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = message, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            GlassButton(text = dismissText, onClick = onDismiss, style = GlassButtonStyle.Secondary)
            Spacer(Modifier.width(10.dp))
            GlassButton(text = confirmText, onClick = onConfirm, style = GlassButtonStyle.Primary)
        }
    }
}
