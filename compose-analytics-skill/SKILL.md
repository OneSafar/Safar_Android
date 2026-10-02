---
name: compose-analytics-visualization
description: Build polished, dynamic Jetpack Compose analytics screens by discovering, importing, adapting, and integrating chart/visualization components from Vico, Drafter, and JetCo. Use when implementing user analytics, progress dashboards, trends, heatmaps, charts, graphs, animated metrics, or data visualizations in an Android/Compose codebase.
---

# Compose Analytics & Data Visualization Skill

## Purpose

Use this skill whenever a Jetpack Compose Android feature needs high-quality, dynamic data visualization driven by real application/user data.

Primary approved upstream sources:

- Vico: https://github.com/patrykandpatrick/vico
- Drafter: https://github.com/AndroidPoet/Drafter
- JetCo: https://github.com/developerchunk/JetCo

The goal is NOT to invent every visualization from scratch. Prefer production-ready library APIs or reusable open-source Compose implementations, then adapt them to the host app's architecture, design system, and live data.

## Core Rules

1. Never assume an API, artifact coordinate, version, package name, or sample from memory when repository/network access is available.
2. Before importing a visualization, inspect the current upstream README, docs, latest stable release/tag, Gradle installation instructions, sample code, and license.
3. Prefer adding a library dependency over copying its internal source code.
4. Copy/adapt source code only when:
   - the desired visualization is not exposed as a usable library API,
   - the copied component is reasonably self-contained,
   - its license permits the intended use,
   - required notices/attribution are preserved,
   - copying does not drag a large portion of the upstream project into the app.
5. Never copy code whose license is missing, ambiguous, or incompatible. In that case, reimplement the behavior from first principles using Compose Canvas/layout APIs.
6. Do not use hard-coded demo data in production UI. Sample values are allowed only in @Preview, tests, or clearly named fake/sample providers.
7. Production visualizations must accept state/data as parameters and remain independent of networking/storage code.
8. Preserve the existing app architecture. Do not introduce a second architecture just for charts.
9. Avoid unnecessary dependencies. If an existing approved library already solves the requirement well, use it.
10. Compile/test after integration and fix API/version mismatches instead of leaving pseudocode.

## Source Selection

Choose the source based on the visualization need.

### Prefer Vico for

- time-series analytics
- line charts
- area-like trend charts
- bar/column charts
- combined Cartesian visualizations
- axes, labels, markers, scrolling/zooming, chart interactions
- production dashboards where conventional analytics charts need extensive styling

Vico is the default choice for standard analytics unless another source offers a materially better component.

### Prefer Drafter for

- heatmaps / contribution calendars
- radar charts
- gauges
- Sankey diagrams
- treemaps
- funnels
- Gantt/timeline charts
- waterfall charts
- bubble/scatter visualizations
- specialized/nonstandard charts
- visualization requirements not conveniently covered by Vico

### Prefer JetCo for

- lightweight reusable Compose chart/component examples
- visual treatments or interactions that are easier to adapt than introducing another large dependency
- pie charts, live/multi-line graphs, bar variants, or other components when the current JetCo implementation fits the requested design

JetCo should often be treated as an implementation/example source. Inspect its current installation model before deciding between dependency use and adaptation.

### Prefer custom Compose Canvas for

- tiny sparklines
- progress rings/arcs
- streak paths
- custom journey/progress illustrations
- bespoke decorative analytics
- simple visuals where a dependency would cost more than the implementation
- cases where upstream licensing or compatibility prevents reuse

## Mandatory Workflow

### Step 1 — Inspect the host project

Before changing code, inspect:

- settings.gradle / settings.gradle.kts
- project and app build.gradle(.kts)
- libs.versions.toml if present
- Compose BOM / Compose version
- Kotlin version
- Android Gradle Plugin version
- minSdk / targetSdk
- package/module structure
- Material 2 vs Material 3
- current theme/design tokens
- existing analytics models/repositories/ViewModels
- whether the project already contains a charting library

Do not add duplicate libraries if an adequate chart dependency already exists.

### Step 2 — Understand the data contract

Locate the real data source that should drive the visualization.

Trace the flow when available:

API/database -> repository -> domain/use case -> ViewModel -> UI state -> visualization composable

Determine:

- units (minutes, hours, percent, count, score, etc.)
- time granularity
- missing/null values
- ordering
- timezone/date handling
- maximum expected number of points
- loading/error/empty states
- whether data updates live while the screen is visible

Never silently invent missing analytics semantics.

### Step 3 — Choose a visualization

Use data semantics, not visual novelty.

Typical mapping:

- trend over time -> line/area
- independent category comparison -> bar/column
- composition of a small total -> donut/pie
- consistency by day -> heatmap/contribution calendar
- multiple normalized dimensions -> radar (only when genuinely useful)
- completion toward target -> progress ring/gauge
- relationship between two numeric variables -> scatter/bubble
- flow between categories -> Sankey
- hierarchical composition -> treemap/sunburst

Avoid misleading visualizations. Do not use pie/donut for unrelated categories or long time series.

### Step 4 — Inspect upstream sources

For each candidate repository:

1. Open the repository root.
2. Read README/documentation.
3. Inspect latest stable release/tag if releases are used.
4. Confirm dependency coordinates or module usage from current source.
5. Inspect the relevant sample implementation.
6. Inspect LICENSE.
7. Check whether the relevant API appears current and maintained.
8. Verify compatibility with the host project's Kotlin/Compose/Android setup.

Never rely solely on a blog post or old copied snippet when upstream source is available.

### Step 5 — Decide dependency vs adaptation

Use this order:

A. Existing dependency already in project.
B. Official upstream library dependency.
C. Small source adaptation from a compatible licensed example/component.
D. Custom Compose implementation.

Do not vendor an entire repository merely to obtain one chart.

### Step 6 — Add dependencies cleanly

Follow the host project's dependency style.

If it uses Version Catalog:

- add the version/library alias to gradle/libs.versions.toml
- reference the alias from the appropriate module

If it uses direct Gradle dependencies:

- add the dependency in the existing style

Do not unnecessarily migrate the project to Version Catalog.

Use the latest compatible STABLE version verified from upstream. Avoid alpha/beta/RC unless the project already accepts prerelease dependencies or the user explicitly requests one.

After modifying Gradle, sync/build before proceeding deeply into UI work.

### Step 7 — Create an app-owned visualization wrapper

Do not scatter third-party APIs throughout feature screens.

Create app-owned composables such as:

```kotlin
@Composable
fun StudyTimeTrendChart(
    points: List<StudyTimePoint>,
    modifier: Modifier = Modifier,
)
```

or:

```kotlin
@Composable
fun ConsistencyHeatmap(
    days: List<DailyStudyStat>,
    modifier: Modifier = Modifier,
)
```

The wrapper should translate domain/UI models to library-specific models internally.

Benefits:

- the screen is not coupled tightly to the library
- future library replacement is easier
- styling stays centralized
- testing/previews are simpler

### Step 8 — Separate domain/UI models from chart models

Preferred structure:

```text
analytics/
  data/
  domain/
  presentation/
    AnalyticsViewModel.kt
    AnalyticsUiState.kt
    AnalyticsScreen.kt
    components/
      StudyTrendChart.kt
      SubjectProgressChart.kt
      ConsistencyHeatmap.kt
      ProgressRing.kt
```

Example model:

```kotlin
data class DailyStudyStat(
    val date: LocalDate,
    val minutes: Int,
)
```

Do not expose Vico/Drafter/JetCo model classes from repositories or domain layers.

### Step 9 — Connect dynamic state

The chart must render from observable app state, typically:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

Then pass the relevant values:

```kotlin
StudyTimeTrendChart(
    points = uiState.studyHistory,
)
```

Prefer StateFlow/Flow and lifecycle-aware collection when that matches the existing architecture.

When upstream chart models require mutation/transactions, update them using recommended APIs and Compose side effects (`LaunchedEffect`, `remember`, etc.) so recomposition does not recreate expensive models unnecessarily.

### Step 10 — Polish to product quality

Unless the product design says otherwise:

- use the host app's colors, typography, spacing, and corner radii
- support light and dark theme
- keep chart backgrounds transparent unless a card/container is explicitly needed
- use subtle grid lines
- limit label density
- format units clearly
- use smooth but restrained animation
- provide touch marker/tooltip where useful
- respect reduced-motion/accessibility expectations when practical
- provide semantic descriptions/content descriptions for important summarized values
- avoid rainbow palettes unless categories require distinct colors
- do not make every UI element a separate card/container
- avoid visual clutter

For dense mobile analytics, prioritize glanceability over desktop-style chart complexity.

## Performance Rules

- Do not perform aggregation/sorting of large datasets directly on every recomposition.
- Aggregate in repository/domain/ViewModel when appropriate.
- Use `remember` / `derivedStateOf` for UI transformations where appropriate.
- Use immutable/stable UI models where practical.
- Limit visible points or provide scrolling/zoom for large time series.
- Avoid creating brushes, paths, formatters, and other expensive objects repeatedly if they can be remembered.
- Test with realistic maximum dataset size, not only five demo points.

## Empty, Loading, and Error States

Every production visualization must handle:

- loading
- no data
- partial data
- error where applicable

Do not display a fake chart when there is no user data.

A good empty state can show the chart frame/illustration plus a concise explanation such as "Your study trend will appear after your first session," but it must not imply fabricated analytics.

## Source Reuse & Licensing

Current approved repositories must still be checked at execution time.

Known repository roots:

- https://github.com/patrykandpatrick/vico
- https://github.com/AndroidPoet/Drafter
- https://github.com/developerchunk/JetCo

If source is copied/adapted:

1. Open the repository LICENSE.
2. Confirm reuse is permitted.
3. Preserve required copyright/license notices.
4. Add attribution to the project notices/third-party acknowledgements when required.
5. Record the upstream repository and, ideally, commit/tag used in a nearby comment or third-party notice when substantial code is adapted.

Do not paste large upstream files unchanged if a dependency can provide the same functionality.

## GitHub/Network Access Behavior

When the coding environment has GitHub or web tools, actively use them.

The agent should:

- browse/open the approved repository
- search within it for the relevant chart/sample
- inspect current README/docs/source
- fetch only the files/sections needed
- use release metadata/tags where relevant

If `git` and internet access are available, a shallow temporary clone is acceptable for inspection:

```bash
git clone --depth 1 https://github.com/OWNER/REPO.git /tmp/REPO
```

Do NOT copy the cloned repository wholesale into the application.

If network/GitHub access is unavailable, do not hallucinate current dependency versions or APIs. Use already-installed dependencies/current project knowledge, or implement a self-contained Compose solution and clearly note that upstream verification could not be performed.

## Quality Gate

Before declaring the implementation complete:

1. Gradle resolves successfully.
2. Kotlin compiles.
3. Relevant tests pass or at least the affected module builds.
4. No unresolved imports.
5. No deprecated API introduced when a current equivalent is available.
6. No hard-coded production sample data.
7. Chart reacts to state changes.
8. Empty state works.
9. Dark/light theme checked if the app supports both.
10. Labels do not clip/overflow on common phone widths.
11. Landscape/small width behavior is reasonable if relevant.
12. Large realistic dataset does not obviously cause excessive recomposition/jank.
13. License obligations are satisfied.

## Output Expectations

When making the change, report concisely:

- visualization selected and why
- upstream library/source used
- dependency/version actually added (if any)
- files changed/created
- how real app data reaches the visualization
- build/test result
- any licensing attribution added

Do not return only a code snippet if repository editing is available. Implement the change in the codebase.

## Design Principle

The library is a rendering implementation, not the product design.

Start from the analytics question the user needs answered, select the appropriate visual form, and then use Vico, Drafter, JetCo, or custom Compose to implement it. The finished screen should look native to the host application rather than like an upstream demo gallery.
