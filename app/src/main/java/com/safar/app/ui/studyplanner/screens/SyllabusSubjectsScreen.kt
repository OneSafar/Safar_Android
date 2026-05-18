package com.safar.app.ui.studyplanner.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.isLightBackground
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.studyplanner.StudyPlannerViewModel
import com.safar.app.ui.studyplanner.StudyPlannerUiState
import com.safar.app.ui.components.SafarErrorState
import com.safar.app.ui.components.SafarResultSlot
import com.safar.app.ui.components.SyllabusRowSkeleton
import com.safar.app.ui.studyplanner.SubjectUiModel
import com.safar.app.ui.studyplanner.logic.parseBulkSubjectsFromTxt
import com.safar.app.ui.studyplanner.logic.countBulkSubjectsTopics
import com.safar.app.ui.studyplanner.logic.countBulkSubjectsChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusSubjectsScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val accentBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    var addSubject by remember { mutableStateOf(false) }
    var renameSubject by remember { mutableStateOf<SubjectUiModel?>(null) }
    var deleteSubject by remember { mutableStateOf<SubjectUiModel?>(null) }
    var addChapterFor by remember { mutableStateOf<SubjectUiModel?>(null) }

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }

    if (addSubject) TextInputDialog("Add Subject", "Subject name", onDismiss = { addSubject = false }) { actions.addSubject(it); addSubject = false }
    renameSubject?.let { subject -> TextInputDialog("Rename Subject", subject.name, onDismiss = { renameSubject = null }) { actions.renameSubject(subject.id, it); renameSubject = null } }
    deleteSubject?.let { subject -> ConfirmActionDialog("Delete subject?", "This will delete ${subject.name}.", { deleteSubject = null }) { actions.deleteSubject(subject.id); deleteSubject = null } }
    addChapterFor?.let { subject -> TextInputDialog("Add Chapter", "Chapter name", onDismiss = { addChapterFor = null }) { actions.addChapter(subject.id, it); addChapterFor = null } }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8F9F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
                IconButton(onClick = { addSubject = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            if (state.error != null && subjects.isEmpty() && !state.loading) {
                SafarResultSlot(modifier = Modifier.fillMaxSize()) {
                    SafarErrorState(message = state.error!!, onRetry = { actions.refreshPlans() })
                }
            } else if (state.loading && subjects.isEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(6) {
                        SyllabusRowSkeleton()
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Import Syllabus Card (Inline)
                    item(span = { GridItemSpan(2) }) {
                        ImportSyllabusCard(
                            state = state,
                            actions = actions,
                            isDark = isDark,
                            accentBlue = accentBlue
                        )
                    }

                    // Section 2: Header showing Subjects & details
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "Subjects",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${subjects.size} subjects • ${subjects.sumOf { it.topicCount }} topics",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Section 3: Subject cards or manual add helper
                    if (subjects.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No subjects yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Start building your syllabus or paste one above.",
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { addSubject = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                                    ) {
                                        Text("Add Subject Manually")
                                    }
                                }
                            }
                        }
                    } else {
                        items(subjects, key = { it.id }) { subject ->
                            val onSubjectClick = remember(subject.id, planId, onNavigate) {
                                {
                                    viewModel.selectSubject(subject.id)
                                    onNavigate(
                                        Routes.ROUTE_SYLLABUS_CHAPTERS
                                            .replace("{planId}", planId)
                                            .replace("{subjectId}", subject.id)
                                    )
                                }
                            }
                            val onRenameSubject = remember(subject) { { renameSubject = subject } }
                            val onDeleteSubject = remember(subject) { { deleteSubject = subject } }
                            val onAddChapter = remember(subject) { { addChapterFor = subject } }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onSubjectClick),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .heightIn(min = 160.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = subject.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SubjectOverflowMenu(
                                            onRename = onRenameSubject,
                                            onDelete = onDeleteSubject,
                                            onAddChapter = onAddChapter,
                                            isDark = isDark
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val progressPercent = subject.completionPercentage / 100f
                                        LinearProgressIndicator(
                                            progress = { progressPercent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(99.dp)),
                                            color = accentBlue,
                                            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        )
                                        Column {
                                            Text(
                                                text = "${subject.completionPercentage}% complete •",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "${subject.topicCount} topics",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSyllabusCard(
    state: StudyPlannerUiState,
    actions: PlannerActions,
    isDark: Boolean,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
    )
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val part = withContext(Dispatchers.IO) { buildSyllabusUploadPart(context, uri) }
            if (part == null) {
                actions.setError("Could not read the selected file.")
                return@launch
            }
            actions.importSyllabusFile(part.first, part.second)
        }
    }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        text = state.syllabusImportDraft.trim()
        actions.clearSyllabusImportDraft()
    }
    val parsed = remember(text) { parseBulkSubjectsFromTxt(text) }
    val groups = parsed.getOrNull()
    val topicCount = groups?.let { countBulkSubjectsTopics(it) } ?: 0
    val chapterCount = groups?.let { countBulkSubjectsChapters(it) } ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Import Syllabus",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
            )

            // Import File Button
            OutlinedButton(
                onClick = { filePicker.launch(mimeTypes) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, accentBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentBlue
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload Icon",
                    modifier = Modifier.size(18.dp),
                    tint = accentBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Import File",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = accentBlue
                )
            }

            if (state.syllabusImportFileName != null) {
                Text(
                    text = "Imported: ${state.syllabusImportFileName}",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                    fontWeight = FontWeight.Medium
                )
            }

            // Text Area Input
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Paste the Syllabus",
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Use - for subject, _ for chapter, and > for topic.",
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8F9F7),
                    unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8F9F7),
                    focusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    focusedTextColor = if (isDark) Color.White else Color(0xFF1E293B),
                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF1E293B)
                )
            )

            // Dynamic Stats/Format label
            if (text.isNotBlank()) {
                if (parsed.isSuccess && groups != null) {
                    Text(
                        text = "$topicCount topics / $chapterCount chapters / ${groups.size} subjects",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = parsed.exceptionOrNull()?.message ?: "Invalid format",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Submit Button
            val isEnabled = chapterCount > 0 && parsed.isSuccess
            Button(
                onClick = {
                    actions.importFullSyllabusFromTxt(text)
                    text = ""
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                )
            ) {
                Text(
                    text = "Add to Plan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SubjectOverflowMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddChapter: () -> Unit,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Add Chapter") }, onClick = { expanded = false; onAddChapter() })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { expanded = false; onDelete() })
        }
    }
}

private fun buildSyllabusUploadPart(context: Context, uri: Uri): Pair<MultipartBody.Part, String>? {
    val resolver = context.contentResolver
    val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
    val name = rawName?.takeIf { it.isNotBlank() } ?: "syllabus.pdf"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    val resolverMime = resolver.getType(uri)?.lowercase(Locale.US)
    val extensionMime = when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt" -> "text/plain"
        else -> null
    }
    val mimeType = when {
        !resolverMime.isNullOrBlank() && resolverMime != "application/octet-stream" -> resolverMime
        extensionMime != null -> extensionMime
        else -> "application/octet-stream"
    }

    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    val part = MultipartBody.Part.createFormData("file", name, body)
    return part to name
}

