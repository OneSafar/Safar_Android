THis is the Notification Implementation Plan. It says all about how and what to integration in the notification and what to exclude. Understand this Plan and then start the implementation. 

Notification integration should be **purposeful**, not like ecommerce apps that push offers constantly.

Big apps usually use this model:

**Backend/server → Firebase Cloud Messaging → Android app → notification channel → deep link into correct screen.**

For local app events, they do not always call the server. They also use **local notifications** from the Android app itself.

Your SAFAR app already has some notification foundation: it has `POST_NOTIFICATIONS`, Firebase Messaging dependency, WorkManager dependency, and an existing `TimerService` that already creates an ongoing timer notification with play/pause/reset behavior. 

## 1. Types of notifications SAFAR should use

SAFAR should have **two notification systems**.

### A. Local notifications

Generated inside the Android app.

Use these for:

* timer running notification
* focus session complete
* break started
* daily study reminder
* streak reminder
* blocked app attempt summary
* planned session reminder

These do **not** need your backend every time.

### B. Push notifications

Sent from your backend using Firebase Cloud Messaging.

Use these for:

* new course/content update
* live class alert
* admin announcement
* community reply/mention
* account/security/payment/subscription updates
* major app campaign or challenge

Firebase Cloud Messaging is the standard Google system for sending messages to Android apps, either as notification messages or data messages. Firebase also supports sending to single device tokens, topics, or groups. ([Firebase][1])

---

## 2. What sections of SAFAR should send notifications?

### Recommended notification map

| SAFAR section               | Notification examples                                        |                     Source |         Priority |
| --------------------------- | ------------------------------------------------------------ | -------------------------: | ---------------: |
| **Ekagra Timer**            | Focus started, timer running, session complete, break time   | Local app / `TimerService` |      High/useful |
| **Focus Shield**            | “You tried to open Instagram 3 times during focus” summary   |                  Local app | Low/summary only |
| **Dashboard/Home**          | Daily study reminder, streak protection, weekly progress     |       Local app or backend |           Medium |
| **Courses / Study Content** | New lesson, new test, new PDF, class reminder                |                Backend FCM |      Medium/high |
| **Achievements**            | Badge unlocked, streak milestone                             |              Local/backend |       Low/medium |
| **Mehfil / Community**      | Reply, mention, teacher/admin message                        |                Backend FCM |           Medium |
| **Profile / Account**       | Login alert, subscription, payment, important account update |                Backend FCM |             High |
| **Admin/Marketing**         | App challenge, campaign, important launch message            |                Backend FCM |      Low, opt-in |

For a productivity app, the strongest notification sections should be:

1. **Ekagra Timer**
2. **Focus Shield**
3. **Daily study reminder**
4. **Streak/goal protection**
5. **Course/live class updates**
6. **Important account/system messages**

Do **not** notify for every small thing. That will make students disable notifications.

---

## 3. Notification channels SAFAR should create

Android notification channels are required on modern Android so users can control categories separately. Android’s official docs say each channel needs a unique ID, user-visible name, and importance level. ([Android Developers][2])

Create these channels:

```text
focus_timer
```

For ongoing timer, focus session active, break running.

```text
study_reminders
```

For daily study reminder, planned session reminder, streak warning.

```text
course_updates
```

For new class, new lesson, new test, new material.

```text
achievements
```

For badges, streak milestones, goals completed.

```text
community
```

For Mehfil replies, mentions, teacher responses.

```text
account_system
```

For login, payment, subscription, important system messages.

```text
announcements
```

For admin announcements and important campaigns.

Do **not** put everything into one channel. If users dislike marketing but want timer notifications, they should be able to disable only marketing.

---

## 4. Android implementation plan

### Phase 1 — Clean up notification foundation

You already have:

* `POST_NOTIFICATIONS`
* Firebase Messaging dependency
* WorkManager dependency
* `TimerService` notification

Now add a proper notification package:

```text
app/src/main/java/com/safar/app/notifications/
    SafarNotificationChannels.kt
    SafarNotificationManager.kt
    SafarFirebaseMessagingService.kt
    NotificationDeepLinkHandler.kt
    StudyReminderWorker.kt
```

### Phase 2 — Create all notification channels at app start

Call this from `SafarApplication.onCreate()`:

```kotlin
SafarNotificationChannels.createAll(context)
```

Create channels for:

* focus timer
* reminders
* courses
* achievements
* community
* system
* announcements

### Phase 3 — Request notification permission properly

Since SAFAR targets modern Android and already declares `POST_NOTIFICATIONS`, you must request notification permission at runtime on Android 13+. Android says newly installed apps on Android 13+ need the user’s permission before sending non-exempt notifications. ([Android Developers][3])

Do **not** ask immediately on first app open.

Ask when the user reaches a relevant moment, such as:

* starts first Ekagra timer
* enables Focus Shield
* sets daily study reminder
* joins course/live class reminders

Best copy:

> SAFAR sends helpful reminders for your focus timer, study streak, and important class updates. You can control these anytime.

### Phase 4 — Keep timer notification local

Your `TimerService` should continue owning the active timer notification because it already handles foreground service, countdown, and play/pause/reset controls. 

Improve it by using channel:

```text
focus_timer
```

Timer notifications should include:

* remaining time
* mode: Focus / Break
* action: Pause
* action: Resume
* action: Reset
* tap opens Ekagra screen

### Phase 5 — Add local scheduled reminders

Use WorkManager for:

* daily study reminder
* evening streak reminder
* weekly progress summary

Android’s background work docs describe WorkManager as the library for scheduled and persistent background tasks. ([Android Developers][4])

Examples:

* “Study reminder at 7:00 PM”
* “You haven’t completed your focus session today”
* “Weekly SAFAR progress is ready”

For exact alarm-like timing, Android has stricter rules, so start with WorkManager unless you truly need exact-to-the-minute alerts.

### Phase 6 — Add Firebase Cloud Messaging for backend notifications

Your app already has Firebase Messaging dependency in Gradle, but you should verify that Firebase is fully configured with:

* `google-services.json`
* Google Services Gradle plugin
* Firebase project
* app package registered for both `prod` and `qa`

Add:

```kotlin
class SafarFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Send token to SAFAR backend
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Read message.data
        // Build notification through SafarNotificationManager
    }
}
```

Firebase’s Android docs say apps receive FCM messages by extending `FirebaseMessagingService` and overriding `onMessageReceived` / `onDeletedMessages`. ([Firebase][5])

### Phase 7 — Backend token storage

When Firebase gives a token, send it to your backend:

```text
POST /api/device-tokens
```

Payload:

```json
{
  "userId": "123",
  "deviceToken": "fcm_token_here",
  "platform": "android",
  "appVersion": "1.5",
  "flavor": "prod",
  "notificationsEnabled": true
}
```

Backend should store:

* user ID
* device token
* app version
* language
* exam/course category
* notification preferences
* last active date

When sending a push, your backend calls FCM HTTP v1. Firebase’s HTTP v1 API supports sending to a specific registration token, topic, or condition. ([Firebase][6])

---

## 5. Notification payload format

Use **data messages** for most SAFAR notifications because you want control over channel, deep link, image, and screen routing. Firebase distinguishes notification messages, which the SDK can display automatically, from data messages, which your app handles. ([Firebase][7])

Example backend payload:

```json
{
  "type": "course_update",
  "title": "New Ekagra practice session added",
  "body": "Start today’s 25-minute focus challenge.",
  "channel": "course_updates",
  "deepLink": "safar://ekagra",
  "priority": "normal",
  "imageUrl": null
}
```

Deep links should open:

* `safar://ekagra`
* `safar://dashboard`
* `safar://course/{id}`
* `safar://mehfil/thread/{id}`
* `safar://profile/subscription`

---

## 6. What notifications SAFAR should send

### Must-have for launch

#### 1. Focus timer running

Source: local `TimerService`
When: timer is active
Channel: `focus_timer`
Purpose: keep timer alive and visible

#### 2. Focus session completed

Source: local
When: timer reaches zero
Channel: `focus_timer`
Copy:

> Focus session complete. Great work — take a mindful break.

#### 3. Break reminder

Source: local
When: break starts or ends
Channel: `focus_timer`
Copy:

> Break finished. Ready for your next session?

#### 4. Daily study reminder

Source: local WorkManager
When: user-selected time
Channel: `study_reminders`
Copy:

> Your study time is ready. Start a 25-minute Ekagra session.

#### 5. Streak protection

Source: local/backend
When: evening and no session completed
Channel: `study_reminders`
Copy:

> You’re one session away from keeping your streak alive.

#### 6. New class/content update

Source: backend FCM
When: teacher/admin uploads important material
Channel: `course_updates`
Copy:

> New practice set is available for your exam prep.

#### 7. Important account/system alert

Source: backend FCM
When: subscription/payment/security/admin critical update
Channel: `account_system`

---

## 7. What SAFAR should avoid

Avoid notifications like:

* “Open the app now” with no value
* every small badge
* every app blocker attempt in real time
* every dashboard update
* repeated motivational spam
* marketing during focus sessions
* notifications late at night

For Focus Shield, do **not** notify every time the user opens a blocked app. Better:

> You avoided 6 distractions during today’s focus session.

Send this only after the session ends.

---

## 8. Recommended notification preferences screen

Add this in Profile or Settings:

```text
Notifications
  [ ] Focus timer updates
  [ ] Daily study reminders
  [ ] Streak reminders
  [ ] Course/class updates
  [ ] Achievements
  [ ] Community replies
  [ ] SAFAR announcements
```

Also add:

* reminder time
* quiet hours
* weekly summary toggle
* exam/course notification preference

This is important because users should control productivity notifications instead of feeling controlled by the app.

---

## 9. Implementation order

### Step 1

Create `SafarNotificationChannels`.

### Step 2

Create `SafarNotificationManager`.

### Step 3

Move/clean timer notification code inside `TimerService` to use `focus_timer`.

### Step 4

Add runtime notification permission request inside Ekagra and notification settings.

### Step 5

Add `StudyReminderWorker` with WorkManager.

### Step 6

Add `SafarFirebaseMessagingService`.

### Step 7

Add backend token registration API.

### Step 8

Add deep link handling in `MainActivity` and `SafarNavGraph`.

### Step 9

Add notification preferences in `SafarDataStore`.

### Step 10

Add admin/backend trigger rules.

---

## 10. Simple backend notification architecture

```text
User action / Admin action / Scheduled job
        ↓
SAFAR backend notification service
        ↓
Check user preferences + quiet hours
        ↓
Build FCM payload
        ↓
Firebase Cloud Messaging
        ↓
Android app
        ↓
Correct notification channel
        ↓
Tap opens correct SAFAR screen
```

This is close to how serious apps handle notifications: not directly from random screens, but through a centralized notification service with templates, targeting, user preferences, and analytics.

---

## Final recommendation for SAFAR

For launch, implement only these notification categories:

1. **Focus timer**
2. **Study reminders**
3. **Streak reminders**
4. **Course/class updates**
5. **Account/system alerts**

Leave these for later:

* community replies
* marketing campaigns
* weekly progress summaries
* achievement celebrations

That keeps SAFAR useful and respectful — exactly what a productivity app should be.
