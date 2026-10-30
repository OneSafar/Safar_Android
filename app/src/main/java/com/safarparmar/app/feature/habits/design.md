# Safar Habit Tracker — mockup-led design

Use the supplied IMG-20261028-WA0005.jpg for the visual structure: warm ivory, forest green pills and actions, rounded cards, a sprout banner, progress ring with category legend, pastel timeline rows and emoji checklist rows.

## Four primary screens

- Overview: Overview / Timetable / Habits navigation, Stay on Track banner, date strip, progress card, a short daily-habit preview with Add Habit, and schedule preview. Daily habits stay before the schedule preview so habit creation remains easy to find. No floating button covers content.
- Timetable: opens as its own page with a back arrow. Its single Day / Week / Subjects selector is never shown below the overview navigation. Pastel timed activities and a filled Add Timetable Activity button follow the reference.
- Habits: opens as its own page with a back arrow, Add Habit, daily checklist, and a plainly labeled From your timetable section for explicitly linked activities. No extra tab strip or empty linked-habit promotion.
- Add/Edit Timetable Activity: the existing form, category selector, title, subject, date, time, repeat, reminder, colors, Also show in Habits toggle, Save and Delete. The toggle explains that one check updates both lists.

## Deliberate user-requested changes from the image

No bottom navigation, no stacked tab bars or underlines, no floating habit-creation control, no random habit icons. Users choose emoji through the AndroidX palette. Timetable actions use clear labels rather than sessions. Habit history and management remain in the overview options menu. Existing responsive save behavior remains intact.

The overview progress counts real habits and timetable activities once each; the linked representation does not add a duplicate task. Data and backend contracts are unchanged.
