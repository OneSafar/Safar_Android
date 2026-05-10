No: the risky plan is “make every website button appear somewhere on Android.” That creates a cluttered app. The better plan is **feature parity by capability**, not **button parity by layout**.

Your Android app should expose every backend capability, but not always as the same visible button. Some actions should become bottom-sheet actions, overflow-menu items, swipe actions, or contextual actions.

Below is the implementation plan I’d use.

---

# Study Planner Android Implementation Plan

Assumption: you are building Android with **React Native / Expo TSX** because your existing files are TSX. If you are using Kotlin/Jetpack Compose, the screen structure and API contract still apply.

## 0. Core rule

Do **not** port `StudyPlanner(1).tsx` directly.

That file is too large and mixes models, API calls, onboarding, bulk import, topic editing, calendar logic, premium gating, insights, UI rendering, and modals in one place. The Android app should instead have:

```txt
planner/
  api/
  model/
  hooks/
  screens/
  components/
  utils/
```

Your goal is:

```txt
Same backend capability
Different Android-native UI
Same user outcomes
Less visual density
```

---

# 1. First build a feature parity contract

Before UI, create a **feature parity matrix**. This is what prevents missing backend actions.

Your web app currently includes these major feature groups:

1. Plan dashboard / plan list
2. Quick start from exam template
3. Custom plan creation
4. Paste syllabus during plan creation
5. View existing plan
6. Today dashboard
7. Syllabus management
8. Calendar management
9. Plan settings
10. Auto-schedule
11. Reschedule / clear future dates / reset plan
12. Insights
13. Premium gates
14. Onboarding / beginner guide
15. Bulk add
16. File import for syllabus
17. Template import inside an existing plan
18. Rename / delete subject, chapter, topic
19. Topic status changes
20. Topic notes
21. Search and filters

The web files confirm the planner uses `today`, `syllabus`, `calendar`, `plan`, and `insights` as the main planner sections, and the core data model has `Plan`, `Subject`, `Chapter`, `Topic`, `CalendarItem`, and `PlannerInsights`. 

---

# 2. Android navigation structure

Use this structure:

```txt
PlannerStack
  StudyPlansScreen
  QuickStartFlow
    TemplatePickerScreen
    TemplateConfigScreen
    CustomPlanScreen
    CustomPasteSyllabusScreen
  PlannerHome
    Bottom Tabs:
      Today
      Syllabus
      Calendar
      Plan
      Insights
```

Use a **bottom navigation bar** for the five planner sections. Material Design says navigation bars are meant for switching between views on smaller handheld screens. ([Material Design][1])

Use a **top app bar** for current plan title, back button, and overflow actions. Material Design defines top app bars as the place for title and screen-level actions. ([Material Design][2])

Every tappable item must have at least a **48dp × 48dp** touch target. Android’s accessibility guidance explicitly recommends this for touch interfaces. ([Android Developers][3])

---

# 3. Required Android screen map

## A. `StudyPlansScreen`

Purpose: show all existing study plans and allow creation/deletion.

### Web capabilities to preserve

| Feature     |             Backend/API | Android placement   |
| ----------- | ----------------------: | ------------------- |
| Fetch plans |            `GET /plans` | screen load         |
| Open plan   |        local navigation | card tap            |
| New plan    |             local state | FAB / top-right `+` |
| Delete plan | `DELETE /plans/:planId` | card overflow menu  |

Your web page already fetches plan list from `/plans`, enters QuickStart when no plan is selected, and deletes plans through `DELETE /plans/:id`.  

### Android UI

```txt
Top App Bar
Study Planner                         +

[Plan Card]
SSC CGL 2026
84 days left · 6 subjects · 230 topics
Progress 42%
⋮

[Plan Card]
NEET Prep
19 days left · 5 subjects · 180 topics
Progress 67%
⋮
```

### Important Android decision

Do not hide delete behind hover. Use:

```txt
⋮ menu:
  Rename / Edit Plan
  Delete Plan
```

---

## B. `QuickStartFlow`

Purpose: create a plan either from a template or custom syllabus.

The web QuickStart supports loading templates, selecting template, configuring exam date, daily goal, off days, title, and then creating from `/plans/from-template`. It also supports custom plan creation via `POST /plans`, then creating subjects, chapters, topics, and auto-distributing.  

### Flow

```txt
Step 1: Choose Mode
  - Use Exam Template
  - Custom Plan

Step 2A: Template Picker
  - SSC CGL
  - Railway NTPC
  - Bank PO
  - JEE Mains
  - NEET UG

Step 3A: Configure Template
  - Plan title
  - Exam date
  - Daily goal
  - Off days
  - Generate Plan

Step 2B: Custom Plan
  - Plan title
  - Exam name
  - Exam date
  - Daily goal
  - Off days

Step 3B: Optional Paste Syllabus
  - Paste syllabus
  - Preview subjects/topics count
  - Create Plan
```

### API contract

| Capability                     | Endpoint                                                             |
| ------------------------------ | -------------------------------------------------------------------- |
| Load templates                 | `GET /plans/templates`                                               |
| Create from template           | `POST /plans/from-template`                                          |
| Create custom plan             | `POST /plans`                                                        |
| Add subject                    | `POST /plans/:planId/subjects`                                       |
| Add chapter                    | `POST /plans/:planId/subjects/:subjectId/chapters`                   |
| Add topic                      | `POST /plans/:planId/subjects/:subjectId/chapters/:chapterId/topics` |
| Auto-distribute after creation | `POST /plans/:planId/auto-distribute`                                |

### Android UI rule

Do not put template selection and configuration on one screen. Web can do that; Android should not.

---

# 4. Planner root layout

## `PlannerHome`

```txt
Top App Bar:
  Back
  Plan title
  More menu

Bottom Tabs:
  Today
  Syllabus
  Calendar
  Plan
  Insights
```

Use lazy loading per tab, but keep the loaded plan in a shared store.

Recommended state:

```ts
type PlannerStore = {
  plan: Plan | null;
  calendar: Record<string, CalendarItem[]>;
  insights: PlannerInsights | null;
  loading: boolean;
  error: string | null;

  fetchPlan(): Promise<void>;
  refreshCalendar(): Promise<void>;
  patchPlan(payload): Promise<void>;
  patchTopic(topicId, payload): Promise<void>;
};
```

Do not let every screen fetch independently. That causes stale UI.

---

# 5. `TodayScreen`

Purpose: “What should I study now?”

This should be the default tab after opening a plan.

### Must include

| Feature                            | Android UI                |
| ---------------------------------- | ------------------------- |
| Plan progress                      | progress card             |
| Days until exam                    | header metric             |
| Today’s planned topics             | checklist                 |
| Overdue topics                     | collapsible section       |
| Upcoming topics                    | compact list              |
| Quick actions                      | buttons/cards             |
| Open topic                         | topic detail bottom sheet |
| Mark done / in progress / revision | topic action buttons      |

### Layout

```txt
SSC CGL 2026
84 days left

[Progress Card]
42% complete
96 / 230 topics done
Required pace: 3.2 topics/day

Today
[ ] Polity · Parliament
[ ] Maths · Ratio
[ ] English · Error spotting

Quick Actions
[Add Topics] [Build Schedule]
[Open Calendar] [Insights]

Overdue
8 topics need attention
```

### Backend/API used

| Action                    | Endpoint                               |
| ------------------------- | -------------------------------------- |
| Load plan                 | `GET /plans/:planId`                   |
| Load calendar             | `GET /plans/:planId/calendar`          |
| Update topic status       | `PATCH /plans/:planId/topics/:topicId` |
| Update topic planned date | `PATCH /plans/:planId/topics/:topicId` |

Your web implementation already patches topics and refreshes calendar after topic changes. 

---

# 6. `SyllabusScreen`

Purpose: manage subject → chapter → topic hierarchy.

Do not use org-chart as primary Android UI. Your web code even forces `syllabusLayoutMode` back to `"org-chart"`, which is exactly the kind of web-first behavior that should not be copied to Android. 

## Android layout

```txt
Search syllabus...

Filter chips:
All · Todo · In Progress · Done · Revision

Subjects
Maths                         42%
12 chapters · 80 topics       >

Reasoning                     30%
9 chapters · 65 topics        >
```

Tap subject:

```txt
Maths

Algebra
  Linear Equations        Todo
  Quadratic Equations     Done

Geometry
  Triangles               In Progress
  Circles                 Revision
```

Tap topic → bottom sheet:

```txt
Linear Equations

Status
Todo · In Progress · Done · Revision

Planned Date
18 May 2026

Notes
[textarea]

Actions
[Save] [Delete]
```

Material bottom sheets are appropriate for secondary actions and contextual content anchored to the bottom. ([Material Design][4])

## Syllabus feature parity

| Web feature          | Android implementation               |
| -------------------- | ------------------------------------ |
| Add subject          | FAB or `+ Subject` button            |
| Add chapter          | Subject detail screen / bottom sheet |
| Add topic            | Chapter section / bottom sheet       |
| Multi-line topic add | “Add multiple topics” textarea       |
| Rename subject       | overflow menu                        |
| Rename chapter       | overflow menu                        |
| Rename topic         | topic bottom sheet                   |
| Delete subject       | destructive confirm dialog           |
| Delete chapter       | destructive confirm dialog           |
| Delete topic         | destructive confirm dialog           |
| Search syllabus      | top search input                     |
| Filter by status     | filter chips                         |
| Filter by subject    | subject screen or dropdown           |
| Bulk Add             | top action / FAB menu                |
| Template import      | overflow action: “Import template”   |

Your web implementation already supports add subject, add chapter, add topic, rename subject/chapter/topic, delete subject/chapter/topic, search/filter, visual add, bulk add, and template import into an existing plan.   

---

# 7. `BulkAddScreen` / `BulkAddBottomSheet`

This is too important to hide.

Your web version supports bulk add with two modes: manual and `.txt` file format. It parses text into subjects, chapters, and topics, and file import uses `/syllabus/import`. 

## Android flow

```txt
Bulk Add

Tabs:
  Paste Text
  Import File

Paste Text mode:
  Subject selector / New subject
  Default chapter
  Textarea
  Preview:
    3 chapters
    42 topics
  [Import Topics]

Import File mode:
  Upload .txt / supported file
  Show converted syllabus text
  Preview
  [Import]
```

## API contract

| Feature              | Endpoint                                                             |
| -------------------- | -------------------------------------------------------------------- |
| Upload syllabus file | `POST /syllabus/import`                                              |
| Add created subject  | `POST /plans/:planId/subjects`                                       |
| Add created chapter  | `POST /plans/:planId/subjects/:subjectId/chapters`                   |
| Add created topic    | `POST /plans/:planId/subjects/:subjectId/chapters/:chapterId/topics` |

## Premium behavior

If non-premium, show upgrade sheet before import. Your web app gates bulk add behind premium. 

---

# 8. `CalendarScreen`

Purpose: schedule management, not just a visual month grid.

Do not make the desktop month grid the primary UI. It is too dense for Android.

## Android layout

```txt
May 2026

Week strip:
Mon Tue Wed Thu Fri Sat Sun
 4   5   6   7   8   9  10

Today · 8 May
3 planned · 1 done

[ ] Polity · Parliament
[ ] Maths · Ratio
[ ] English · Error Spotting

Actions:
[Move Topics] [Clear Day]
```

## Secondary month view

Add a button:

```txt
View Month
```

Then show compact month heatmap:

```txt
May 2026
[calendar grid with dots/counts]
```

Tap a day → day agenda bottom sheet.

## Calendar feature parity

| Feature                      | Android UI                 |
| ---------------------------- | -------------------------- |
| View scheduled topics        | agenda list                |
| Pick date                    | week strip / month sheet   |
| See off days                 | label in week strip        |
| See overdue                  | red badge                  |
| See done count               | progress indicator         |
| Move selected topics to date | multi-select + date picker |
| Clear topics from date       | day action                 |
| Open topic in syllabus       | topic row action           |
| Mark topic status            | topic bottom sheet         |

The web already supports moving topics to dates and clearing topics from a date by patching topic planned dates. 

---

# 9. `PlanScreen`

Purpose: settings and schedule controls.

This is where many “missing button” complaints can happen, so preserve all controls.

## Android layout

```txt
Plan Settings

Basics
Plan title
Exam type
Exam date

Study Capacity
Daily goal
Off days

Advanced Scheduling
[ ] Include revision topics
[ ] Keep already planned dates

Schedule Management
[Create Planner Calendar]
[Reschedule]
[Clear Future Dates]
[Reset Entire Plan]
```

## Feature parity

| Web feature             | Backend/API                            | Android placement   |
| ----------------------- | -------------------------------------- | ------------------- |
| Save title              | `PATCH /plans/:planId`                 | Basics card         |
| Save exam type          | `PATCH /plans/:planId`                 | Basics card         |
| Save exam date          | `PATCH /plans/:planId`                 | Basics card         |
| Save daily goal         | `PATCH /plans/:planId`                 | Capacity card       |
| Save off days           | `PATCH /plans/:planId`                 | Capacity card       |
| Include revision topics | local option passed to auto-distribute | Advanced scheduling |
| Keep existing dates     | local option passed to auto-distribute | Advanced scheduling |
| Create planner calendar | `POST /plans/:planId/auto-distribute`  | primary CTA         |
| Reschedule              | calendar/date management flow          | Schedule card       |
| Clear future dates      | batch `PATCH /topics/:topicId`         | danger action       |
| Reset entire plan       | batch `PATCH /topics/:topicId`         | danger action       |

The web version saves plan metadata with `PATCH /plans/:planId`, saves capacity settings, patches topics, clears future dates, resets topics, and calls auto-distribute. 

---

# 10. `InsightsScreen`

Purpose: show whether the student is safe or in trouble.

Your web model already contains insight data for summary, consistency, workload, coverage, backlog, and recommendations. 

## Android layout

```txt
Status
On Track / Needs Attention / At Risk

Main card
Required pace: 3.2 topics/day
Forecast finish: 2 Aug
Buffer: 10 days

Cards
Remaining Topics
Available Study Days
Overdue
Revision Needed

Consistency
Study streak
Active days last 14
Best weekday

Workload
Next 14 days
Overload days
Empty study days

Coverage
Subject progress list
Lagging chapters

Recommendations
[Reschedule overdue topics]
[Increase daily goal]
[Review weak chapters]
```

## Important decision

Do not start with complex charts. Start with status, numbers, and actions.

---

# 11. Premium system

Your Android app must mirror the premium gates because otherwise users will hit backend failures in confusing places.

Your web app gates:

| Premium feature            | Android behavior                    |
| -------------------------- | ----------------------------------- |
| More than free topic limit | show upgrade sheet                  |
| Auto-schedule              | show upgrade sheet                  |
| Bulk add                   | show upgrade sheet                  |
| Reschedule                 | show upgrade sheet                  |
| Exam templates             | show upgrade sheet where applicable |

The web file defines premium modal reasons for `topic_limit`, `auto_schedule`, `bulk_add`, `reschedule`, and `template`. 

## Android implementation

Create one reusable component:

```txt
PremiumGateSheet
  icon
  title
  description
  primary CTA: Unlock Premium
  secondary CTA: Not now
```

Create one helper:

```ts
function requirePremium(reason: PremiumReason, action: () => void) {
  if (!plan.features.isPremium) {
    openPremiumSheet(reason);
    return;
  }
  action();
}
```

Use this everywhere:

```txt
Bulk Add
Auto Schedule
Reschedule
Clear Future Dates
Template Import
Topic limit overflow
```

---

# 12. API layer design

Do not scatter `fetch()` across screens.

Create:

```txt
planner/api/plannerApi.ts
```

Suggested functions:

```ts
export const plannerApi = {
  listPlans,
  getPlan,
  getCalendar,
  getTemplates,
  getTemplateDetail,
  createPlan,
  createPlanFromTemplate,
  deletePlan,

  updatePlan,
  upgradePlan,

  addSubject,
  renameSubject,
  deleteSubject,

  addChapter,
  renameChapter,
  deleteChapter,

  addTopic,
  updateTopic,
  deleteTopic,

  autoDistribute,
  importSyllabusFile,
};
```

## Endpoint map

```txt
GET     /api/plans
POST    /api/plans
DELETE  /api/plans/:planId

GET     /api/plans/templates
GET     /api/plans/templates/:templateId
POST    /api/plans/from-template

GET     /api/plans/:planId
PATCH   /api/plans/:planId
POST    /api/plans/:planId/upgrade

GET     /api/plans/:planId/calendar
POST    /api/plans/:planId/auto-distribute

POST    /api/plans/:planId/subjects
PATCH   /api/plans/:planId/subjects/:subjectId
DELETE  /api/plans/:planId/subjects/:subjectId

POST    /api/plans/:planId/subjects/:subjectId/chapters
PATCH   /api/plans/:planId/subjects/:subjectId/chapters/:chapterId
DELETE  /api/plans/:planId/subjects/:subjectId/chapters/:chapterId

POST    /api/plans/:planId/subjects/:subjectId/chapters/:chapterId/topics
PATCH   /api/plans/:planId/topics/:topicId
DELETE  /api/plans/:planId/topics/:topicId

POST    /api/syllabus/import
```

This map is the most important part of the plan. If every endpoint above has at least one Android UI path, users will not complain that the app is missing website features.

---

# 13. Shared TypeScript model

Create:

```txt
planner/model/types.ts
```

Use the same shapes as web:

```ts
export type TopicStatus =
  | "todo"
  | "in_progress"
  | "done"
  | "revision_needed";

export type PlannerSection =
  | "today"
  | "plan"
  | "syllabus"
  | "calendar"
  | "insights";

export type Topic = {
  id: string;
  name: string;
  status: TopicStatus;
  plannedDate?: string;
  completedDate?: string;
  notes?: string;
};

export type Chapter = {
  id: string;
  name: string;
  topics: Topic[];
};

export type Subject = {
  id: string;
  name: string;
  color: string;
  weeklyTarget?: number;
  monthlyTarget?: number;
  chapters: Chapter[];
};

export type Plan = {
  id: string;
  title: string;
  examType?: string;
  examDate?: string;
  description?: string;
  dailyGoal?: number;
  offDays: number[];
  features: {
    isPremium: boolean;
    unlockedAt?: string;
  };
  subjects: Subject[];
  progress?: {
    totalTopics: number;
    doneTopics: number;
    completionPercent: number;
  };
};
```

Keep these in Android instead of importing web UI files.

---

# 14. Shared planner utilities

Create:

```txt
planner/utils/date.ts
planner/utils/progress.ts
planner/utils/syllabusParser.ts
planner/utils/insights.ts
```

Move these logic pieces from web:

```txt
toIsoDateOnly
formatDate
dayDiff
flattenTopics
plannerProgress
chapterPercent
subjectPercent
parseBulkTopicsByChapter
parseBulkSubjectsFromTxt
simulateForecastCompletionDate
countStudyDaysBetween
```

Your web file already has these utilities mixed into the component file. They should become pure functions in Android. 

---

# 15. Component inventory

Build reusable Android components before screens:

```txt
PlannerCard
ProgressRing / ProgressBar
MetricCard
TopicRow
TopicStatusChip
SubjectRow
ChapterAccordion
DayAgendaList
WeekStrip
DatePickerSheet
PremiumGateSheet
ConfirmDeleteSheet
BulkAddSheet
TemplateCard
EmptyState
ErrorState
LoadingState
```

## Touch behavior

Avoid:

```txt
hover
group-hover
tiny text buttons
hidden actions
desktop popovers
dense 7-column grids as primary UI
```

Use:

```txt
bottom sheets
overflow menus
visible icon buttons
large list rows
long-press only as shortcut, never only path
```

---

# 16. Onboarding / beginner guide

Your web onboarding sequence is:

```txt
set_exam_date
add_topics
build_schedule
open_calendar
```

The web code stores onboarding state and maps each guided action to a required view. 

Android should keep the same flow, but display it as a checklist card:

```txt
Setup Guide

1. Set exam date          Done
2. Add topics             Next
3. Build schedule         Locked
4. Review calendar        Locked
```

Do not use desktop-style guided tour overlays on Android. They become annoying and fragile.

### Android behavior

| Step           | Action                   |
| -------------- | ------------------------ |
| Set exam date  | navigate to Plan tab     |
| Add topics     | navigate to Syllabus tab |
| Build schedule | call auto-distribute     |
| Open calendar  | navigate to Calendar tab |

Persist state using AsyncStorage:

```txt
study-planner-onboarding-v2
```

---

# 17. Handling destructive actions

Destructive actions must be preserved but hidden behind confirmation sheets.

Use the same confirm sheet for:

```txt
Delete plan
Delete subject
Delete chapter
Delete topic
Clear future dates
Reset entire plan
Clear syllabus after exam type change
```

Your web app already uses pending delete state for subject/chapter/topic and special IDs for clear future/reset plan. 

Android should not use special fake topic IDs in UI code. Better:

```ts
type PendingAction =
  | { type: "delete_plan"; planId: string }
  | { type: "delete_subject"; subjectId: string }
  | { type: "delete_chapter"; subjectId: string; chapterId: string }
  | { type: "delete_topic"; topicId: string }
  | { type: "clear_future_dates" }
  | { type: "reset_plan" };
```

Cleaner and safer.

---

# 18. State management plan

Use either Zustand or TanStack Query. My recommendation:

```txt
TanStack Query for server state
Zustand for local UI state
AsyncStorage for onboarding/preferences
```

## Server state

```txt
plans query
plan detail query
calendar query
templates query
insights query
```

## Mutations

```txt
create plan
delete plan
update plan
add subject/chapter/topic
update subject/chapter/topic
delete subject/chapter/topic
patch topic
bulk import
auto distribute
upgrade premium
```

After every mutation:

```txt
invalidate plan detail
invalidate calendar
invalidate insights if affected
```

Do not manually patch every nested object unless necessary. The web frequently refreshes calendar after topic changes; keep that pattern.

---

# 19. Offline / poor network behavior

Do not overbuild offline editing in version 1. It will multiply complexity.

But do add:

```txt
Loading skeletons
Retry buttons
Optimistic status update for Mark Done
Queued toast/snackbar errors
Disabled buttons while request is running
```

Critical mutations like bulk add, reset plan, and auto-distribute should not be optimistic.

---

# 20. Android UI design tokens

Use a single design file:

```txt
planner/theme/plannerTheme.ts
```

Recommended values:

```txt
Screen padding: 16dp
Card padding: 16dp
Card radius: 20–24dp
Section gap: 20–24dp
Small gap: 8–12dp
Minimum tap target: 48dp
Bottom nav height: 72–80dp
Top app bar height: 56–64dp
```

Keep from web:

```txt
blue primary color
rounded cards
soft shadows
dark mode
status colors
subject colors
premium emoji/icon personality
```

Change from web:

```txt
no hover states
no marquee descriptions
no dense dashboard grids
no org-chart-first syllabus
no popover calendar details
no tiny uppercase-heavy controls
```

---

# 21. Adaptive layout plan

For normal phones:

```txt
single column
bottom nav
bottom sheets
agenda-first calendar
subject drill-down syllabus
```

For tablets/foldables:

```txt
navigation rail
list-detail layout
calendar month + day panel side by side
syllabus subject list + chapter/topic detail side by side
```

Android’s adaptive layout guidance recommends using window size classes to adapt UI by app window size, and its docs call out list-detail/canonical layouts for larger screens. ([Android Developers][5]) ([Android Developers][6])

---

# 22. Development phases

## Phase 1 — API parity layer

Build `plannerApi.ts`.

Deliverables:

```txt
All endpoints wrapped
Auth token / cookies handled
Typed request/response models
Central error normalizer
```

Acceptance test:

```txt
Each web backend call has an Android function.
No screen calls fetch directly.
```

---

## Phase 2 — Shared planner store

Build:

```txt
usePlan(planId)
useCalendar(planId)
useTemplates()
usePlannerMutations(planId)
usePremiumGate(plan)
```

Acceptance test:

```txt
Open plan
Patch topic
Calendar refreshes
Plan progress refreshes
Errors show snackbar
```

---

## Phase 3 — StudyPlansScreen + QuickStart

Build:

```txt
StudyPlansScreen
TemplatePickerScreen
TemplateConfigScreen
CustomPlanScreen
CustomPasteSyllabusScreen
```

Acceptance test:

```txt
Create from template
Create custom empty plan
Create custom plan with pasted syllabus
Delete plan
Open existing plan
```

---

## Phase 4 — Planner shell

Build:

```txt
PlannerHome
Top app bar
Bottom tabs
Shared loading/error states
Plan title overflow menu
```

Acceptance test:

```txt
Tabs switch correctly
Back returns to plan list
Plan title visible
Overflow menu opens settings/actions
```

---

## Phase 5 — Today tab

Build:

```txt
Progress card
Today topic list
Overdue section
Upcoming section
Topic detail sheet
Status update buttons
```

Acceptance test:

```txt
Mark topic done
Mark in progress
Mark revision needed
Open topic in syllabus
Calendar refreshes after status/date change
```

---

## Phase 6 — Syllabus tab

Build:

```txt
Search
Status filters
Subject list
Subject detail
Chapter accordion
Topic rows
Add subject/chapter/topic sheets
Rename/delete flows
Bulk add entry point
Template import entry point
```

Acceptance test:

```txt
Add subject
Add chapter
Add one topic
Add multiple topics
Rename all levels
Delete all levels
Search topics
Filter by status
Import template into existing plan
```

---

## Phase 7 — Bulk add + file import

Build:

```txt
BulkAddSheet
Paste parser
TXT parser
File picker
Import preview
Premium gate
```

Acceptance test:

```txt
Bulk paste topics into existing subject
Bulk paste into new subject
TXT full subject/chapter/topic format works
File import calls /syllabus/import
Non-premium user sees gate
```

---

## Phase 8 — Calendar tab

Build:

```txt
Week strip
Daily agenda
Month compact view
Day detail sheet
Move topics to date
Clear topics from date
Open topic detail
```

Acceptance test:

```txt
Tap date
See planned topics
Move selected topics
Clear planned dates
Mark done from calendar
Overdue labels show correctly
```

---

## Phase 9 — Plan tab

Build:

```txt
Basics card
Capacity card
Advanced scheduling card
Schedule management card
Danger zone
```

Acceptance test:

```txt
Update title
Update exam type
Update exam date
Update daily goal
Update off days
Build schedule
Clear future dates
Reset entire plan
Non-premium gates appear where needed
```

---

## Phase 10 — Insights tab

Build:

```txt
Status summary
Pace card
Consistency cards
Workload cards
Coverage list
Backlog cards
Recommendations
```

Acceptance test:

```txt
Insights load
Recommendations navigate to target tab
Subject progress matches syllabus
Overdue/revision numbers match plan data
```

---

## Phase 11 — Onboarding

Build:

```txt
Setup checklist card
Resume guide
Skip guide
Step completion detection
```

Acceptance test:

```txt
New user sees setup guide
Set exam date completes step 1
Add topics completes step 2
Build schedule completes step 3
Open calendar completes guide
Skip/resume works
```

---

## Phase 12 — QA parity checklist

Create a literal checklist and test it before release:

```txt
Plan list
☐ Fetch plans
☐ Open plan
☐ Delete plan
☐ Empty state
☐ Create new plan

QuickStart
☐ Load templates
☐ Create from template
☐ Configure title
☐ Configure exam date
☐ Configure daily goal
☐ Configure off days
☐ Custom plan
☐ Custom pasted syllabus

Today
☐ Progress visible
☐ Today tasks visible
☐ Overdue visible
☐ Upcoming visible
☐ Mark done
☐ Mark in progress
☐ Mark revision needed
☐ Edit notes
☐ Open topic in syllabus

Syllabus
☐ Search
☐ Filter status
☐ Add subject
☐ Rename subject
☐ Delete subject
☐ Add chapter
☐ Rename chapter
☐ Delete chapter
☐ Add topic
☐ Rename topic
☐ Delete topic
☐ Set topic date
☐ Edit notes
☐ Bulk add
☐ Import template

Calendar
☐ View week
☐ View month
☐ Pick day
☐ See planned topics
☐ Move topic to date
☐ Clear topic date
☐ Mark topic status
☐ See overdue count
☐ See off days

Plan
☐ Edit title
☐ Edit exam type
☐ Edit exam date
☐ Edit daily goal
☐ Edit off days
☐ Include revision topics
☐ Keep existing dates
☐ Create planner calendar
☐ Reschedule
☐ Clear future dates
☐ Reset entire plan

Insights
☐ Completion percent
☐ Remaining topics
☐ Days until exam
☐ Required topics/day
☐ Forecast date
☐ Buffer days
☐ Streak
☐ Workload
☐ Coverage
☐ Backlog
☐ Recommendations

Premium
☐ Topic limit gate
☐ Auto-schedule gate
☐ Bulk add gate
☐ Reschedule gate
☐ Template gate
☐ Upgrade action
```

---

# 23. Better way to avoid missing backend APIs

Do this instead of relying on memory:

## Create a `plannerFeatureRegistry.ts`

```ts
export const plannerFeatureRegistry = [
  {
    id: "create_plan_from_template",
    label: "Create plan from template",
    endpoint: "POST /api/plans/from-template",
    androidEntryPoints: [
      "QuickStartFlow.TemplateConfigScreen.GenerateButton",
    ],
  },
  {
    id: "bulk_add",
    label: "Bulk add topics",
    endpoint: "POST /api/plans/:planId/subjects/.../topics",
    androidEntryPoints: [
      "SyllabusScreen.BulkAddButton",
      "SubjectDetailScreen.BulkAddAction",
    ],
    premiumReason: "bulk_add",
  },
  {
    id: "auto_schedule",
    label: "Create planner calendar",
    endpoint: "POST /api/plans/:planId/auto-distribute",
    androidEntryPoints: [
      "PlanScreen.CreatePlannerCalendarButton",
      "TodayScreen.BuildScheduleQuickAction",
      "Onboarding.Step3",
    ],
    premiumReason: "auto_schedule",
  },
];
```

Then add a QA test that fails if:

```txt
feature has no endpoint
feature has no Android entry point
premium feature has no gate
destructive feature has no confirmation
```

This is the “perfect plan” part. It turns feature parity into something testable.

---

# 24. Final implementation priority

Build in this order:

```txt
1. API layer
2. Feature registry
3. Plan list + QuickStart
4. Planner shell
5. Today
6. Syllabus
7. Plan settings
8. Calendar
9. Bulk add / file import
10. Insights
11. Premium gates
12. Onboarding
13. Tablet/foldable adaptive layout
14. QA parity pass
```

The most important correction: **don’t design Android from the web UI. Design Android from the backend capabilities.** The web TSX tells you what capabilities exist; Android decides where those capabilities belong.

