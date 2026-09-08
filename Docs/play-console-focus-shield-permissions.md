# Play Console Notes: YouTube Insights Accessibility

Use this material when submitting a build that contains YouTube Insights. Legal and Play Console copy must be reviewed before release.

## Prominent in-app disclosure

> YouTube Focus uses Android’s AccessibilityService API while enabled to read visible text and screen controls in the YouTube app.

> Data accessed: names (including channel or account display names), personal identifiers (such as YouTube @handles), and other user-generated content visible on screen (such as video titles, descriptions and comments). This information may be present in the screen text read by the service.

> Purpose: SAFAR uses channel names and handles, player controls and screen text to identify the channel being watched, detect videos and Shorts, apply your blocking choices, and measure viewing time. Comments and unrelated account text are not used to identify channels.

> To enforce your blocking settings, Safar can pause playback, go back from blocked content, and show blocking controls over YouTube.

> Data sent and saved: newly detected channel @handles are sent to SAFAR’s server to resolve channel identities and add them to the shared channel catalogue. Channel names, identifiers and your channel choices are saved on this device. Daily viewing totals by category are synced to SAFAR for your analytics. Raw screen text, video titles, descriptions and comments are not uploaded by this feature.

> This access is optional. You can turn it off anytime in Android Settings → Accessibility → SAFAR YouTube Focus.

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

Data Safety should declare the derived usage totals according to the production backend's retention and account-deletion behavior. Detected channel handles are transmitted to the shared server catalogue. Disclose that transfer and daily usage totals in the Accessibility declaration, privacy policy and applicable Data Safety answers. Distinguish on-device screen access from off-device collection. Verify backend retention, logging and deletion before submission.

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

## Updated consent

Disclosure version 2 replaces the old Boolean consent. Existing users must accept the updated disclosure before YouTube Focus can run, even if Android permission remains enabled. Verify fresh install, upgrade, decline, accept with permission already enabled, and accept before granting permission.
