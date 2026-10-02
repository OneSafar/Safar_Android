# Dropdown Menu (distilled from https://composables.com/ui/docs/dropdown-menu)

Anchored menus for selections, overflow actions, and contextual commands. Always confirm against the live page; this is a working snapshot (library `com.composables:ui:0.2.0`).

## Contents
- Structure
- API summary
- Example 1: selection menu with check marks
- Example 2: overflow menu in a toolbar
- Behavior notes

## Structure

```
DropdownMenu(expanded, onExpandedChange, ..., panel = { ... }) { anchor }
 └─ panel scope (DropdownMenuScope)
     └─ DropdownMenuPanel { ... }                    (DropdownMenuPanelContentScope)
         ├─ DropdownMenuLabel { Text("Group") }      non-interactive heading
         ├─ DropdownMenuItem(onClick, leading, trailing, style, ...) { Text("Item") }
         └─ DropdownMenuSeparator()
 └─ anchor: the trigger composable (Button / IconButton) that flips `expanded`
```

## API summary

**DropdownMenu**
`expanded: Boolean`, `onExpandedChange: (Boolean) -> Unit`, `modifier`, `side: DropdownMenuSide = Bottom`, `alignment: DropdownMenuAlignment = Start`, `sideOffset: Dp = 4.dp`, `alignmentOffset: Dp = 0.dp`, `panel: @Composable DropdownMenuScope.() -> Unit`, `anchor: @Composable () -> Unit`

**DropdownMenuPanel** (call inside `panel`)
`modifier`, `shape` (theme `menuShape`), `backgroundColor` (theme `panelColor`), `contentColor` (theme `onPanelColor`), `shadow` (theme `raisedShadow`), `minWidth = 160.dp`, `maxWidth = 320.dp`, `enter: EnterTransition?`, `exit: ExitTransition?`, `content`

**DropdownMenuItem** (call inside `DropdownMenuPanel`)
`onClick: () -> Unit`, `modifier`, `enabled = true`, `closeOnClick = true`, `style: DropdownMenuItemStyle = Default`, `leading: (@Composable () -> Unit)?`, `trailing: (@Composable () -> Unit)?`, `content: @Composable RowScope.() -> Unit`

**DropdownMenuLabel** (inside panel): `modifier`, `contentColor` (muted), `content: RowScope`

**DropdownMenuSeparator** (inside panel): `modifier`

**Value classes**
- `DropdownMenuSide`: `Top`, `Bottom`, `Start`, `End`
- `DropdownMenuAlignment`: `Start`, `Center`, `End`
- `DropdownMenuItemStyle`: `Default`, `Destructive`

## Example 1: selection menu with check marks

A ghost button shows the current value and opens a menu; the selected row shows a check in the leading slot, others reserve the same 16dp with a `Spacer` so labels stay aligned.

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronsUpDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Sun
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text

@Composable
fun DropdownMenuExample() {
  var expanded by remember { mutableStateOf(false) }
  var selectedColorScheme by remember { mutableStateOf("Light") }

  DropdownMenu(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    alignment = DropdownMenuAlignment.End,
    panel = {
      DropdownMenuPanel {
        DropdownMenuItem(
          onClick = {
            selectedColorScheme = "System"
            expanded = false
          },
          leading = {
            if (selectedColorScheme == "System") {
              Icon(imageVector = Lucide.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            } else {
              Spacer(Modifier.width(16.dp))
            }
          },
          trailing = {
            Icon(imageVector = Lucide.Monitor, contentDescription = null, modifier = Modifier.size(16.dp))
          },
        ) {
          Text("System")
        }
        // "Dark" (Lucide.Moon) and "Light" (Lucide.Sun) items repeat the same pattern.
      }
    },
  ) {
    Button(
      onClick = { expanded = expanded.not() },
      modifier = Modifier.fillMaxWidth(),
      style = ButtonStyle.Ghost,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(imageVector = Lucide.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Color scheme")
        Spacer(Modifier.weight(1f))
        Text(selectedColorScheme)
        Icon(imageVector = Lucide.ChevronsUpDown, contentDescription = null, modifier = Modifier.size(16.dp))
      }
    }
  }
}
```

For a real app, generate the items from a list instead of repeating them (`options.forEach { option -> DropdownMenuItem(...) }`), which is safe because items are ordinary composables.

## Example 2: overflow menu in a toolbar

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.Trash2
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuItemStyle
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.Toolbar

@Composable
fun DropdownMenuToolbarExample() {
  var expanded by remember { mutableStateOf(false) }

  Toolbar(
    modifier = Modifier.fillMaxWidth(),
    title = { Text("Details") },
    trailing = {
      DropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        alignment = DropdownMenuAlignment.End,
        panel = {
          DropdownMenuPanel {
            DropdownMenuItem(
              onClick = { expanded = false },
              leading = { Icon(imageVector = Lucide.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) { Text("Share") }
            DropdownMenuItem(
              onClick = { expanded = false },
              leading = { Icon(imageVector = Lucide.Heart, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) { Text("Add to favorites") }
            DropdownMenuItem(
              onClick = { expanded = false },
              leading = { Icon(imageVector = Lucide.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) { Text("Copy link") }
            DropdownMenuItem(
              onClick = { expanded = false },
              style = DropdownMenuItemStyle.Destructive,
              leading = { Icon(imageVector = Lucide.Trash2, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) { Text("Delete") }
          }
        },
      ) {
        IconButton(onClick = { expanded = expanded.not() }, style = ButtonStyle.Ghost) {
          Icon(
            imageVector = Lucide.EllipsisVertical,
            contentDescription = "More options",
            modifier = Modifier.size(18.dp),
          )
        }
      }
    },
  )
}
```

## Behavior notes

- Menu item minimum height adapts to input: 48dp on touch, 36dp on pointer devices (from `LocalInteractionMode`). No manual sizing is needed.
- Items highlight on hover, focus, and press using the theme's `secondaryColor`; disabled items use the theme's `disabledAlpha`.
- `Destructive` items use `destructiveColor` for their text and icon; `trailing` content is drawn in `mutedColor`.
- The panel animates in (scale from 0.96 plus fade) and out automatically; pass `enter`/`exit` only to override.
- Panel width is content-based between 160dp and 320dp; override with `minWidth`/`maxWidth` on `DropdownMenuPanel`.
- Use `side = DropdownMenuSide.Top` for triggers near the bottom of the screen, and `alignment = End` for triggers on the right edge (as in the toolbar example).
- Set `closeOnClick = false` on an item for multi-select behavior (checkable items that keep the menu open).
