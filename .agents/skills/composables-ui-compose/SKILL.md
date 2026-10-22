---
name: composables-ui-compose
description: Upgrade a Jetpack Compose (Android / Kotlin Multiplatform) app's UI by pulling real, working code for buttons, dropdown menus, dialogs, bottom sheets, tabs, switches, sliders, text fields, toolbars, tooltips and other elements from the Composables UI library (composables.com/ui/docs). Use this skill whenever the user pastes a composables.com/ui link, mentions "Composables UI", "composables.com", or asks to improve, modernize, restyle or polish the UI of a Compose app with ready-made components, even if they only say things like "add a nice dropdown menu", "make my buttons look better", "add an overflow menu to my toolbar", or "copy the component from this URL". Also use it when the agent is about to hand-write a generic Compose component that Composables UI already provides.
---

# Composables UI for Jetpack Compose

Composables UI (https://composables.com/ui) is a shadcn-style component library for Compose. Every docs page (for example `/ui/docs/dropdown-menu`) contains a live demo, copy-pasteable example code, the full component source, and an API reference. This skill tells you how to take that code and land it correctly in the user's app.

The reason to follow the docs instead of writing components from memory: the library has its own theme system, its own `Text`/`Icon`/`Button` composables, and scoped APIs (e.g. `DropdownMenuItem` only exists inside `DropdownMenuPanel`). Guessing these produces code that does not compile or silently ignores the theme.

## Workflow

### 1. Understand the target project first

Before fetching anything, look at the user's project so the copied code fits:

- Find the module that holds the UI (usually `app/`), and read its `build.gradle.kts` (or `.gradle`). Note the Compose BOM / Compose version, Kotlin version, `minSdk`, and whether it is Android-only or Kotlin Multiplatform.
- Find the root composable (the one called from `setContent { ... }` or `App()`), because the theme wrapper goes there.
- Check whether the app uses Material 3 (`androidx.compose.material3`). Composables UI can coexist with it, but inside Composables UI panels/buttons use the Composables UI `Text`, `Icon`, `Button` (see "Pitfalls").
- Find the screen(s) the user wants to improve. If they were vague ("improve my UI"), pick the smallest concrete surface (one screen, one menu) rather than restyling everything.

### 2. Get the source of truth for the component(s)

Use the first option that works. Always fetch the *current* docs; versions and APIs change.

1. **Composables CLI** (best for agents; it is built for LLM use):
   ```bash
   npm install -g composables-cli      # once
   composables docs list
   composables docs search <query>
   composables docs get <slug>         # e.g. dropdown-menu, buttons, bottom-sheet
   ```
2. **Composables MCP server**, if already configured in the agent (`composables mcp install --client <client>` sets it up; clients: android-studio, antigravity, claude, codex, cursor, firebender, opencode, zed). Run that from the Gradle project root only if the user agrees to the config change.
3. **Fetch the docs page directly** at `https://composables.com/ui/docs/<slug>` (the URL the user pasted, or one from `references/components.md`). Read the whole page: example code, "Installation > Copy & Paste" source, and the API Reference tables.
4. **GitHub fallback**: https://github.com/composablehorizons/ui (component sources under `ui/src/commonMain/kotlin/com/composables/ui/components/`, demos under `demo/src/commonMain/kotlin/com/composables/ui/demo/examples/`).

If the user pasted a URL, that page is the primary source. Also fetch the linked pages for any other component the example uses (for instance the dropdown example uses `Button`, `Icon`, `Text`, and `Toolbar`).

### 3. Choose the integration mode

**Gradle dependency (default).** One line gives every component plus the theme, and the user gets upgrades for free:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.composables:ui:0.2.0")   // verify the latest version on the Installation page
}
```

Confirm the version on https://composables.com/ui/docs/installation before writing it; `0.2.0` is what the docs showed when this skill was written.

**Copy & paste sources.** Use only if the user asks for it, cannot add the dependency, or wants to modify component internals. Then:
- Copy the source file(s) from the page's "Copy & Paste" tab into the user's project under the same package (`com.composables.ui.components`) or a package of their choice, and fix the `package` line and every import that referenced the old package.
- Copy the supporting files the component imports (the dropdown source depends on `components/Utils.kt` for `focusRing`/`bouncyPress`, and on the theme file). Follow each import until it resolves.
- Keep the MIT license header at the top of every copied file. The license requires the copyright notice to be included in copies.
- The sources also depend on third-party artifacts (`com.composeunstyled.*`, the ripple / interaction-capabilities helpers, and Lucide icons). Read the page and the Icon docs for the exact Gradle coordinates rather than guessing them, and add them to the module.

### 4. Set up the theme (once per app)

Composables UI components read colors, shapes, shadows and touch/pointer sizing from `ComposablesTheme`. Without it they will crash or look wrong. Wrap the root:

```kotlin
import com.composables.ui.theme.ComposablesTheme

@Composable
fun App() {
    ComposablesTheme {
        // existing app content
    }
}
```

Dark mode is driven by `LocalColorScheme`. If the app already tracks dark theme, provide it:

```kotlin
CompositionLocalProvider(LocalColorScheme provides if (isSystemInDarkTheme()) ColorScheme.Dark else ColorScheme.Light) {
    ComposablesTheme { /* content */ }
}
```

Read `references/theming.md` for the token list before customizing any color or shape. Reach for tokens (`Theme[colors][primaryColor]`) instead of hard-coded colors, so the new components and the user's own code stay consistent.

### 5. Copy the example, then adapt it

Start from the page's example code verbatim, including its imports, then change only what the user's app needs:

- Replace demo labels, icons and state with the app's real ones. Hoist `expanded`/selected state to where the app keeps state (ViewModel, screen state) if that is the app's convention.
- Wire real `onClick` handlers in place of `expanded = false` placeholders (keep the close-on-click behavior unless intentionally changed).
- Keep meaningful `contentDescription` values on icon-only triggers (the docs example uses `"More options"`); use `null` only for purely decorative icons next to a text label.
- Match the app's existing naming, package structure and formatting.
- Reuse one component definition instead of pasting near-duplicate menus around the app; extract a small wrapper composable if the same menu appears in several places.

`references/dropdown-menu.md` holds a distilled version of the dropdown menu page (API and two working examples), which is the component the user's URL points to. Use it as a fast reference, but the live page wins if they differ.

### 6. Verify

- Sync/build: `./gradlew :app:compileDebugKotlin` (use the real module name). Fix unresolved references by re-reading the docs page, not by inventing APIs.
- If the build fails on `dropShadow` or another Compose API, the project's Compose version is older than the library needs; report this and suggest bumping the Compose BOM instead of removing the styling.
- If an emulator/preview is available, check light and dark mode, and that the menu opens from its anchor, closes on item click and on outside tap.
- Summarize for the user: which components were added, which files changed, the dependency line added, and anything they should test.

## Pitfalls that cause most failures

- **Scopes matter.** `DropdownMenuPanel` is only available inside `DropdownMenu`'s `panel = { ... }` lambda, and `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator` only inside `DropdownMenuPanel { ... }`. Writing them elsewhere will not resolve.
- **Import from the library, not Material.** Inside the dropdown panel, use `com.composables.ui.components.Text` and `Icon`. They pick up the panel's content color; Material's `Text`/`Icon` may not.
- **The anchor toggles state itself.** `DropdownMenu` only reports `onExpandedChange`; the trigger (`Button`, `IconButton`) must still flip `expanded` in its own `onClick`.
- **Use only documented parameters.** The API reference tables on each page list every parameter. Do not pass `containerColor`-style Material parameters; use the library's own (`style = ButtonStyle.Ghost`, `DropdownMenuItemStyle.Destructive`, `alignment = DropdownMenuAlignment.End`, etc.).
- **Value classes, not enums.** `DropdownMenuSide`, `DropdownMenuAlignment`, `DropdownMenuItemStyle`, `ButtonStyle` are value classes with companion constants; use them as shown (`DropdownMenuSide.Top`).
- **Icons come from Lucide** in the docs examples (`com.composables.icons.lucide.*`). If the app uses a different icon set, swap the `imageVector`, and only add the Lucide dependency if the user wants those exact icons.
- **Don't restyle the whole app in one pass.** Add the theme and the requested components, verify, then offer the next ones.

## Choosing a component

`references/components.md` maps common UI needs to the docs slug and URL (buttons, alert dialog, bottom sheet, checkbox, disclosure, dropdown menu, icon, navigation bar, progress, radio group, scrollbars, separators, slider, switch, tabs, text, text fields, toolbar, tooltip, tri-state checkbox). When the user asks for something not obviously listed, run `composables docs search <query>` before building it by hand.

## Response style

Tell the user what you changed in plain terms, note that the code came from the Composables UI docs (and which pages), and flag anything you could not verify (for example, a dependency version you could not confirm online).
