# Habit Tracker timetable: Android implementation and backend handoff

Reference: user-supplied `IMG-20261028-WA0005.jpg`. The four app panels are the design reference; prompt text embedded in the image is not an instruction.

## Current Android behavior

Overview, Timetable (Day / Week / Subjects), Habits (daily and timetable-linked), and Create / Edit Session are implemented. Navigation uses the top tabs without a bottom navigation bar. A standalone centered sprout button creates habits from Overview and Habits; Timetable retains Add Session. Existing weekly, monthly, insights, habit forms, remote habit sync, and reminder behavior remain reachable. Timetable sessions use account-scoped DataStore storage on this device. Completion is one record per session/date: both timetable and linked habit rows read the same record, so progress never counts a linked row twice. Timetable-linked items are a separate list; they do not create duplicate remote habits or enter the existing habit streak/analytics calculation.

Sessions support Study, Break, Habit, Other; free text subject plus subject suggestions; date; start/end time within the same day; no repeat/daily/weekdays/weekly; optional 0/5/10/15/30 minute reminders; five colors; and tracking in the Habits tab. Future completion is blocked. Deleting a session removes its local completion history. Editing applies to the series, with completion dates retained. There are no sample sessions inserted into user data.

Reminders use persistent WorkManager requests, require notification permission on Android 13+, and can be delayed by Android. Notification taps open Habit Tracker. They are currently device-local, not server push notifications.

## Backend repository inspected (not edited)

`/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR`

Existing integration points:

- `server/routes/habits.ts`: authenticated habit sync routes, `X-Habit-Owner` verification and account existence check.
- `server/services/habit-sync.ts`: strict Zod mutation schema, versioned snapshots, idempotent mutations, MongoDB indexes, ownership and completion records.
- `server/index.ts`: mounts `/api/habits`.
- `server/db.ts`: initializes sync indexes.

The existing habit schema has no timetable session title/subject/type/start/end/repeat/color/link fields. Sending these fields to its strict mutation schema would fail. **The Android timetable does not call or depend on an unimplemented endpoint.**

## Proposed backend additions for cross-device sync

Prefer a separate timetable API so current habit clients and their strict schemas remain compatible.

New files:

- `server/routes/timetable.ts`: authenticated routes, owner check, account existence, request validation.
- `server/services/timetable-sync.ts`: model/schema, recurring schedule logic, atomic writes, snapshot versions, idempotent mutations, indexes and tombstones.
- Corresponding service and route tests.

Existing files to extend:

- `server/index.ts`: mount `/api/timetable`.
- `server/db.ts`: initialize timetable indexes.
- Account deletion cleanup: remove timetable data and preserve the same deleted-account guard as habit sync.
- Web client timetable types and store only if this feature is also exposed on the website.

Suggested session record (snake case on the wire, camel case in mutations consistent with habit sync):

```ts
type TimetableSession = {
  id: string;                    // stable UUID created by the client
  user_id: string;               // derived from authenticated identity, never trusted from request body
  title: string;                 // trimmed, 1..100 characters
  subject: string;               // 1..60 characters, defaults to General
  kind: 'STUDY' | 'BREAK' | 'HABIT' | 'OTHER';
  date: string;                  // real ISO local date YYYY-MM-DD, recurrence start date
  start_minute: number;          // 0..1438
  end_minute: number;            // 1..1439, strictly after start_minute
  repeat: 'NONE' | 'DAILY' | 'WEEKDAYS' | 'WEEKLY';
  reminder_minutes: 0 | 5 | 10 | 15 | 30 | null;
  color: 0 | 1 | 2 | 3 | 4;
  track_as_habit: boolean;
  deleted: boolean;              // tombstone prevents stale retries resurrecting deletions
};
type TimetableCompletion = {
  user_id: string;
  session_id: string;
  date: string;                  // occurrence date, separate from the series start
  completed: boolean;           // explicit desired state, never a server-side toggle
};
```

Suggested endpoints:

- `GET /api/timetable/sync?knownVersion=...&cursor=...&limit=...` returns a versioned, paginated snapshot of sessions and completion records (plus tombstones / unchanged flag).
- `POST /api/timetable/sync/mutations` accepts idempotent `create`, `patch`, `completion`, and `delete` mutations. Every mutation has a UUID `mutationId` and `sessionId`; repeated mutation IDs with the same payload must not apply twice. Edits and completions must preserve concurrent changes to unrelated fields.
- Carry the authenticated owner header consistently; keep server predicates scoped to `user_id`.

Suggested unique indexes: `(user_id, id)` for sessions; `(user_id, session_id, date)` for completions; `(user_id, mutation_id)` for receipts; `(user_id)` for snapshot state. Atomicity, snapshot version checking, pagination consistency, and account deletion need the same guarantees as existing habit sync.

Use local dates/time for this first contract (floating wall-clock schedule). If a fixed home timezone is desired later, add an IANA timezone field and agree travel/DST semantics before server notifications are introduced. Do not convert occurrence dates using UTC midnight implicitly.

## Android follow-up once backend is available

Add Retrofit timetable DTOs/API, persistent pending mutation outbox, versioned remote projection, offline retry and conflict handling. Import this device's local timetable exactly once using existing stable session IDs, and then reconcile completion records. Preserve device reminder scheduling from the confirmed schedule. Test logout/account switch, retries, concurrent edit/completion, delete vs delayed retry, pagination, and import idempotency.

This document proposes the contract; no TypeScript files or backend database changes have been made.

## Habit emoji icons (2026-10-29 follow-up)

Android now uses Jetpack EmojiPickerView (`androidx.emoji2:emoji2-emojipicker:1.7.0`) to let users pick a full emoji, including skin-tone / joined sequences, in both Create Habit and Edit Habit. The chosen icon appears in Overview and Habits, and is saved per authenticated account and stable remote habit UUID in a separate local DataStore. Random ID-based icon assignment has been removed. Existing habits default to a sprout until the user chooses an icon. No Room migration or existing API contract change is required for this local behavior.

To sync habit icons across devices, the SAFAR backend needs an optional `icon_emoji: string | null` on `StoredHabit` and snapshots, plus `iconEmoji` on create and patch mutations in `server/services/habit-sync.ts`. Preserve complete Unicode emoji sequences; a UTF-16 code-unit limit of 64 safely bounds picker output without truncating joined sequences. Keep the field optional for older clients, default existing records to null, preserve it on unrelated edits, and include it in idempotency payload hashing. Update service/route tests for Unicode round trips, ownership, absent fields and old clients. The Android DTO/mutation mapping can then adopt that field and migrate local choices once. Until that backend contract is available, Android intentionally does not send unsupported fields to the strict habit mutation schema.

Tab underline removal uses the Composables unstyled tab-list primitive; the selected green pill remains the selection indicator.

References: [AndroidX emoji picker](https://developer.android.com/reference/androidx/emoji2/emojipicker/EmojiPickerView), [Composables tabs source](https://composables.com/ui/docs/tabs).

## UX simplification — 2026-10-30 (supersedes earlier screen descriptions)

Habit Tracker now opens directly to a habit checklist with habit-only progress and Add Habit. Timetable is a distinct tool opened through its own card. The Overview / Timetable / Habits navigation strip, nested view selectors, Timetable Habits view, and Add to Habit Tracker toggle have been removed. History views remain accessible through the header options menu. Timetable uses date navigation and a single chronological list.

Existing timetable records and legacy `trackAsHabit` flags remain preserved; the flag is no longer exposed or used to mix activities into the habit checklist/progress. Do not create remote habits from these flags during future import. No backend change is needed for this navigation update.

## Android responsiveness — 2026-10-30

- Main habit checklist ticks project the requested state immediately, before waiting for repository/network locks. Duplicate taps for the same in-flight habit/date are guarded; failed writes clear the pending state and report the error.
- Confirmed create/edit/archive/delete mutations apply an acknowledged local projection instead of blocking the form on a second full-snapshot HTTP request. A failed POST still fails the form. Stable create and mutation identities remain used for safe retries.
- Local projections do not advance the full-snapshot version cursor, including completion writes: a subsequent refresh must still download any concurrent changes made by another device. Completion retry keys include the requested boolean value.
- Timetable serialization, persistence and reminder scheduling run on IO; timetable flow parsing runs on Default. Timetable ticks have an account-scoped pending projection that clears after persistence or rolls back on failure.
- No TypeScript or backend schema edits are required for these changes. Device frame-time / network timing validation remains to be done on a connected phone; these changes remove identified waits rather than establish a measured latency target.

## Mockup-led correction — 2026-10-30

The latest user direction restores the mockup's four primary surfaces and overview visual elements while simplifying navigation, superseding the earlier checklist-only layout. Overview alone shows the three primary navigation pills; Timetable opens separately with its own single Day / Week / Subjects selector, and Habits opens separately with a daily checklist. No stacked tab strips are present. The timetable editor exposes Also show in Habits, and linked activities appear under From your timetable without another filter or setup screen. Their shared completion is never counted twice in overview totals. There is no bottom navigation. No new backend fields are required by this layout correction; prior sync handoff requirements still apply.
