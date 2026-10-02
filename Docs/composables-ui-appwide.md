# SAFAR Composables UI pass

For the current conversion inventory and manual QA checklist, see [composables-ui-migration-status.md](composables-ui-migration-status.md). The whole-app migration is still in progress.

SAFAR keeps Material 3 for existing screens while Composables UI supplies shared interactive controls. The root `SafarTheme` now installs `SafarComposablesTheme`, whose colors and shapes follow SAFAR's active light or dark palette. Composables UI's neutral default palette is no longer used for app controls. The tracker supplies its pink accent locally; the rest of the app uses SAFAR purple for library actions and switches.

The documented `composeunstyled` and interaction-capabilities helpers are explicit dependencies so controls retain the library's touch and pointer sizing behavior.

The shared drawer scaffold uses the Composables UI Toolbar and ghost icon buttons for its menu and back actions. This applies to the main feature screens using that scaffold. The Settings, Profile, Habits, Kavach analytics, Ekagra duration, Nishtha Goals, and Study Planner switches use the library where their behavior maps directly. The planner's shared dialog text actions and hairline separators use library controls. Other separators across feature screens use the library with their existing colors and thicknesses. Material 3's shared shapes were rounded to align with the library's controls.

Home's notification and video actions, Dhyan's live-session refresh action, and Dashboard's retry action also use the documented ghost button behavior. Their existing callbacks and visible labels remain.

Custom controls with feature-specific behavior remain in place, including the drawer's animated day/night switch, glass planner buttons, and Kavach/Mehfil/YouTube switches with dedicated color states. Forms that use Material's value-based text fields remain on Material 3 because Composables UI's current field API is state-based; changing them requires a separate state and keyboard behavior migration. Existing callbacks and data flows were preserved.

Sources: [theming](https://composables.com/ui/docs/theming), [buttons](https://composables.com/ui/docs/buttons), [switch](https://composables.com/ui/docs/switch), and [separators](https://composables.com/ui/docs/separators). The custom theme structure is adapted from the documented MIT-licensed theme source; its notice is in `SafarComposablesTheme.kt`.

Verification: `:app:compileQaDebugKotlin`, `:app:compileQaReleaseKotlin`, `:app:testQaDebugUnitTest` (396 tests), and `:app:assembleQaDebug` passed. Runtime appearance awaits the user's manual device test; no emulator was used.
