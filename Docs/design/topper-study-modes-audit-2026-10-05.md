**Parmar tracker: student experience audit — 5 October 2026**

**Verdict: useful and substantially improved, but not yet a robust habit tracker. Overall: 6/10.**

The two modes now have understandable purposes. Official schedule gives the student Parmar’s daily classes without requiring personal dates. My own pace gives the student control over which released lectures to study and when. They share completion history and preserve personal plans when switching.

The weakest parts are recovery after a missed day, trust after a connection failure, and the amount of planning UI a returning student must pass before reaching today’s work. These need attention before describing the experience as polished and reliable for the public. This judgment covers these Android modes, not the whole app’s release readiness.

**Evidence and limits**

Reviewed the current Android dashboard, mode selector, personal checklist, official list, details, planning dialogs, calendar filtering, completion feedback, state handling, repository/cache, and tracker tests. The supplied screenshots informed the visual assessment; they show earlier iterations and do not prove the latest info buttons and Undo banner have been exercised on a device.

Ran all 93 existing tracker JVM tests successfully, including Android QA debug compilation required by the test task. Ran four additional temporary audit probes successfully. Those four probes assert the problematic current behaviour; their passing confirms the findings, not correct behaviour. Temporary probes live outside the repository at `/private/tmp/topper-audit-tests/feature/toppersbatch/UserJourneyAuditTest.kt`. Logs are `/private/tmp/topper-user-audit.log` and `/private/tmp/topper-user-audit-probes.log`.

No device checks, accessibility sessions, performance measurements, live-server compatibility checks, or new release build were performed in this audit. The user is handling manual checks. No application code was changed. The find-bugs skill informed boundary/error-path review; the scoped Omen scan returned no files and supplied no useful risk evidence.

**Scorecard**

Scores are reviewer judgments, not measured student usability results.

| Dimension | Score | Reason |
|---|---:|---|
| Official daily experience | 7/10 | Simple checklist and correct official date ordering; weak continuity after missing a class. |
| Own-pace daily experience | 6/10 | One-tap Add and organized subjects work; planning controls interrupt the daily checklist and upcoming visibility is incomplete. |
| Structure and cleanliness | 7/10 | Subject grouping, prominent lecture numbers, separators and collapsed secondary content are improvements. |
| Consistency | 6/10 | Clear shared history, but revisions and watch tasks behave differently; mode preference is local to the device. |
| Robustness | 5/10 | Watch saves, duplicate-tap protection and completion Undo are sound foundations; confirmed revision and offline recovery gaps remain. |
| Habit support | 4/10 | Completion/activity views show effort, but missed-day recovery and durable plan-adherence history are weak. |

**What works and should stay**

- Personal dates and official dates have separate purposes. Official reference content in own-pace mode does not add tasks or show competing completion checkboxes.
- A released lecture can be added to today in one tap. Students choose their workload without mandatory quotas or upfront schedule setup.
- Pending tasks are grouped in a stable subject order and sorted numerically within each subject. Completed planned tasks move to a collapsed group.
- Retained watch dates keep planned watch totals stable through completion and Undo. Actual lectures finished today remain a separate timestamp-based count.
- Watch plan saves apply the returned lecture before dismissing the dialog. Failed saves keep the date/dialog recoverable; version conflicts refresh the lecture for retry.
- Watch completion has optimistic feedback, rollback on failure, an eight-second Undo action, and an alternative untick action in completed tasks.
- Today completion checkboxes and mode info buttons have 48 dp targets. Mode controls and suggestion rows adapt to narrow widths/enlarged text in code.
- The mode info dialogs explain both choices and say switching retains saved history. They do not switch mode when opened.

**Findings, in priority order**

**1. P1 — A revision can save successfully while the screen still shows it unfinished. Confirmed by a temporary state test.**

Student scenario: tick today’s revision; the completion request succeeds; the following overview refresh fails. The state contains “Revision completed.” and an error, but the task is still unchecked and the old plan version remains. The student cannot tell whether to retry, and a retry can use stale version data.

Unlike watch-date saves and watch completion, revision completion does not apply the returned lecture locally. The generic mutation helper discards its result and relies on a second request. Rescheduling and importing prior progress also rely on this refresh-based path; those paths were inspected but not separately reproduced.

Evidence: [revision completion dispatch](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchViewModel.kt:340), [generic success handling](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchViewModel.kt:261).

Smallest fix: apply the confirmed returned lecture/overview before showing success, then refresh in the background. Failed refreshes should not undo a confirmed local result.

**2. P1 — A failed edit removes the last offline snapshot. Confirmed by a temporary repository test.**

Student scenario: the tracker opened with cached history; the connection goes away; a tick or edit fails. The visible in-memory state may recover, but the repository has already cleared the persisted overview. Reopening the app while still offline can therefore leave the student without their previously available tracker view. Server history is not deleted; the lost data is the local cached copy.

Cache invalidation happens before every mutation. Successful watch completion also does not immediately persist the returned local state, so the offline snapshot remains absent until a later successful overview refresh.

Evidence: [mutation cache invalidation](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchRepository.kt:33), [completion result handling](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchViewModel.kt:225).

Smallest fix: retain a last-confirmed snapshot, preserve the protection against stale in-flight refreshes, and persist confirmed mutation results. An ambiguous network failure should mark the view as awaiting refresh, rather than erase its offline usability or claim the snapshot is current.

**3. P2 — Official mode does not provide a clear recovery path after a missed day. Confirmed selection behaviour; UX/product gap.**

Student scenario: miss Monday’s class and return Tuesday. Today and Next official lecture select Tuesday/future classes. Monday’s unfinished lecture remains in Library but drops out of the primary daily flow. The timetable is doing what its name promises, but the tracker is no longer helping the student recover.

This is not evidence that completion history is lost, and it is not necessary to change the official schedule or automatically move dates. It needs a visible route to earlier unfinished work.

Evidence: [official date filtering](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/BatchStudyMode.kt:11).

Smallest fix: add a compact “Earlier unfinished classes · N” link/disclosure below today’s official checklist. Keep the official timetable primary.

**4. P2 — Next in your plan hides future lectures when that subject has an earlier unfinished plan. Confirmed by a model probe matching the panel selection.**

Student scenario: English lecture 1 is planned today or overdue, and lecture 2 is planned tomorrow. The upcoming preview first chooses the earliest unfinished English task, then discards it because it is not future-dated. It never selects tomorrow’s lecture. The preview can disappear despite saved future plans.

It also previews at most one watch lecture per subject and does not include future revision sessions. That limited scope is not stated by the broad heading “Next in your plan.”

Evidence: [upcoming preview selection](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/PersonalPlanPanel.kt:39).

Smallest fix: select upcoming tasks directly after filtering to future dates. Use a heading matching the actual scope, or include revisions in the task preview.

**5. P2 — Own-pace Today prioritizes adding work over doing work. UX judgment.**

The summary card is followed by Add lectures, up to four suggestion rows, an unavailable-subject disclosure and Browse all lectures. Only then does the student reach To study. This makes adding convenient while building a long plan, but costs returning students repeated scrolling to reach the daily checklist. Enlarged text increases that cost.

Evidence: [Add section](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/PersonalPlanPanel.kt:67), [checklist placement](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/PersonalPlanPanel.kt:116).

Smallest fix: keep suggestions expanded for an empty plan. With pending work, show the checklist immediately after its summary and make Add more a compact action/disclosure. Keep addition easy without requiring the daily study action to sit beneath it.

**6. P2 — Recovery is inconsistent between watches, revisions and removals. Confirmed controls; UX judgment.**

An accidental watch tick has Undo and can later be unticked. A completed revision checkbox is disabled and has no equivalent Undo path. Removing a watch from today saves immediately with no Undo; adding it back does not restore its cleared reminder. Removing a revision plan can remove all pending future revision dates, not just today’s task. The confirmation explains this, which is good, but the scope remains much broader than the watch-row removal.

Evidence: [task actions and revision confirmation](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/PersonalPlanPanel.kt:160), [removal behavior](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchViewModel.kt:308).

Smallest fix: offer Undo for watch removal with its previous date/reminder. Until a revision Undo operation exists, make revision completion explicitly deliberate. Continue to describe whole-plan revision removal clearly rather than imply that only today is removed.

**7. P3 — Several correct counters still require the student to understand different definitions. UX judgment.**

Overall batch completion, planned tasks fulfilled today, and lectures actually completed today measure different sets. Revisions contribute to the plan counter but not the lecture-completion count. A lecture completed on an earlier day can already fulfill a retained plan assigned today. Those rules are defensible; different numbers should not look like competing answers to “How much did I do today?”

Smallest fix: make “Today’s plan” the daily primary counter, retain “Lectures finished today” as the actual-activity label, and explicitly show the watch/revision workload. Do not combine the sets to make the numbers match artificially.

**8. P3 — Mode choice is remembered only on this device. Confirmed implementation; expectation risk.**

Completion history and plans are server data, while the study-mode preference is an account-keyed SharedPreferences entry. On another device/reinstall, the mode defaults from whether any personal watch date exists. A student who follows Official schedule but has old personal dates may return in My own pace on that device. This is not currently an account-synced setting.

Evidence: [preference persistence](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/BatchOverviewCache.kt:24), [fallback selection](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/BatchStudyMode.kt:8).

This is lower priority than save reliability. An explicit first-use choice would improve intent recognition; syncing the choice is a later scope decision.

**Why the habit-tracker score is lower**

The daily loop should help the student choose a manageable commitment, perform it, recover from a missed day, and understand consistency over time. The current system handles choosing lectures and recording completion reasonably well. Activity charts exist, but they primarily describe lecture completion. Revision effort is less visible, missed-day recovery is secondary, and rescheduling overwrites watch dates rather than preserving an immutable record of the original daily commitment. It cannot reliably answer “How often did I follow the plan I made?”

No fixed daily lecture quota is necessary. Different students and lectures need different workloads. The improvement is clearer daily commitment and recovery, not more mandatory setup or a larger feature set.

**Remaining checks and lower-priority observations**

- The whole Today dashboard is one LazyColumn item and task rows are composed with `forEach`. Large expanded plans are not lazy per task. Performance risk is unmeasured; do not claim lag without device evidence. [Dashboard item](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchScreen.kt:416).
- Repeated completion confetti may distract during bulk marking. Prefer celebrating a meaningful daily milestone; reduced-motion and TalkBack behavior need device checks. [Confetti implementation](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/BatchCompletionFeedback.kt:62).
- The Undo banner uses a fixed eight-second timeout. Later unticking remains possible, but accessibility-adjusted timeout behavior is not implemented in this flow. [Completion timeout](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchViewModel.kt:251).
- Compatibility fallback deliberately rejects unsupported reminders/revision planning rather than silently dropping them. The UI can still offer these before discovering an older server’s limitations. Live deployment support was not verified here. [Compatibility handling](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchRepository.kt:47).
- IST day refresh exists on the lifecycle/minute cycle, but midnight rollover, expanded-text layouts and the latest interactive flow were not exercised on a device in this audit.

**Smallest useful fix order**

1. Make revision/bulk mutation success immediately trustworthy, and preserve offline snapshots safely.
2. Correct the future-plan preview.
3. Put pending work above expanded Add controls once a plan exists.
4. Provide visible missed-class recovery in Official mode.
5. Make accidental removal/revision completion recoverable and clarify remaining counter labels.

Keep the two modes, existing backend and shared history. The next improvement should make the daily loop shorter and more trustworthy; another broad redesign is not required.
