# Ekagra feature tutorial

Open Ekagra → overflow menu → **Learn Ekagra**. An invitation is offered once when
Ekagra is idle and no save/setup flow is pending. Dismissal and lesson completion
use independent, versioned DataStore keys, unrelated to the retired butterfly tours.

Four short guides replace the original seven choices:

- Start a focus session (five steps): modes and timer, Settings, Music/Theme together, Start, and Pause/End.
- Enable Kavach with the timer (four steps): With Ekagra profile, blocked apps, enable before Start, and Active status.
- Join a study circle (four steps): Join Group, public/code joining, create Public/Private, and circle selection.
- Save and review a session (three steps): name/tag/link options, confirmation, and History.

Only Settings and Music/Theme steps show the real bottom navigation, with both
personalization entries highlighted together. The core avoids separate settings
forms. Details remain behind Info. Practice uses the live timer, navigation,
Kavach profile selector, study-circle rows, and create/join form presentation.
All callbacks are inert; creating or joining a circle never touches the account.
All copy has English, Hindi, and Hinglish resources.

## Isolation and lifecycle

`EkagraTutorialViewModel` owns tutorial navigation only. It has no service,
repository, permission, or playback dependencies. Its ViewModel lifetime retains
the step across configuration changes; it deliberately has no SavedStateHandle,
so a new process starts with practice closed.

`EkagraTutorialDemoState` maps each named scene to deterministic presentation.
Next/Back control the demonstrations. The spotlight consumes stage input; the
actual screen underneath the full-screen practice dialog stays mounted unchanged.
Closing practice discards the sample state. Only invitation/completion bookkeeping
is persisted. Recheck eligibility while practice is open to handle an externally
started timer or a newly pending flow.

Settings, Pomodoro, audio, themes, save confirmation, History, and circle summaries
reuse presentation extracted from the live app. Production containers retain their
existing callbacks and lifecycle work. Practice supplies sample values and inert
callbacks. In particular, never use the live `KavachSummaryPills`, `FocusHistoryTab`,
or `AudioLibraryPanel` inside practice: use their presentation counterparts.

The practice timer shares `EkagraTimerBackground` with the live timer, including
the selected visual theme, app light/dark appearance, and break palette. Its ink
also follows the live timer; no tutorial-only canvas or forced dark mode is used.
The shared backdrop extends behind the translucent guidance card.

Every practice panel paints a surface matched to its content colors. The audio
library also paints its own palette background, and selected theme cards pair
primaryContainer with onPrimaryContainer. Screenshot tests check at least 4.5:1
text contrast for circle choices in light and dark mode.
Large timer digits meet 3:1 contrast, including different selected timer themes.

The app-picker step uses `AppPickerContent`, the same header, search, bulk
selection controls, rows, switches, and Save apps button as the live screen.
Practice supplies local sample apps, no-op callbacks, and disables row entrance
animation; only `AppPickerScreen` loads installed apps and persists selection.
The floating overlay and circle chooser are local illustrations. Practice never
enumerates installed apps, creates a system overlay, or navigates to real circles.

## Adding a feature guide

Use `FeatureTutorialStep` with stable IDs and resource-backed copy, then annotate
controls with `Modifier.tutorialTarget(id)`. The modifier is inert outside
`FeatureTutorialHost`. Targets register measured window bounds and a
BringIntoViewRequester, and unregister by identity on disposal.

Compose the host within the same window as its targets. Shared dialog/sheet
presentation can render in the practice window, as Ekagra does. If another feature
needs real separate windows, move the host into that window and retain the same
controller. Do not use coordinates measured in a different window.

The timer preview uses Ekagra’s normal toolbar, summary, timer and bottom navigation.
The stage keeps its full screen constraints; no tutorial header or guidance area
pushes the navigation upward. A rounded translucent speech bubble sits above
Titli beside the measured spotlight, clamped to the available screen. Its 21sp heading
and 13sp instruction follow a roughly 1.618 size ratio. A compact step counter
and one short instruction are always visible; the info button opens detailed
copy in a dismissible dialog. It remains
scrollable at large font sizes. Targets that cannot be found expose Retry, Back,
and Skip. Canvas transitions use Compose animation timing, honoring the system's
animator duration scale. The guidance is announced as a polite live region while
the display-only sample controls are hidden from accessibility traversal.

## Verification

Production variant: `./gradlew :app:compileProdDebugKotlin`.

Focused unit tests:

```sh
./gradlew :app:testProdDebugUnitTest \
  --tests '*EkagraTutorialTest' \
  --tests '*EkagraTimeAccountingTest' \
  --tests '*EkagraJournalHistoryTest'
```

Compose tests are `EkagraTutorialInstrumentedTest` and
`FeatureTutorialInstrumentedTest`. They exercise every lesson, replay, navigation,
large text, target scrolling/re-registration, missing-target recovery, input
isolation, invitation/busy behavior, translations, and preference versioning.
Run on an emulator in portrait and landscape; also verify with animator duration
scale zero. The tests need no signed-in user, timer service, or Hilt host.

At implementation time the full androidTest source set has unrelated stale Habits
migration/reminder tests that do not compile. Run these tutorial tests using a
temporary Gradle init script that limits both the androidTest java and kotlin source
directories to copies of the two tutorial test files. Do not remove or change the
unrelated tests to get a green tutorial run.

## Help access

Titli appears beside the existing overflow menu; Learn Ekagra also remains in
the menu. First use introduces Titli, then begins the core guide. Later taps open
the four-guide list.
There is no app-wide Help header or reserved space above feature toolbars.
Practice windows retain their own Help action for returning to the lesson picker.

The shared practice window captures the feature’s `LocalDensity` before opening
its Android dialog and reapplies it inside. This preserves the app’s display
density and font-scale policy for the picker, sample screens, and guidance.
Info content also uses that captured scaling. Regression checks compare dp
control bounds and sp text bounds with the feature across scaling changes.
