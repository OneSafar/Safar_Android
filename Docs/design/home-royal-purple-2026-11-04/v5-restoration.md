# Requested v5 Home restoration — 4 November 2026

This follow-up supersedes the static hero and added phone shortcuts described in the earlier implementation notes.

- Removed the Study with me card and Home/Plan/Focus/Journal/You bottom bar.
- Replaced the tall static calm-mind hero with the five v5 module images, headlines, descriptions and destinations: Ekagra, Nishtha, Mehfil, Dhyan, and Exam Planner. The hero now starts at 166dp rather than up to 260dp and grows only when text requires it.
- Restored the v5 YouTube Focus / Habit Tracker / Timetable banner carousel above Your tools, using the exact original images. Both carousels rotate while Home is active; the module dots and banner swipes permit manual navigation.
- Kept the purple circular Your tools grid and its destinations unchanged.
- Renamed the filled dashboard action to Go to Dashboard and removed its flanking icons.
- Ported v5's Timetable entry route and initial-section flag so that its banner opens Timetable directly instead of falling back to the habit overview.

No existing raster assets changed. The Timetable banner was copied byte-for-byte from v5; the other banner and module images already match v5. No image generation was used. Tablet Home keeps its existing renderer.

Validation: production debug app and instrumentation APK builds passed. All six USB-device layout/navigation scenarios passed at 320/375/414/768dp, light/dark, and font scales 1.0/1.15/1.45. Checks cover every module description/route, all three banner routes, the unchanged tool routes, the dashboard action, and absence of the removed card and shortcut bar.
