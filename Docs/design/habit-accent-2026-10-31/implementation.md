# Habit Tracker color accent alignment

Color-only update based on compactProfileColors, MehfilFlatColors and TutorialVisualStyle. Core light roles: blush FFF5F0 canvas, FFFDFC paper, 39132E ink, 75616F muted, 542347 action, EDE0F4 selection, FFCBBE peach and C9EFE0 mint. Dark mode retains the canonical 131316 canvas with warm 211921/342735 surfaces, FFF0F6 ink, CFBECB muted, F0BEDC action and 39132E action foreground. Destructive states remain red.

HabitTheme.kt retains existing token names and getters, replacing their old forest-green/cream values. HabitAccentTheme scopes inherited Material colors and the existing Composables accent to HabitScreen and HabitInsightsScreen; shapes and typography are inherited. Timetable pastel categories retain the same indices/order and now use mint/peach/lilac/blush. The existing plant illustration colors remain unchanged. Heatmaps retain levels and thresholds, now rendered with a plum/lilac density ramp. Graph card default green/blue fills become plum. Action/completion foregrounds now use OnRoyalPurple for contrast in dark mode.

No button layout, shape, position, size, typography, navigation callback, state or persistence changes. No dependencies or global palette modifications.

Source comparison against task-start snapshots verified unchanged dimension/shape/spacing/alignment/typography-bearing lines in all 11 changed component files; the additional palette file defines only color scopes/tokens. Core light/dark text and button pairs passed 4.5:1 contrast calculations. Scoped whitespace checks passed. No Kotlin compilation, Gradle build, tests, installation or device inspection was performed. Visual validation remains pending.
