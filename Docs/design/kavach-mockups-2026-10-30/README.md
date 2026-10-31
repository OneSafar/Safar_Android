<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V4 -->
# Kavach Plum Command

Selected direction: 02-plum-command-selected.png. Screens audited: standalone/settings, permission setup, app picker and conflict dialog, active session, native WindowManager blocking overlay, analytics and detail sheets, category editor, about/onboarding.

Shared KavachCommandTheme supplies ivory/paper, plum actions/ink, peach selections and native Material colour equivalents for coexistence with Composables UI. Library buttons/fields/switch/checkbox and popup wrappers use existing dependency; no new dependency. The native blocking overlay intentionally remains native Android, with plum colours; no enforcement migration.

Preserved behavior: required usage and overlay permissions, optional other access, blocked-package configuration, independent YouTube Focus control and conflict handling, strict/Normal Quick Unlock policy, active-session callbacks, existing analytics categories and values. Category meaning colours remain semantic. The active timer continues to show actual secondsLeft, not static mockup text. Analytics use actual data, never sample chart values. Illustrations, app icons and labels in boards are conceptual and not bundled as runtime assets.

Implementation scope is visual/presentation and reusable component adapters, not repository, ViewModel, service, activation/readiness, analytics calculations or strict-unlock policies. Dark equivalents supplied. Validation uses compilation and focused existing policy tests; emulator/device inspection omitted per user instruction.
