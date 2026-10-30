package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.studyplanner.create.PlanSource

/** Explicit source choices feed the existing wizard paths; no plan is created here. */
@Composable
fun ChoosePathStep(onChoose: (PlanSource) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.planner_warm_choose_source), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.planner_warm_choose_source_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PlannerSourceChoice(stringResource(R.string.planner_use_template), stringResource(R.string.planner_use_template_body), Icons.Default.School) { onChoose(PlanSource.Template) }
        PlannerSourceChoice(stringResource(R.string.planner_warm_use_saved), stringResource(R.string.planner_reuse_or_resume), Icons.Default.Description) { onChoose(PlanSource.Saved) }
        PlannerSourceChoice(stringResource(R.string.planner_warm_create_syllabus), stringResource(R.string.planner_build_myself_body), Icons.Default.Edit) { onChoose(PlanSource.Manual) }
    }
}

@Composable
private fun PlannerSourceChoice(title: String, body: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
