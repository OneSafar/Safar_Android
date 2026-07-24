package com.safarparmar.app.ui.nishtha.journal

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.JournalEntry
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.IstDateUtils
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
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        JournalScreenContent(
            viewModel = viewModel,
            openSheetOnLoad = openSheetOnLoad,
            isLight = isLight,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun JournalScreenContent(
    viewModel: NishthaViewModel,
    openSheetOnLoad: Boolean,
    isLight: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWriteSheet by remember { mutableStateOf(openSheetOnLoad) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var promptContext by remember { mutableStateOf<String?>(null) }
    var showJournals by remember { mutableStateOf(false) }
    var selectedJournal by remember { mutableStateOf<JournalEntry?>(null) }
    val pagerState = rememberPagerState { journalPrompts.size }
    val accent = PlannerFlatColors.PrimaryAccent
    val journalAccent = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlannerFlatColors.BgCream),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Flat hairline header ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlanEyebrow("Nishtha", modifier = Modifier.weight(1f))
                    if (uiState.journals.isNotEmpty()) {
                        FlatOutlineChip(
                            label = if (showJournals) "Hide" else "View All",
                            onClick = { showJournals = !showJournals },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Journal",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${uiState.journals.size} entries",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted,
                )
                Spacer(Modifier.height(16.dp))
                PlanHairline()
            }

            // ── Today's Prompt — macOS glass pager tiles ────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 8.dp),
            ) {
                Text(
                    text = "Today's Prompt",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark,
                )
                Spacer(Modifier.height(12.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 10.dp,
                ) { page ->
                    PromptGlassCard(
                        prompt = journalPrompts[page],
                        page = page,
                        pageCount = journalPrompts.size,
                        isLight = isLight,
                        accent = journalAccent,
                        onAnswer = {
                            val prompt = journalPrompts[page]
                            promptContext = prompt
                            titleInput = prompt
                            showWriteSheet = true
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            PlanHairline(modifier = Modifier.padding(horizontal = 20.dp), alpha = 0.6f)

            when {
                uiState.isLoadingJournals && uiState.journals.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(4) { GoalRowSkeleton() }
                    }
                }
                showJournals && uiState.journals.isNotEmpty() -> {
                    SafarPullRefreshBox(
                        isRefreshing = uiState.isLoadingJournals,
                        onRefresh = { viewModel.onEvent(NishthaEvent.LoadJournals) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.journals, key = { it.id }) { entry ->
                                JournalGlassCard(
                                    entry = entry,
                                    isLight = isLight,
                                    accent = journalAccent,
                                    onClick = { selectedJournal = entry },
                                )
                            }
                        }
                    }
                }
                !showJournals -> {
                    JournalEmptyState(
                        hasEntries = uiState.journals.isNotEmpty(),
                        accent = journalAccent,
                    )
                }
            }
        }

        // Flat accent FAB (not Material tonal FAB)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .shadow(
                    elevation = if (isLight) 6.dp else 12.dp,
                    shape = CircleShape,
                    spotColor = accent.copy(alpha = 0.35f),
                    ambientColor = accent.copy(alpha = 0.2f),
                )
                .clip(CircleShape)
                .background(accent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        titleInput = ""
                        promptContext = null
                        showWriteSheet = true
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New entry",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        if (showWriteSheet) {
            JournalWriteSheet(
                titleInput = titleInput,
                onTitleChange = { titleInput = it },
                bodyInput = bodyInput,
                onBodyChange = { bodyInput = it },
                promptContext = promptContext,
                isSaving = uiState.isSavingJournal,
                error = uiState.journalError,
                accent = accent,
                onDismiss = { showWriteSheet = false },
                onSave = {
                    val html = buildString {
                        if (titleInput.isNotBlank()) {
                            append("<h2>${android.text.TextUtils.htmlEncode(titleInput.trim())}</h2>")
                        }
                        append("<p>${android.text.TextUtils.htmlEncode(bodyInput.trim())}</p>")
                    }
                    viewModel.onEvent(NishthaEvent.SaveJournal(html, titleInput.ifBlank { null }, null))
                },
            )
        }

        if (selectedJournal != null) {
            JournalDetailSheet(
                entry = selectedJournal!!,
                accent = accent,
                onDismiss = { selectedJournal = null },
            )
        }
    }
}

// ─── Flat hairline chips / empty state ───────────────────────────────────────

@Composable
private fun FlatOutlineChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PlannerFlatColors.TextMuted,
        )
    }
}

@Composable
private fun JournalEmptyState(hasEntries: Boolean, accent: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_book_open_user),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = accent,
                )
            }
            Text(
                text = if (hasEntries) "Tap + to write" else "No entries yet",
                fontFamily = LoraFontFamily,
                fontSize = 18.sp,
                color = PlannerFlatColors.TextDark,
            )
            Text(
                text = "Swipe the prompt above or tap +",
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── macOS Control Center tiles ──────────────────────────────────────────────

@Composable
private fun PromptGlassCard(
    prompt: String,
    page: Int,
    pageCount: Int,
    isLight: Boolean,
    accent: Color,
    onAnswer: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val bodyColor = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E)
    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    }
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(bodyColor, shape)
            .border(0.5.dp, borderBrush, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = prompt,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = accent,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(pageCount) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 16.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) accent
                                else PlannerFlatColors.BorderSoft,
                            ),
                    )
                }
            }
            // Flat accent action inside the glass card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAnswer,
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    "Answer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun JournalGlassCard(
    entry: JournalEntry,
    isLight: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val dateStr = remember(entry.timestamp) { formatJournalDate(entry.timestamp) }
    val title = remember(entry.content) { journalTitle(entry.content) }
    val preview = remember(entry.content) {
        journalPlainText(entry.content.replace(Regex("<h[23]>.*?</h[23]>"), ""))
            .replace(Regex("\\s+"), " ")
            .take(120)
    }

    val shape = RoundedCornerShape(20.dp)
    val bodyColor = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E)
    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    }
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    val titleColor = if (isLight) Color.Black else Color.White
    val subtitleColor = if (isLight) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(bodyColor, shape)
            .border(0.5.dp, borderBrush, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_book_open_user),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title ?: "Untitled entry",
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateStr,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    color = subtitleColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 17.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Flat hairline sheets ────────────────────────────────────────────────────

/**
 * In-tab bottom sheet so Nishtha's floating bottom nav stays visible.
 * [ModalBottomSheet] renders in its own window and covers the tab bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalInlineSheetScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(PlannerFlatColors.BgCream)
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle(color = PlannerFlatColors.BorderSoft)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun JournalWriteSheet(
    titleInput: String,
    onTitleChange: (String) -> Unit,
    bodyInput: String,
    onBodyChange: (String) -> Unit,
    promptContext: String?,
    isSaving: Boolean,
    error: String?,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    JournalInlineSheetScaffold(onDismiss = onDismiss) {
        PlanEyebrow("Journal")
        Spacer(Modifier.height(10.dp))
        Text(
            text = "New Entry",
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            color = PlannerFlatColors.TextDark,
        )
        Spacer(Modifier.height(14.dp))
        PlanHairline()
        Spacer(Modifier.height(16.dp))

        if (promptContext != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accent,
                )
                Text(
                    promptContext,
                    fontSize = 13.sp,
                    color = accent,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            PlanHairline(alpha = 0.5f)
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "Title",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
        )
        Spacer(Modifier.height(8.dp))
        FlatJournalField(
            value = titleInput,
            onValueChange = onTitleChange,
            placeholder = "Give your entry a title...",
            accent = accent,
            singleLine = true,
        )

        Spacer(Modifier.height(18.dp))
        PlanHairline(alpha = 0.5f)
        Spacer(Modifier.height(16.dp))

        Text(
            "What's on your mind?",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
        )
        Spacer(Modifier.height(8.dp))
        FlatJournalField(
            value = bodyInput,
            onValueChange = onBodyChange,
            placeholder = "Start writing...",
            accent = accent,
            singleLine = false,
            minHeight = 160.dp,
            minLines = 5,
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(22.dp))
        PlanHairline()
        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlatSecondaryButton(
                label = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            FlatPrimaryButton(
                label = "Save Entry",
                enabled = bodyInput.isNotBlank() && !isSaving,
                loading = isSaving,
                accent = accent,
                onClick = onSave,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun JournalDetailSheet(
    entry: JournalEntry,
    accent: Color,
    onDismiss: () -> Unit,
) {
    val dateStr = remember(entry.timestamp) { formatJournalDate(entry.timestamp) }
    val title = remember(entry.content) { journalTitle(entry.content) }
    val body = remember(entry.content) {
        journalPlainText(entry.content.replace(Regex("<h[23]>.*?</h[23]>"), ""))
    }

    JournalInlineSheetScaffold(onDismiss = onDismiss) {
        PlanEyebrow("Entry")
        Spacer(Modifier.height(10.dp))
        Text(
            text = dateStr,
            fontSize = 13.sp,
            color = accent,
            fontWeight = FontWeight.Medium,
        )
        if (title != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
        }
        Spacer(Modifier.height(14.dp))
        PlanHairline()
        Spacer(Modifier.height(16.dp))
        Text(
            text = body,
            fontSize = 15.sp,
            color = PlannerFlatColors.TextDark,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(24.dp))
        PlanHairline()
        Spacer(Modifier.height(16.dp))
        FlatPrimaryButton(
            label = "Close",
            enabled = true,
            loading = false,
            accent = accent,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FlatJournalField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, fontStyle = FontStyle.Italic, color = PlannerFlatColors.TextMuted)
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = PlannerFlatColors.BorderSoft,
            focusedTextColor = PlannerFlatColors.TextDark,
            unfocusedTextColor = PlannerFlatColors.TextDark,
            cursorColor = accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun FlatPrimaryButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) accent else PlannerFlatColors.BorderSoft.copy(alpha = 0.55f),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FlatSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextMuted,
        )
    }
}

private fun journalTitle(content: String): String? =
    Regex("<h[23]>(.*?)</h[23]>", RegexOption.DOT_MATCHES_ALL)
        .find(content)
        ?.groupValues
        ?.get(1)
        ?.let(::journalPlainText)
        ?.takeIf(String::isNotBlank)

private fun journalPlainText(html: String): String =
    androidx.core.text.HtmlCompat.fromHtml(
        html,
        androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY,
    ).toString().trim()

private fun formatJournalDate(ts: String): String = runCatching {
    val zdt = ZonedDateTime.parse(ts).withZoneSameInstant(IstDateUtils.zone)
    zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy · h:mm a", Locale.getDefault()))
}.getOrDefault(ts.take(10))
