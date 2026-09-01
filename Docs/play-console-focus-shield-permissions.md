# Play Console Notes: YouTube Insights Accessibility

Use this material when submitting a build that contains YouTube Insights. Legal and Play Console copy must be reviewed before release.

## Prominent in-app disclosure

> YouTube Insights uses Android Accessibility access only while the feature is enabled. It reads visible information inside the YouTube app—such as whether a video or Short is playing and the displayed channel handle—to measure productive, distracting, Shorts and unidentified time. When blocked content opens, SAFAR pauses it and leaves that YouTube screen automatically. A newly detected exact channel handle is sent to SAFAR's server to verify its permanent YouTube Channel ID and add that verified identity to the shared channel catalogue. Productive-channel choices, video titles and screen content stay on this device. You can disable YouTube Insights or revoke Accessibility access at any time.

The disclosure appears in the dedicated YouTube Study Mode onboarding before Android Accessibility Settings opens. Consent version and time are recorded locally. The feature is off by default and is separate from Kavach app blocking.

## Accessibility declaration

- API: `AccessibilityService`
- Accessibility tool: `false`
- Restricted package: `com.google.android.youtube`
- Purpose: optional YouTube content measurement and user-configured blocking
- Information accessed: visible YouTube UI semantics needed to identify player state, Shorts and channel name
- Actions: no gesture injection. For blocked content only, SAFAR invokes the visible player's Pause accessibility action when available, sends an idempotent media-pause command, and invokes Android Back to leave the blocked YouTube surface. These actions are disclosed before permission is requested.
- Local data: channel names, allowlist, viewing intervals and parser input
- Synced data: channel-free daily totals for productive, distracting, Shorts and unidentified seconds, split into entire-day and Protected-time totals
- Never retained or uploaded: Accessibility node snapshots, arbitrary screen text, video titles, typed text, messages or passwords

## Store listing and privacy policy

State that YouTube Study Mode is optional, uses Accessibility, is not a disability-support tool, measures YouTube whenever enabled, and can separately block Shorts or distracting channels during Kavach time or always. Explain that starter choices and newly detected channels default to distracting until the student marks them productive. Channel-block notifications may offer local-only **Mark Productive** and **Manage channels** actions.

Data Safety should declare the derived usage totals according to the production backend's retention and account-deletion behavior. Do not declare channel names or raw Accessibility content as collected because the implementation keeps them on-device. Revalidate this statement whenever telemetry or crash logging changes.

## Reviewer video checklist

1. Open the separate YouTube Study Mode drawer destination and show it off by default.
2. Complete onboarding, starter-channel choices and the complete prominent disclosure.
3. Open Android Accessibility Settings and grant SAFAR access.
4. Show separate Shorts and distracting-channel scope controls.
5. Play a normal video, a Short and a channel later marked productive.
6. Show that blocked playback pauses and SAFAR automatically leaves the blocked YouTube surface.
7. Open YouTube Study Mode analytics and show category totals and local channel controls.
8. Block a newly detected channel and demonstrate both notification actions.
9. Disable the feature, then revoke permission, demonstrating that measurement stops.

## Release checks

- Keep `isAccessibilityTool=false`.
- Do not add gesture capability.
- Confirm the service XML remains restricted to YouTube.
- Verify the disclosure, privacy policy, store listing, Data Safety form and reviewer video all describe the shipped behavior identically.
- Play approval cannot be guaranteed; submit the actual implementation and an honest, narrow justification.
