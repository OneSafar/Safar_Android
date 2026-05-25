# SAFAR Android Notification System Audit

This document contains a comprehensive audit of the notification system in the SAFAR Android application, along with a detailed testing plan, ADB commands, and proposed debug-only utilities.

---

## A. Summary of Current Notification Architecture

The SAFAR app implements a hybrid notification architecture consisting of:
1. **Local Ongoing Foreground Service Notifications**: Used for active focus sessions (`TimerService.kt`) to keep the timer running in the background and display real-time updates (remaining time, mode, pause/resume/reset actions).
2. **Local Scheduled Notifications (WorkManager)**: Used for daily study reminders, morning reflections/nudges, and exam countdowns/planner alerts.
3. **Remote Push Notifications (Firebase Cloud Messaging)**: Sent from the backend to the device token via FCM data payloads. These are intercepted and shown using the app's local notification manager.

All notifications (except the ongoing timer service notifications) are processed and displayed by a unified `SafarNotificationManager.kt` wrapper which enforces styling standards (Sparkle icon, brand color tinting) and quiet hours suppression.

---

## B. Notification Inventory Table

| Notification Name / Type | File Path (Definition) | Trigger Location & Condition | Notification Class & Trigger Type | Channel ID | ID Logic | Screen Opened & Deep Link | Foreground / Background / Killed / Reboot | Permissions Required |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Focus Timer Ongoing** | [TimerService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/ekagra/TimerService.kt) | `TimerService.start()` / `updateNotification()`. Triggered when a focus or break session is active. | Local / Foreground Service (`startForeground` / `notify`) | `focus_timer` | `1001` (constant) | `safar://ekagra` (Ekagra Screen) | FG: Yes / BG: Yes / Killed: No / Reboot: No | `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |
| **Focus Shield Status** | [TimerService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/ekagra/TimerService.kt) | `enableFocusShieldForSession()`. Triggered when Focus Shield is turned on. | Local / Service Direct (`notify`) | `focus_shield_status` | `1004` (constant) | `safar://ekagra` (Ekagra Screen) | FG: Yes / BG: Yes / Killed: No / Reboot: No | `POST_NOTIFICATIONS` |
| **Focus Shield Blocked App** | [TimerService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/ekagra/TimerService.kt) | `handleFocusShieldBlockedIntent()`. Triggered when blocked package is opened. | Local / Service Direct (`notify`) | `focus_shield_blocked` | `1003` (constant) | `safar://ekagra` (Ekagra Screen with Return Action) | FG: Yes / BG: Yes / Killed: No / Reboot: No | `POST_NOTIFICATIONS` |
| **Focus Timer Completion** | [TimerService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/ekagra/TimerService.kt) | `showCompletionNotification()`. Triggered when focus or break countdown ends. | Local / `SafarNotificationManager.show` | `focus_timer` | `1002` (constant) | `safar://ekagra` (Ekagra Screen) | FG: Yes / BG: Yes / Killed: No / Reboot: No | `POST_NOTIFICATIONS` |
| **Morning Nudge** | [MorningNudgeWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/MorningNudgeWorker.kt) | `MorningNudgeWorker.doWork()`. Runs daily at 6:30 AM. | Local WorkManager / `SafarNotificationManager.show` | `study_reminders` | Random (`10000-99999`) | `safar://dashboard` (Dashboard Screen) | FG: Yes / BG: Yes / Killed: Yes / Reboot: Yes | `POST_NOTIFICATIONS` |
| **Planner Alerts** | [PlannerAlertsWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/PlannerAlertsWorker.kt) | `PlannerAlertsWorker.doWork()`. Runs daily at custom time (e.g. 19:00). Checks for overdue tasks, exam countdowns, pace. | Local WorkManager / `SafarNotificationManager.show` | `study_reminders` | Random (`10000-99999`) | `safar://studyplanner` (Study Planner Screen) | FG: Yes / BG: Yes / Killed: Yes / Reboot: Yes | `POST_NOTIFICATIONS` |
| **Daily Study Reminder** | [StudyReminderWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/StudyReminderWorker.kt) | `StudyReminderWorker.doWork()`. Runs daily at custom time. | Local WorkManager / `SafarNotificationManager.show` | `study_reminders` | Random (`10000-99999`) | `safar://ekagra` (Ekagra Screen) | FG: Yes / BG: Yes / Killed: Yes / Reboot: Yes | `POST_NOTIFICATIONS` |
| **FCM Push Notifications** | [SafarFirebaseMessagingService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/SafarFirebaseMessagingService.kt) | Server-triggered message delivered to `onMessageReceived`. | FCM Remote Push / `SafarNotificationManager.show` | Dynamic (normalized) | Random (`10000-99999`) | Dynamic (via `deepLink` data) | FG: Yes / BG: Yes / Killed: Yes / Reboot: Yes (if online) | `POST_NOTIFICATIONS` |

---

## C. Files Inspected

* **[AndroidManifest.xml](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/AndroidManifest.xml)**: Inspected declarations of permissions, activities, services, accessibility configs, and deep link intent filters.
* **[build.gradle.kts](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/build.gradle.kts)**: Audited product flavors (`qa`, `prod`), applicationIdSuffix (`.qa`), build types, targetSdk, dependencies (WorkManager, FCM).
* **[SafarNotificationChannels.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/SafarNotificationChannels.kt)**: Checked channel IDs, names, importance, and normalization logic.
* **[SafarNotificationManager.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/SafarNotificationManager.kt)**: Analyzed notification construction, small icon styling, and quiet hours suppression code.
* **[SafarFirebaseMessagingService.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/SafarFirebaseMessagingService.kt)**: Audited incoming FCM message parsing and user preferences checking (`isChannelEnabled`).
* **[MorningNudgeWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/MorningNudgeWorker.kt)**: Inspected random quote assets loading and WorkManager daily scheduling calculation.
* **[PlannerAlertsWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/PlannerAlertsWorker.kt)**: Checked Hilt entry point injection, database queries for tasks/exams, and custom scheduling delay logic.
* **[StudyReminderWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/StudyReminderWorker.kt)**: Inspected basic enqueuing logic.
* **[MainActivity.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/MainActivity.kt)**: Checked intent parameters parsing and route interception.
* **[SafarNavGraph.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/navigation/SafarNavGraph.kt)**: Checked route consumption and redirection rules.
* **[SettingsViewModel.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/ui/settings/SettingsViewModel.kt)**: Audited scheduling/canceling of workers when toggling settings.
* **[SafarDataStore.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/data/local/SafarDataStore.kt)**: Examined default preference values (e.g. default quiet hours `"22:00"` to `"07:00"`).

---

## D. Permissions and Manifest Findings

1. **Declared Permissions**:
   * `android.permission.INTERNET`: Required for FCM.
   * `android.permission.POST_NOTIFICATIONS`: Declared in manifest and requested at runtime for Android 13+.
   * `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for `TimerService` ongoing notification.
2. **Missing Permissions**:
   * No `RECEIVE_BOOT_COMPLETED` is declared in the manifest. While WorkManager adds this internally to reschedule itself, any reboot-based custom receivers (none exist yet) would fail without it.
   * `AlarmManager` permissions (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`) are not present, which is correct since the app does not use exact alarms.
3. **Service Declarations**:
   * `SafarFirebaseMessagingService` is declared with action `com.google.firebase.MESSAGING_EVENT` and `android:exported="false"`, which is correct and secure.
   * `TimerService` is declared with `foregroundServiceType="mediaPlayback"` and `android:exported="false"`.
4. **Deep Link Intent Filter**:
   * `MainActivity` declares an intent filter for `<data android:scheme="safar" />` with action `android.intent.action.VIEW`. This maps standard deep links (e.g., `safar://ekagra`) correctly.

---

## E. Channel Findings

The application defines 10 channels inside `SafarNotificationChannels.kt`.
* `focus_timer`: Name "Focus timer", IMPORTANCE_LOW (Ongoing timer indicator & completion alerts).
* `focus_shield_alerts`: Name "Kavach alerts", IMPORTANCE_DEFAULT. *(Legacy, not used in code)*.
* `focus_shield_status`: Name "Kavach status", IMPORTANCE_LOW (Ongoing status indicator).
* `focus_shield_blocked`: Name "Kavach blocked app", IMPORTANCE_DEFAULT (Blocked app notifications).
* `study_reminders`: Name "Study reminders", IMPORTANCE_DEFAULT (Nudges, streak protection, planning).
* `course_updates`: Name "Course updates", IMPORTANCE_DEFAULT.
* `achievements`: Name "Achievements", IMPORTANCE_LOW.
* `community`: Name "Community", IMPORTANCE_DEFAULT.
* `account_system`: Name "Account and system", IMPORTANCE_HIGH (Security, billing).
* `announcements`: Name "Announcements", IMPORTANCE_LOW.

**Findings & Observations**:
* All channels are created during `SafarApplication.onCreate()`, ensuring they are registered before any notification is posted.
* Normalization logic in `normalize()` returns `ACCOUNT_SYSTEM` if an unrecognized channel ID is provided.

---

## F. Scheduler Findings

The app uses WorkManager for periodic 24-hour tasks:

### 1. WorkManager Worker Scheduling
* **MorningNudgeWorker**: Scheduled daily (hardcoded for 6:30 AM) with unique name `daily_morning_nudge`.
* **PlannerAlertsWorker**: Scheduled daily at the user's custom daily reminder time with unique name `planner_alerts_worker`.
* **StudyReminderWorker**: Scheduled daily at the user's custom daily reminder time (default 19:00) with unique name `daily_study_reminder`.

### 2. Major Rescheduling Risk (Reschedule-Loop Issue)
In `SafarApplication.kt` (lines 35-45), we schedule workers on every app startup:
```kotlin
appScope.launch {
    if (dataStore.notificationsEnabled.first() && dataStore.dailyStudyReminderEnabled.first()) {
        StudyReminderWorker.schedule(this@SafarApplication, dataStore.dailyReminderTime.first())
        PlannerAlertsWorker.schedule(this@SafarApplication, dataStore.dailyReminderTime.first())
        MorningNudgeWorker.schedule(this@SafarApplication, 6, 30)
    }
}
```
In the scheduling companion functions, `ExistingPeriodicWorkPolicy.UPDATE` is used:
```kotlin
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    WORK_NAME,
    ExistingPeriodicWorkPolicy.UPDATE,
    request
)
```
* **The Problem**: Because `initialDelay` is calculated relative to `now` (the moment the app starts), calling `schedule` on every app launch changes the initial delay. WorkManager treats this as an update to the work specifications and resets the internal delay timer. If a user opens the app frequently (e.g. multiple times throughout the day), **the workers will constantly have their delay pushed back and will NEVER execute.**
* **The Solution**: Use `ExistingPeriodicWorkPolicy.KEEP` during application startup, and only use `UPDATE` when the user explicitly modifies the time in the Settings screen.

---

## G. FCM Findings

* **FCM Registration**: `SafarApplication.fetchAndStoreFcmToken()` fetches the token and saves it via `NotificationTokenRegistrar.saveAndRegister()`.
* **Syncing**: The registrar sends the token to the backend API (`POST /api/device-tokens`) once every 6 hours or when forced.
* **Payload Interception**:
  * In `SafarFirebaseMessagingService.onMessageReceived()`, the app pulls values from `RemoteMessage.data` (data messages) rather than notification objects.
  * It maps payload properties (`title`, `body`, `channel`, `deepLink`, `priority`) to a local notification.
  * Checks user preferences via `isChannelEnabled(channel)` before displaying.
* **Google Services Configuration**: `google-services.json` contains two clients matching the configurations:
  * Client 1: Package `com.safar.app` (prod)
  * Client 2: Package `com.safar.app.qa` (qa)

---

## H. Tap/DeepLink Findings

* **PendingIntent Construction**: Built via `NotificationDeepLinkHandler.activityIntent(context, deepLink)` with `FLAG_ACTIVITY_SINGLE_TOP` and `FLAG_ACTIVITY_CLEAR_TOP`.
* **Intent Extras**: The route is placed in `EXTRA_ROUTE`, and raw deep link is placed in `EXTRA_DEEP_LINK`.
* **NavGraph Integration**: `MainActivity` saves the route in `notificationRoute` which is observed by `SafarNavGraph` to navigate to the target screen.
* **Critical Missing Mapping Bug**:
  * `PlannerAlertsWorker` posts notifications with deepLink = `safar://studyplanner`.
  * However, `NotificationDeepLinkHandler.routeFor()` **does not map** the host `"studyplanner"` to any route.
  * As a result, tapping the planner alert notification falls back to `Routes.HOME` instead of navigating to the Study Planner screen!
  * **Solution**: Add `"studyplanner" -> Routes.STUDY_PLANNER` in `NotificationDeepLinkHandler.routeFor()`.

---

## I. Problems or Risks Found

1. **Morning Nudge Suppressed by Default Quiet Hours**:
   * `MorningNudgeWorker` is hardcoded to fire at **6:30 AM**.
   * The default quiet hours in `SafarDataStore.kt` are **22:00 (10 PM) to 07:00 (7 AM)**.
   * `SafarNotificationManager` checks quiet hours for the `study_reminders` channel.
   * Since 6:30 AM is within the quiet hours window, the morning nudge notification is **always suppressed** by default!
2. **Timer Completion Silenced in Late Night Sessions**:
   * `TimerService` completion notifications use the `focus_timer` channel.
   * `SafarNotificationManager` filters all channels (except `account_system`) against quiet hours.
   * If a user completes a focus session after 10 PM, the completion notification is suppressed! Focus timer and Focus Shield events must be exempt from quiet hours.
3. **App Launch Reschedule Loop (WorkManager)**:
   * (Detailed in Section F) Frequent app openings reset periodic work delays, causing daily study reminders to never fire.
4. **Deep Link App Launch Race Condition**:
   * In `SafarNavGraph.kt`, when a notification deep link is consumed on startup, the nav graph navigates immediately if `isLoggedIn != false`.
   * If the login state is still loading (`isLoggedIn` is `null`), the app navigates to the target screen before authentication completes, causing API calls to fail or screen to show empty data.
   * **Solution**: Change the check to `if (isLoggedIn == true)` so deep links are only navigated after login confirmation is complete.
5. **No System Level Block Check**:
   * `canPostNotifications()` does not verify if notifications are disabled globally at the OS level or if the specific channel is muted by the user.

---

## J. Recommended Debug-Only Notification Test Panel

To verify all notifications immediately, we can implement a debug-only UI panel in the `debug` source set. This keeps it completely out of release builds.

### 1. Panel Location
* Code file: `app/src/debug/java/com/safar/app/ui/debug/NotificationTestPanel.kt`
* Manifest configuration: Create `app/src/debug/AndroidManifest.xml` to declare debug-only routes or receivers.

### 2. Manifest Setup (Debug-Only)
Create `app/src/debug/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.safar.app">
    <application>
        <!-- Debug Receiver to trigger notifications via ADB -->
        <receiver
            android:name=".ui.debug.NotificationDebugReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.safar.app.ACTION_TRIGGER_DEBUG_NOTIFICATION" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

### 3. Debug Receiver Code
Add `app/src/debug/java/com/safar/app/ui/debug/NotificationDebugReceiver.kt`:
```kotlin
package com.safar.app.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safar.app.notifications.SafarNotificationManager
import com.safar.app.notifications.SafarNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.safar.app.ACTION_TRIGGER_DEBUG_NOTIFICATION") {
            val type = intent.getStringExtra("type") ?: "nudge"
            val title = intent.getStringExtra("title") ?: "Debug Notification"
            val body = intent.getStringExtra("body") ?: "This is a debug test."
            val deepLink = intent.getStringExtra("deepLink") ?: "safar://dashboard"
            val channel = intent.getStringExtra("channel") ?: SafarNotificationChannels.STUDY_REMINDERS

            CoroutineScope(Dispatchers.Default).launch {
                SafarNotificationManager(context).show(
                    title = title,
                    body = body,
                    channelId = channel,
                    deepLink = deepLink
                )
            }
        }
    }
}
```

### 4. Compose UI Panel Code Structure
Add `app/src/debug/java/com/safar/app/ui/debug/NotificationTestPanel.kt`:
```kotlin
package com.safar.app.ui.debug

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.safar.app.notifications.*
import kotlinx.coroutines.launch

@Composable
fun NotificationTestPanel(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationManager = context.getSystemService(NotificationManager::class.java)

    var customTitle by remember { mutableStateOf("Simulated Title") }
    var customBody by remember { mutableStateOf("Simulated notification content body.") }
    var customDeepLink by remember { mutableStateOf("safar://studyplanner") }
    var customChannel by remember { mutableStateOf(SafarNotificationChannels.STUDY_REMINDERS) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Notification Debug Panel", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- System Status Checks ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("System Status", style = MaterialTheme.typography.titleMedium)
                    val globalEnabled = notificationManager.areNotificationsEnabled()
                    Text("Global Notifications: ${if (globalEnabled) "ENABLED" else "BLOCKED"}")
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val ch = notificationManager.getNotificationChannel(customChannel)
                        val importance = ch?.importance ?: NotificationManager.IMPORTANCE_NONE
                        Text("Selected Channel Importance: $importance (Enabled: ${importance != NotificationManager.IMPORTANCE_NONE})")
                    }
                }
            }
        }

        // --- Trigger Local Notification ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Trigger Custom Local Notification", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = customTitle, onValueChange = { customTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = customBody, onValueChange = { customBody = it }, label = { Text("Body") })
                    OutlinedTextField(value = customDeepLink, onValueChange = { customDeepLink = it }, label = { Text("Deep Link") })

                    Button(
                        onClick = {
                            scope.launch {
                                SafarNotificationManager(context).show(
                                    title = customTitle,
                                    body = customBody,
                                    channelId = customChannel,
                                    deepLink = customDeepLink
                                )
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Post Notification Now")
                    }
                }
            }
        }

        // --- WorkManager Immediate Triggers ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Trigger Workers (WorkManager)", style = MaterialTheme.typography.titleMedium)
                    
                    Button(onClick = {
                        val request = OneTimeWorkRequestBuilder<MorningNudgeWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                    }) {
                        Text("Run MorningNudgeWorker Immediately")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(onClick = {
                        val request = OneTimeWorkRequestBuilder<StudyReminderWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                    }) {
                        Text("Run StudyReminderWorker Immediately")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(onClick = {
                        val request = OneTimeWorkRequestBuilder<PlannerAlertsWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                    }) {
                        Text("Run PlannerAlertsWorker Immediately")
                    }
                }
            }
        }

        item {
            Button(onClick = onBack) {
                Text("Close Panel")
            }
        }
    }
}
```

---

## K. Manual Test Matrix

| Notification ID / Key | Trigger Scenario | Manual Debug Trigger Available? | Foreground Test Steps | Background Test Steps | Killed App Test Steps | Permission Denied Test | Channel Blocked Test | Deep Link Tap Action Test | Expected Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **focus_timer_ongoing (1001)** | Start Ekagra Focus session. | Yes (Start session in app). | Start timer. Observe ongoing notification updates every second. | Minimize app. Observe notification in shade. | Swipe app from Recents. Timer service remains running with notification active. | Revoke permission. Notification disappears; timer runs silently. | Disable channel. Notification disappears; timer runs silently. | Tap notification. Re-opens Ekagra focus screen directly. | Notification updates and controls timer state. |
| **focus_timer_done (1002)** | Timer finishes. | Yes (Fast-forward timer in debug or trigger Panel). | Set timer to 5s. Wait for completion. | Lock phone/background. Wait for completion. | (Not applicable during active countdown). | Deny permission. No alert shown. | Disable channel. No alert shown. | Tap notification. Opens Ekagra screen. | Shows "Focus session complete" / "Break finished". |
| **focus_shield_blocked (1003)** | Open blocked app. | Yes (Trigger panel or launch blocked app). | Enable Focus Shield. Open Instagram. | (Same). | (Not applicable). | Deny permission. Accessibility blocks app but no notification posted. | Disable channel. Accessibility blocks app but no notification posted. | Tap notification. Returns to Ekagra screen. | Notification displays app name blocked; includes action "Return to Focus". |
| **morning_nudge** | 6:30 AM schedule. | Yes (Panel -> Run MorningNudgeWorker). | Run worker. Observe notification immediately. | Run worker with app closed. | Run worker with app force-stopped. | Deny permission. No alert. | Disable channel. No alert. | Tap notification. Opens Dashboard screen. | Notification displays custom title/body quote from assets. |
| **daily_study_reminder** | Custom scheduled time. | Yes (Panel -> Run StudyReminderWorker). | Run worker. | Run worker. | Run worker. | Deny permission. No alert. | Disable channel. No alert. | Tap notification. Opens Ekagra screen. | Displays "Your study time is ready". |
| **planner_alerts** | Custom scheduled time. | Yes (Panel -> Run PlannerAlertsWorker). | Run worker. | Run worker. | Run worker. | Deny permission. No alert. | Disable channel. No alert. | Tap notification. Opens Study Planner. | Displays alert for overdue tasks /countdown /pace warnings. |
| **FCM Push** | Remote payload sent. | Yes (Panel -> Custom Notification with route). | Submit JSON with title/body/deeplink. | Minimize app. Submit JSON. | Clear app. Submit JSON. | Deny permission. No alert. | Disable channel. No alert. | Tap notification. Opens deep-linked target screen. | Displays notification using normalized channel and triggers navigation. |

---

## L. ADB Commands for My Package Names

Generate ADB testing commands for both build configurations.

### 1. Product Build Flavor (Prod) — `com.safar.app`

* **List installed app packages**:
  ```bash
  adb shell pm list packages | findstr "safar"
  ```
* **Grant POST_NOTIFICATIONS**:
  ```bash
  adb shell pm grant com.safar.app android.permission.POST_NOTIFICATIONS
  ```
* **Revoke POST_NOTIFICATIONS**:
  ```bash
  adb shell pm revoke com.safar.app android.permission.POST_NOTIFICATIONS
  ```
* **Force-stop App**:
  ```bash
  adb shell am force-stop com.safar.app
  ```
* **Clear App Data**:
  ```bash
  adb shell pm clear com.safar.app
  ```
* **View Logcat Logs**:
  ```bash
  adb logcat -s SAFAR_FCM FocusShield SafarNotificationManager TimerService *:E
  ```
* **Trigger Debug Broadcast Receiver**:
  ```bash
  adb shell am broadcast -a com.safar.app.ACTION_TRIGGER_DEBUG_NOTIFICATION --es type "morning_nudge" --es title "ADB Nudge" --es body "Good Morning from ADB!"
  ```

### 2. QA Build Flavor (QA) — `com.safar.app.qa`

* **List installed app packages**:
  ```bash
  adb shell pm list packages | findstr "safar"
  ```
* **Grant POST_NOTIFICATIONS**:
  ```bash
  adb shell pm grant com.safar.app.qa android.permission.POST_NOTIFICATIONS
  ```
* **Revoke POST_NOTIFICATIONS**:
  ```bash
  adb shell pm revoke com.safar.app.qa android.permission.POST_NOTIFICATIONS
  ```
* **Force-stop App**:
  ```bash
  adb shell am force-stop com.safar.app.qa
  ```
* **Clear App Data**:
  ```bash
  adb shell pm clear com.safar.app.qa
  ```
* **View Logcat Logs**:
  ```bash
  adb logcat -s SAFAR_FCM FocusShield SafarNotificationManager TimerService *:E
  ```
* **Trigger Debug Broadcast Receiver**:
  ```bash
  adb shell am broadcast -a com.safar.app.qa.ACTION_TRIGGER_DEBUG_NOTIFICATION --es type "morning_nudge" --es title "ADB Nudge" --es body "Good Morning from ADB!" -n com.safar.app.qa/com.safar.app.ui.debug.NotificationDebugReceiver
  ```

---

## M. Automated Test Plan

Suggesting automated tests to protect notification integrity over time.

### 1. Unit Tests for Quiet Hours Logic
* **Test Name**: `quietHoursSuppression_respectsWindowRange`
* **File Path**: `app/src/test/java/com/safar/app/notifications/QuietHoursTest.kt`
* **Verifies**: Verifies that `shouldSuppressByQuietHours` correctly flags current times falling inside/outside quiet windows (especially windows spanning midnight, e.g., 22:00 to 07:00).
* **Mock/Fake Dependencies**: Mock/fake `SafarDataStore` to inject custom quiet hours values, and inject a fake `Clock` dependency to mock the current time.

### 2. Deep Link Mapping Test
* **Test Name**: `deepLinkMapping_mapsStudyPlannerRoute`
* **File Path**: `app/src/test/java/com/safar/app/notifications/NotificationDeepLinkHandlerTest.kt`
* **Verifies**: Verifies that `safar://studyplanner` matches `Routes.STUDY_PLANNER`.
* **Mock/Fake Dependencies**: None (pure utility unit test).

### 3. WorkManager Worker Execution Test
* **Test Name**: `studyReminderWorker_postsNotificationOnDoWork`
* **File Path**: `app/src/test/java/com/safar/app/notifications/StudyReminderWorkerTest.kt`
* **Verifies**: Verifies that when the worker executes, it builds the notification with correct fields and schedules it to run.
* **Mock/Fake Dependencies**: Use `WorkManagerTestInitHelper` (WorkManager testing library) and mock `SafarNotificationManager`.

---

## N. Minimal Code Changes Recommended

To fix the critical bugs identified during the audit, we recommend implementing the following fixes:

### 1. Fix Missing Deep Link Mapping
Add the `"studyplanner"` host to `NotificationDeepLinkHandler.routeFor()` so clicking planner notifications navigates to the Study Planner screen.

* **Target File**: [NotificationDeepLinkHandler.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/NotificationDeepLinkHandler.kt)
* **Changes**:
```kotlin
// ...
        return when (host) {
            "ekagra" -> Routes.EKAGRA
            "dashboard" -> Routes.DASHBOARD
            "studyplanner" -> Routes.STUDY_PLANNER // <-- ADD THIS
// ...
```

### 2. Fix Quiet Hours Suppressing Ongoing/Critical Alerts
Exempt focus timer alerts and active focus shield events from quiet hours. Only external push/pull reminders should be suppressed.

* **Target File**: [SafarNotificationManager.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/SafarNotificationManager.kt)
* **Changes**:
```kotlin
    private suspend fun shouldSuppressByQuietHours(channelId: String): Boolean {
        // Never suppress critical system, timer, or active shielding alerts.
        if (channelId == SafarNotificationChannels.ACCOUNT_SYSTEM ||
            channelId == SafarNotificationChannels.FOCUS_TIMER ||
            channelId == SafarNotificationChannels.FOCUS_SHIELD_STATUS ||
            channelId == SafarNotificationChannels.FOCUS_SHIELD_BLOCKED
        ) return false
// ...
```

### 3. Fix App Startup Reschedule-Loop
Modify `SafarApplication.kt` to use `ExistingPeriodicWorkPolicy.KEEP` instead of `ExistingPeriodicWorkPolicy.UPDATE` on startup. This prevents enqueued periodic alarms from constantly having their initial delay reset every time the app is opened.

* **Target File**: [MorningNudgeWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/MorningNudgeWorker.kt), [StudyReminderWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/StudyReminderWorker.kt), and [PlannerAlertsWorker.kt](file:///d:/SAFAR_PARENT/Safar_Android/Safar_Android/app/src/main/java/com/safar/app/notifications/PlannerAlertsWorker.kt)
* **Changes** (Change `ExistingPeriodicWorkPolicy.UPDATE` to `ExistingPeriodicWorkPolicy.KEEP` in the companions of all workers, or check if the work is enqueued first).
* Also, in `SettingsViewModel.kt`, we can continue using `ExistingPeriodicWorkPolicy.UPDATE` to dynamically apply new schedules when the user explicitly updates settings.

---

## O. Questions or Missing Context

1. **Morning Nudge Timing**: Is 6:30 AM intended to be customisable, or should it stay hardcoded? If it's hardcoded, should we adjust the default quiet hours end time to 6:00 AM so nudges are not blocked by default?
2. **WorkManager constraints**: Do you want network or device idle constraints added to `PlannerAlertsWorker`? Currently, it queries remote/local data directly, meaning it runs even without internet.
