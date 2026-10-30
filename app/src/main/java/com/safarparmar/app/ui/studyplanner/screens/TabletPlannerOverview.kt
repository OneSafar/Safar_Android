package com.safarparmar.app.ui.studyplanner.screens

import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.components.IconButton
import com.composables.ui.components.ButtonStyle
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.feature.toppersbatch.rememberBatchStrings
import com.safarparmar.app.ui.components.SafarButton
import com.safarparmar.app.ui.components.SafarMenuAction
import com.safarparmar.app.ui.components.SafarOverflowMenu
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.logic.rollup
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import com.safarparmar.app.ui.studyplanner.logic.examBadgeLabel

/** A local preview: selecting an exam does not load or activate its full plan. */
@Composable
internal fun TabletPlannerOverview(
    previewId: String?, onPreviewSelect: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    previewScrollState: androidx.compose.foundation.ScrollState,
    plans: List<StudyPlan>, draftCount: Int, loading: Boolean, activePlanId: String?,
    onRefresh: () -> Unit, onNewPlan: () -> Unit, onDrafts: () -> Unit,
    onTopperBatch: () -> Unit, onGkBatch: () -> Unit,
    onOpenPlan: (StudyPlan) -> Unit, onRename: (StudyPlan) -> Unit, onDelete: (StudyPlan) -> Unit,
) {
    val selected = plans.firstOrNull { it.id == previewId }
        ?: plans.firstOrNull { it.id == activePlanId } ?: plans.firstOrNull()
    val numbers = remember(plans) {
        plans.sortedWith(compareBy<StudyPlan>({ it.createdAt.orEmpty() }, { it.id }))
            .mapIndexed { index, plan -> plan.id to index + 1 }.toMap()
    }
    val batchStrings = rememberBatchStrings()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.planner_tablet_title), fontSize = 25.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.planner_tablet_subtitle), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PlannerPrimaryAction(stringResource(R.string.planner_new_plan), Icons.Default.Add, onNewPlan)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SafarPullRefreshBox(
                isRefreshing = loading && plans.isNotEmpty(), onRefresh = onRefresh,
                modifier = Modifier.weight(0.61f).fillMaxHeight(),
            ) {
                LazyColumn(Modifier.fillMaxSize().selectableGroup(), state = listState, contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item(key = "batch_poster") { TabletBatchHero(onTopperBatch) }
                    item(key = "plan_shortcuts") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                PlannerGk5Button(
                                    title = batchStrings.text(R.string.gk5_tracker_title),
                                    subtitle = batchStrings.text(R.string.gk5_tracker_subtitle),
                                    onClick = onGkBatch,
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                TabletPlannerShortcut(stringResource(R.string.planner_drafts_count, draftCount),
                                    stringResource(R.string.planner_resume_building), Icons.AutoMirrored.Outlined.Article,
                                    MaterialTheme.colorScheme.surface, onDrafts)
                            }
                        }
                    }
                    item(key = "exam_heading") {
                        Text(stringResource(R.string.planner_active_exams_count, plans.size), fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (loading && plans.isEmpty()) {
                        items(3) { com.safarparmar.app.ui.components.PlanCardSkeleton() }
                    } else if (plans.isEmpty()) {
                        item { Text(stringResource(R.string.planner_no_target_exam), modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(plans, key = { it.id }) { plan ->
                        TabletExamRow(plan, numbers[plan.id] ?: 1, selected?.id == plan.id,
                            { onPreviewSelect(plan.id) }, { onRename(plan) }, { onDelete(plan) })
                    }
                }
            }
            Surface(Modifier.weight(0.39f).fillMaxHeight().padding(bottom = 20.dp),
                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft)) {
                if (selected != null) {
                    key(selected.id) {
                        TabletPlanPreview(selected, numbers[selected.id] ?: 1, previewScrollState,
                            onOpen = { onOpenPlan(selected) }, onRename = { onRename(selected) }, onDelete = { onDelete(selected) })
                    }
                } else {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(stringResource(R.string.planner_tablet_preview_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletBatchHero(onClick: () -> Unit) {
    val strings = rememberBatchStrings()
    SafarButton(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(0.dp)) {
        Image(
            painter = painterResource(R.drawable.parmar_tracker_tablet_banner),
            contentDescription = strings.text(R.string.toppers_batch_parmar_topper_batch_tracker),
            modifier = Modifier.fillMaxWidth().aspectRatio(3f),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun TabletPlannerShortcut(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, onClick: () -> Unit, primary: Boolean = false) {
    val ink = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    SafarButton(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) MaterialTheme.colorScheme.primary else color,
            contentColor = ink), contentPadding = PaddingValues(12.dp)) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp, color = ink.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TabletExamRow(plan: StudyPlan, number: Int, selected: Boolean,
    onSelect: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().clip(shape).background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface)
        .border(1.dp, if (selected) accent else PlannerFlatColors.BorderSoft, shape)
        .selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(plan.title.ifBlank { plan.examType.orEmpty() }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.planner_tablet_plan_number, number) +
                (planCreatedDate(plan.createdAt)?.let { " · $it" } ?: ""), fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PlanDateBadge(plan)
        PlanOptions(plan.title, onRename, onDelete)
    }
}

@Composable
private fun PlanOptions(title: String, onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SafarOverflowMenu(expanded, { expanded = it }, listOf(
        SafarMenuAction(stringResource(R.string.planner_rename_exam), onRename, Icons.Default.Edit),
        SafarMenuAction(stringResource(R.string.common_delete), onDelete, Icons.Default.Delete, destructive = true),
    )) {
        IconButton(onClick = { expanded = !expanded }, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.planner_tablet_plan_options, title), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlanDateBadge(plan: StudyPlan) {
    val days = daysUntil(plan.examDate)
    val label = examBadgeLabel(days)
    if (label.isNotBlank()) {
        val color = if (days != null && days >= 0) {
            MaterialTheme.colorScheme.secondary
        } else MaterialTheme.colorScheme.onSurfaceVariant
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color,
            modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun TabletPlanPreview(plan: StudyPlan, number: Int, scrollState: androidx.compose.foundation.ScrollState, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.verticalScroll(scrollState).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Spacer(Modifier.weight(1f))
            PlanOptions(plan.title, onRename, onDelete)
        }
        Text(plan.title.ifBlank { plan.examType.orEmpty() }, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.planner_tablet_plan_number, number) +
            (planCreatedDate(plan.createdAt)?.let { " · $it" } ?: ""), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PlanDateBadge(plan)
        val progress = remember(plan) { plan.rollup() }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress.overallProgressPercent.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Text("${progress.overallProgressPercent}%", color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold)
        PlannerPrimaryAction(stringResource(R.string.planner_tablet_open_plan), Icons.Default.PlayArrow, onOpen, Modifier.fillMaxWidth())
        Text(stringResource(R.string.planner_tablet_about_plan), fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        PreviewMetadata(stringResource(R.string.planner_exam_name), plan.title.ifBlank { plan.examType.orEmpty() })
        PreviewMetadata(stringResource(R.string.planner_tablet_plan_number_label), stringResource(R.string.planner_tablet_plan_number, number))
        planCreatedDate(plan.createdAt)?.let { PreviewMetadata(stringResource(R.string.planner_tablet_created_on), it) }
        plan.examDate?.takeIf { it.isNotBlank() }?.let { PreviewMetadata(stringResource(R.string.planner_tablet_exam_date), it.take(10)) }
        plan.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewMetadata(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(0.4f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(0.6f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PlannerPrimaryAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    SafarButton(onClick, modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
