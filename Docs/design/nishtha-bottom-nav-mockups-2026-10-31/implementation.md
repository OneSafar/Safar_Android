# Attached plum navigation and Journal FAB

Implemented the approved plum/mint colors with the second reference's attached shape: full width, only upper corners rounded, no horizontal inset or gap beneath the bar. AttachedNishthaNavigation.kt uses native Material 3 NavigationBar/NavigationBarItem for tab semantics, keyboard/touch selection, consistent layout and navigation insets. The plum surface extends through its bottom safe area, while controls remain above system navigation. Mint highlights only the active icon; its label is mint and semibold, inactive icons/labels are blush. The existing four labels and icon pairs remain localized and unchanged.

NishthaBottomBar delegates to this shared renderer for every tab, replacing the previous split between Journal/Streaks and Check-In/Goals. WarmNishthaTabs also delegates to it for compatibility. Analytics remains outside the four-tab list. Tab selection still invokes the existing back-stack callback.

Journal's full-width New entry button is replaced by a 56dp circular plum plus FAB, aligned bottom-end with 16dp bottom clearance above the parent navigation. It invokes exactly the same onNew callback: clear title/body/prompt state and open the existing JournalWriteSheet. List content gains 96dp bottom padding so entries remain reachable beyond the overlaid FAB. Save, prompt answer, editing, refresh and overflow actions are unchanged.

Source checks confirmed the shared renderer, top-only corner shape, mint selection, existing editor callbacks, FAB binding and list clearance. No Kotlin compilation, Gradle build, runtime test, installation or device inspection was performed, per the user's continuing instructions. Final visuals and gesture-area legibility require manual verification.

Reviewed the installed Composables UI navigation bar documentation (https://composables.com/ui/docs/navigation-bar). Its item API fills the whole selected item; native Material 3 NavigationBarItem was used here for the requested icon-only indicator and system-safe attached surface. No dependencies were added.
