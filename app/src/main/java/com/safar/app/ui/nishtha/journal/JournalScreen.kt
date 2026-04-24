package com.safar.app.ui.nishtha.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.domain.model.JournalEntry
import com.safar.app.ui.nishtha.NishthaEvent
import com.safar.app.ui.nishtha.NishthaViewModel
import com.safar.app.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val journalPrompts = listOf(
    "What is one thing you are grateful for right now?",
    "What is one thing in your life you would never want to change?",
    "What felt heavy today, and what still felt right?",
    "Which part of you needs a little more kindness right now?",
    "What is one tiny thing you are proud of today?",
    "What did you avoid today, and why?",
    "What made today even slightly better than yesterday?",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(viewModel: NishthaViewModel = hiltViewModel(), openSheetOnLoad: Boolean = false) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showWriteSheet by remember { mutableStateOf(openSheetOnLoad) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var promptContext by remember { mutableStateOf<String?>(null) }
    var showJournals by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState { journalPrompts.size }

    LaunchedEffect(uiState.journalSaveSuccess) {
        if (uiState.journalSaveSuccess) {
            Toast.makeText(context, "Journal saved!", Toast.LENGTH_SHORT).show()
            showWriteSheet = false
            titleInput = ""
            bodyInput = ""
            promptContext = null
            viewModel.onEvent(NishthaEvent.ClearJournalSuccess)
        }
    }

    LaunchedEffect(uiState.journalError) {
        if (uiState.journalError != null) {
            Toast.makeText(context, uiState.journalError, Toast.LENGTH_SHORT).show()
        }
    }

    if (showWriteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWriteSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("New Entry", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                // Show prompt as read-only context if answering a prompt
                if (promptContext != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Violet500.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✨", fontSize = 14.sp)
                            Text(promptContext!!, fontSize = 13.sp, color = Violet600, fontStyle = FontStyle.Italic, lineHeight = 19.sp)
                        }
                    }
                }
                OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Title") }, placeholder = { Text("Give your entry a title...", fontStyle = FontStyle.Italic) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = bodyInput, onValueChange = { bodyInput = it }, label = { Text("What's on your mind?") }, placeholder = { Text("Start writing...", fontStyle = FontStyle.Italic) }, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), minLines = 5, shape = RoundedCornerShape(12.dp))
                if (uiState.journalError != null) Text(uiState.journalError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showWriteSheet = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val html = buildString {
                                if (titleInput.isNotBlank()) append("<h2>${titleInput.trim()}</h2>")
                                append("<p>${bodyInput.trim()}</p>")
                            }
                            viewModel.onEvent(NishthaEvent.SaveJournal(html, titleInput.ifBlank { null }, null))
                        },
                        enabled = bodyInput.isNotBlank() && !uiState.isSavingJournal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    ) {
                        if (uiState.isSavingJournal) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text("Save Entry")
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Journal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("${uiState.journals.size} entries", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (uiState.journals.isNotEmpty()) {
                    TextButton(onClick = { showJournals = !showJournals }) {
                        Text(if (showJournals) "Hide" else "View All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }

            // Swipeable prompts slider — always shown below the header
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                Text("Today's Prompt", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                journalPrompts[page],
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Violet600,
                                fontStyle = FontStyle.Italic,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(journalPrompts.size) { i ->
                                        Box(
                                            modifier = Modifier.size(if (i == page) 16.dp else 6.dp, 6.dp)
                                                .clip(CircleShape)
                                                .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f))
                                        )
                                    }
                                }
                                Button(
                                    onClick = { val prompt = journalPrompts[page]; promptContext = prompt; titleInput = prompt; showWriteSheet = true },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                                ) {
                                    Text("Answer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (showJournals && uiState.journals.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.journals, key = { it.id }) { entry -> JournalCard(entry) }
                }
            } else if (!showJournals) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📖", fontSize = 48.sp)
                        Text(if (uiState.journals.isEmpty()) "No entries yet" else "Tap + to write", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Swipe the prompt above or tap +", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { titleInput = ""; promptContext = null; showWriteSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, contentDescription = "New entry", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun JournalCard(entry: JournalEntry) {
    val dateStr = remember(entry.timestamp) { formatJournalDate(entry.timestamp) }
    val title = remember(entry.content) { Regex("<h[23]>(.*?)</h[23]>").find(entry.content)?.groupValues?.get(1)?.trim() }
    // Strip h2/h3 tags first, then strip all remaining tags → clean body text only
    val preview = remember(entry.content) {
        entry.content
            .replace(Regex("<h[23]>.*?</h[23]>"), "")
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
    }

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(dateStr, fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.Medium)
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(preview, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 19.sp)
        }
    }
}

private fun formatJournalDate(ts: String): String = runCatching {
    val zdt = ZonedDateTime.parse(ts)
    zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy · h:mm a", Locale.getDefault()))
}.getOrDefault(ts.take(10))
