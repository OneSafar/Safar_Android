# Titli feature guides

Four features now use the same spotlight, speech bubble, Back/Next/Skip, information dialog, and OK action as Ekagra and Goals. Each feature has two independent lessons. More detail stays behind the information button.

| Feature | Lesson | Steps |
| --- | --- | --- |
| Parmar Topper Batch Tracker | How to track a lecture | Choose a subject → find a week → mark a lecture done → plan revision |
| Parmar Topper Batch Tracker | Plan and review your study | Official/Personal → Calendar → Progress |
| Habit Tracker | How to build a habit | New Habit → name → repeat days → mark today done |
| Habit Tracker | Review and change habits | Weekly → Month → edit pencil |
| Mehfil | How to share a post | Choose a space → new post → write → space/name visibility/share |
| Mehfil | Save posts and connect | Post actions → Saved → Connections |
| Dhyan | How to start a session | Duration → Play → Pause → Reset |
| Dhyan | Choose guidance and sound | Breathing techniques → audio library entry → mute |

## Entry and replay

Use Titli in the existing feature toolbar. No new toolbar is added. The first Titli introduction starts the core lesson; subsequent taps open that feature's lesson list. Home's help menu also routes to all four features, waits for eligibility, and starts their core lesson. If a feature remains busy, its busy message explains that the current activity must be closed first.

The existing release gate remains **off by default**. Build a preview explicitly:

```sh
./gradlew :app:assembleProdDebug -PtitliTutorialsEnabled=true -x :app:copyApksToOutputs
```

APK: `app/build/outputs/apk/prod/debug/app-prod-debug.apk`.

## Practice boundary

- All practice scenes use sample data and model-free native presentation components.
- Batch practice uses the tracker's actual palette and bridges Material/Unstyled text color after the Composables theme is installed.
- Lecture rendering now accepts explicit actions; the real screen supplies its ViewModel adapter and link launcher, while practice supplies neither.
- Habit and Mehfil forms can render inline within the practice window. Their real bottom sheets keep the same presentation and real callbacks.
- Mehfil practice does not compose the account-backed drawer scaffold. It reuses Community, Saved, Connections, the post form, and the native bottom bar with local callbacks.
- Dhyan practice never composes the live countdown/player/session-tracking function. It reuses the actual orb, slider, controls, and guidance row.
- No practice action starts services, plays audio, opens links, requests permissions, publishes posts, sends chat requests, or changes habits/lectures/settings.
- The shared overlay consumes practice-canvas input and removes it from accessibility traversal; tutorial controls remain accessible.
- The real feature stays mounted with its current tab and selections. Skip, Close, and system Back dismiss practice. Tutorial state survives rotation through its ViewModel; a new process starts with practice closed.
- Completion bookkeeping uses separate version-one keys for the four features. Legacy butterfly flags do not hide these lessons.
- English, Hindi, and Hinglish copy lives in each locale's `feature_practice_tutorials.xml`.

## Verification

Use the explicitly scoped tutorial source-set initializer while unrelated legacy instrumentation sources are being updated. It includes the tutorial, Ekagra, and Goals test directories and does not alter normal builds.

```sh
./gradlew --offline -I tools/gradle/tutorial-tests.init.gradle \
  :app:testProdDebugUnitTest \
  --tests '*ScreenPracticeTutorialTest' --tests '*EkagraTutorialTest' --tests '*GoalsTutorialTest' \
  -PtitliTutorialsEnabled=true -x :app:copyApksToOutputs

ANDROID_SERIAL=emulator-5554 ./gradlew --offline -I tools/gradle/tutorial-tests.init.gradle \
  :app:connectedProdDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.safarparmar.app.ui.tutorial.ScreenPracticeInstrumentedTest,com.safarparmar.app.ui.tutorial.TitliHelpInstrumentedTest,com.safarparmar.app.ui.tutorial.FeatureTutorialInstrumentedTest \
  -PtitliTutorialsEnabled=true -x :app:copyApksToOutputs
```

The suites cover all 28 target registrations, forward/backward navigation, replay, completion versus Skip, rotation retention versus fresh-process state, all menu destinations, waiting for loading to finish, localized instructions, reduced motion, and dark mode at 1.6× font scale. They render the native practice scenes without a Hilt owner or domain dependencies. Screenshots are captured in the test app's cache as `titli-*.png`.

Physical-phone installation and a manual TalkBack review remain separate checks. Existing default builds continue to hide Titli until the release flag is deliberately enabled.

### Verified build

- Tutorial-enabled Prod Debug APK assembled successfully.
- 17 unit tests passed (new practice controller plus Ekagra and Goals regressions).
- 10 current emulator UI tests passed (five native-practice tests and five Titli help/menu tests).
- Six shared spotlight/interaction tests also passed; the four main practice tests passed in landscape.
- All 28 scenes were checked in light mode and in dark mode with 1.6× text and motion disabled.
- Native Batch text foreground and exact tracker background colors have an explicit light/dark regression test.
- Visual snapshots are in `app/build/reports/tutorial-previews/`; final portrait files end with `-portrait.png`.

The emulator was Android 17 / API 37.1 (`Resizable_Experimental`). No physical phone was installed or changed.

## Study Circle, Nishtha, and Exam Planner (20 October 2026)

Added seven short lessons (27 steps) using the same floating Titli spotlight:

| Feature | Lesson scope |
| --- | --- |
| Study Circle | Join a public circle, use an invite code, open a joined circle; create public/private circles, read member ranks, understand live focusing |
| Check-in | Choose mood, adjust intensity and optional details, save, review history |
| Journal | Use a prompt, write an entry, read a saved entry |
| Streaks | Check-in/login counts, active-day calendar, completed-goal trend |
| Analytics | Overview, Goals, Ekagra, Monthly Review |
| Exam Planner | One five-step overview: Plan, Home, Syllabus, Calendar, Progress |

Exam Planner deliberately skips detailed plan setup. Its preview uses the native exam row, daily task/add controls, subject row, shared month grid, progress summaries, and five-tab bottom bar. No PlannerActions implementation or planner ViewModel is supplied.

Check-in, Streaks, and Analytics now expose model-free presentation functions. Journal and Circle reuse their native cards, fields, buttons, and member rows. The tutorial callbacks are local no-ops; the shared host prepares the next snapshot. No real check-ins, entries, streak restores, reports, circle membership, connection requests, or plan updates occur. English, Hindi, and Hinglish copy is in `more_feature_tutorials.xml`.

All six new feature guides register in the global Titli menu and wait for loading/draft flows to finish before opening. Existing tutorial release gating is unchanged.

### Verification for the new guides

- Tutorial-enabled Prod Debug app and instrumentation APKs assembled successfully.
- 17 relevant unit tests passed (4 practice controller, 7 Ekagra, 6 Goals).
- 17 emulator UI tests passed: all 55 practice scenes in light mode and dark mode at 1.6× fonts, backward navigation/replay, translations, global Titli routing, overlay retry/scroll/input isolation, and the fixed Planner bottom-bar regression.
- Three additional wide-window tests passed at 2400×1080, including every scene in both themes and the Planner bottom-bar check. The test runner resets device rotation, so the landscape validation used a wide display-size override set before launch.
- Portrait and wide screenshots were reviewed for native colors, contrast, cutouts, and layout. Snapshots are in `app/build/reports/tutorial-previews/`.
- Build outputs were isolated in `/tmp/safar-titli-more-validation` to avoid another validation build sharing this checkout's generated files. Kotlin compilation used a temporary 6 GiB compiler heap; project build settings were not changed for this workaround.
- Preview APK: `Outputs/tutorial-previews-2026-10-20/Safar-Titli-StudyCircle-Nishtha-ExamPlanner.apk` (Titli enabled). Normal release gating remains unchanged.

The Android 17/API 37.1 emulator was used; no physical phone was installed. Physical-phone and manual TalkBack checks remain for manual review.

### Final source-only validation follow-up

After the shared validation fixes, compared all 911 files under `app/src/main`, `app/src/test`, and `app/src/androidTest` with `/tmp/safar-habit-checks-20261020`: zero differences. Preserved the Study Circle theme import and Exam Planner observable locale fix.

The matching source passed 774 JVM tests across 130 suites, full instrumentation-source compilation, debug/release compilation including Hilt, and release lint with zero errors (906 warnings and eight hints remain). Evidence: `Outputs/logs/habit-reminders-2026-10-20/verified-checks.log` and its `jvm-results` directory. This follow-up did not rebuild APK/AAB files or open an emulator. Earlier preview APKs predate these final shared validation fixes.
