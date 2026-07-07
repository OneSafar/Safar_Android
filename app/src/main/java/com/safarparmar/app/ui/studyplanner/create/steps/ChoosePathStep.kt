package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.studyplanner.create.PlanSource
import com.safarparmar.app.ui.studyplanner.screens.CreatePlanModeCard

@Composable
fun ChoosePathStep(
    onChoose: (PlanSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Create your study plan",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Choose how you want to start",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CreatePlanModeCard(
            icon = Icons.Default.School,
            title = "Use a template",
            subtitle = "SSC, UPSC, Railways, Defence — ready to customize",
            selected = false,
            onClick = { onChoose(PlanSource.Template) },
        )
        CreatePlanModeCard(
            icon = Icons.Default.Edit,
            title = "Build it myself",
            subtitle = "Add your own subjects and topics",
            selected = false,
            onClick = { onChoose(PlanSource.Manual) },
        )
        CreatePlanModeCard(
            icon = Icons.Default.ContentPaste,
            title = "Paste your syllabus",
            subtitle = "Copy from PDF, website, or notes — we'll organize it",
            selected = false,
            onClick = { onChoose(PlanSource.Paste) },
        )
    }
}
