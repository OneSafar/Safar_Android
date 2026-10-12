# SAFAR permission tutorials

## Where a tutorial earns its place

| Flow | Treatment | Entry points |
| --- | --- | --- |
| YouTube Focus accessibility | Find the named service, enable it with informed consent, return and select channels | Inline first setup; guide sheet when reconnecting |
| KAVACH Usage Access | Select SAFAR if needed, enable usage access, return | KAVACH onboarding, shield settings, app Settings |
| KAVACH Display Over Apps | Select SAFAR if needed, enable display permission, return | KAVACH onboarding, shield settings, app Settings |
| Optional Notification Shield | Find Notification shield, review Android’s sensitive access warning, enable and return | KAVACH onboarding, shield settings, app Settings |
| Ordinary notifications | Short rationale before the native prompt; no multi-step walkthrough | Existing KAVACH permission explanation; native app notification settings remain direct |

Do not introduce generic battery-optimization or exact-alarm tutorials without a corresponding supported settings flow. The current manifest has no exact-alarm permission request. Existing battery troubleshooting messages do not constitute a setup flow.

Normal timer controls, navigation, channel switches and community actions should remain discoverable in their screens, not acquire mandatory permission-style tutorials. The existing butterfly tours are globally disabled in `TourManager`; this change does not re-enable those unrelated overlays.

## Visual and behavior contract

- Native Compose typography, icons and surfaces. No raster illustrations, blur, glass, video, new font downloads or animation dependencies.
- One demonstration at a time; 450 ms opacity transitions, 3.2 seconds to read each step, then stop on the final step. No endless animation loop.
- Manual step chips pause progression. Replay starts again from the beginning after completion.
- Pause in the background. Animation-scale changes and touch-exploration changes immediately disable automatic progression. All instructions remain manually accessible.
- Settings illustrations are explicitly labeled as examples; OEM labels vary. Per-app settings links may skip the first illustrated list.
- Existing permission request callbacks remain authoritative. Illustrations never grant permissions or claim a permission was granted.
- Disclosure of sensitive Android access is preserved; do not claim Notification Access cannot read notification content.
- Ordinary notification access and notification-listener access are different permissions and must never be routed to each other.

## Verification checklist

1. First-time YouTube setup: guide is inline; primary action uses the existing disclosure acceptance and Settings callback. Return after granting and continue to channel selection.
2. YouTube permission repair: guide sheet opens; dismiss does not change settings; primary action opens Accessibility. A grant closes stale guide state.
3. KAVACH onboarding and settings: exercise Usage Access, Overlay, and Notification Shield. Dismiss preserves disabled permission state; returning refreshes using the existing permission logic.
4. App Settings: check each permission row goes to its own permission, especially standard Notifications versus Notification Shield.
5. Pause, choose steps, replay; background for more than 3.2 seconds and confirm progression pauses.
6. Disable system animations or enable TalkBack while a guide is open: no auto-advance. Manual steps still work.
7. Check light/dark themes, 320 dp width, landscape, and large fonts. Sheet scrolls; step chips scroll horizontally if needed; text is not truncated.

Content regression tests: `PermissionWalkthroughTest`. App target for validation: `testProdDebugUnitTest`; do not substitute a QA build for device testing.
