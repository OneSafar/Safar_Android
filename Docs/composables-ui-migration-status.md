# SAFAR Composables UI migration status

The app installs `com.composables:ui:0.2.0`, `com.composables:composeunstyled:2.7.0`, and `com.composables:compose-interaction-capabilities:1.1.0`. `SafarTheme` wraps the app with `SafarComposablesTheme`; its colors come from the active SAFAR palette, and its text style uses SAFAR's Hanken Grotesk typography. The Toppers Batch provides its existing pink accent locally. No brand colors were intentionally changed.

## Converted in this pass

- The shared drawer app bar uses the Composables UI `Toolbar` and ghost icon buttons. It retains the hamburger, optional back action, centered title, route actions, and caller supplied color or gradient.
- Study Planner, Toppers Batch lectures, Ekagra, Study Circle, Goals, and plan task action menus use Composables UI dropdown menus. The shared selection field and Support request history picker also use its dropdown.
- Toppers Batch uses library buttons, icon buttons, alert dialog, text fields, radio group, checkboxes, determinate progress indicators, and bottom navigation. Its existing tracker actions and pink accent remain.
- The shared inline refresh bar uses the library's indeterminate progress indicator.
- Determinate progress bars in Achievements, Dashboard, Study Planner, Nishtha Goals and Analytics, and Kavach onboarding use the animated Composables UI progress indicator while retaining their feature colors and track heights.
- Bottom sheets across Toppers Batch, Study Planner, Goals, Dashboard, Study Circle, Habits, Mehfil, Kavach, Ekagra, Home announcements, audio, and analytics now use Composables UI. Existing surface colors, size caps, and dismiss callbacks were carried over.
- Standard alert dialogs across the app now use Composables UI dialog surfaces; the Toppers Batch keeps its existing Composables UI dialog wrapper for keyboard and scroll behavior.
- Simple text actions, icon actions, and primary buttons across features now use Composables UI buttons. Explicit content colors were retained where the library supports them.
- Direct `Text` and `Icon` imports were migrated where the library API matched. Files using Material-only arguments such as painter icons, text layout callbacks, and custom text decorations remain on Material until those calls can be adapted without losing behavior.

## Final control pass

- Feature-specific text inputs now use Composables UI state-based fields through value bridges. The richer bridge carries existing field colors, icons, multiline limits, errors, cursor and selection colors, IME actions, and password masking. Admin notification selectors use the shared Composables UI dropdown.
- Custom-color buttons, outlined buttons, switches, checkboxes, sliders, radio indicators, and filter chips use adapted Composables UI/Compose Unstyled control code. The adapters keep SAFAR's feature colors and callbacks. Selection controls retain 48 dp touch targets.
- Material circular progress calls were replaced with a theme-aware Canvas indicator. Dhyan's timed breathing bar uses a direct progress renderer so the timer is not delayed by a second progress animation.
- Both Dhyan option sheets now use Composables UI bottom sheets. A local theme override retains their exact black 45% scrim and existing surface color.

The app still uses Material 3 for some layout surfaces, typography slots, specialized controls, and platform integrations. This migration covers the requested common interactive control families; it does not imply every `androidx.compose.material3` import has been removed. Runtime appearance still needs the manual review below.

## Manual review checklist

1. Open Home, Study Planner, Toppers Batch, Ekagra, Goals, Study Circle, Support, and Profile. Check that titles stay centered and hamburger/back buttons remain on the left.
2. Open every three-dot action menu. Check anchor position, outside-tap dismissal, keyboard focus, action labels, disabled states, and destructive action color.
3. In Toppers Batch, switch Today/Lectures/Progress; open a subject; complete and undo a lecture; plan/edit revision dates; rename and delete a lecture. Check that dialogs scroll with the keyboard visible.
4. Type into Toppers Batch dialogs and verify characters, cursor position, and saved values. Check that the text field does not reset while typing.
5. Compare light and dark modes. Look for text or icons with poor contrast, unexpected colors, truncated labels, or mismatched type sizes.
6. Check phone and wider layouts, especially the tracker navigation bar and app bar title beside two left-side actions.
7. Open sheets in Goals, Habits, Study Planner, Ekagra, Kavach, Mehfil, Home announcements, Dashboard, and Study Circle. Check scrolling, swipe and back dismissal, keyboard insets, and whether any sheet closes while a save is running.
8. Open exit, notification permission, Settings reminder and privacy, and Premium dialogs. Check button order, action results, text wrapping, and accessibility focus.
9. Check small icon and text buttons throughout Home, Study Planner, Goals, Habits, Ekagra, Study Circle, Premium, and Live. Look for changed target size, wrapping, unintended background fills, and any action that no longer fires.
10. Check the progress bars in Achievements, Dashboard, Study Planner, Goals, Analytics, and Kavach onboarding. Confirm the fill reaches the expected value and its track and subject color remain correct.
11. Type, erase, paste, and move the cursor in forms across Goals, Study Planner, Study Circle, Mehfil, Admin notifications, Profile, Support, Habits, and Toppers Batch. Check that the text never resets or duplicates. In Delete Account, check that the password stays masked until the visibility button is tapped and the Done key confirms only when allowed.
12. Check Dhyan's technique and sound sheets, their black scrim, swipe/back dismissal, and the breathing speed slider. The breathing progress bar should follow the timer without lag.
13. Check colored switches in Mehfil, YouTube Focus, and Kavach, plus checkboxes in Ekagra and Habit export. Confirm checked, unchecked, and disabled colors; tap targets; and keyboard or screen-reader focus.
14. Check radio choices in Premium, Ekagra, and Habit export, and the selected filter chips in Achievements, Study Planner, and Home updates. Confirm the label color and selected state match the prior SAFAR palette.
15. In Ekagra timer setup and Study Planner, try typing letters into number-only fields and paste a value longer than the limit. Rejected characters should never appear, and the cursor should remain usable. Repeat with Live comments, Support, Study Circle names, and the Habit name length limit.

Verification: `:app:compileQaDebugKotlin`, `:app:testQaDebugUnitTest`, and `:app:assembleQaDebug` passed after the final control pass. The QA APK is at `app/build/outputs/apk/qa/debug/app-qa-debug.apk`. No emulator was used; runtime appearance needs manual review.

One full QA unit test run had an intermittent `HabitViewModelTest` failure caused by a missing Android main dispatcher. That test passed in isolation, and the full suite passed on rerun.

Sources: [Composables UI toolbar](https://composables.com/ui/docs/toolbar), [dropdown menu](https://composables.com/ui/docs/dropdown-menu), [navigation bar](https://composables.com/ui/docs/navigation-bar), [buttons](https://composables.com/ui/docs/buttons), [alert dialog](https://composables.com/ui/docs/alert-dialog), [text fields](https://composables.com/ui/docs/text-fields), [checkbox](https://composables.com/ui/docs/checkbox), [radio group](https://composables.com/ui/docs/radio-group), and [progress](https://composables.com/ui/docs/progress).
