# Composables UI theming (distilled from https://composables.com/ui/docs/theming)

Read this before customizing colors, shapes or dark mode. Confirm against the live page if something does not compile.

## Setup

```kotlin
import com.composables.ui.theme.ComposablesTheme

@Composable
fun App() {
    ComposablesTheme {
        // app content
    }
}
```

`ComposablesTheme` also decides `InteractionMode` (Touch vs Pointer) from device capabilities, which drives component sizing: bigger/rounder on touch (for example buttons are pill-shaped, body text 17sp), tighter on pointer devices (10dp button corners, 15sp text).

Dark mode: provide `LocalColorScheme` (`ColorScheme.Light` or `ColorScheme.Dark`; default is Light). Colors animate between schemes.

## Reading tokens

```kotlin
import com.composeunstyled.theme.Theme
import com.composables.ui.theme.colors
import com.composables.ui.theme.primaryColor

Box(Modifier.background(Theme[colors][primaryColor]))
```

Pattern: `Theme[property][token]`. Only works inside `ComposablesTheme`.

## Properties and tokens

**colors** (import each token from `com.composables.ui.theme`)

| Token | Use |
| --- | --- |
| `backgroundColor` / `onBackgroundColor` | Screen background and content on it |
| `panelColor` / `onPanelColor` | Cards, dialogs, sheets, menus and content on them |
| `mutedColor` | Lower-emphasis text and icons |
| `primaryColor` / `onPrimaryColor` | Primary actions |
| `secondaryColor` / `onSecondaryColor` | Secondary actions, hover highlight |
| `controlColor` / `onControlColor` | Control backgrounds (progress track) |
| `thumbColor` | Slider/toggle thumb |
| `switchTrackColor`, `switchSelectedTrackColor`, `switchThumbColor` | Switch parts |
| `selectedControlColor` / `onSelectedControlColor` | Selected controls |
| `destructiveColor` / `onDestructiveColor` | Deletion and danger actions |
| `borderColor` | Borders and separators |
| `fieldColor` / `onFieldColor` | Text field background and content |
| `scrimColor` | Scrim behind modals |
| `ringColor` | Focus ring |

**shapes**: `smallShape` (6dp), `mediumShape` (12dp), `largeShape` (16dp), `buttonShape`, `dialogShape` (16dp), `sheetShape` (24dp top corners), `menuShape` (16dp), `fieldShape`.

**shadows**: `raisedShadow`, `overlayShadow`.

**alphas**: `disabledAlpha` (0.33).

**indications**: `defaultIndication`, `inverseIndication` (ripples).

**textSelectionColors**: `defaultTextSelectionColors`.

## Default palette (for matching the app's existing look)

Light: background `#F4F4F4`, panel `#FFFFFF`, primary `#000000`, muted `#777777`, border `#E0E0E0`, destructive `#DC2626`.
Dark: background `#0A0A0A`, panel `#171717`, primary `#F5F5F5`, muted `#A3A3A3`, border `#404040`, destructive `#F87171`.

The look is neutral black/white. If the user wants brand colors, the cleanest route is a custom theme built with the same structure as `ComposablesTheme` (copy its `buildTheme { ... }` definition from the theming page's Copy & Paste tab and change the color values), rather than overriding colors at every call site.

## Compatibility with Material 3

Wrapping the app in `ComposablesTheme` does not remove `MaterialTheme`. Nest them if the app still has Material screens:

```kotlin
MaterialTheme(/* existing */) {
    ComposablesTheme {
        // screens using Composables UI components
    }
}
```

Use one system's components per surface; do not put Material `Text` inside a Composables UI panel expecting theme colors.
