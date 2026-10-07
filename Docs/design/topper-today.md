# Today: UI and interaction design

Hallmark critique: Philosophy 5 · Hierarchy 5 · Execution 4 · Specificity 5 · Restraint 5 · Variety 4.

Use the existing SAFAR Material colour scheme and typography. Keep Android navigation, task dates, completion history and save handling intact. This is a single-screen redesign.

1. Compact overall batch progress.
2. Two-choice study-mode control, with a short explanation of the selected mode.
3. Today’s summary shows actual progress and a thin progress bar. Personal pending tasks are grouped under English, Reasoning, Maths and GK/GS in that order, then numeric lecture order within each subject. Completed planned tasks remain counted and are grouped under a separate collapsed section. Subject groups can be folded individually.
4. Own pace: place the add area before the long checklist so adding never requires scrolling through all planned tasks. Use compact available lecture rows with a bold subject heading, prominent lecture number, topic and a one-tap Add action. Pending task date editing uses an accessible 48 dp calendar button rather than a repeated full text row. Hide unavailable subjects behind a disclosure. Browse all lectures remains an optional Library link.
5. Upcoming personal plans or official classes start collapsed, with a count.
6. Continue learning retains the existing subject tiles.
7. Completed lectures remain collapsed.
8. Own pace: Parmar's schedule is clearly marked For reference. No competing task counter or completion checkboxes; details and dates remain accessible.

Spacing: 4/8/12/16/24 dp. Main card radius 20 dp, secondary row radius 14 dp. Accent indicates selected mode, primary Add and progress. Subject dots use existing subject colours. Secondary information uses onSurfaceVariant. Completion feedback is a compact factual notice that offers Undo for eight seconds without dismissing a newer notice.

Completion checkboxes occupy 48 dp touch targets. An Undo action in the completion banner restores the unfinished lecture while retaining its watch date. Remove from today clears only an unfinished watch assignment; revision removal explicitly confirms clearing all pending revision dates. Each study mode has an independent information button that does not change the selection. Touch targets are at least 48 dp. The mode switch stacks on narrow screens or enlarged text. Add rows place their action below the content when width or text scale requires it. Topics wrap rather than truncate. Disclosures expose expanded/collapsed state; checkboxes retain descriptive labels. Both light and dark themes use existing theme tokens.

The HTML reference is illustrative, using data from the screenshots. The Android implementation is authoritative. Preview interactions do not call the backend. Device checks and screenshots remain with the user; scoped unit tests and Kotlin compilation are automated.
