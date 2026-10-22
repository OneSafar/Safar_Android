# Composables UI component catalog

Base URL: `https://composables.com/ui/docs/<slug>`
CLI equivalent: `composables docs get <slug>`

This list reflects the docs sidebar at the time the skill was written. Run `composables docs list` to see if new components were added.

## Getting started pages

| Slug | Read it when |
| --- | --- |
| `overview` | You need the big picture of the library |
| `installation` | Adding the Gradle dependency or choosing copy-paste; check the latest version here |
| `components` | Browsing all components |
| `theming` | Setting up `ComposablesTheme`, tokens, dark mode (also summarized in `theming.md`) |
| `cli` | Using `composables docs / add / init / mcp` |
| `mcp` | Configuring the MCP server for the agent |

## Components

| UI need | Slug | Notes |
| --- | --- | --- |
| Buttons, icon buttons, ghost/other styles | `buttons` | `Button`, `IconButton`, `ButtonStyle.*` |
| Confirmation / destructive dialogs | `alert-dialog` | |
| Slide-up panels | `bottom-sheet` | |
| Checkboxes | `checkbox` | |
| Expand/collapse sections | `disclosure` | |
| Overflow menus, pickers, contextual actions | `dropdown-menu` | See `dropdown-menu.md` |
| Icons | `icon` | Also the place to confirm the Lucide icon dependency |
| Bottom/side navigation | `navigation-bar` | |
| Loading / progress | `progress` | Linear and circular indicators |
| Single-choice lists | `radio-group` | |
| Scrollbars | `scrollbars` | |
| Dividers | `separators` | |
| Sliders | `slider` | |
| On/off toggles | `switch` | |
| Tabbed content | `tabs` | |
| Text | `text` | Use the library `Text` inside its components |
| Inputs / forms | `text-fields` | |
| App bar with title and trailing actions | `toolbar` | Combines well with `dropdown-menu` |
| Hover/long-press hints | `tooltip` | |
| Checkbox with indeterminate state | `tri-state-checkbox` | |

## Mapping requests to components (examples)

- "Add a three-dot menu to my top bar" -> `toolbar` + `dropdown-menu` (overflow example)
- "Let the user pick light/dark/system" -> `dropdown-menu` (color scheme example) or `radio-group`
- "Nicer buttons" -> `buttons`
- "Confirm before deleting" -> `alert-dialog` (and `DropdownMenuItemStyle.Destructive` if it starts from a menu)
- "Settings screen with toggles" -> `switch`, `separators`, `text`
- "Filter by category" -> `tabs` or `dropdown-menu`

## Combination rule

Most examples use several components together (the dropdown demos use `Button`, `IconButton`, `Icon`, `Text`, `Toolbar`). Fetch every page the example imports from `com.composables.ui.components` so no API is guessed.
