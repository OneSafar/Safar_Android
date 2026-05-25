What is wrong in the current Plan Tab
=====================================

1\. The top spacing is still broken
-----------------------------------

There is a huge empty area between the top app bar and the first card. That makes the entire screen feel like it starts late.

The first useful content should begin much closer to the top bar.

2\. The summary card is too cramped
-----------------------------------

This row is bad:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   NE...   14 days left   8% complete   3 overdue   download   `

Problems:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   - Plan title is truncated too aggressively.  - Too many metrics are forced into one horizontal row.  - The download icon has unclear meaning.  - “0 today” is floating awkwardly below.  - Progress bar has no visual hierarchy.   `

It is compact, but it is not elegant.

3\. Add Topics / Schedule buttons look disabled
-----------------------------------------------

The pale grey/purple buttons look like inactive buttons. They do not feel like confident actions.

One should be primary. One should be secondary.

4\. “Today’s focus” is not strong enough
----------------------------------------

This should be the star of the screen.

Instead, it appears like a loose section in the middle. If there are no topics today, the empty state should be a clean card, not floating text.

5\. Plan Details still does not belong here
-------------------------------------------

The expanded Plan Details form makes the Plan Tab feel like a settings page. It should not live inline in the main dashboard.

Plan details should open in a **bottom sheet** or separate edit screen.

The Plan Tab should show study status, today’s work, overdue work, and next steps — not large form fields.

6\. More Options / Reset Plan is too scary and prominent
--------------------------------------------------------

Reset Plan is a dangerous action. It should be hidden behind a confirmation flow inside More Options, not shown as a huge red button in the normal scroll.

7\. Bottom area still feels unsafe
----------------------------------

The content is too close to the bottom navigation. The bottom navigation itself is fine in concept — five destinations is acceptable for compact screens according to Android’s navigation bar guidance — but the page content must be padded and scroll-safe around it. ([Android Developers](https://developer.android.com/develop/ui/compose/components/navigation-bar))

Android system bars/navigation bars can obscure app UI if insets are not handled correctly, and Compose provides WindowInsets/safe drawing tools to avoid that. ([Android Developers](https://developer.android.com/develop/ui/compose/system/insets?utm_source=chatgpt.com))

The new Plan Tab concept
========================

The Plan Tab should become:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Study Command Center   `

Not:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Settings + summary + random action buttons   `

Recommended structure:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   1. Compact plan status card  2. Today's mission card  3. Smart action row  4. Overdue rescue list  5. Upcoming queue  6. More / settings entry   `

The screen should answer, in order:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   1. How much time is left?  2. What do I study today?  3. Am I behind?  4. What is next?  5. Where are settings/actions?   `

Use LazyColumn as the Plan Tab root. Compose lazy lists support content padding and spacing directly, and Scaffold padding can be passed into list content so bottom bars do not cover the final items. ([Android Developers](https://developer.android.com/develop/ui/compose/lists))

Final visual direction
======================

Keep your current style:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   - rounded cards  - soft off-white/light background  - navy/indigo brand color  - orange urgency pill  - red overdue signal  - clean typography   `

But remove:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   - huge blank gaps  - inline settings forms  - oversized buttons  - cramped metric rows  - floating empty text  - dangerous actions in primary view   `

New layout
==========

Top card
--------

Instead of the current cramped row, use:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   NEET UG                         14 days left  May 31, 2026                    More  Progress bar  8% complete   •   0 today   •   3 overdue   `

The plan title should not become NE... unless the screen is extremely narrow. It can use maxLines = 1 with ellipsis, but give it proper weight.

Today card
----------

If today has topics:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Today's mission                 2 planned  [ ] Microbes in Sewage Treatment      Biology • Microbes & Applications  [ ] Microbes as Biocontrol Agents      Biology • Microbes & Applications   `

If today has no topics:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Today's mission                 0 planned  No topics planned today  Schedule topics   `

This should be a proper card.

Action row
----------

Use two clear actions:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Add Topics     Schedule   `

Make Add Topics primary, Schedule secondary.

Do not use pale buttons that look disabled.

Overdue section
---------------

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Overdue                         3 pending  • Microbes in Sewage Treatment          [ ]  • Microbes as Biocontrol Agents         [ ]  View all overdue   `

Only show 2–3 items on the Plan Tab. The full list belongs in Calendar/Syllabus.

Plan Details
------------

Collapsed inline row only:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Plan settings  NEET UG • 6 topics/day   `

Tap opens bottom sheet.

Do **not** expand large form fields inline inside the Plan Tab.

Refactoring plan
================

Phase 1 — Replace Plan Tab layout
---------------------------------

Create:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   PlanTabV2.kt  PlanStatusCard.kt  TodayMissionCard.kt  PlanActionRow.kt  PlannerTaskRow.kt  PlanSettingsSheet.kt  PlanOverflowMenu.kt   `

Do not touch Insights tab.

Phase 2 — Move inline settings to bottom sheet
----------------------------------------------

Remove inline expanded Plan Details form from the Plan Tab.

Replace with:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   PlanSettingsEntryCard   `

On click:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   PlanSettingsSheet   `

The sheet contains:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   - Plan title  - Exam type  - Exam date  - Topics per day  - Rest days  - Save details  - Export  - Reset plan   `

Danger actions should be at the bottom.

Phase 3 — Make task rows denser
-------------------------------

Use compact task rows instead of big cards.

Target height:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   72dp–84dp   `

Minimum touch target remains safe, but avoid huge blank padding.

Phase 4 — Bottom-safe scrolling
-------------------------------

Use:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   LazyColumn(      contentPadding = PaddingValues(          start = 16.dp,          top = 12.dp,          end = 16.dp,          bottom = 20.dp      )  )   `

The outer Scaffold should provide bottom bar padding. Do not add random giant Spacer(80.dp) unless needed.

Code: redesigned Plan Tab skeleton
==================================

Use this as the new direction. You may need to adjust imports and exact model names to match your current files.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun PlanTabV2(      plan: StudyPlan,      state: StudyPlannerUiState,      actions: PlannerActions,      modifier: Modifier = Modifier,  ) {      val today = remember { todayKey() }      val todayItems = remember(state.calendar, today) {          state.calendar[today].orEmpty()      }      val overdueItems = remember(state.calendar, today) {          state.calendar              .filterKeys { it < today }              .values              .flatten()              .filter { it.status != TopicStatus.DONE }      }      val upcomingItems = remember(state.calendar, today) {          state.calendar              .filterKeys { it > today }              .values              .flatten()              .filter { it.status != TopicStatus.DONE }              .take(3)      }      var showSettings by remember { mutableStateOf(false) }      var showMore by remember { mutableStateOf(false) }      if (showSettings) {          PlanSettingsSheet(              plan = plan,              actions = actions,              onDismiss = { showSettings = false },          )      }      LazyColumn(          modifier = modifier.fillMaxSize(),          contentPadding = PaddingValues(              start = 16.dp,              top = 12.dp,              end = 16.dp,              bottom = 20.dp,          ),          verticalArrangement = Arrangement.spacedBy(14.dp),      ) {          item {              PlanStatusCard(                  plan = plan,                  todayCount = todayItems.size,                  overdueCount = overdueItems.size,                  onMoreClick = { showMore = true },                  showMore = showMore,                  onDismissMore = { showMore = false },                  onExportClick = {                      showMore = false                      // call existing export action/helper                  },                  onSettingsClick = {                      showMore = false                      showSettings = true                  },              )          }          item {              TodayMissionCard(                  todayItems = todayItems,                  onScheduleClick = {                      actions.setSection(PlannerSection.CALENDAR)                  },                  onTopicDoneChange = { topicId, done ->                      actions.updateTopic(                          topicId = topicId,                          status = if (done) TopicStatus.DONE else TopicStatus.TODO,                      )                  },              )          }          item {              PlanActionRow(                  onAddTopics = {                      actions.setSection(PlannerSection.SYLLABUS)                  },                  onSchedule = {                      actions.setSection(PlannerSection.CALENDAR)                  },              )          }          if (overdueItems.isNotEmpty()) {              item {                  PlannerSectionHeader(                      title = "Overdue",                      trailing = "${overdueItems.size} pending",                  )              }              items(                  items = overdueItems.take(3),                  key = { it.topicId },                  contentType = { "overdueTopic" },              ) { item ->                  PlannerTaskRow(                      item = item,                      accent = MaterialTheme.colorScheme.error,                      onDoneChange = { done ->                          actions.updateTopic(                              topicId = item.topicId,                              status = if (done) TopicStatus.DONE else TopicStatus.TODO,                          )                      },                  )              }              if (overdueItems.size > 3) {                  item {                      TextButton(                          onClick = { actions.setSection(PlannerSection.CALENDAR) },                          modifier = Modifier.fillMaxWidth(),                      ) {                          Text("View all overdue")                      }                  }              }          }          if (upcomingItems.isNotEmpty()) {              item {                  PlannerSectionHeader(                      title = "Upcoming",                      trailing = "${upcomingItems.size} next",                  )              }              items(                  items = upcomingItems,                  key = { it.topicId },                  contentType = { "upcomingTopic" },              ) { item ->                  PlannerTaskRow(                      item = item,                      accent = MaterialTheme.colorScheme.primary,                      onDoneChange = { done ->                          actions.updateTopic(                              topicId = item.topicId,                              status = if (done) TopicStatus.DONE else TopicStatus.TODO,                          )                      },                  )              }          }          item {              PlanSettingsEntryCard(                  plan = plan,                  onClick = { showSettings = true },              )          }      }  }   `

Code: compact status card
=========================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun PlanStatusCard(      plan: StudyPlan,      todayCount: Int,      overdueCount: Int,      showMore: Boolean,      onMoreClick: () -> Unit,      onDismissMore: () -> Unit,      onExportClick: () -> Unit,      onSettingsClick: () -> Unit,      modifier: Modifier = Modifier,  ) {      val progress = plan.rollup()      val daysLeft = daysUntil(plan.examDate)      val scheme = MaterialTheme.colorScheme      Card(          modifier = modifier.fillMaxWidth(),          shape = RoundedCornerShape(24.dp),          colors = CardDefaults.cardColors(              containerColor = scheme.surface,          ),          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),      ) {          Column(              modifier = Modifier.padding(16.dp),              verticalArrangement = Arrangement.spacedBy(12.dp),          ) {              Row(                  verticalAlignment = Alignment.Top,              ) {                  Column(                      modifier = Modifier.weight(1f),                      verticalArrangement = Arrangement.spacedBy(4.dp),                  ) {                      Text(                          text = plan.title,                          style = MaterialTheme.typography.titleLarge,                          fontWeight = FontWeight.ExtraBold,                          color = scheme.onSurface,                          maxLines = 1,                          overflow = TextOverflow.Ellipsis,                      )                      Text(                          text = readableDate(plan.examDate),                          style = MaterialTheme.typography.bodyMedium,                          color = scheme.onSurfaceVariant,                          maxLines = 1,                          overflow = TextOverflow.Ellipsis,                      )                  }                  Spacer(Modifier.width(10.dp))                  DaysLeftPill(daysLeft = daysLeft)                  Box {                      IconButton(onClick = onMoreClick) {                          Icon(                              imageVector = Icons.Default.MoreVert,                              contentDescription = "More plan options",                          )                      }                      DropdownMenu(                          expanded = showMore,                          onDismissRequest = onDismissMore,                      ) {                          DropdownMenuItem(                              text = { Text("Export PDF") },                              leadingIcon = {                                  Icon(Icons.Default.FileDownload, contentDescription = null)                              },                              onClick = onExportClick,                          )                          DropdownMenuItem(                              text = { Text("Plan settings") },                              leadingIcon = {                                  Icon(Icons.Default.Settings, contentDescription = null)                              },                              onClick = onSettingsClick,                          )                      }                  }              }              LinearProgressIndicator(                  progress = { progress.completionPercent / 100f },                  modifier = Modifier                      .fillMaxWidth()                      .height(7.dp)                      .clip(RoundedCornerShape(99.dp)),                  color = scheme.primary,                  trackColor = scheme.surfaceVariant,              )              Row(                  modifier = Modifier.fillMaxWidth(),                  horizontalArrangement = Arrangement.spacedBy(8.dp),              ) {                  CompactMetricChip(                      value = "${progress.completionPercent}%",                      label = "complete",                      modifier = Modifier.weight(1f),                  )                  CompactMetricChip(                      value = "$todayCount",                      label = "today",                      modifier = Modifier.weight(1f),                  )                  CompactMetricChip(                      value = "$overdueCount",                      label = "overdue",                      valueColor = if (overdueCount > 0) scheme.error else scheme.onSurface,                      modifier = Modifier.weight(1f),                  )              }          }      }  }   `

Code: days-left pill
====================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun DaysLeftPill(      daysLeft: Long?,      modifier: Modifier = Modifier,  ) {      val scheme = MaterialTheme.colorScheme      val background = when {          daysLeft == null -> scheme.surfaceVariant          daysLeft < 0 -> scheme.error          daysLeft <= 7 -> Color(0xFFEF4444)          daysLeft <= 21 -> Color(0xFFF97316)          else -> scheme.primary      }      val text = when {          daysLeft == null -> "No date"          daysLeft < 0 -> "Past"          daysLeft == 0L -> "Today"          daysLeft == 1L -> "1 day left"          else -> "$daysLeft days left"      }      Box(          modifier = modifier              .clip(RoundedCornerShape(999.dp))              .background(background)              .padding(horizontal = 12.dp, vertical = 8.dp),          contentAlignment = Alignment.Center,      ) {          Text(              text = text,              style = MaterialTheme.typography.labelLarge,              fontWeight = FontWeight.Bold,              color = Color.White,              maxLines = 1,              overflow = TextOverflow.Ellipsis,          )      }  }   `

Code: compact metric chip
=========================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun CompactMetricChip(      value: String,      label: String,      modifier: Modifier = Modifier,      valueColor: Color = MaterialTheme.colorScheme.onSurface,  ) {      val scheme = MaterialTheme.colorScheme      Row(          modifier = modifier              .clip(RoundedCornerShape(14.dp))              .background(scheme.surfaceVariant.copy(alpha = 0.55f))              .padding(horizontal = 10.dp, vertical = 8.dp),          horizontalArrangement = Arrangement.Center,          verticalAlignment = Alignment.CenterVertically,      ) {          Text(              text = value,              style = MaterialTheme.typography.labelLarge,              fontWeight = FontWeight.Bold,              color = valueColor,              maxLines = 1,          )          Spacer(Modifier.width(4.dp))          Text(              text = label,              style = MaterialTheme.typography.labelMedium,              color = scheme.onSurfaceVariant,              maxLines = 1,              overflow = TextOverflow.Ellipsis,          )      }  }   `

Code: Today mission card
========================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun TodayMissionCard(      todayItems: List,      onScheduleClick: () -> Unit,      onTopicDoneChange: (topicId: String, done: Boolean) -> Unit,      modifier: Modifier = Modifier,  ) {      val scheme = MaterialTheme.colorScheme      Card(          modifier = modifier.fillMaxWidth(),          shape = RoundedCornerShape(24.dp),          colors = CardDefaults.cardColors(containerColor = scheme.surface),          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),      ) {          Column(              modifier = Modifier.padding(16.dp),              verticalArrangement = Arrangement.spacedBy(12.dp),          ) {              PlannerSectionHeader(                  title = "Today's mission",                  trailing = "${todayItems.size} planned",              )              if (todayItems.isEmpty()) {                  Column(                      modifier = Modifier                          .fillMaxWidth()                          .clip(RoundedCornerShape(18.dp))                          .background(scheme.surfaceVariant.copy(alpha = 0.45f))                          .padding(16.dp),                      horizontalAlignment = Alignment.CenterHorizontally,                      verticalArrangement = Arrangement.spacedBy(8.dp),                  ) {                      Text(                          text = "No topics planned today",                          style = MaterialTheme.typography.bodyLarge,                          color = scheme.onSurfaceVariant,                      )                      TextButton(onClick = onScheduleClick) {                          Text("Schedule topics")                      }                  }              } else {                  todayItems.take(3).forEach { item ->                      PlannerTaskRow(                          item = item,                          accent = scheme.primary,                          onDoneChange = { done ->                              onTopicDoneChange(item.topicId, done)                          },                      )                  }                  if (todayItems.size > 3) {                      TextButton(                          onClick = onScheduleClick,                          modifier = Modifier.align(Alignment.End),                      ) {                          Text("View all today")                      }                  }              }          }      }  }   `

Code: section header
====================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun PlannerSectionHeader(      title: String,      trailing: String? = null,      modifier: Modifier = Modifier,  ) {      Row(          modifier = modifier.fillMaxWidth(),          verticalAlignment = Alignment.CenterVertically,      ) {          Text(              text = title,              style = MaterialTheme.typography.titleMedium,              fontWeight = FontWeight.ExtraBold,              color = MaterialTheme.colorScheme.onSurface,              modifier = Modifier.weight(1f),              maxLines = 1,              overflow = TextOverflow.Ellipsis,          )          if (trailing != null) {              Text(                  text = trailing,                  style = MaterialTheme.typography.bodyMedium,                  color = MaterialTheme.colorScheme.onSurfaceVariant,                  maxLines = 1,                  overflow = TextOverflow.Ellipsis,              )          }      }  }   `

Code: action row
================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun PlanActionRow(      onAddTopics: () -> Unit,      onSchedule: () -> Unit,      modifier: Modifier = Modifier,  ) {      Row(          modifier = modifier.fillMaxWidth(),          horizontalArrangement = Arrangement.spacedBy(12.dp),      ) {          Button(              onClick = onAddTopics,              modifier = Modifier                  .weight(1f)                  .heightIn(min = 50.dp),              shape = RoundedCornerShape(18.dp),          ) {              Icon(                  imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,                  contentDescription = null,                  modifier = Modifier.size(18.dp),              )              Spacer(Modifier.width(8.dp))              Text(                  text = "Add Topics",                  maxLines = 1,                  overflow = TextOverflow.Ellipsis,              )          }          OutlinedButton(              onClick = onSchedule,              modifier = Modifier                  .weight(1f)                  .heightIn(min = 50.dp),              shape = RoundedCornerShape(18.dp),          ) {              Icon(                  imageVector = Icons.Default.CalendarMonth,                  contentDescription = null,                  modifier = Modifier.size(18.dp),              )              Spacer(Modifier.width(8.dp))              Text(                  text = "Schedule",                  maxLines = 1,                  overflow = TextOverflow.Ellipsis,              )          }      }  }   `

Code: compact task row
======================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun PlannerTaskRow(      item: CalendarTopicItem,      accent: Color,      onDoneChange: (Boolean) -> Unit,      modifier: Modifier = Modifier,  ) {      val scheme = MaterialTheme.colorScheme      val done = item.status == TopicStatus.DONE      Row(          modifier = modifier              .fillMaxWidth()              .clip(RoundedCornerShape(18.dp))              .background(scheme.surfaceVariant.copy(alpha = 0.35f))              .padding(horizontal = 12.dp, vertical = 10.dp),          verticalAlignment = Alignment.CenterVertically,      ) {          Box(              modifier = Modifier                  .size(10.dp)                  .clip(CircleShape)                  .background(if (done) scheme.primary else accent),          )          Spacer(Modifier.width(12.dp))          Column(              modifier = Modifier.weight(1f),              verticalArrangement = Arrangement.spacedBy(3.dp),          ) {              Text(                  text = item.topicName,                  style = MaterialTheme.typography.bodyLarge,                  fontWeight = FontWeight.Bold,                  color = scheme.onSurface,                  maxLines = 2,                  overflow = TextOverflow.Ellipsis,              )              Text(                  text = buildString {                      append(item.subjectName)                      if (!item.chapterName.isNullOrBlank()) {                          append(" • ")                          append(item.chapterName)                      }                  },                  style = MaterialTheme.typography.bodySmall,                  color = scheme.onSurfaceVariant,                  maxLines = 1,                  overflow = TextOverflow.Ellipsis,              )          }          Checkbox(              checked = done,              onCheckedChange = onDoneChange,          )      }  }   `

Code: Plan settings entry
=========================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun PlanSettingsEntryCard(      plan: StudyPlan,      onClick: () -> Unit,      modifier: Modifier = Modifier,  ) {      val scheme = MaterialTheme.colorScheme      Card(          modifier = modifier              .fillMaxWidth()              .clickable(onClick = onClick),          shape = RoundedCornerShape(22.dp),          colors = CardDefaults.cardColors(              containerColor = scheme.surface,          ),          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),      ) {          Row(              modifier = Modifier.padding(16.dp),              verticalAlignment = Alignment.CenterVertically,          ) {              Column(                  modifier = Modifier.weight(1f),                  verticalArrangement = Arrangement.spacedBy(4.dp),              ) {                  Text(                      text = "Plan settings",                      style = MaterialTheme.typography.titleMedium,                      fontWeight = FontWeight.Bold,                      color = scheme.onSurface,                  )                  Text(                      text = "${plan.examType ?: "Study Plan"} • ${plan.dailyGoal ?: 0} topics/day",                      style = MaterialTheme.typography.bodyMedium,                      color = scheme.onSurfaceVariant,                      maxLines = 1,                      overflow = TextOverflow.Ellipsis,                  )              }              Icon(                  imageVector = Icons.Default.ChevronRight,                  contentDescription = null,                  tint = scheme.onSurfaceVariant,              )          }      }  }   `

Code: settings bottom sheet
===========================

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @OptIn(ExperimentalMaterial3Api::class)  @Composable  private fun PlanSettingsSheet(      plan: StudyPlan,      actions: PlannerActions,      onDismiss: () -> Unit,  ) {      var title by remember(plan.id) { mutableStateOf(plan.title) }      var examType by remember(plan.id) { mutableStateOf(plan.examType.orEmpty()) }      var examDate by remember(plan.id) { mutableStateOf(plan.examDate.orEmpty()) }      var dailyGoal by remember(plan.id) { mutableStateOf((plan.dailyGoal ?: 3).toString()) }      ModalBottomSheet(          onDismissRequest = onDismiss,          dragHandle = { BottomSheetDefaults.DragHandle() },      ) {          Column(              modifier = Modifier                  .fillMaxWidth()                  .navigationBarsPadding()                  .verticalScroll(rememberScrollState())                  .padding(horizontal = 20.dp)                  .padding(bottom = 24.dp),              verticalArrangement = Arrangement.spacedBy(14.dp),          ) {              Text(                  text = "Plan settings",                  style = MaterialTheme.typography.titleLarge,                  fontWeight = FontWeight.Bold,              )              OutlinedTextField(                  value = title,                  onValueChange = { title = it },                  label = { Text("Plan title") },                  modifier = Modifier.fillMaxWidth(),                  singleLine = true,              )              OutlinedTextField(                  value = examType,                  onValueChange = { examType = it },                  label = { Text("Exam type") },                  modifier = Modifier.fillMaxWidth(),                  singleLine = true,              )              PlannerExamDateField(                  examDateIso = examDate,                  onExamDateChange = { examDate = it },              )              OutlinedTextField(                  value = dailyGoal,                  onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) },                  label = { Text("Topics per day") },                  modifier = Modifier.fillMaxWidth(),                  singleLine = true,              )              Button(                  onClick = {                      actions.updatePlan(                          UpdatePlanRequest(                              title = title.trim().ifBlank { plan.title },                              examType = examType.trim().ifBlank { null },                              examDate = examDate.ifBlank { null },                              dailyGoal = dailyGoal.toIntOrNull()?.coerceAtLeast(1),                              offDays = plan.offDays,                          )                      )                      onDismiss()                  },                  modifier = Modifier                      .fillMaxWidth()                      .heightIn(min = 52.dp),                  shape = RoundedCornerShape(16.dp),              ) {                  Text("Save details")              }              HorizontalDivider()              TextButton(                  onClick = {                      // Existing reset flow should be called here.                      // Keep confirmation dialog before destructive action.                  },                  modifier = Modifier.fillMaxWidth(),              ) {                  Text(                      text = "Reset plan",                      color = MaterialTheme.colorScheme.error,                      fontWeight = FontWeight.Bold,                  )              }          }      }  }   `

Coding agent prompt
===================

Use this with your coding agent:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Redesign the Study Planner Plan Tab completely.  Hard rules:  - Do not change Insights tab. It must remain exactly the same.  - Do not edit InsightsTab, PlannerInsightsCalculator, PlannerInsightsModels, or Insights-specific UI.  - Do not remove features.  - Do not change backend/API/data models.  - Do not rewrite ViewModel workflows.  - Preserve PlannerActions usage.  - Smartphone-only optimization for this pass.  - Preserve existing dirty repo changes.  Problem:  The current Plan Tab redesign is still poor. It is compact but visually cramped and unfinished:  - Huge top empty space.  - Summary card crams too many metrics into one row.  - Plan title truncates as “NE...” too early.  - Download icon meaning is unclear.  - Add Topics / Schedule buttons look disabled.  - Today’s focus empty state floats awkwardly.  - Overdue section lacks strong structure.  - Plan Details inline form is bloated and should not live inside the dashboard.  - More Options / Reset Plan is too prominent.  - Bottom area still feels crowded.  Goal:  Turn Plan Tab into a mature “Study Command Center”.  New information architecture:  1. Compact plan status card.  2. Today's mission card.  3. Smart action row.  4. Overdue rescue list.  5. Upcoming queue.  6. Plan settings entry.  Root composable:  - Use LazyColumn.  - Use contentPadding.  - Use verticalArrangement.spacedBy.  - Use stable item keys for lists.  - Let Scaffold innerPadding protect content from bottom nav.  - Do not manually overlay content under bottom nav.  Top status card:  Show:  - Plan title  - Exam date  - Days-left pill  - Progress bar  - Compact metrics: completion %, today count, overdue count  - More menu for export/settings  Do not:  - Force all metrics into one cramped row beside title.  - Show plan title as “NE...” unless absolutely necessary.  - Use large inner metric boxes.  - Use unclear standalone download icon without menu/label.  Today mission:  If topics exist:  - Show “Today’s mission”  - Show count planned  - Show first 1–3 compact task rows  - Provide View all today if more exist  If no topics:  - Show compact card:    “No topics planned today”    “Schedule topics”  Quick actions:  - Primary: Add Topics  - Secondary: Schedule  - Use Button + OutlinedButton.  - Height around 50–52dp.  - Do not use pale disabled-looking buttons.  Overdue:  - Section header: “Overdue” + “N pending”  - Show first 2–3 overdue tasks.  - Use red/accent dot.  - Add “View all overdue” if more exist.  Upcoming:  - Show next 2–3 upcoming tasks.  - Keep compact.  Plan Details:  - Remove inline expanded form from Plan Tab.  - Replace with compact “Plan settings” entry card.  - Open PlanSettingsSheet.  - Sheet contains title, exam type, date, topics per day, off days if already supported, save button, export/reset/move actions as appropriate.  - Destructive actions must require confirmation.  Task rows:  - Compact rows around 72–84dp.  - Left status dot.  - Middle title and subject/chapter metadata.  - Right checkbox/status action.  - Title maxLines 2.  - Metadata maxLines 1.  - Use ellipsis.  - Maintain touch target accessibility.  Typography:  - Reduce excessive letter spacing.  - Use MaterialTheme typography.  - Bold headings, readable body.  - Avoid over-designed spacing in functional labels.  Colors:  - Keep current brand: navy/indigo, teal, orange urgency, red overdue.  - Use bright color only for primary CTA.  - Secondary actions should be outlined/tonal.  - Reset/danger actions only in More/Settings with confirmation.  Insets and bottom nav:  - Ensure Plan Tab content is not hidden behind bottom nav.  - Use Scaffold.bottomBar for nav if needed.  - Use navigationBarsPadding on bottom nav/sheets.  - Bottom nav labels must remain single-line.  - Shared bottom nav changes are allowed only if they do not change Insights content.  Implementation:  Create or update:  - PlanTabV2.kt  - PlanStatusCard.kt  - TodayMissionCard.kt  - PlanActionRow.kt  - PlannerTaskRow.kt  - PlanSettingsSheet.kt  - PlanSettingsEntryCard.kt  - PlannerSectionHeader.kt  Testing:  Run:  ./gradlew :app:compileQaDebugKotlin  Manual test:  - Plan with today topics.  - Plan with no today topics.  - Plan with overdue topics.  - Plan with upcoming topics.  - Long plan title.  - Long topic names.  - Normal font.  - Large font.  - Gesture navigation.  - 3-button navigation.  - Light mode.  - Dark mode.  - Verify Insights tab unchanged.  Acceptance:  - Plan Tab looks like a real productivity dashboard.  - First useful content starts near the top.  - Summary card is readable and compact.  - Today’s mission is the main focus.  - Settings form no longer bloats the main screen.  - Action buttons look intentional, not disabled.  - No content collides with bottom nav.  - All existing features remain accessible.   `

My final recommendation: **stop patching the existing Plan Tab**. Replace it with the new component structure above. The current design is fighting itself because settings, dashboard, task list, empty states, and destructive actions are all competing on one screen. The new design separates them cleanly while keeping the same overall Safar visual identity.