# Compact Profile implementation — V4

Implemented the selected phone, tablet portrait and tablet landscape layouts in **Safar_Android_v4**. V5 was not modified in this task.

## Layout

`ProfileCompactLayout.kt` owns the scoped blush, plum, peach, mint and dark-mode tokens, native membership illustration and available-width layout:

- Below 700 dp of available content width, or at font scale 1.5 and higher: phone flow with identity, grouped personal/academic form, membership, save and account actions.
- 700–999 dp: horizontal identity card, then details form beside membership/save/account actions.
- 1000 dp and wider: 320 dp identity/membership/account sidebar beside a wider details editor, with the Save Profile action aligned below the editor.

The workspace caps at 1200 dp. Thresholds use the width after existing rail and insets; expanding the rail can therefore trigger a narrower layout. V4's existing rail is reused in its compact/floating configuration and remains expandable. The layout scrolls when needed and keeps IME space. Larger text falls back to a single column instead of squeezing controls into two panes. A second compact-width check stacks the identity card where necessary.

Membership art is drawn as native paths inside a separately measured Canvas beside the management action, below membership text; it cannot cover or fade text. Colors are scoped to Profile, including dropdown menus. No image-generation assets or new dependencies are required by the app.

## Preserved behavior

The original ProfileViewModel is unchanged from the task's starting working tree. Profile save, name validation, loading/error feedback, avatar upload/preview, email-copy behavior, gender/exam/preparation options and their backend values, Premium management callback, logout/delete confirmations, support contact and footer remain wired to existing handlers. Name editing is inline; all selector rows use the existing Composables UI anchored menu. The approved mockups' sample identity/exam values were not hardcoded.

`Titli.kt` and `AnimatedTitliButterfly.kt` are unchanged against HEAD. The original scaffold help button is enabled for all sizes. The prior private tablet renderer remains in the file but is no longer the active screen path, preserving the existing working-tree implementation rather than deleting it.

English, Hindi and Roman Hindi resources add only the display label “Your details”. All existing content resources remain intact. The selected design's Profile save button replaces the old small top-bar save control. Account actions are separate rows; destructive action uses the semantic error palette. Fields disable during loading/save; avatar change retains its upload guard and a larger camera touch target.

## Verification

Kotlin compilation passed before final build integration. **Full `:app:assembleProdDebug` passed**, including Kotlin, Java, Hilt, dex and APK packaging. Output is saved under `Outputs/logs/profile-compact-2026-10-28/assemble.log`. Source review confirms unchanged ViewModel, confirmation handlers and original Titli artwork.

No tests, emulator/device runs, profile writes, purchases, deployment or store upload were performed, following the user's request to handle testing. Device visuals, enlarged text, keyboard/IME, dark mode and portrait/landscape transitions remain for manual review.

Composables UI sources/API reference: https://composables.com/ui/docs/buttons and https://composables.com/ui/docs/dropdown-menu. Existing dependency and app theme are reused.
