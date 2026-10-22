# Goals guided practice

Tap Titli in the existing top bar on Goals, or open **Learn Goals** in the action strip. A dismissible invitation is
shown once when goals have finished loading and no form, save, or goal action is
pending. Tutorial invitation and lesson completion use `goals` version 1 keys,
independent of Ekagra and the disabled butterfly tours.

Four short guides replace the previous six choices:

- Create and finish a goal (five steps): Add, name/create, checkbox in Today, optional study time/completion, and History.
- Plan or repeat a goal (four steps): future date, Upcoming, daily repeat, and reusing tasks.
- Edit or recover a goal (four steps): goal menu, edit, missed work, and restore.
- Review progress (three steps): Status, Insights, and linked Ekagra time.

Naming and creation share a step. The completion preview shows time controls and
completion choices together. Extra choices stay in Info rather than separate
walkthrough steps. The stationary Titli action returns to the four-guide picker.
Compact action instructions and expanded Info copy have English, Hindi, and
Hinglish resources.

## Safe presentation

`GoalsTutorialViewModel` and `GoalsTutorialDemoState` have no service, repository,
permission, or account dependencies. Practice is a modal window using the shared
`FeatureTutorialHost` and `FeatureTutorialPracticeWindow`, exactly as Ekagra.
The actual Goals canvas keeps its full screen constraints beneath Titli’s 96%
opaque speech bubble, without a separate tutorial header or reserved card area. The
shared host supplies the 38% spotlight dimming, 21sp/13sp heading/instruction,
compact progress, Info details, and Back/Next/Skip. Header spacing, safe insets,
system-bar appearance, and system Back handling also use Ekagra’s shared window.
Next/Back prepare deterministic sample scenes. The scrim
intercepts sample control input. The underlying screen stays mounted with its
selection, scroll position, and forms unchanged. Only tutorial progress persists.
Configuration changes retain the step; a new process starts with practice closed.

`GoalScreenPresentation` contains the header, shared create/edit form, study-time
controls, completion actions, repeat picker, recovery list and missed-day choices.
Live screen callbacks retain their existing goal operations. Practice supplies
sample data and inert callbacks. `HistoryTab` fetches linked focus contributions;
`GoalHistoryContent` renders supplied contributions without fetching live data.

Do not add the live ViewModel-backed history container or account navigation to
practice. Keep samples assigned to the demonstrated date. Ekagra contributions
are separate from manual minutes; never teach recording the same minutes twice.

## Validation

Use `:app:compileProdDebugKotlin` and focused unit tests `GoalsTutorialTest`,
`GoalScreenLogicTest`, and `GoalHistoryVisibilityTest`. Compose tests are
`GoalsTutorialInstrumentedTest` and the shared `FeatureTutorialInstrumentedTest`.
They cover every lesson and target, Back/Next/replay, compact Info, large text/dark
mode, translations, eligibility, preference independence, missing-target recovery,
and input isolation without a Hilt host. Pixel checks verify the guidance
composites over the real Goals canvas in both light and dark mode.

The repository's unrelated stale Habits androidTest sources still fail compilation.
For a focused run, use a temporary Gradle init script pointing both androidTest
source directories at copies of the two tutorial test files. Package debug and
androidTest APKs with `:app:packageProdDebug :app:packageProdDebugAndroidTest`;
the assemble finalizer has an unrelated output-copy dependency issue when building
both APKs together. Install with `adb -s <serial> install -r` to preserve existing
app data, then run the two classes with the AndroidJUnitRunner. Never uninstall or
clear the user's app data for a tutorial check.

The shared practice window captures the feature’s `LocalDensity` before opening
its Android dialog and reapplies it inside. This preserves the app’s display
density and font-scale policy for the picker, sample screens, and guidance.
Info content also uses that captured scaling. Regression checks compare dp
control bounds and sp text bounds with the feature across scaling changes.
