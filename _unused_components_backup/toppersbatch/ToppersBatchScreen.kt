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

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
)

@Composable
fun ToppersBatchScreen(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: ToppersBatchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedSubject = state.overview?.subjects?.firstOrNull {
        it.id == (state.todaySubjectId ?: state.selectedSubjectId)
    }
    val pageTitle = selectedSubject?.name ?: when (state.section) {
        BatchSection.TODAY -> "Parmar Toppers Batch"
        else -> state.section.title
    }
    BackHandler(enabled = selectedSubject != null) { viewModel.backFromSubject() }
    SafarDrawerScaffold(
        title = pageTitle, currentRoute = Routes.STUDY_PLANNER,
        isDarkTheme = isDarkTheme, onNavigate = onNavigate, onToggleDarkTheme = onToggleDarkTheme,
        topBarContentColor = Color.White,
        topBarGradient = Brush.horizontalGradient(listOf(Color(0xFF831843), Color(0xFFBE185D), Color(0xFFDB2777))),
        secondaryNavigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        secondaryNavigationContentDescription = if (state.todaySubjectId != null) "Back to Today"
            else if (selectedSubject != null) "Back to Lectures" else "Back to Study Planner",
        onSecondaryNavigationClick = { if (selectedSubject != null) viewModel.backFromSubject() else onBack() },
    ) { outerPadding ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            Modifier.fillMaxSize().padding(top = outerPadding.calculateTopPadding()),
        ) {
            val wide = maxWidth >= 700.dp
            val baseColors = MaterialTheme.colorScheme
            val batchColors = baseColors.copy(
                background = if (isDarkTheme) Color(0xFF19151A) else Color(0xFFFAF7F2),
                surface = if (isDarkTheme) Color(0xFF251E25) else Color(0xFFFFFDFC),
                surfaceVariant = if (isDarkTheme) Color(0xFF352C35) else Color(0xFFF2ECEF),
                outlineVariant = if (isDarkTheme) Color(0xFF514450) else Color(0xFFE8DCE2),
                primary = Color(0xFFBE185D),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFCE7F3),
                onPrimaryContainer = Color(0xFF831843),
                secondary = Color(0xFF9D174D),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFCE7F3),
                onSecondaryContainer = Color(0xFF831843),
                surfaceContainerHigh = if (isDarkTheme) Color(0xFF352630) else Color(0xFFFFF8FB),
            )
            MaterialTheme(colorScheme = batchColors, shapes = MaterialTheme.shapes.copy(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp))) {
                CompositionLocalProvider(
                    LocalColorScheme provides if (isDarkTheme) ColorScheme.Dark else ColorScheme.Light,
                    LocalSafarComposablesAccent provides batchColors.primary,
                ) {
                    SafarComposablesTheme { Row(Modifier.fillMaxSize()) {
                if (wide && state.gate == BatchGate.READY) BatchRail(state.section, viewModel::select)
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = { if (!wide && state.gate == BatchGate.READY) BatchBottomNav(state.section, viewModel::select) },
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        when (state.gate) {
                            BatchGate.LOADING -> CenterMessage("Opening your Toppers Batch…")
                            BatchGate.PREMIUM -> GateMessage("SAFAR Premium is needed for Toppers Batch.", "See Premium") { onNavigate(Routes.PREMIUM) }
                            BatchGate.UNAVAILABLE -> GateMessage("Toppers Batch is not available for this account yet.", "Try again", viewModel::open)
                            BatchGate.ERROR -> GateMessage(state.error ?: "Could not open Toppers Batch.", "Try again", viewModel::open)
                            BatchGate.READY -> BatchBody(state, viewModel)
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
    NavigationRail {
        navigation.forEach { item ->
            NavigationRailItem(selected = selected == item.section, onClick = { choose(item.section) },
                colors = NavigationRailItemDefaults.colors(selectedIconColor = Color(0xFFBE185D),
                    selectedTextColor = Color(0xFF9D174D), indicatorColor = Color(0xFFFCE7F3)),
                icon = { Icon(item.icon, contentDescription = null) }, label = { Text(item.section.title) }, alwaysShowLabel = true)
        }
    }
}

/** The three tracker destinations use the shared Composables UI navigation bar. */
@Composable private fun BatchBottomNav(selected: BatchSection, choose: (BatchSection) -> Unit) {
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
                    com.composables.ui.components.Text(item.section.title,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun CenterMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message) }
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
    val overview = state.overview ?: return
    val subject = overview.subjects.firstOrNull { it.id == state.selectedSubjectId }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.error != null || state.message != null) item {
            Notice(state.error ?: state.message.orEmpty(), state.error != null, vm::clearNotice)
        }
        when (state.section) {
            BatchSection.TODAY -> item {
                val focused = overview.subjects.firstOrNull { it.id == state.todaySubjectId }
                if (focused != null) TodaySubjectFocus(state, focused, vm)
                else ToppersBatchDashboard(state, vm)
            }
            BatchSection.COURSES -> if (subject == null) item { CoursesContent(state, vm) }
                else subjectItems(state, subject, vm)
            BatchSection.PROGRESS -> item { ProgressContent(state, vm) }
            BatchSection.CALENDAR -> item { CalendarContent(state, vm) }
        }
    }
}

private fun Modifier.fullContentWidth(): Modifier = this.fillMaxWidth()

@Composable private fun Notice(message: String, error: Boolean, dismiss: () -> Unit) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = dismiss) { Text("Close") }
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
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFBE185D), modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text("See lectures") }
        }
    }
}

@Composable private fun CoursesContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    val subjects = listOf("english", "reasoning", "mathematics", "gk")
        .mapNotNull { key -> overview.subjects.firstOrNull { it.key == key } }
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your subjects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Browse every lecture, backlog, and revision plan by subject.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        subjects.forEach { subject ->
            SubjectRow(subject, overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }) {
                vm.selectSubject(subject.id)
            }
        }
    }
}

@Composable private fun CourseSwitch(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(color = if (selected) Color(0xFF9D174D) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp), modifier = modifier.clickable(role = Role.Tab, onClick = onClick)) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable private fun SubjectRow(subject: BatchSubject, progress: SubjectProgress?, onClick: () -> Unit) {
    val done = progress?.completed ?: 0
    val total = progress?.total ?: 0
    val accent = subjectProgressColor(subject)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { contentDescription =
        "Open ${subject.name}, $done of $total lectures done" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Text(subject.name.take(1).uppercase(Locale.ENGLISH), color = accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProgressIndicator(progress = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(), height = 5.dp, indicatorColor = accent,
                    trackColor = accent.copy(alpha = 0.13f))
            }
            Text("$done / $total", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("›", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun subjectProgressColor(subject: BatchSubject): Color {
    return when {
        subject.name.contains("english", ignoreCase = true) -> Color(0xFFBE185D)
        subject.name.contains("math", ignoreCase = true) -> Color(0xFFEA580C)
        subject.name.contains("reason", ignoreCase = true) -> Color(0xFF5B21B6)
        else -> subject.color.toComposeColor() ?: Color(0xFFBE185D)
    }
}

private fun LazyListScope.subjectItems(state: BatchUiState, subject: BatchSubject, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    val lectures = overview.lectures.filter { it.subjectId == subject.id }
    val backlogIds = overview.watchList.firstOrNull { it.subjectId == subject.id }?.backlogLectureIds.orEmpty().toSet()
    val visible = when (state.lectureTab) {
        LectureTab.LECTURES -> lectures
        LectureTab.OLDER -> lectures.filter { it.id in backlogIds }
        LectureTab.REVISION -> lectures.filter { it.pendingRevision }
    }
    item(key = "subject-intro") { SubjectIntro(state, subject, lectures, visible.isEmpty(), vm) }
    var lastGroup: Pair<String, String>? = null
    visible.forEach { lecture ->
        val group = lecture.sourceMonth to lecture.sourceWeek
        if (state.lectureTab == LectureTab.LECTURES && group != lastGroup) {
            item(key = "group-${group.first}-${group.second}") {
                Surface(color = Color(0xFFFDF2F8), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fullContentWidth()) {
                    Text("${group.first} · ${group.second}", modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge, color = Color(0xFF9D174D),
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        lastGroup = group
        item(key = lecture.id) { Box(Modifier.fullContentWidth()) { LectureCard(lecture, subject, state, vm) } }
    }
}

@Composable private fun SubjectIntro(state: BatchUiState, subject: BatchSubject, lectures: List<BatchLecture>,
                                     empty: Boolean, vm: ToppersBatchViewModel) {
    val watch = state.overview?.watchList?.firstOrNull { it.subjectId == subject.id }
    val done = lectures.count { it.completedAt != null }
    val pink = Color(0xFFBE185D)
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8), contentColor = Color(0xFF831843)),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your progress", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("${if (lectures.isEmpty()) 0 else done * 100 / lectures.size}%",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("$done of ${lectures.size} lectures done")
                ProgressIndicator(progress = if (lectures.isEmpty()) 0f else done.toFloat() / lectures.size,
                    modifier = Modifier.fillMaxWidth(), height = 7.dp, indicatorColor = pink, trackColor = Color(0xFFF9C9DE))
            }
        }
        Text("Follow your own lecture progress. Parmar class dates are for live events only.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val context = LocalContext.current
        subject.externalUrl?.takeIf { it.startsWith("https://") }?.let { url ->
            OutlinedButton(onClick = { openExternal(context, url) }) { Text("Open course website") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LectureTab.entries.forEach { tab ->
                FilterChip(selected = state.lectureTab == tab, onClick = { vm.selectLectureTab(tab) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pink,
                        selectedLabelColor = Color.White),
                    label = { Text(if (tab == LectureTab.OLDER) "Backlog ${watch?.backlogLectureIds?.size ?: 0}" else if (tab == LectureTab.REVISION) "Revision ${lectures.count { it.pendingRevision }}" else tab.title) })
            }
        }
        if (state.lectureTab == LectureTab.OLDER) Text(
            "Skipped unfinished lectures, plus lectures you add here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (empty) {
            if (state.lectureTab != LectureTab.LECTURES) EmptyAction(
                title = if (state.lectureTab == LectureTab.OLDER) "No backlog" else "No revision planned",
                detail = if (state.lectureTab == LectureTab.OLDER)
                    "Skipped lectures and lectures you add will appear here."
                else "Finish a lecture, then choose when to revise it.",
                icon = if (state.lectureTab == LectureTab.OLDER) Icons.Default.Schedule else Icons.Default.EventRepeat,
            ) { vm.selectLectureTab(LectureTab.LECTURES) }
            else Text(if (subject.key == "gk") "GK lectures will appear when the official list is added." else "No lectures here yet.")
        }
    }
}

@Composable private fun SubjectOptionsArea(state: BatchUiState, subject: BatchSubject,
                                            vm: ToppersBatchViewModel) {
    var showOptions by rememberSaveable(subject.id) { mutableStateOf(false) }
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton(onClick = { showOptions = !showOptions }) { Text(if (showOptions) "Hide course options" else "Course options") }
        if (showOptions) SubjectOptions(subject, state, vm)
    }
}

@Composable private fun ProgressContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    val active = overview.subjects.filter { it.enabled }
    val rows = overview.progress.bySubject.filter { row -> active.any { it.id == row.subjectId } }
    val done = rows.sumOf { it.completed }
    val total = rows.sumOf { it.total }
    val behind = rows.sumOf { it.backlog }
    val fraction = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val percentLabel = if (done > 0 && fraction < 0.01f)
        String.format(Locale.ENGLISH, "%.1f%%", fraction * 100f)
    else "${(fraction * 100).toInt()}%"
    val pink = Color(0xFF9D174D)
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Batch progress", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(percentLabel, style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("$done of $total lectures complete", Modifier.weight(1f).padding(bottom = 5.dp),
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
                        Text("Lectures left", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$behind", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("In your backlog", style = MaterialTheme.typography.bodySmall,
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
        SectionHeading("By subject")
        if (active.isEmpty()) Text("No subjects to show yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Lectures completed in each subject",
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
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.selectSubject(subject.id) }
                            .semantics { contentDescription =
                                "Open ${subject.name}, $subjectDone of $subjectTotal lectures done" },
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(subject.name, Modifier.weight(1f),
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
            CourseSwitch("Month", !listMode, Modifier.weight(1f)) { listMode = false }
            CourseSwitch("List", listMode, Modifier.weight(1f)) { listMode = true }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { vm.changeMonth(-1) }, modifier = Modifier.semantics { contentDescription = "Previous month" }) {
                Text("‹", style = MaterialTheme.typography.headlineSmall)
            }
            Text(month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.year,
                Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1)
            IconButton(onClick = { vm.changeMonth(1) }, modifier = Modifier.semantics { contentDescription = "Next month" }) {
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = vm::currentMonth) { Text("Today") }
        }
        if (!listMode) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
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
                                .clickable(role = Role.Button) { vm.selectDay(key) }
                                .semantics {
                                    this.selected = selected
                                    contentDescription = "$key, $count lectures done, $classes classes planned"
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
                Text("1 done", style = MaterialTheme.typography.labelSmall)
                Box(Modifier.size(10.dp).background(Color(0xFFF9C9DE), RoundedCornerShape(2.dp)))
                Text("2 done", style = MaterialTheme.typography.labelSmall)
                Box(Modifier.size(10.dp).background(Color(0xFFED8DB8), RoundedCornerShape(2.dp)))
                Text("3+ done", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(6.dp).background(classDot, CircleShape))
                Text("Class", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).border(2.dp, selectionOutline, RoundedCornerShape(2.dp)))
                Text("Selected day", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (activityDates.isEmpty()) Text("No finished lectures or planned classes this month.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            activityDates.forEach { key ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull()
                val doneCount = calendar?.days?.get(key)?.size ?: 0
                val classCount = calendar?.classes?.get(key)?.size ?: 0
                Card(onClick = { vm.selectDay(key) }, modifier = Modifier.fillMaxWidth(),
                    border = if (key == state.selectedDay) BorderStroke(2.dp, selectionOutline) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(date?.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) ?: key,
                            Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("$doneCount done · $classCount classes", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(selectedDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)) ?: state.selectedDay,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (doneRows.isEmpty() && plannedRows.isEmpty()) {
                    Text("No lectures finished or classes planned for this day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (doneRows.isNotEmpty()) {
                        Text("Finished on this day (${doneRows.size})", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = completedText)
                        doneRows.forEach { row -> CalendarEventRow(row, state, planned = false) }
                    }
                    if (doneRows.isNotEmpty() && plannedRows.isNotEmpty()) HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                    if (plannedRows.isNotEmpty()) {
                        Text("Classes planned for this day (${plannedRows.size})", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        plannedRows.forEach { row -> CalendarEventRow(row, state, planned = true) }
                    }
                }
            }
        }
    }
}

@Composable private fun CalendarEventRow(row: BatchLecture, state: BatchUiState, planned: Boolean) {
    val subject = state.overview?.subjects?.firstOrNull { it.id == row.subjectId }?.name.orEmpty()
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(subject, style = MaterialTheme.typography.labelMedium, color = Color(0xFF9D174D))
            Text(row.displayTopic, fontWeight = FontWeight.SemiBold)
            if (planned) Text(listOfNotNull(row.classTime, if (row.completedAt == null) "Not done" else "Done").joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ManageContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    Column(Modifier.fullContentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading("Deleted subjects", "Your saved work stays here. Restore an item when you need it.")
        if (overview.removedSubjects.isEmpty()) Text("Nothing deleted.")
        overview.removedSubjects.forEach { subject -> RestoreRow(subject.name) { vm.restoreSubject(subject) } }
        SectionHeading("Deleted lectures")
        if (overview.removedLectures.isEmpty()) Text("Nothing deleted.")
        overview.removedLectures.forEach { lecture -> RestoreRow(lecture.displayTopic) { vm.restoreLecture(lecture) } }
    }
}

@Composable private fun RestoreRow(title: String, onRestore: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        OutlinedButton(onClick = onRestore) { Text("Restore") }
    }
}

private fun String?.toComposeColor(): Color? = this?.takeIf { it.matches(Regex("^#[0-9A-Fa-f]{6}$")) }
    ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

private fun openExternal(context: android.content.Context, url: String) {
    if (url.startsWith("https://")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
