package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Icon
import com.composables.ui.components.Tab
import com.composables.ui.components.TabList
import com.composables.ui.components.Tabs
import com.composables.ui.components.Text
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme

/** Shared by every planner destination, including the standalone Syllabus route. */
@Composable
internal fun PlannerTopNavigation(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    val sections = listOf(PlannerSection.YOUR_EXAMS, PlannerSection.PLAN, PlannerSection.SYLLABUS,
        PlannerSection.CALENDAR, PlannerSection.INSIGHTS)
    val icons = listOf(Icons.Default.School, Icons.Default.Today, Icons.AutoMirrored.Filled.FactCheck,
        Icons.Default.CalendarMonth, Icons.Default.Insights)
    val labels = listOf(R.string.planner_warm_exams, R.string.planner_warm_today,
        R.string.planner_warm_syllabus, R.string.planner_warm_calendar, R.string.planner_warm_progress)
    val parent = if (selected == PlannerSection.REVISION) PlannerSection.PLAN else selected
    Row(Modifier.fillMaxWidth().heightIn(min = 61.6.dp).padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.nav_study_planner), modifier = Modifier.weight(1f), fontSize = 20.7.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        CompositionLocalProvider(LocalSafarComposablesAccent provides PlannerFlatColors.PrimaryAccent) {
            SafarComposablesTheme {
                Tabs(selectedTab = parent, onSelectedTabChange = onSelect, orderedTabs = sections,
                    modifier = Modifier.weight(3.5f)) {
                    TabList(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        dividerColor = androidx.compose.ui.graphics.Color.Transparent) {
                        sections.forEachIndexed { index, section ->
                            val color = if (parent == section) PlannerFlatColors.PrimaryAccent
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            Tab(key = section, activateOnFocus = false,
                                modifier = Modifier.heightIn(min = 52.8.dp).padding(horizontal = 4.dp)
                                    .background(
                                        if (parent == section) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        else androidx.compose.ui.graphics.Color.Transparent,
                                        RoundedCornerShape(18.dp),
                                    ),
                                icon = { Icon(icons[index], contentDescription = null, tint = color, modifier = Modifier.size(23.dp)) },
                                text = { Text(stringResource(labels[index]), color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.95.sp) })
                        }
                    }
                }
            }
        }
    }
}
