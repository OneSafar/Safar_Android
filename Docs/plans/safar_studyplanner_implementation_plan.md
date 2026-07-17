# SAFAR StudyPlanner — Full Implementation Plan

> Stack: Android (Kotlin + Jetpack Compose + Room) · Backend (Node.js + Express)  
> Order: Fix bugs first → add storage → add features → add analytics

---

## TABLE OF CONTENTS

1. Fix: Pasted Syllabus Bug
2. Fix: Null Days-Left Rendering
3. Offline Room Cache
4. Backlog Redistribution Algorithm
5. Dashboard Today-Card
6. Analytics Events
7. Consistent Premium Gating
8. One-Tap Ekagra Focus Session
9. Backend: Bulk Import Endpoint
10. Notification Deduplication

---

## 1. FIX — Pasted Syllabus Bug

**Problem:** `pasteSyllabus` text is collected in QuickStart UI but `createPlan()` is called without using it. User thinks import worked. Plan is empty.

**Fix:** After plan creation, if pasted text exists, run it through the existing `importFullSyllabusFromTxt()` parser.

### Android — StudyPlannerViewModel.kt

```kotlin
// BEFORE (broken)
fun quickCreatePlan(name: String, examDate: String, pasteSyllabus: String) {
    viewModelScope.launch {
        val plan = createPlan(name, examDate)
        // pasteSyllabus is never used ← BUG
    }
}

// AFTER (fixed)
fun quickCreatePlan(name: String, examDate: String, pasteSyllabus: String) {
    viewModelScope.launch {
        val plan = createPlan(name, examDate) ?: return@launch

        if (pasteSyllabus.isNotBlank()) {
            _uiState.update { it.copy(isImporting = true, importStatus = "Importing syllabus...") }
            importFullSyllabusFromTxt(plan.id, pasteSyllabus)
            _uiState.update { it.copy(isImporting = false, importStatus = null) }
        }

        openPlan(plan.id)
    }
}
```

### Android — StudyPlannerScreen.kt (QuickStart UI)

```kotlin
// Show import progress indicator after plan creation
if (uiState.isImporting) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(
        text = uiState.importStatus ?: "Importing...",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
```

### Add to StudyPlannerUiState.kt

```kotlin
data class StudyPlannerUiState(
    // ... existing fields
    val isImporting: Boolean = false,
    val importStatus: String? = null
)
```

---

## 2. FIX — Null Days-Left Rendering

**Problem:** `text = "${daysUntil(plan.examDate)}"` renders as "null" when exam date is missing.

### Android — PlanStatusCard.kt

```kotlin
// BEFORE
Text(text = "${daysUntil(plan.examDate)} days left")

// AFTER
val daysLeft = plan.examDate?.let { daysUntil(it) }

Text(
    text = when {
        daysLeft == null -> "Set exam date"
        daysLeft < 0    -> "Exam passed"
        daysLeft == 0   -> "Exam today!"
        else            -> "$daysLeft days left"
    },
    color = when {
        daysLeft == null          -> MaterialTheme.colorScheme.outline
        daysLeft != null && daysLeft <= 7 -> MaterialTheme.colorScheme.error
        else                      -> MaterialTheme.colorScheme.onSurface
    }
)
```

### Utility — DateUtils.kt

```kotlin
fun daysUntil(examDate: String?): Int? {
    if (examDate.isNullOrBlank()) return null
    return try {
        val date = LocalDate.parse(examDate.take(10))
        ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
    } catch (e: Exception) {
        null
    }
}
```

---

## 3. OFFLINE ROOM CACHE

**Strategy:** Cache-first reads. Write-through on mutations. Sync on app open when online.

### Step 1 — Room Entities

```kotlin
// data/local/entity/StudyPlanEntity.kt
@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val examType: String?,
    val examDate: String?,
    val dailyGoalMinutes: Int,
    val offDays: String, // JSON array stored as string
    val isPremium: Boolean,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// data/local/entity/StudySubjectEntity.kt
@Entity(tableName = "study_subjects", foreignKeys = [
    ForeignKey(entity = StudyPlanEntity::class, parentColumns = ["id"],
               childColumns = ["planId"], onDelete = ForeignKey.CASCADE)
])
data class StudySubjectEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val name: String,
    val color: String?
)

// data/local/entity/StudyTopicEntity.kt
@Entity(tableName = "study_topics", foreignKeys = [
    ForeignKey(entity = StudySubjectEntity::class, parentColumns = ["id"],
               childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE)
])
data class StudyTopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val chapterId: String,
    val name: String,
    val status: String, // "TODO" | "DONE" | "REVISION_NEEDED"
    val plannedDate: String?,
    val completedDate: String?,
    val notes: String?,
    val isDirty: Boolean = false // true = pending sync
)
```

### Step 2 — DAOs

```kotlin
// data/local/dao/StudyPlanDao.kt
@Dao
interface StudyPlanDao {

    @Query("SELECT * FROM study_plans ORDER BY lastSyncedAt DESC")
    fun observeAllPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE id = :planId")
    suspend fun getPlanById(planId: String): StudyPlanEntity?

    @Upsert
    suspend fun upsertPlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String)

    @Query("UPDATE study_plans SET lastSyncedAt = :time WHERE id = :planId")
    suspend fun updateSyncTime(planId: String, time: Long = System.currentTimeMillis())
}

@Dao
interface StudyTopicDao {

    @Query("SELECT * FROM study_topics WHERE plannedDate = :date AND isDirty = 0")
    suspend fun getTopicsForDate(date: String): List<StudyTopicEntity>

    @Query("SELECT * FROM study_topics WHERE isDirty = 1")
    suspend fun getDirtyTopics(): List<StudyTopicEntity>

    @Upsert
    suspend fun upsertTopic(topic: StudyTopicEntity)

    @Query("UPDATE study_topics SET status = :status, completedDate = :date, isDirty = 1 WHERE id = :topicId")
    suspend fun markTopicDone(topicId: String, status: String, date: String)
}
```

### Step 3 — Database

```kotlin
// data/local/SafarDatabase.kt
@Database(
    entities = [StudyPlanEntity::class, StudySubjectEntity::class,
                StudyTopicEntity::class],
    version = 2,
    exportSchema = true
)
abstract class SafarDatabase : RoomDatabase() {
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyTopicDao(): StudyTopicDao
}
```

### Step 4 — Repository with Cache-First Pattern

```kotlin
// data/repository/StudyPlannerRepositoryImpl.kt
class StudyPlannerRepositoryImpl(
    private val api: PlannerApi,
    private val planDao: StudyPlanDao,
    private val topicDao: StudyTopicDao
) : StudyPlannerRepository {

    override fun observePlans(): Flow<List<StudyPlan>> =
        planDao.observeAllPlans().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncPlans() {
        try {
            val remote = api.getPlans()
            remote.forEach { plan ->
                planDao.upsertPlan(plan.toEntity())
                // sync subjects/topics similarly
            }
        } catch (e: Exception) {
            // offline — cached data stays valid, no crash
        }
    }

    override suspend fun markTopicDone(topicId: String) {
        val today = LocalDate.now().toString()
        // Write to local immediately (optimistic)
        topicDao.markTopicDone(topicId, "DONE", today)
        // Sync to backend
        try {
            api.updateTopicStatus(topicId, "DONE", today)
            // clear dirty flag
        } catch (e: Exception) {
            // stays dirty, SyncWorker will retry
        }
    }
}
```

### Step 5 — Background Sync Worker

```kotlin
// notifications/SyncWorker.kt
class SyncWorker(context: Context, params: WorkerParameters,
                 private val repository: StudyPlannerRepository) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Push dirty topics
            repository.flushDirtyTopics()
            // Pull fresh data
            repository.syncPlans()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("planner_sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
```

---

## 4. BACKLOG REDISTRIBUTION ALGORITHM

**Logic:** Take all overdue topics. Count remaining study days (excluding off-days) between today and exam date. Distribute overdue topics evenly across the next N study days, respecting daily goal.

### Android — BacklogRedistributor.kt

```kotlin
object BacklogRedistributor {

    data class RedistributionResult(
        val topicUpdates: Map<String, String>, // topicId -> newPlannedDate
        val daysNeeded: Int,
        val dailyAddition: Int
    )

    fun redistribute(
        overdue: List<StudyTopic>,
        examDate: LocalDate,
        offDays: Set<DayOfWeek>,
        todayTopicCount: Int,
        dailyGoal: Int
    ): RedistributionResult {
        if (overdue.isEmpty()) return RedistributionResult(emptyMap(), 0, 0)

        val studyDays = getStudyDays(LocalDate.now(), examDate, offDays)
        if (studyDays.isEmpty()) return RedistributionResult(emptyMap(), 0, 0)

        val slotsPerDay = maxOf(1, dailyGoal / 30) // assume ~30 min per topic
        val totalSlots = studyDays.size * slotsPerDay

        val updates = mutableMapOf<String, String>()
        var dayIndex = 0
        var slotInDay = if (dayIndex == 0) todayTopicCount else 0

        overdue.forEach { topic ->
            // Move to next day if current day is full
            if (slotInDay >= slotsPerDay) {
                dayIndex++
                slotInDay = 0
            }
            if (dayIndex >= studyDays.size) dayIndex = studyDays.size - 1

            updates[topic.id] = studyDays[dayIndex].toString()
            slotInDay++
        }

        return RedistributionResult(
            topicUpdates = updates,
            daysNeeded = dayIndex + 1,
            dailyAddition = overdue.size / maxOf(1, studyDays.size)
        )
    }

    private fun getStudyDays(
        from: LocalDate,
        to: LocalDate,
        offDays: Set<DayOfWeek>
    ): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var current = from
        while (!current.isAfter(to)) {
            if (current.dayOfWeek !in offDays) days.add(current)
            current = current.plusDays(1)
        }
        return days
    }
}
```

### Android — ViewModel integration

```kotlin
// StudyPlannerViewModel.kt
fun redistributeBacklog(plan: StudyPlan) {
    viewModelScope.launch {
        val overdue = getOverdueTopics(plan)
        val examDate = LocalDate.parse(plan.examDate?.take(10) ?: return@launch)
        val offDays = plan.offDays.map { DayOfWeek.valueOf(it) }.toSet()
        val todayCount = getTodayTopics(plan).size

        val result = BacklogRedistributor.redistribute(
            overdue = overdue,
            examDate = examDate,
            offDays = offDays,
            todayTopicCount = todayCount,
            dailyGoal = plan.dailyGoalMinutes
        )

        // Apply updates
        result.topicUpdates.forEach { (topicId, newDate) ->
            repository.updateTopicDate(topicId, newDate)
        }

        _uiState.update { it.copy(
            snackbarMessage = "Moved ${overdue.size} overdue topics across ${result.daysNeeded} days"
        )}
    }
}
```

### Android — UI trigger in PlanTabScreen.kt

```kotlin
// Show smart recovery banner when overdue > 0
if (overdueTopics.isNotEmpty()) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null,
                 tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${overdueTopics.size} topics overdue",
                     style = MaterialTheme.typography.titleSmall)
                Text("Tap to redistribute automatically",
                     style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { viewModel.redistributeBacklog(currentPlan) }) {
                Text("Fix Now")
            }
        }
    }
}
```

### Backend — Node.js bulk update endpoint

```javascript
// routes/planner.js
// POST /plans/:planId/redistribute-backlog
router.post('/:planId/redistribute-backlog', authenticate, async (req, res) => {
  const { planId } = req.params;
  const { topicUpdates } = req.body; // { topicId: newDate }[]

  try {
    const updates = Object.entries(topicUpdates).map(([topicId, newDate]) =>
      db.StudyTopic.update(
        { plannedDate: newDate },
        { where: { id: topicId, planId } }
      )
    );
    await Promise.all(updates);
    res.json({ updated: Object.keys(topicUpdates).length });
  } catch (err) {
    res.status(500).json({ error: 'Redistribution failed' });
  }
});
```

---

## 5. DASHBOARD TODAY-CARD

**Goal:** Surface today's planned topics on the home screen without opening the planner.

### Android — TodayStudyCard.kt (new composable)

```kotlin
@Composable
fun TodayStudyCard(
    todayTopics: List<StudyTopic>,
    planTitle: String,
    daysLeft: Int?,
    onTopicDone: (String) -> Unit,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onOpenPlanner,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null,
                     tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(planTitle, style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Bold)
                    daysLeft?.let {
                        Text("$it days to exam",
                             style = MaterialTheme.typography.bodySmall,
                             color = if (it <= 7) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.outline)
                    }
                }
                Text("${todayTopics.count { it.status == "DONE" }}/${todayTopics.size}",
                     style = MaterialTheme.typography.labelLarge,
                     color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            val progress = if (todayTopics.isEmpty()) 0f
                           else todayTopics.count { it.status == "DONE" }.toFloat() / todayTopics.size
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Topics (max 3 shown)
            if (todayTopics.isEmpty()) {
                Text("No topics planned for today 🎉",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.outline)
            } else {
                todayTopics.take(3).forEach { topic ->
                    TodayTopicRow(topic = topic, onDone = { onTopicDone(topic.id) })
                }
                if (todayTopics.size > 3) {
                    Text("+${todayTopics.size - 3} more",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TodayTopicRow(topic: StudyTopic, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = topic.status == "DONE",
            onCheckedChange = { if (it) onDone() },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = topic.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (topic.status == "DONE") TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}
```

### Android — HomeScreen.kt integration

```kotlin
// Add inside HomeScreen LazyColumn, after the greeting section:
item {
    val todayTopics by homeViewModel.todayTopics.collectAsStateWithLifecycle()
    val activePlan by homeViewModel.activePlan.collectAsStateWithLifecycle()

    activePlan?.let { plan ->
        TodayStudyCard(
            todayTopics = todayTopics,
            planTitle = plan.title,
            daysLeft = daysUntil(plan.examDate),
            onTopicDone = { homeViewModel.markTopicDone(it) },
            onOpenPlanner = { navController.navigate(Routes.STUDY_PLANNER) }
        )
    }
}
```

### Android — HomeViewModel.kt addition

```kotlin
// HomeViewModel.kt
val todayTopics: StateFlow<List<StudyTopic>> = studyPlannerRepository
    .getTopicsForDate(LocalDate.now().toString())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

val activePlan: StateFlow<StudyPlan?> = studyPlannerRepository
    .observePlans()
    .map { plans -> plans.firstOrNull { it.examDate != null } } // most urgent
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

fun markTopicDone(topicId: String) {
    viewModelScope.launch {
        studyPlannerRepository.markTopicDone(topicId)
    }
}
```

---

## 6. ANALYTICS EVENTS

**Pattern:** Fire-and-forget. Never block UI. Local queue with flush.

### Android — PlannerAnalytics.kt

```kotlin
object PlannerAnalytics {

    // All event names
    object Events {
        const val PLANNER_OPENED           = "study_planner_opened"
        const val PLAN_CREATE_STARTED      = "plan_create_started"
        const val PLAN_CREATED_TEMPLATE    = "plan_created_template"
        const val PLAN_CREATED_CUSTOM      = "plan_created_custom"
        const val SYLLABUS_IMPORT_STARTED  = "syllabus_import_started"
        const val SYLLABUS_IMPORT_SUCCESS  = "syllabus_import_succeeded"
        const val SYLLABUS_IMPORT_FAILED   = "syllabus_import_failed"
        const val TOPIC_COMPLETED          = "topic_completed"
        const val TOPIC_MARKED_REVISION    = "topic_marked_revision"
        const val AUTO_SCHEDULE_CLICKED    = "auto_schedule_clicked"
        const val AUTO_SCHEDULE_SUCCESS    = "auto_schedule_succeeded"
        const val BACKLOG_REDISTRIBUTED    = "backlog_redistributed"
        const val CALENDAR_DAY_OPENED      = "calendar_day_opened"
        const val INSIGHTS_OPENED          = "insights_opened"
        const val PREMIUM_GATE_VIEWED      = "premium_gate_viewed"
        const val PREMIUM_UPGRADE_CLICKED  = "premium_upgrade_clicked"
        const val NOTIFICATION_RECEIVED    = "planner_notification_received"
        const val EKAGRA_STARTED_FROM_PLAN = "ekagra_started_from_plan"
    }

    // Call this from anywhere — non-blocking
    fun track(event: String, properties: Map<String, Any> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SafarAnalytics.log(event, properties)
            } catch (e: Exception) {
                // Never crash the app for analytics
            }
        }
    }
}
```

### Android — Usage examples in ViewModel

```kotlin
// In StudyPlannerViewModel.kt

// When planner is opened:
PlannerAnalytics.track(PlannerAnalytics.Events.PLANNER_OPENED)

// When plan is created from template:
PlannerAnalytics.track(PlannerAnalytics.Events.PLAN_CREATED_TEMPLATE, mapOf(
    "template_name" to templateName,
    "exam_type" to examType
))

// When topic is completed:
PlannerAnalytics.track(PlannerAnalytics.Events.TOPIC_COMPLETED, mapOf(
    "plan_id" to planId,
    "days_before_exam" to (daysUntil(plan.examDate) ?: -1),
    "topics_done_today" to todayDoneCount
))

// When backlog is redistributed:
PlannerAnalytics.track(PlannerAnalytics.Events.BACKLOG_REDISTRIBUTED, mapOf(
    "topics_moved" to overdue.size,
    "days_spread" to result.daysNeeded
))

// When premium gate is shown:
PlannerAnalytics.track(PlannerAnalytics.Events.PREMIUM_GATE_VIEWED, mapOf(
    "reason" to premiumReason.name
))
```

### Backend — Analytics ingestion endpoint

```javascript
// routes/analytics.js
router.post('/events', authenticate, async (req, res) => {
  const { events } = req.body; // [{ event, properties, timestamp }]
  const userId = req.user.id;

  try {
    const rows = events.map(e => ({
      userId,
      event: e.event,
      properties: JSON.stringify(e.properties || {}),
      occurredAt: new Date(e.timestamp || Date.now())
    }));
    await db.AnalyticsEvent.bulkCreate(rows);
    res.json({ received: rows.length });
  } catch (err) {
    res.status(500).json({ error: 'Failed to record events' });
  }
});
```

---

## 7. CONSISTENT PREMIUM GATING

**Problem:** Auto-schedule is gated server-side but UI doesn't consistently show the gate before calling.

### Android — PremiumGate.kt

```kotlin
// Single source of truth for all premium feature checks
object PremiumGate {

    enum class Feature(val displayName: String, val description: String) {
        AUTO_SCHEDULE("Auto Schedule", "Automatically plan your study days"),
        ADVANCED_INSIGHTS("Advanced Insights", "Detailed analytics and forecasts"),
        BULK_IMPORT("Bulk Syllabus Import", "Import large syllabi instantly"),
        AI_PLAN_GENERATOR("AI Plan Generator", "Let AI create your study plan")
    }

    fun check(
        feature: Feature,
        isPremium: Boolean,
        onAllowed: () -> Unit,
        onGated: (Feature) -> Unit
    ) {
        if (isPremium) onAllowed() else onGated(feature)
    }
}
```

### Android — PremiumBottomSheet.kt (unified gate UI)

```kotlin
@Composable
fun PremiumGateSheet(
    feature: PremiumGate.Feature,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null,
                 modifier = Modifier.size(48.dp),
                 tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Premium Feature", style = MaterialTheme.typography.titleLarge,
                 fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(feature.displayName, style = MaterialTheme.typography.titleMedium,
                 color = MaterialTheme.colorScheme.primary)
            Text(feature.description, style = MaterialTheme.typography.bodyMedium,
                 textAlign = TextAlign.Center,
                 modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                PlannerAnalytics.track(PlannerAnalytics.Events.PREMIUM_UPGRADE_CLICKED,
                    mapOf("feature" to feature.name))
                onUpgrade()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Upgrade to Premium")
            }
            TextButton(onClick = onDismiss) { Text("Maybe later") }
        }
    }
}
```

### Android — Usage in StudyPlannerScreen.kt

```kotlin
// Replace every direct premium check with:
var gatedFeature by remember { mutableStateOf<PremiumGate.Feature?>(null) }

Button(onClick = {
    PremiumGate.check(
        feature = PremiumGate.Feature.AUTO_SCHEDULE,
        isPremium = uiState.isPremium,
        onAllowed = { viewModel.autoDistribute(planId) },
        onGated = { gatedFeature = it }
    )
}) { Text("Auto Schedule") }

gatedFeature?.let { feature ->
    PremiumGateSheet(
        feature = feature,
        onUpgrade = { navController.navigate(Routes.PREMIUM) },
        onDismiss = { gatedFeature = null }
    )
}
```

---

## 8. ONE-TAP EKAGRA FOCUS SESSION FROM PLAN

**Goal:** Every planned topic has a "Focus" button. Tapping it opens Ekagra pre-configured for that topic.

### Android — EkagraLauncher.kt

```kotlin
object EkagraLauncher {
    data class FocusContext(
        val topicId: String,
        val topicName: String,
        val subjectName: String,
        val planId: String,
        val suggestedMinutes: Int = 30
    )

    fun launch(navController: NavController, context: FocusContext) {
        PlannerAnalytics.track(PlannerAnalytics.Events.EKAGRA_STARTED_FROM_PLAN, mapOf(
            "topic_id" to context.topicId,
            "plan_id" to context.planId
        ))
        navController.navigate(
            Routes.EKAGRA +
            "?topicId=${context.topicId}" +
            "&topicName=${Uri.encode(context.topicName)}" +
            "&subjectName=${Uri.encode(context.subjectName)}" +
            "&duration=${context.suggestedMinutes}"
        )
    }
}
```

### Android — TopicCard.kt (add Focus button)

```kotlin
@Composable
fun TopicCard(
    topic: StudyTopic,
    subjectName: String,
    planId: String,
    onDone: () -> Unit,
    onFocus: (EkagraLauncher.FocusContext) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = topic.status == "DONE", onCheckedChange = { if (it) onDone() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(topic.name, style = MaterialTheme.typography.bodyMedium)
                Text(subjectName, style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.outline)
            }
            // Focus button
            if (topic.status != "DONE") {
                IconButton(onClick = {
                    onFocus(EkagraLauncher.FocusContext(
                        topicId = topic.id,
                        topicName = topic.name,
                        subjectName = subjectName,
                        planId = planId
                    ))
                }) {
                    Icon(Icons.Default.Timer, contentDescription = "Start focus session",
                         tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
```

### Android — Routes.kt (update Ekagra route)

```kotlin
// Add optional query params to Ekagra route
const val EKAGRA = "ekagra"
const val EKAGRA_FROM_PLAN = "ekagra?topicId={topicId}&topicName={topicName}&subjectName={subjectName}&duration={duration}"
```

### Android — EkagraScreen.kt (consume plan context)

```kotlin
// In SafarNavGraph.kt
composable(
    route = Routes.EKAGRA_FROM_PLAN,
    arguments = listOf(
        navArgument("topicId") { defaultValue = "" },
        navArgument("topicName") { defaultValue = "" },
        navArgument("subjectName") { defaultValue = "" },
        navArgument("duration") { defaultValue = "30"; type = NavType.IntType }
    )
) { backStackEntry ->
    EkagraScreen(
        prefilledTopicId = backStackEntry.arguments?.getString("topicId"),
        prefilledTitle = backStackEntry.arguments?.getString("topicName"),
        prefilledDuration = backStackEntry.arguments?.getInt("duration") ?: 30
    )
}
```

---

## 9. BACKEND — BULK SYLLABUS IMPORT ENDPOINT

**Problem:** Android currently makes 1 API call per subject, chapter, and topic — hundreds of calls for a large syllabus.

**Fix:** Single POST that accepts the entire parsed tree.

### Node.js — routes/planner.js

```javascript
// POST /plans/:planId/syllabus/bulk-import
router.post('/:planId/syllabus/bulk-import', authenticate, async (req, res) => {
  const { planId } = req.params;
  const { subjects } = req.body;
  // subjects: [{ name, color, chapters: [{ name, topics: [{ name }] }] }]

  const transaction = await db.sequelize.transaction();
  try {
    for (const subject of subjects) {
      const subjectRecord = await db.StudySubject.create({
        id: uuidv4(),
        planId,
        name: subject.name,
        color: subject.color || null
      }, { transaction });

      for (const chapter of subject.chapters || []) {
        const chapterRecord = await db.StudyChapter.create({
          id: uuidv4(),
          subjectId: subjectRecord.id,
          name: chapter.name
        }, { transaction });

        const topicRows = (chapter.topics || []).map(t => ({
          id: uuidv4(),
          chapterId: chapterRecord.id,
          name: t.name,
          status: 'TODO'
        }));
        if (topicRows.length > 0) {
          await db.StudyTopic.bulkCreate(topicRows, { transaction });
        }
      }
    }

    await transaction.commit();
    res.json({ success: true, subjectsCreated: subjects.length });
  } catch (err) {
    await transaction.rollback();
    res.status(500).json({ error: 'Bulk import failed', detail: err.message });
  }
});
```

### Android — Update importFullSyllabusFromTxt()

```kotlin
// Instead of sequential calls, parse first, then send one request
suspend fun importFullSyllabusFromTxt(planId: String, text: String) {
    val parsed = parseSyllabus(text) // returns List<SubjectTree>
    // Single API call
    safeApiCall { api.bulkImportSyllabus(planId, BulkImportRequest(subjects = parsed)) }
}

// Parser stays the same — just returns a tree instead of making calls
fun parseSyllabus(text: String): List<SubjectTree> {
    val subjects = mutableListOf<SubjectTree>()
    var currentSubject: SubjectTree? = null
    var currentChapter: ChapterTree? = null

    text.lines().forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("- ") -> {
                currentSubject = SubjectTree(name = trimmed.removePrefix("- "))
                subjects.add(currentSubject!!)
                currentChapter = null
            }
            trimmed.startsWith("_ ") -> {
                currentChapter = ChapterTree(name = trimmed.removePrefix("_ "))
                currentSubject?.chapters?.add(currentChapter!!)
            }
            trimmed.startsWith("> ") -> {
                currentChapter?.topics?.add(TopicTree(name = trimmed.removePrefix("> ")))
            }
        }
    }
    return subjects
}
```

---

## 10. NOTIFICATION DEDUPLICATION

**Problem:** PlannerAlertsWorker fires daily for every plan. Overdue notification can fire repeatedly for the same topics.

### Android — Add to SafarDataStore.kt

```kotlin
// Store last alert timestamps per plan+type
suspend fun getLastAlertTime(planId: String, alertType: String): Long {
    return dataStore.data.map { prefs ->
        prefs[longPreferencesKey("alert_${planId}_${alertType}")] ?: 0L
    }.first()
}

suspend fun setLastAlertTime(planId: String, alertType: String) {
    dataStore.edit { prefs ->
        prefs[longPreferencesKey("alert_${planId}_${alertType}")] = System.currentTimeMillis()
    }
}
```

### Android — PlannerAlertsWorker.kt

```kotlin
// Before sending any notification, check if we already sent it recently
private suspend fun shouldSendAlert(planId: String, alertType: String, cooldownHours: Int = 24): Boolean {
    val lastSent = dataStore.getLastAlertTime(planId, alertType)
    val cooldownMs = cooldownHours * 60 * 60 * 1000L
    return System.currentTimeMillis() - lastSent > cooldownMs
}

// Usage:
if (overdueTopics.isNotEmpty() && shouldSendAlert(plan.id, "overdue")) {
    sendOverdueNotification(plan, overdueTopics.size)
    dataStore.setLastAlertTime(plan.id, "overdue")
}

if (daysLeft == 7 && shouldSendAlert(plan.id, "exam_7d", cooldownHours = 48)) {
    sendExamCountdownNotification(plan, daysLeft)
    dataStore.setLastAlertTime(plan.id, "exam_7d")
}
```

---

## IMPLEMENTATION ORDER (Recommended Sprint Plan)

| Week | Task | Why |
|------|------|-----|
| 1 | Fix pasted syllabus bug + null days-left | Zero trust cost, 1-day fix |
| 1 | Add notification deduplication | Stops user annoyance immediately |
| 2 | Backend bulk import endpoint | Required before Room cache is useful |
| 2 | Room entities + DAOs | Foundation for everything offline |
| 3 | Cache-first repository + SyncWorker | Offline works |
| 3 | Analytics events | Now you can measure |
| 4 | Dashboard today-card | Biggest retention lift |
| 4 | Backlog redistribution algorithm | Core differentiator |
| 5 | Premium gating unification | Monetization integrity |
| 5 | One-tap Ekagra from plan | Feature connection, engagement lift |

---

## TESTING CHECKLIST

```
□ parseSyllabus() — test with 0, 1, 100 topics; malformed input
□ BacklogRedistributor.redistribute() — edge cases: exam tomorrow, all off-days, empty overdue
□ daysUntil() — null date, past date, today, future date
□ SyncWorker — no network, partial failure, dirty topic flush
□ PremiumGate.check() — both premium and non-premium paths for every Feature
□ TodayStudyCard — 0 topics, 1-3 topics, >3 topics, all done
□ Bulk import endpoint — transaction rollback on partial failure
□ Notification dedup — same alert not sent twice within cooldown
```
