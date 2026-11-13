# Exam Planner — Syllabus tab: UX audit & simplification plan

Date: 2026-11-11 · Checked on: Moto ZA222QNQ7D (720×1600) with the real "SSC CGL" plan (10 subjects / 54 chapters / 378 topics)
Screenshots: `Docs/syllabus-ux-audit/`

---

## 1. What the Syllabus tab is today (full map)

```
Syllabus tab (SyllabusSubjectsScreen.kt)
├─ Plan-title chip (PlannerHome, StudyPlannerScreen.kt:1488)  → Exams list
├─ Plan title AGAIN (SyllabusMagazineHeader)                   → tap = full-title card pinned above bottom bar
├─ Progress card: "syllabus done 4%", counts, "41 chapters not started"
├─ "Exam date passed" / "N days left" text                      → secretly opens Plan Settings
├─ [Change your study plan] button → sheet with 5 rows
│    ├─ Add subject               (same as "+ Add subject" lower down)
│    ├─ Change plan details       → Plan Settings sheet
│    ├─ Change order or topic size → help dialog (instructions are wrong — see bugs)
│    ├─ Save syllabus for reuse   → confirm dialog
│    └─ Create new study dates    → confirm → autoDistribute(strategy="sequential")
├─ Ratings-changed bar (conditional) → rebuild / set exam date
├─ Course-map nudge "Study first: X · 3 topics late  Open >"
├─ Search field (filters subjects only)
├─ [Organize] toggle + hint text
├─ "Your subjects"  [+ Add subject]
└─ Subject row (name, counts, state, bar, ⋮, "Open >")
     ⋮ = Move up/down (only in Organize), Add chapter, Rename, Mark all done, Delete
     tap → Subject screen (full screen, own top bar + 2nd Titli button)
          ├─ counts + %, "Chapters  [+ Add chapter]"
          └─ Chapter row (name, state, bar, ⋮, chevron, Easy/Normal/Tough chips on weighted plans)
               ⋮ = Move up/down, Rename, Mark all done, Delete
               tap → Topics bottom sheet (capped at 60% height)
                    ├─ "Topics  [+ Add topic]" (inline text row)
                    └─ Topic row: one-way checkbox, name, effort bars, date, [Study today], ⋮
                         ⋮ = Move up/down, Rename, Change date, Mark done, To revise, Delete
                              → date picker / RevisionScheduleSheet / dialogs
```

Counted on the device: **11 separate blocks sit above the subject list**, so on a normal phone **only 1 of 10 subjects is visible** without scrolling (`03_syllabus_top.png`). Reaching a single topic takes **tab → subject screen → chapter sheet → ⋮ → action → dialog** (5 levels).

---

## 2. Why users are confused (root causes)

1. **No single job.** The tab tries to be 4 things at once: progress report, syllabus editor, scheduler ("Create new study dates", rebuild bar), and settings hub. Users can't predict what it's for, so they ignore it.
2. **Plan settings are hidden.** On Today there's a clear "⚙ Plan settings" button. On Syllabus, settings are reachable only via (a) the "Exam date passed" text, which doesn't look tappable, or (b) Change your study plan → Change plan details (2 taps, different name). This is the "can't change plan settings" complaint.
3. **Edit actions are everywhere and look the same as study actions.** Add / rename / delete / reorder / mark-done / study-today / revise / change-date are all mixed on the same rows and in ⋮ menus at 3 levels.
4. **Several flows are broken or tell the user wrong things** (section 3). These are what make people give up.
5. **Syllabus and Today disagree.** Today says "345 Not Assigned Topics · Missed 11" (`10_today.png`). Syllabus says "Study first: Computer Fundamentals · 3 topics late". The most important fact — **91% of the syllabus has no study date and the exam date has passed** — isn't shown on Syllabus at all.

---

## 3. Bugs / misleading behaviour found (verified)

| # | Problem | Where | Verified |
|---|---|---|---|
| B1 | **Changing the exam date never offers to reschedule.** `PlanSettingsSheet` takes `onExamDateChanged` but never calls it, so `RescheduleFlowSheet`, `armRebuild()` and Syllabus's `pendingRebuild` branch are all dead code. Topics keep their old dates after a date change. | `plan/PlanTabV2Components.kt:271` (param only), `PlanTabScreen.kt:271-293`, `SyllabusSubjectsScreen.kt:296` | code (grep: no call site) |
| B2 | **Two "Create new study dates" buttons use different algorithms.** Syllabus sheet hard-codes `strategy="sequential"`; Plan Settings hard-codes `"interleaved"`. Neither uses the user's saved `preferredStudyStrategy`. Same label, different result. | `SyllabusSubjectsScreen.kt:304-308`, `PlanTabV2Components.kt:483` | code |
| B3 | **Help dialog gives wrong instructions.** "Press and hold a subject… then move it" — but drag only works after tapping "Organize". On the device, press-and-hold just **opened the subject**. | `SyllabusSubjectsScreen.kt:1005`, `canReorderSyllabus` at :293 | device (`06_after_drag.png`) |
| B4 | **Help dialog points to a control that may not exist.** "Choose Easy, Normal, or Tough" — those chips are hidden on "Same every day" plans (`weightedPlanning == false`). The SSC CGL plan has none. | `SyllabusSubjectsScreen.kt:465, 1006` | device |
| B5 | **Sheet copy is false:** "Your plan won't change until you save." There's no save; actions apply immediately. | `strings.xml:1355` | device (`04_change_sheet.png`) |
| B6 | **Search only filters subjects.** Searching "Syllogism" shows the parent subject; opening it shows all chapters unfiltered; the topic is 2 more levels down with no highlight. | `SyllabusSubjectsScreen.kt:285-292`, ChaptersPane uses unfiltered `subject.chapters` | code |
| B7 | **Topic checkbox is one-way** (`enabled = !done`) and the ⋮ has no "Mark not done". A mis-tap can only be fixed from the 4-second undo snackbar or the Today › Done tab. | `SyllabusRedesign.kt:950, 1028` | code |
| B8 | **"To revise" is offered for topics never studied** ("Not planned", unchecked). | `SyllabusRedesign.kt:1031` | device (topic ⋮) |
| B9 | **Topics sheet is capped at 60% height** — only ~3 topics visible for an 8-topic chapter; each row repeats a "Study today" pill. | `ChapterTopicsSheet.kt:94` | device (`07_topics_sheet.png`) |
| B10 | Plan title shown twice; tapping the 2nd one pins a "full title" card above the bottom bar. | `StudyPlannerScreen.kt:1488`, `SyllabusRedesign.kt:108`, `SyllabusFullPlanTitleCard` | device |
| B11 | Two Titli buttons on the subject screen (app bar + page). | `SyllabusSubjectsScreen.kt:410` | device |
| B12 | A second copy of the Syllabus screen exists as a standalone route (`syllabus/subjects/{planId}`), opened from the Plan tab's empty state (`PlanTabScreen.kt:538`). Same screen, different chrome. | `Routes.kt:59`, `SafarNavGraph.kt:553` | code |
| B13 | Snackbar grammar: "1 topics marked as done". | `StudyPlannerViewModel.kt:2044` | code |
| B14 | (Outside tab) Exams list ⋮ is announced as "Topic options" to TalkBack. | Exams list card | device |

---

## 4. The new job of the Syllabus tab

> **Syllabus = "How ready am I for the exam, and what's left?"**
> Today = what to do now. Calendar = when. **Syllabus = how much is covered.** Progress = trends.

Everything on the tab must serve that one question. Editing the syllabus becomes a mode you enter on purpose, not something sprinkled on every row. Scheduling and settings live in **one** place each.

This gives the tab a reason to be opened every few days: it's where you *see the syllabus shrink*, and where the planner tells you honestly whether you'll finish in time.

---

## 5. New layout (phone)

```
┌────────────────────────────────────────────┐
│ [SSC CGL ▾]                 ⚙ Plan settings │  ← same PlanHomeHeader as Today (fixes B10, settings discoverability)
├────────────────────────────────────────────┤
│ READINESS CARD                              │
│  4% covered · 15 of 378 topics             │
│  ▓░░░░░░░░░░░░░░░░░░░░░                     │
│  ⚠ 345 topics have no study date yet       │  ← ONE status line, picked by priority (5.1)
│  [ Give them dates ]                        │  ← ONE action button
├────────────────────────────────────────────┤
│ 🔍 Search topics                    [Edit]  │  ← Edit replaces "Organize" + "Change your study plan"
├────────────────────────────────────────────┤
│ General Intelligence & Reasoning   6%  ▓░░ │
│   2 / 34 · 3 late                           │  ← compact 2-line row, orange "late" when >0
│ Quantitative Aptitude             10%  ▓░░ │
│ English Comprehension              2%  ░░░ │
│ General Awareness                  0%  ░░░ │
│ …                                           │  ← target: 6+ subjects visible on 720×1600
└────────────────────────────────────────────┘
```

### 5.1 Readiness card — one status, one button

Pick the **first** matching state; show only that one. This replaces: progress card, "days left" text, Change-plan button, ratings bar, course-map nudge (5 blocks → 1).

| Priority | Condition | Line (verbatim) | Button | Action |
|---|---|---|---|---|
| 1 | exam date passed | "Your exam date has passed." | **Set new exam date** | open Plan Settings with `focusExamDate = true` |
| 2 | no exam date | "Add your exam date to see if you'll finish in time." | **Add exam date** | same |
| 3 | unscheduled > 0 | "{n} topics don't have a study date yet." | **Give them dates** | open existing `UnscheduledTopicsScreen` (same as Today's "Schedule") |
| 4 | ratings changed (`pendingRatingChapterIds`) | "You changed chapter ratings. Update your dates to match." | **Update dates** | `rebuildAfterRatingChanges()` |
| 5 | late topics | "{n} topics are late in {chapter}." | **Catch up** | open that chapter's topic sheet (existing nudge path) |
| 6 | forecast after exam | "At your pace you'll finish on {date} — {k} days after your exam." | **Study more each day** | Plan Settings `focusDailyGoal = true` |
| 7 | on track | "On track. You'll finish around {date}." | — | — |

Top line always: "**{p}% covered · {done} of {total} topics**" + "{d} days left" on the right (from `daysUntil`).
Forecast date: `velocityForecastCompletionDate` already computed in `PlannerInsightsCalculator` (states 6/7). If null (no recent pace), skip 6/7 and show "On track · no late topics".

### 5.2 Study mode (default) vs Edit mode

**Study mode** — only progress actions:
- Subject row: tap → subject. **No ⋮, no "Open" label.**
- Chapter row: tap → topics sheet. **No ⋮.** Rating shown only as a small read-only tag ("Tough") if set.
- Topic row: **two-way checkbox** · name · **one trailing date chip**:
  - "Today" (accent) / "14 Nov" / "Late · 2 Nov" (orange) / "+ Plan" (unplanned)
  - Tap chip → tiny menu: **Study today · Tomorrow · Pick a date**. (Merges "Study today" pill + "Change date".)
  - Tap row → topic actions sheet: **Focus now** (same `onFocus` as Today) · **Revise** (only if done) · **Mark not done** (only if done).

**Edit mode** — tap **[Edit]**; header changes to "Editing syllabus · [Done]":
- "+ Add subject / + Add chapter / + Add topic" appear.
- Each row shows a drag handle (≡) — drag works immediately, no long-press guessing (fixes B3).
- Each row shows ⋮ = Rename · Mark all done · Delete.
- Chapter rows show Easy/Normal/Tough chips (weighted plans only).
- Leaving Edit mode with order/size changes → one bar: "Your syllabus changed. Update your dates?" [Update dates] — the single place a rebuild is offered from Syllabus.

### 5.3 Search
Show **flat topic results** (not subjects): each result = topic name + "Subject · Chapter" + date chip, with the same row actions. Tap opens that chapter's sheet scrolled to the topic. Subjects/chapters still match by name and appear as a header row above their topics.

### 5.4 Topics sheet
- Raise cap from 60% → ~92% (or open as a full-height sheet with `skipPartiallyExpanded`).
- Header: "{chapter} · {done}/{total} done" + progress bar.
- When a chapter reaches 100% show snackbar "Chapter finished · {x} of {y} chapters done" (reuse `mutateSelected(successMessage=…)`).

---

## 6. What to remove

| Remove | Where its job goes |
|---|---|
| "Change your study plan" button + 5-row sheet (`SyllabusChangePlanSheet`, `SyllabusChangePlanBand`) | Add → Edit mode · Plan details → ⚙ Plan settings · Order/size → Edit mode · Save for reuse → Plan settings · New dates → Plan settings (single button) |
| Order/size help dialog (`showChangeHelp`) | Edit mode is self-explanatory (visible handles) |
| `showNewDatesConfirm` dialog + `buildSchedule` in Syllabus | Plan settings' single "Create new study dates" |
| Second plan title + `SyllabusFullPlanTitleCard` | Shared header chip |
| "N days left / Exam date passed" text button | Readiness card |
| "N chapters not started" line | Not needed; subject bars show it |
| `SyllabusRatingRebuildBar` | Readiness card priority 4 |
| `SyllabusCourseMapNudge` as its own block | Readiness card priority 5 (keep `courseMapNudge()` logic) |
| "Organize" text button + hint | [Edit] |
| ⋮ menus in Study mode, "Open" label on subject rows | Edit mode / row tap |
| "Study today" pill on every topic row | Date chip |
| Second Titli button on subject screen | keep one |
| Standalone `syllabus/subjects/{planId}` route usage from Plan tab | `actions.setSection(PlannerSection.SYLLABUS)` (already done for the other Plan-tab path at `StudyPlannerScreen.kt:1546`) |

---

## 7. Plan settings — make it the one place

Same sheet everywhere (Today header and Syllabus header both open it with the same args, including `onExport` and `onReset` — Syllabus currently passes `null` / `{}`).

Sections:
1. **Exam** — title, exam date
2. **Daily study** — topics per day, rest days, **Study style: Balanced / Deep focus** (new row; uses existing `setPreferredStudyStrategy`; today this is only chosen at creation or inside the Rebuild sheet)
3. **Dates** — "Create new study dates" (**one** button; uses `preferredStudyStrategy`, fixes B2)
4. **Syllabus** — "Save syllabus for reuse" / "Update saved syllabus"
5. Export PDF · Reset plan

And fix B1: in `performSave()`, if `examDate` changed **and** the plan has scheduled topics, call `onExamDateChanged(newDate)` so `RescheduleFlowSheet` appears. Host the reschedule sheet wherever settings is hosted (or lift both into `PlannerHome` so Syllabus gets it too).

Out of reach without server work (note only): switching "Same every day" ↔ effort-weighted after creation — `weightedPlanning` is not in `UpdatePlanRequest` (`PlannerApi.kt:278`).

---

## 8. Retention hooks (all reuse existing data)

1. **Honest finish-date forecast** on the readiness card (5.1 states 6/7). "You'll finish 12 days after your exam" is the single strongest reason to come back and adjust.
2. **"Late" counts on subject rows** (orange) — the list itself tells you where to go.
3. **Focus now from any topic** — studying from Syllabus starts an Ekagra session, same as Today's Focus button; progress closes the loop back into the syllabus %.
4. **Chapter-finished moments** — snackbar + bar animation when a chapter hits 100% ("12 of 54 chapters done").
5. **Today → Syllabus link**: the "Subject · Chapter" subtitle on Today's task rows opens Syllabus at that chapter (reuse `activeSubjectId` + `openTopicsChapterId`, already used by the nudge). Makes Syllabus part of the daily path instead of a separate destination.
6. **Same numbers everywhere**: readiness card and Today must report the same unscheduled/late counts (both from `flattenTopics()`), so trust isn't broken.

---

## 9. Build order (each stage ships alone)

**Stage 1 — fix what's broken (small, no redesign)**
B1 (exam-date → reschedule), B2 (one strategy source), B3/B4/B5 (delete help dialog + false copy), B7 (two-way checkbox), B8 (Revise only if done), B9 (sheet height), B10/B11 (dup title, dup Titli), B13 (plural), Syllabus passes `onExport`/`onReset` to settings.

**Stage 2 — header + settings**
Shared `PlanHomeHeader` on Syllabus with ⚙ Plan settings; readiness card (5.1) replacing 5 blocks; remove Change-plan sheet; move Save-for-reuse + study style into Plan settings.

**Stage 3 — Study/Edit modes**
Compact subject rows, Edit toggle with drag handles + add + ⋮, date chip on topic rows, topic actions sheet with Focus now.

**Stage 4 — search + retention**
Flat topic search, chapter-finished snackbar, Today subtitle → Syllabus deep link, late counts on subject rows.

### Test list
- Change exam date with scheduled topics → reschedule sheet appears (from Today **and** Syllabus).
- "Create new study dates" with Deep focus saved → result is sequential; with Balanced → interleaved.
- Plan with past exam date → card shows priority 1 only.
- Plan with 345 unscheduled → card shows priority 3; button opens the same screen as Today's Schedule; counts match Today.
- Edit mode: drag subject/chapter/topic without long-press; Done → "Update your dates?" bar appears only if order/size changed.
- Same-every-day plan: no difficulty chips anywhere, no mention of them.
- Uncheck a done topic in Syllabus → status back to pending, Today updates.
- "To revise" absent on unstudied topic.
- Search "Syllogism" → topic row shown directly; tap opens sheet at that topic.
- 720×1600 phone: ≥6 subjects visible on first screen.

---

## 10. Open questions (with recommendation)

1. **Should Edit mode hide add/rename/delete entirely in Study mode?** Recommend **yes** — post-creation edits are rare; hiding them is the biggest clutter cut. Risk: someone looking for "Add subject" — mitigated by [Edit] being next to search.
2. **Flex/Strict planning mode toggle** (memory says it existed in Plan settings; it's not in the current sheet). Recommend leaving it out — one fewer concept.
3. **Tablet two-pane** keeps the same rules; Edit toggle sits above the subject pane.
