package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Text
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SafarButton
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors

/** Dashboard slots reuse Home's live state and handlers; each lower pane scrolls independently. */
@Composable
internal fun TabletPlanHomeLayout(
    agendaListState: androidx.compose.foundation.lazy.LazyListState,
    checklistScrollState: androidx.compose.foundation.ScrollState,
    summary: @Composable RowScope.() -> Unit,
    warning: (@Composable () -> Unit)?,
    agendaHeader: @Composable ColumnScope.() -> Unit,
    agendaItems: LazyListScope.() -> Unit,
    dailyTodo: @Composable () -> Unit,
    showFinishDay: Boolean,
    onFinishDay: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Keep the full progress ring visible, outside the agenda's scroll area.
        TabletHomePanel {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically, content = summary)
        }
        warning?.invoke()
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TabletHomePanel(Modifier.weight(0.67f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), content = agendaHeader)
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = agendaListState,
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        content = agendaItems)
                    if (showFinishDay) {
                        PlanHairline()
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                            SafarButton(onFinishDay, Modifier.widthIn(min = 190.dp, max = 260.dp).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PlannerFlatColors.PrimaryAccent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary)) {
                                Text(stringResource(R.string.planner_done_for_day), fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
            TabletHomePanel(Modifier.weight(0.33f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().verticalScroll(checklistScrollState).padding(vertical = 16.dp)) {
                    dailyTodo()
                }
            }
        }
    }
}

@Composable
private fun TabletHomePanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft), content = content)
}
