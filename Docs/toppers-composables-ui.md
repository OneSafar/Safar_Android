# Parmar tracker: Composables UI integration

The Composables CLI 0.10.1 and its stdio MCP server are installed. The server was initialized and queried for component documentation. The tracker uses `com.composables:ui:0.2.0` after the phased SAFAR build upgrade.

## Build phases

1. Kotlin 2.4.0, KSP 2.3.12, Hilt 2.60.1, and Room 2.8.5.
2. Compose BOM 2026.06.01, with Material 3 selected by the BOM.
3. AGP 9.1.1, Gradle 9.3.1, and compile SDK 37. The existing target SDK remains 36. AGP's built-in Kotlin replaces `org.jetbrains.kotlin.android`.
4. Composables UI 0.2.0 and its theme, scoped to the Parmar tracker.

`ToppersBatchScreen.kt` supplies the Composables UI light or dark color scheme inside the tracker while retaining SAFAR's pink Material 3 palette. Library `HorizontalSeparator` is used in tracker sections. `ToppersBatchComponents.kt` uses the library ghost `Button` for text actions and retains the existing Material 3 wrappers for branded primary buttons, fields, and dialogs. This keeps existing accessibility labels, callbacks, and form layout intact. The earlier MIT source adaptations and notices are retained.

The subsequent [app-wide UI pass](composables-ui-appwide.md) replaced the neutral library theme with SAFAR color tokens and kept this tracker's pink accent as a local override.

The tracker remains Today, Lectures, and Progress. Calendar remains hidden. No emulator testing was performed, at the user's request.

Documentation consulted through CLI/MCP: [theming](https://composables.com/ui/docs/theming), [buttons](https://composables.com/ui/docs/buttons), [separators](https://composables.com/ui/docs/separators), and [navigation bar](https://composables.com/ui/docs/navigation-bar).

## Verification

- `:app:compileQaDebugKotlin` passed after each phase.
- `:app:testQaDebugUnitTest` passed: 396 tests, 0 failures, 0 skipped.
- `:app:assembleQaDebug` passed.
- `:app:assembleQaRelease` passed, including R8 shrinking and vital lint.
- Runtime behavior and device layouts await the user's manual testing.
