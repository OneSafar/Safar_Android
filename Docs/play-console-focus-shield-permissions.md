# Play Console Notes: Focus Shield Accessibility

Use this content when submitting a build that includes Focus Shield.

## Store Listing Disclosure

SAFAR includes Focus Shield, an optional app-blocking feature for study sessions. Users choose distracting apps to block, then start a focus timer. While that timer is running, SAFAR uses Android Accessibility to detect the package name of the app the user opens. SAFAR compares that package name with the user's selected blocked app list. If a selected app opens, SAFAR shows its own block screen and notification so the user can return to the focus session.

SAFAR does not use Display over other apps. It does not read messages, passwords, typed text, contacts, photos, or screen content. It does not click buttons, change settings, prevent uninstall, or control the device. Blocked app choices stay on the user's device.

## Accessibility Declaration

API: `AccessibilityService`

Accessibility tool flag: `isAccessibilityTool=false`

Purpose: App blocking / focus protection during user-started study timers.

Why it is required: Focus Shield needs Accessibility events to detect opened app package names immediately while a focus timer is active. The app compares the opened package name with the user's selected blocked app list. Without this service, the timer still works, but reliable blocked-app detection cannot work.

Data accessed: Opened app package names from Android Accessibility window events.

Data use: Local comparison against the user's selected blocked apps. Used only while a focus timer is running and Focus Shield is enabled.

Data sharing: Not sold. Not shared for ads or analytics. Blocked app choices stay on device.

User controls: Focus Shield is off by default. Users must enable it from Ekagra > Focus Shield, accept a separate in-app Accessibility disclosure, choose apps to block, and grant the SAFAR Focus Shield Accessibility service in Android settings. Users can disable Focus Shield in SAFAR or revoke the Accessibility service at any time.

## Demo Video Checklist

1. Open SAFAR and navigate to Ekagra > Focus Shield.
2. Show that Focus Shield is off by default.
3. Turn Focus Shield on and show the in-app Accessibility disclosure.
4. Accept the disclosure.
5. Open Android Accessibility settings from SAFAR and enable SAFAR Focus Shield.
6. Return to SAFAR and select one app to block.
7. Start a focus timer.
8. Close or dismiss PiP if it appears.
9. Open the selected app and show SAFAR's block screen.
10. Return to SAFAR and disable Focus Shield.

## Important

Do not set `isAccessibilityTool=true` unless SAFAR's primary purpose changes to disability support. Do not add `SYSTEM_ALERT_WINDOW` or draw-over-other-apps behavior for Focus Shield. Do not use Accessibility to click controls, change settings, prevent uninstall, read screen text, or automate user actions.
