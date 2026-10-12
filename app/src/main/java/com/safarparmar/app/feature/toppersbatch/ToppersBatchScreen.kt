/*
 * Copyright (c) 2026 Composable Horizons
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Today
import com.safarparmar.app.feature.toppersbatch.BatchAlertDialog as AlertDialog
import com.safarparmar.app.feature.toppersbatch.BatchButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.safarparmar.app.feature.toppersbatch.BatchFilterChip as FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.composables.ui.components.Icon
import com.safarparmar.app.feature.toppersbatch.BatchIconButton as IconButton
import com.composables.ui.components.ProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import com.safarparmar.app.feature.toppersbatch.BatchOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.composables.ui.components.Text
import com.safarparmar.app.feature.toppersbatch.BatchTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import androidx.compose.ui.platform.testTag
import java.util.Locale
import com.composables.ui.theme.ColorScheme
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.composables.ui.theme.LocalColorScheme
import com.composables.ui.components.HorizontalSeparator

private data class BatchNavItem(val section: BatchSection, val icon: ImageVector)
private val navigation = listOf(
    BatchNavItem(BatchSection.TODAY, Icons.Default.Today),
    BatchNavItem(BatchSection.COURSES, Icons.AutoMirrored.Filled.MenuBook),
    BatchNavItem(BatchSection.PROGRESS, Icons.AutoMirrored.Filled.ShowChart),
    BatchNavItem(BatchSection.CALENDAR, Icons.Default.CalendarMonth),
)

@Composable
fun ToppersBatchScreen(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: ToppersBatchViewModel = hiltViewModel(),
) {
    BatchClampedContent {
        ToppersBatchScreenContent(isDarkTheme, onNavigate, onBack, onToggleDarkTheme, viewModel)
    }
}

@Composable
private fun ToppersBatchScreenContent(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: ToppersBatchViewModel,
) {
    val strings = rememberBatchStrings()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dismissedFeedbackEvent by remember { mutableStateOf<Long?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshReleaseSchedule()
            while (true) {
                kotlinx.coroutines.delay(60_000)
                viewModel.refreshReleaseSchedule()
            }
        }
    }
    val selectedSubject = state.overview?.subjects?.firstOrNull {
        it.id == (state.todaySubjectId ?: state.selectedSubjectId)
    }
    val pageTitle = selectedSubject?.let(strings::subject) ?: when (state.section) {
        BatchSection.TODAY -> (state.overview?.course?.name?.takeIf { strings.isEnglish && it.isNotBlank() } ?: strings.text(R.string.toppers_batch_parmar_toppers_batch))
        else -> strings.text(state.section.titleRes)
    }
    val navigateBack = {
        if (selectedSubject != null) viewModel.backFromSubject()
        else if (state.section != BatchSection.TODAY) viewModel.select(BatchSection.TODAY)
        else onBack()
    }
    BackHandler(enabled = selectedSubject != null || state.section != BatchSection.TODAY) { navigateBack() }
    SafarDrawerScaffold(
        title = pageTitle, currentRoute = Routes.STUDY_PLANNER,
        wrapTopBarTitle = true,
        isDarkTheme = isDarkTheme, onNavigate = onNavigate, onToggleDarkTheme = onToggleDarkTheme,
        topBarContentColor = Color.White,
        topBarGradient = Brush.horizontalGradient(listOf(Color(0xFF831843), Color(0xFFBE185D), Color(0xFFDB2777))),
        secondaryNavigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        secondaryNavigationContentDescription = if (state.todaySubjectId != null) strings.text(R.string.toppers_batch_back_to_today)
            else if (selectedSubject != null) strings.text(R.string.toppers_batch_back_to_count, strings.text(state.subjectReturnSection.titleRes))
            else if (state.section != BatchSection.TODAY) strings.text(R.string.toppers_batch_back_to_today) else strings.text(R.string.toppers_batch_back_to_study_planner),
        onSecondaryNavigationClick = navigateBack,
    ) { outerPadding ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            Modifier.fillMaxSize().padding(top = outerPadding.calculateTopPadding()),
        ) {
            val wide = maxWidth >= 700.dp
            val baseColors = MaterialTheme.colorScheme
            val batchColors = baseColors.batchTrackerColors(isDarkTheme)
            MaterialTheme(colorScheme = batchColors, shapes = MaterialTheme.shapes.copy(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp))) {
                CompositionLocalProvider(
                    LocalColorScheme provides if (isDarkTheme) ColorScheme.Dark else ColorScheme.Light,
                    LocalSafarComposablesAccent provides batchColors.primary,
                ) {
                    SafarComposablesTheme {
                        val feedback = state.completionFeedback
                        if (feedback != null && feedback.event != dismissedFeedbackEvent) {
                            BatchCompletionCelebrationOverlay(
                                feedback = feedback,
                                canUndo = feedback.lectureId !in state.busyIds,
                                onUndo = {
                                    dismissedFeedbackEvent = feedback.event
                                    viewModel.undoCompletion(feedback)
                                },
                                onDismiss = {
                                    dismissedFeedbackEvent = feedback.event
                                },
                            )
                        }
                        Row(Modifier.fillMaxSize()) {
                if (wide && state.gate == BatchGate.READY) BatchRail(state.section, viewModel::select)
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = { if (!wide && state.gate == BatchGate.READY) BatchBottomNav(state.section, viewModel::select) },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.fillMaxSize()) {
                        state.completionFeedback?.let { bannerFeedback ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                BatchCompletionBanner(bannerFeedback, bannerFeedback.lectureId !in state.busyIds) { viewModel.undoCompletion(bannerFeedback) }
                            }
                        }
                        when (state.gate) {
                            BatchGate.LOADING -> CenterMessage(strings.text(R.string.toppers_batch_opening_your_toppers_batch))
                            BatchGate.PREMIUM -> GateMessage(strings.text(R.string.toppers_batch_safar_premium_is_needed_for_toppers_batch), strings.text(R.string.toppers_batch_see_premium)) { onNavigate(Routes.PREMIUM) }
                            BatchGate.UNAVAILABLE -> GateMessage(strings.text(R.string.toppers_batch_toppers_batch_is_not_available_for_this_account_yet), strings.text(R.string.toppers_batch_try_again), viewModel::open)
                            BatchGate.ERROR -> GateMessage(state.error?.let(strings::notice) ?: strings.text(R.string.toppers_batch_could_not_open_toppers_batch), strings.text(R.string.toppers_batch_try_again), viewModel::open)
                            BatchGate.READY -> androidx.compose.runtime.key(state.section, state.selectedSubjectId, state.todaySubjectId) {
                                CompositionLocalProvider(LocalBatchFeatureAccess provides BatchFeatureAccess(
                                    allowed = state.overview?.canUseFeatures == true,
                                    onUpgrade = { onNavigate(Routes.PREMIUM) },
                                )) { BatchBody(state, viewModel) }
                            }
                        }
                    }
                    }
                }
                    } }
                }
            }
        }
    }
}

@Composable private fun BatchRail(selected: BatchSection, choose: (BatchSection) -> Unit) {
    val strings = rememberBatchStrings()
    NavigationRail {
        navigation.forEach { item ->
            NavigationRailItem(selected = selected == item.section, onClick = { choose(item.section) },
                colors = NavigationRailItemDefaults.colors(selectedIconColor = Color(0xFFBE185D),
                    selectedTextColor = Color(0xFF9D174D), indicatorColor = Color(0xFFFCE7F3)),
                icon = { Icon(item.icon, contentDescription = null) }, label = { Text(strings.text(item.section.titleRes)) }, alwaysShowLabel = true)
        }
    }
}

/** The four tracker destinations use the shared Composables UI navigation bar. */
@Composable private fun BatchBottomNav(selected: BatchSection, choose: (BatchSection) -> Unit) {
    val strings = rememberBatchStrings()
    com.composables.ui.components.NavigationBar(modifier = Modifier.fillMaxWidth()) {
        navigation.forEach { item ->
            val active = selected == item.section
            com.composables.ui.components.NavigationBarItem(
                selected = active,
                onClick = { choose(item.section) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    com.composables.ui.components.Icon(item.icon, contentDescription = null,
                        modifier = Modifier.size(22.dp))
                    com.composables.ui.components.Text(strings.text(item.section.titleRes),
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun CenterMessage(message: String) {
    val strings = rememberBatchStrings()
    val infiniteTransition = rememberInfiniteTransition(label = "batch-loading")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    val primary = Color(0xFFBE185D)
    val container = Color(0xFFFCE7F3)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .background(container.copy(alpha = pulseAlpha), CircleShape)
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = primary,
                    trackColor = container,
                    strokeWidth = 3.5.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = message,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.text(R.string.toppers_batch_preparing_your_lectures_and_study_plan),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(primary.copy(alpha = dot1), CircleShape))
                Box(Modifier.size(7.dp).background(primary.copy(alpha = dot2), CircleShape))
                Box(Modifier.size(7.dp).background(primary.copy(alpha = dot3), CircleShape))
            }
        }
    }
}
@Composable private fun GateMessage(message: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClick) { Text(action) }
    }
}

@Composable private fun BatchBody(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val subject = overview.subjects.firstOrNull { it.id == state.selectedSubjectId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("toppers-batch-body"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.error != null || state.message != null) item(key = "batch-notice") {
            Notice(strings.notice(state.error ?: state.message!!), state.error != null, vm::clearNotice)
        }
        when (state.section) {
            BatchSection.TODAY -> item(key = "today-dashboard") {
                val focused = overview.subjects.firstOrNull { it.id == state.todaySubjectId }
                if (focused != null) TodaySubjectFocus(state, focused, vm)
                else ToppersBatchDashboard(state, vm)
            }
            BatchSection.COURSES -> item(key = "library") {
                if (subject != null) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BatchWeeklyAgenda(state, vm)
                    if (state.lectureTab == LectureTab.LECTURES) {
                        SubjectOptionsArea(state, subject, vm)
                    }
                }
                else Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    var showPrior by remember { mutableStateOf(false) }
                    TextButton(onClick = batchFeatureAction({ showPrior = true })) { Text(strings.text(R.string.toppers_batch_already_watched_some_lectures)) }
                    if (showPrior) PriorProgressDialog(state, vm) { showPrior = false }
                    var showDeleted by remember { mutableStateOf(false) }
                    if (overview.removedSubjects.isNotEmpty() || overview.removedLectures.isNotEmpty()) {
                        TextButton(onClick = batchFeatureAction({ showDeleted = true })) { Text(strings.text(R.string.toppers_batch_restore_deleted_items)) }
                    }
                    if (showDeleted) AlertDialog(onDismissRequest = { showDeleted = false },
                        title = { Text(strings.text(R.string.toppers_batch_deleted_items)) },
                        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) { ManageContent(state, vm) } },
                        confirmButton = { TextButton(onClick = batchFeatureAction({ showDeleted = false })) { Text(strings.text(R.string.toppers_batch_close)) } }, dismissButton = {})
                    Text(strings.text(R.string.toppers_batch_your_subjects), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(strings.text(R.string.toppers_batch_browse_lectures_backlog_and_revisions_by_subject), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    overview.subjects.filter { it.enabled }.forEach { row ->
                        SubjectRow(row, overview.progress.bySubject.firstOrNull { it.subjectId == row.id }) { vm.selectSubject(row.id) }
                    }
                }
            }
            BatchSection.PROGRESS -> item { ProgressContent(state, vm) }
            BatchSection.CALENDAR -> item { BatchStudentCalendar(state, vm) }
        }
    }
}

private fun Modifier.fullContentWidth(): Modifier = this.fillMaxWidth()

@Composable private fun Notice(message: String, error: Boolean, dismiss: () -> Unit) {
    val strings = rememberBatchStrings()
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = batchFeatureAction(dismiss)) { Text(strings.text(R.string.toppers_batch_close)) }
        }
    }
}

@Composable private fun SectionHeading(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (detail != null) Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun EmptyAction(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    val strings = rememberBatchStrings()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFBE185D), modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = batchFeatureAction(onClick)) { Text(strings.text(R.string.toppers_batch_see_lectures)) }
        }
    }
}

private fun LazyListScope.libraryItems(state: BatchUiState, vm: ToppersBatchViewModel, strings: BatchStrings) {
    val overview = state.overview ?: return
    val browsingSubjects = state.libraryQuery.isBlank() && state.libraryFilter == LibraryFilter.ALL
    val rows = if (browsingSubjects) emptyList() else overview.libraryRows(state.libraryQuery, state.libraryFilter)
    val pages = lecturePages(rows)
    val selected = (state.lecturePages["library-results"] ?: 0).coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    item(key = "library-controls") {
        Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading(strings.text(R.string.toppers_batch_library), null)
            BatchTextField(value = state.libraryQuery, onValueChange = vm::searchLibrary, singleLine = true,
                label = { Text(strings.text(R.string.toppers_batch_search_lectures)) }, accessibilityLabel = strings.text(R.string.toppers_batch_search_lecture_library))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilter.entries.forEach { filter ->
                    FilterChip(selected = state.libraryFilter == filter, onClick = batchFeatureAction({ vm.filterLibrary(filter) }),
                        label = { Text(strings.text(filter.titleRes)) })
                }
            }
            if (state.libraryQuery.isBlank() && state.libraryFilter == LibraryFilter.ALL) {
                overview.subjects.filter { it.enabled }.forEach { subject ->
                    SubjectRow(subject, overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }) {
                        vm.selectSubject(subject.id)
                    }
                }
            }
            if (!browsingSubjects) {
                Text(strings.quantity(R.plurals.toppers_batch_count_results, rows.size, rows.size), fontWeight = FontWeight.SemiBold)
                if (rows.isEmpty()) Text(strings.text(R.string.toppers_batch_no_matching_lectures_try_another_search_or_filter))
                else LectureWeekBrowser(pages, selected, false) { vm.selectLecturePage("library-results", it) }
            }

        }
    }
    items(pages.getOrNull(selected).orEmpty(), key = { "library-${it.id}" }) { lecture ->
        Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val subject = overview.subjects.firstOrNull { it.id == lecture.subjectId }
            Text(subject?.let(strings::subject).orEmpty(), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            LectureCard(lecture, subject, state, vm)
        }
    }
}

@Composable private fun CourseSwitch(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(color = if (selected) Color(0xFF9D174D) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp), modifier = modifier.clickable(role = Role.Tab, onClick = batchFeatureAction(onClick))) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable private fun SubjectRow(subject: BatchSubject, progress: SubjectProgress?, onClick: () -> Unit) {
    val strings = rememberBatchStrings()
    val done = progress?.completed ?: 0
    val total = progress?.total ?: 0
    val accent = subjectProgressColor(subject)
    Card(onClick = batchFeatureAction(onClick), modifier = Modifier.fillMaxWidth().semantics { contentDescription =
        strings.text(R.string.toppers_batch_open_count_count_of_count_lectures_done, strings.subject(subject), done, total) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (subject.key == "gk") strings.text(R.string.toppers_batch_gk) else strings.subject(subject).take(1).uppercase(strings.locale), color = accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.subject(subject), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subject.key == "gk" && total == 0) Text(strings.text(R.string.toppers_batch_25_week_timetable), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else ProgressIndicator(progress = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(), height = 5.dp, indicatorColor = accent,
                    trackColor = accent.copy(alpha = 0.13f))
            }
            Text(if (subject.key == "gk" && total == 0) strings.text(R.string.toppers_batch_schedule) else "$done / $total", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("›", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun subjectProgressColor(subject: BatchSubject): Color =
    (subject.color ?: subject.defaultColor).toComposeColor() ?: Color(0xFFBE185D)

private fun LazyListScope.subjectItems(state: BatchUiState, subject: BatchSubject, vm: ToppersBatchViewModel, strings: BatchStrings) {
    val overview = state.overview ?: return
    val lectures = overview.lectures.filter { it.subjectId == subject.id }
    val backlogIds = overview.watchList.firstOrNull { it.subjectId == subject.id }?.backlogLectureIds.orEmpty().toSet()
    val visible = when (state.lectureTab) {
        LectureTab.LECTURES -> lectures
        LectureTab.OLDER -> lectures.filter { it.completedAt == null && !it.isLocked(ToppersBatchViewModel.indiaDay()) && it.id in backlogIds }
        LectureTab.REVISION -> lectures.filter { it.pendingRevision }
    }
    item(key = "subject-intro") { SubjectIntro(state, subject, lectures, visible.isEmpty(), vm) }
    if (subject.key == "gk" && lectures.isEmpty() && state.lectureTab == LectureTab.LECTURES) {
        item(key = "gk-schedule-preview") { Text(strings.copy(state.overview, "emptyLectures", strings.text(R.string.toppers_batch_lectures_will_appear_when_the_official_list_is_added))) }
    }
    val pages = lecturePages(visible)
    if (pages.isNotEmpty()) {
        val pageKey = "${subject.id}:${state.lectureTab.name}"
        val selected = (state.lecturePages[pageKey] ?: initialLecturePage(pages, ToppersBatchViewModel.indiaDay())).coerceIn(0, pages.lastIndex)
        item(key = "week-browser") {
            Box(Modifier.fullContentWidth()) {
                LectureWeekBrowser(pages, selected, state.lectureTab == LectureTab.LECTURES) { vm.selectLecturePage(pageKey, it) }
            }
        }
        pages[selected].forEach { lecture ->
            item(key = lecture.id) { Box(Modifier.fullContentWidth()) { LectureCard(lecture, subject, state, vm) } }
        }
    }

}

@Composable private fun SubjectIntro(state: BatchUiState, subject: BatchSubject, lectures: List<BatchLecture>,
                                     empty: Boolean, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val watch = state.overview?.watchList?.firstOrNull { it.subjectId == subject.id }
    val done = lectures.count { it.completedAt != null }
    val pink = Color(0xFFBE185D)
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8), contentColor = Color(0xFF831843)),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.text(R.string.toppers_batch_your_progress), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("${if (lectures.isEmpty()) 0 else done * 100 / lectures.size}%",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(strings.quantity(R.plurals.toppers_batch_count_of_count_lectures_done, lectures.size, done, lectures.size))
                ProgressIndicator(progress = if (lectures.isEmpty()) 0f else done.toFloat() / lectures.size,
                    modifier = Modifier.fillMaxWidth(), height = 7.dp, indicatorColor = pink, trackColor = Color(0xFFF9C9DE))
            }
        }
        val context = LocalContext.current
        subject.externalUrl?.takeIf { it.startsWith("https://") }?.let { url ->
            OutlinedButton(onClick = batchFeatureAction({ openExternal(context, url) })) { Text(strings.text(R.string.toppers_batch_open_course_website)) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LectureTab.entries.forEach { tab ->
                FilterChip(selected = state.lectureTab == tab, onClick = batchFeatureAction({ vm.selectLectureTab(tab) }),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pink,
                        selectedLabelColor = Color.White),
                    label = { Text(if (tab == LectureTab.OLDER) strings.text(R.string.toppers_batch_backlog_count, watch?.backlogLectureIds?.size ?: 0) else if (tab == LectureTab.REVISION) strings.text(R.string.toppers_batch_revision_count, lectures.count { it.pendingRevision || it.revisionTagged }) else strings.text(tab.titleRes)) })
            }
        }
        if (state.lectureTab == LectureTab.OLDER) Text(
            strings.text(R.string.toppers_batch_skipped_unfinished_lectures_plus_lectures_you_add_here),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (empty) {
            if (state.lectureTab != LectureTab.LECTURES) EmptyAction(
                title = if (state.lectureTab == LectureTab.OLDER) strings.text(R.string.toppers_batch_no_backlog) else strings.text(R.string.toppers_batch_no_revision_planned),
                detail = if (state.lectureTab == LectureTab.OLDER)
                    strings.text(R.string.toppers_batch_skipped_lectures_and_lectures_you_add_will_appear_here)
                else strings.text(R.string.toppers_batch_finish_a_lecture_then_choose_when_to_revise_it),
                icon = if (state.lectureTab == LectureTab.OLDER) Icons.Default.Schedule else Icons.Default.EventRepeat,
            ) { vm.selectLectureTab(LectureTab.LECTURES) }
            else Text(if (subject.key == "gk") strings.text(R.string.toppers_batch_gk_lectures_will_appear_when_the_official_list_is_added) else strings.text(R.string.toppers_batch_no_lectures_here_yet))
        }
    }
}

@Composable private fun SubjectOptionsArea(state: BatchUiState, subject: BatchSubject,
                                            vm: ToppersBatchViewModel) {
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
        SubjectOptions(subject, state, vm)
    }
}

@Composable private fun ProgressContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val active = overview.subjects.filter { it.enabled }
    val rows = overview.progress.bySubject.filter { row -> active.any { it.id == row.subjectId } }
    val done = rows.sumOf { it.completed }
    val total = rows.sumOf { it.total }
    val behind = rows.sumOf { it.backlog }
    val fraction = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val percentLabel = strings.percent(fraction)
    val pink = Color(0xFF9D174D)
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BatchReleaseProgressPanel(state, vm)
        DueRevisionPanel(state, vm)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(strings.text(R.string.toppers_batch_whole_course_progress), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(percentLabel, style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(strings.quantity(R.plurals.toppers_batch_count_of_count_lectures_complete, total, done, total), Modifier.weight(1f).padding(bottom = 5.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth(), height = 8.dp,
                    indicatorColor = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${(total - done).coerceAtLeast(0)}", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(strings.text(R.string.toppers_batch_lectures_left), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$behind", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(strings.text(R.string.toppers_batch_in_your_backlog), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        CompletionTrendChart(
            lectures = overview.lectures.filter { lecture -> active.any { it.id == lecture.subjectId } },
            today = LocalDate.parse(ToppersBatchViewModel.indiaDay()),
        )
        RecentActivityCalendar(
            lectures = overview.lectures.filter { lecture -> active.any { it.id == lecture.subjectId } },
            today = LocalDate.parse(ToppersBatchViewModel.indiaDay()),
        )
        Text(strings.quantity(R.plurals.toppers_batch_count_revision_sessions_completed, overview.studyWorkflow.revisionsCompleted, overview.studyWorkflow.revisionsCompleted), style = MaterialTheme.typography.bodyMedium)
        ImportantDates(overview, state.studyMode)
        SectionHeading(strings.text(R.string.toppers_batch_whole_course_by_subject))
        if (active.isEmpty()) Text(strings.text(R.string.toppers_batch_no_subjects_to_show_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(strings.text(R.string.toppers_batch_lectures_completed_in_each_subject),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                active.forEach { subject ->
                    val row = rows.firstOrNull { it.subjectId == subject.id }
                    val subjectDone = row?.completed ?: 0
                    val subjectTotal = row?.total ?: 0
                    val percent = if (subjectTotal == 0) 0 else
                        ((subjectDone.toFloat() / subjectTotal) * 100).toInt()
                    val accent = subjectProgressColor(subject)
                    Column(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = batchFeatureAction({ vm.selectSubject(subject.id) }))
                            .semantics { contentDescription =
                                strings.text(R.string.toppers_batch_open_count_count_of_count_lectures_done, strings.subject(subject), subjectDone, subjectTotal) },
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.subject(subject), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("$subjectDone / $subjectTotal", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$percent%", style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold, color = accent)
                        }
                        ProgressIndicator(
                            progress = if (subjectTotal == 0) 0f else
                                (subjectDone.toFloat() / subjectTotal).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(), height = 9.dp,
                            indicatorColor = accent, trackColor = accent.copy(alpha = 0.13f),
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun CalendarContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val month = runCatching { YearMonth.parse(state.month) }.getOrNull() ?: return
    val calendar = state.calendar
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val calendarDates = List(firstOffset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val completedText = Color(0xFF9D174D)
    val selectionOutline = Color(0xFF344054)
    val classDot = Color(0xFFBE185D)
    var listMode by rememberSaveable { mutableStateOf(false) }
    val activityDates = ((calendar?.days?.keys.orEmpty()) + (calendar?.classes?.keys.orEmpty()))
        .filter { it.startsWith(state.month) }.distinct().sorted()
    LaunchedEffect(listMode, activityDates, state.selectedDay) {
        if (listMode && activityDates.isNotEmpty() && state.selectedDay !in activityDates) {
            vm.selectDay(activityDates.last())
        }
    }
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CourseSwitch(strings.text(R.string.toppers_batch_month), !listMode, Modifier.weight(1f)) { listMode = false }
            CourseSwitch(strings.text(R.string.toppers_batch_list), listMode, Modifier.weight(1f)) { listMode = true }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = batchFeatureAction({ vm.changeMonth(-1) }), modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_previous_month) }) {
                Text("‹", style = MaterialTheme.typography.headlineSmall)
            }
            Text(month.month.getDisplayName(TextStyle.FULL, strings.locale) + " " + month.year,
                Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1)
            IconButton(onClick = batchFeatureAction({ vm.changeMonth(1) }), modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_next_month) }) {
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = batchFeatureAction(vm::currentMonth)) { Text(strings.text(R.string.toppers_batch_today)) }
        }
        if (!listMode) {
            Row(Modifier.fillMaxWidth()) {
                strings.weekdays().forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            calendarDates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (week + List(7 - week.size) { null }).forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f)) else {
                            val key = date.toString()
                            val count = calendar?.days?.get(key)?.size ?: 0
                            val classes = calendar?.classes?.get(key)?.size ?: 0
                            val selected = state.selectedDay == key
                            val colour = when {
                                count >= 3 -> Color(0xFFED8DB8)
                                count == 2 -> Color(0xFFF9C9DE)
                                count == 1 -> Color(0xFFFCE7F3)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Column(Modifier.weight(1f).height(51.dp)
                                .background(colour, RoundedCornerShape(8.dp))
                                .then(if (selected) Modifier.border(2.dp, selectionOutline, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable(role = Role.Button, onClick = batchFeatureAction({ vm.selectDay(key) }))
                                .semantics {
                                    this.selected = selected
                                    contentDescription = strings.text(R.string.toppers_batch_count_count_lectures_done_count_classes_planned, key, count, classes)
                                }
                                .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween) {
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium,
                                    color = if (count > 0) Color(0xFF831843)
                                        else MaterialTheme.colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (classes > 0) Box(Modifier.size(5.dp).background(classDot, CircleShape))
                                    if (count > 0) Text("$count", style = MaterialTheme.typography.labelSmall,
                                        color = completedText)
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(Color(0xFFFCE7F3), RoundedCornerShape(2.dp)))
                Text(strings.text(R.string.toppers_batch_1_done), style = MaterialTheme.typography.labelSmall)
                Box(Modifier.size(10.dp).background(Color(0xFFF9C9DE), RoundedCornerShape(2.dp)))
                Text(strings.text(R.string.toppers_batch_2_done), style = MaterialTheme.typography.labelSmall)
                Box(Modifier.size(10.dp).background(Color(0xFFED8DB8), RoundedCornerShape(2.dp)))
                Text(strings.text(R.string.toppers_batch_3_done), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(6.dp).background(classDot, CircleShape))
                Text(strings.text(R.string.toppers_batch_class), style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).border(2.dp, selectionOutline, RoundedCornerShape(2.dp)))
                Text(strings.text(R.string.toppers_batch_selected_day), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (activityDates.isEmpty()) Text(strings.text(R.string.toppers_batch_no_finished_lectures_or_planned_classes_this_month),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            activityDates.forEach { key ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull()
                val doneCount = calendar?.days?.get(key)?.size ?: 0
                val classCount = calendar?.classes?.get(key)?.size ?: 0
                Card(onClick = batchFeatureAction({ vm.selectDay(key) }), modifier = Modifier.fillMaxWidth(),
                    border = if (key == state.selectedDay) BorderStroke(2.dp, selectionOutline) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(date?.format(DateTimeFormatter.ofPattern("d MMM", strings.locale)) ?: key,
                            Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(strings.quantity(R.plurals.toppers_batch_count_done, doneCount, doneCount, classCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        val selectedDate = runCatching { LocalDate.parse(state.selectedDay) }.getOrNull()
        val doneRows = calendar?.days?.get(state.selectedDay).orEmpty()
        val plannedRows = calendar?.classes?.get(state.selectedDay).orEmpty()
        if (!listMode || state.selectedDay in activityDates) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(selectedDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", strings.locale)) ?: state.selectedDay,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (doneRows.isEmpty() && plannedRows.isEmpty()) {
                    Text(strings.text(R.string.toppers_batch_no_lectures_finished_or_classes_planned_for_this_day),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (doneRows.isNotEmpty()) {
                        Text(strings.quantity(R.plurals.toppers_batch_finished_on_this_day_count, doneRows.size, doneRows.size), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = completedText)
                        doneRows.forEach { row -> CalendarEventRow(row, state, planned = false) }
                    }
                    if (doneRows.isNotEmpty() && plannedRows.isNotEmpty()) HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                    if (plannedRows.isNotEmpty()) {
                        Text(strings.quantity(R.plurals.toppers_batch_classes_planned_for_this_day_count, plannedRows.size, plannedRows.size), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        plannedRows.forEach { row -> CalendarEventRow(row, state, planned = true) }
                    }
                }
            }
        }
    }
}

@Composable private fun CalendarEventRow(row: BatchLecture, state: BatchUiState, planned: Boolean) {
    val strings = rememberBatchStrings()
    val subject = state.overview?.subjects?.firstOrNull { it.id == row.subjectId }?.let(strings::subject).orEmpty()
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(subject, style = MaterialTheme.typography.labelMedium, color = Color(0xFF9D174D))
            Text(row.displayTopic, fontWeight = FontWeight.SemiBold)
            if (planned) Text(listOfNotNull(row.classTime, if (row.completedAt == null) strings.text(R.string.toppers_batch_not_done) else strings.text(R.string.toppers_batch_done)).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ManageContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(strings.text(R.string.toppers_batch_deleted_subjects), strings.text(R.string.toppers_batch_your_saved_work_stays_here_restore_an_item_when_you_need_it))
        if (overview.removedSubjects.isEmpty()) Text(strings.text(R.string.toppers_batch_nothing_deleted))
        overview.removedSubjects.forEach { subject -> RestoreRow(strings.subject(subject)) { vm.restoreSubject(subject) } }
        SectionHeading(strings.text(R.string.toppers_batch_deleted_lectures))
        if (overview.removedLectures.isEmpty()) Text(strings.text(R.string.toppers_batch_nothing_deleted))
        overview.removedLectures.forEach { lecture -> RestoreRow(lecture.displayTopic) { vm.restoreLecture(lecture) } }
    }
}

@Composable private fun RestoreRow(title: String, onRestore: () -> Unit) {
    val strings = rememberBatchStrings()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        OutlinedButton(onClick = batchFeatureAction(onRestore)) { Text(strings.text(R.string.toppers_batch_restore)) }
    }
}

private fun String?.toComposeColor(): Color? = this?.takeIf { it.matches(Regex("^#[0-9A-Fa-f]{6}$")) }
    ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

private fun openExternal(context: android.content.Context, url: String) {
    if (url.startsWith("https://")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
