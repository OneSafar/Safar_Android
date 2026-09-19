# Habit Tracker v4 implementation notes

This folder is the updated SAFAR Habit Tracker feature based on the accepted HTML prototype (`habit_tracker_clean_simple_v5.html`).

## UI changes

- Soft cream / purple / sage palette aligned to the HTML prototype.
- Today / Weekly / Monthly segmented tabs.
- Today: progress summary + clean list with edit/check actions.
- Weekly: aggregate `completed / scheduled` progress for each day, followed by per-habit weekly cards.
- Monthly: horizontally scrollable habit filters, calendar counts, day-detail sheet, streak and elapsed-opportunity completion rate.
- Habit form is intentionally flat: no nested input cards and no Advanced section.
- Weekday chips are rendered only when `Specific days` is selected.
- Long habit names use ellipsis / bounded chip widths to avoid layout overflow.
- Bottom content padding prevents the New Habit FAB from covering tracker content.

## Data / logic changes

Room schema is now version 4.

A new `habit_schedule_revisions` table preserves recurrence history. Editing a schedule no longer makes historical weeks/months use the new recurrence pattern.

Migration `MIGRATION_3_4` creates an initial revision for every existing habit using the currently stored schedule. From v4 onward, every schedule change is versioned by effective date.

Habit creation and schedule edits update the habit row + schedule revision inside a Room transaction.

Completion rate counts only elapsed scheduled opportunities. Future days do not count as failures.

The aggregate current streak uses a bounded 730-day history to keep memory/work predictable while still supporting long streaks.

## Integration

`HabitModule.kt` already registers `MIGRATION_2_3` and `MIGRATION_3_4`.

If your production app has since merged this feature into a different central Room database, merge the new entity and migration into that database instead of copying the standalone `HabitDatabase` unchanged.

## Validation performed here

- Kotlin source was checked for parse/syntax errors with `kotlinc` (no parse errors).
- Schedule revision behavior was sanity-tested for a recurrence change across historical dates.
- The uploaded ZIP only contained the `habits` feature folder, not the full Android Gradle project, so a complete Android/Compose Gradle build could not be run in this environment.
