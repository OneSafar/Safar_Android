# Ekagra navigation responsiveness and controls

## Findings in V4

- `LaunchedEffect(selectedTab)` invoked `TimerService.setDuration(FOCUS, ...)` on a return to an inactive Timer tab. That method cancels alarms and a notification, clears persistence, resets mode, releases music, and ends analytics. Navigation did not need those side effects. It could also change an idle Stopwatch/Break configuration to Focus. Start and explicit duration actions already own configuration.
- The content transition deliberately delayed entry by 40 ms, then ran a 240 ms fade and 300 ms slide, with AnimatedContent's default size animation. Outgoing and incoming screens could be composed together, adding work during rapid tab switching.
- Music and Theme opened panels without changing the selected navigation item. Their input was accepted, but the bar provided no selected feedback.
- Tab presses performed both LongPress and TextHandleMove haptics, in two different handlers. Phone selection dots snapped between items; tablet backgrounds were independent.
- Remaining Material buttons and raw click targets had inconsistent feedback. Tag color targets were only 28 dp and the delete icon was 22 dp.

These are code-level contributors, not a measured attribution of every slow frame. Omen's installed defect command returned no Kotlin findings for this scope; manual call-chain review supplied the evidence.

## Changes

Navigation now changes UI state only. One shared indicator translates across all five controls, with immediate selected semantics and one haptic on a changed selection. Music/Theme highlight while their panel is open and return to the underlying destination when dismissed. Existing back navigation remains intact.

Only the requested content is composed. A 140 ms, immediately visible fade replaces the overlapping size/slide transition. The animation never gates input. Saveable state is retained per destination.

Existing `com.composables:ui:0.2.0` and the existing SAFAR theme integration are reused; no dependency upgrades. Live Composables MCP docs consulted: Buttons, Navigation Bar, Tabs, Slider, Switch, Radio Group, Checkbox, and Text Fields. Remaining Material action buttons, settings fields, sliders, toggles, alert radios, small tag/color actions, theme choices, and checkbox prompts use the library or existing Composables UI adapters. The shared Music panel's category/select/preview actions also use library buttons. Specialized history/tag long-press and selectable goal rows are retained so their extra gestures are not lost. The draggable service-owned floating timer is outside this screen redesign.

The active Ekagra accent now flows into Composables UI's local theme. Touch targets are at least 48 dp for the migrated controls. Numeric validation, session saving, goal linking, Kavach Always On protection, and music callbacks remain wired to their existing handlers.

## Motion contract

- **Trigger:** tap a different navigation action, settings choice, or button.
- **Rules:** update selection/call its handler immediately; do not await motion, network work, or timer configuration. Reselecting the active tab does nothing.
- **Feedback:** one translating highlight, library press/ripple/focus feedback, a single selection haptic, and a short content fade.
- **Loops & modes:** no repeating decoration; rapid selections retarget the animation; system animations disabled snaps the highlight and content. Existing app MotionPolicy and Android animation scaling apply.

The indicator reads animation state in its graphics layer. Content fades read alpha in a graphics layer; neither transition animates layout dimensions. Light/dark palettes and RTL are supported.

## Validation and delivery

- QA compilation and APK assembly succeeded; 201 Ekagra unit tests passed before the final checkbox/Music panel control pass.
- On the USB device, the isolated dark/RTL/1.6× font/reduced-motion navigation test passed and its screenshot was inspected. The initial paused-clock tests exposed test synchronization issues (asserting before a recomposition frame); they now allow one frame and assert the callback before it. Their rerun was stopped when the user requested installation instead of further testing. No emulator was used.
- Production debug is the requested delivery variant. Further device tests were stopped at the user's request.
- Final `:app:assembleProdDebug` succeeded. Installed `app/build/outputs/apk/prod/debug/app-prod-debug.apk` on USB device `667cd680` using `adb install -r`; Android returned `Success`.
