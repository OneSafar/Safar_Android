package com.safarparmar.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuItemStyle
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text

/** One consistent Composables UI overflow menu for app actions. */
data class SafarMenuAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun SafarOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: List<SafarMenuAction>,
    anchor: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        alignment = DropdownMenuAlignment.End,
        panel = {
            DropdownMenuPanel {
                actions.forEach { action ->
                    DropdownMenuItem(
                        onClick = {
                            onExpandedChange(false)
                            action.onClick()
                        },
                        enabled = action.enabled,
                        style = if (action.destructive) DropdownMenuItemStyle.Destructive else DropdownMenuItemStyle.Default,
                        leading = action.icon?.let { image ->
                            { Icon(imageVector = image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        },
                    ) { Text(action.label) }
                }
            }
        },
        anchor = anchor,
    )
}
