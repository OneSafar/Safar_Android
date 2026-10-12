// Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4
// Hallmark · genre: modern-minimal · reference: Kavach Analytics · designed-as-app
package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.data.remote.api.SavedSyllabus
import com.safarparmar.app.ui.studyplanner.components.PlannerDialog
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogText
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogTextAction
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors

@Composable
fun SavedSyllabusPickerStep(
    syllabi: List<SavedSyllabus>,
    selectedSyllabusId: String?,
    isLoading: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    onBuildNew: () -> Unit,
    onEdit: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val rule = MaterialTheme.colorScheme.outlineVariant
    val filteredSyllabi = remember(syllabi, searchQuery) {
        if (searchQuery.isBlank()) syllabi
        else syllabi.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                Text(
                    text = stringResource(R.string.planner_reuse_or_resume),
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted,
                )
                Spacer(Modifier.height(18.dp))
                NewSyllabusRow(onClick = onBuildNew)
                Spacer(Modifier.height(24.dp))
            }

            when {
                isLoading -> item {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.55f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }

                error != null -> item {
                    Column(
                        modifier = Modifier.fillParentMaxHeight(0.55f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(stringResource(R.string.planner_saved_syllabi_load_error), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = muted)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(10.dp)) {
                            Text(stringResource(R.string.common_try_again), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                syllabi.isEmpty() -> item {
                    EmptySyllabiState(onCreate = onBuildNew)
                }

                else -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.planner_your_syllabi),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${syllabi.size}", style = MaterialTheme.typography.labelLarge, color = muted)
                        }
                        Spacer(Modifier.height(12.dp))

                        if (syllabi.size > 4 || searchQuery.isNotBlank()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text(stringResource(R.string.planner_search_syllabi)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_clear_search), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = rule),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    items(filteredSyllabi, key = { it.id }) { syllabus ->
                        val selected = !syllabus.isDraft && syllabus.id == selectedSyllabusId
                        SavedSyllabusRow(
                            syllabus = syllabus,
                            selected = selected,
                            onSelect = { if (syllabus.isDraft) onEdit(syllabus.id) else onSelect(syllabus.id) },
                            onEdit = { onEdit(syllabus.id) },
                            onDelete = { pendingDeleteId = syllabus.id },
                        )
                        HorizontalDivider(color = rule)
                    }

                    if (filteredSyllabi.isEmpty()) {
                        item {
                            Text(
                                "No syllabi match “$searchQuery”.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                                modifier = Modifier.padding(vertical = 28.dp),
                            )
                        }
                    }
                }
            }
        }

        if (syllabi.isNotEmpty() && error == null) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Button(
                    onClick = onContinue,
                    enabled = selectedSyllabusId != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlannerFlatColors.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(50.dp),
                ) {
                    Text(stringResource(R.string.planner_use_syllabus), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    pendingDeleteId?.let { syllabusId ->
        val syllabusName = syllabi.firstOrNull { it.id == syllabusId }?.name ?: stringResource(R.string.planner_this_syllabus)
        PlannerDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = stringResource(R.string.planner_delete_syllabus_question),
            text = { PlannerDialogText(stringResource(R.string.planner_delete_syllabus_body, syllabusName)) },
            dismissButton = { PlannerDialogTextAction(stringResource(R.string.common_cancel), onClick = { pendingDeleteId = null }) },
            confirmButton = {
                PlannerDialogAction(
                    text = stringResource(R.string.common_delete),
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        pendingDeleteId = null
                        onDelete(syllabusId)
                    },
                )
            },
        )
    }
}

@Composable
private fun NewSyllabusRow(onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.planner_create_custom_syllabus), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.planner_create_custom_syllabus_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EmptySyllabiState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.planner_no_custom_syllabi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.planner_no_custom_syllabi_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onCreate, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.planner_create_syllabus), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SavedSyllabusRow(
    syllabus: SavedSyllabus,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.primary
    val draftColor = MaterialTheme.colorScheme.tertiary
    val untitledSyllabus = stringResource(R.string.planner_untitled_syllabus)
    Surface(
        onClick = onSelect,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (syllabus.isDraft) {
                Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(8.dp).background(draftColor, CircleShape))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(if (selected) selectionColor else Color.Transparent, CircleShape)
                        .border(1.5.dp, if (selected) selectionColor else MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        syllabus.name.ifBlank { untitledSyllabus },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (syllabus.isDraft) {
                        Text(stringResource(R.string.common_draft), style = MaterialTheme.typography.labelMedium, color = draftColor, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
                Text(
                    "${syllabus.subjectCount} subjects  ·  ${syllabus.chapterCount} chapters  ·  ${syllabus.topicCount} topics",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.planner_edit_named, syllabus.name), tint = muted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.planner_delete_named, syllabus.name),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
