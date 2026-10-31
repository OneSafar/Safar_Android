package com.safarparmar.app.ui.nishtha.journal

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.ui.platform.LocalResources

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
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
import androidx.compose.ui.text.TextStyle
import com.safarparmar.app.ui.nishtha.NishthaWarmTheme
import com.safarparmar.app.ui.nishtha.nishthaWarmColors
import com.safarparmar.app.ui.nishtha.WarmNishthaCard
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.res.vectorResource
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.DropdownMenuItem
import com.safarparmar.app.ui.components.popups.SafarComposablesDropdownMenu
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.ui.res.stringResource
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

internal val JournalMockupFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

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
fun JournalScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.loadTab(com.safarparmar.app.ui.nishtha.NishthaTab.JOURNAL) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        NishthaWarmTheme(isLight) {
            JournalScreenContent(viewModel = viewModel, isLight = isLight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun JournalScreenContent(
    viewModel: NishthaViewModel,
    isLight: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val localizedResources by rememberUpdatedState(LocalResources.current)
    var showWriteSheet by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var promptContext by remember { mutableStateOf<String?>(null) }
    var showJournals by remember { mutableStateOf(false) }
    var selectedJournal by remember { mutableStateOf<JournalEntry?>(null) }
    com.safarparmar.app.ui.tutorial.ScreenPracticeEntry(com.safarparmar.app.ui.tutorial.TitliFeature.JOURNAL,
        !isLight, eligible = !uiState.isLoadingJournals && !uiState.isSavingJournal && !showWriteSheet && selectedJournal == null)

    val accent = nishthaWarmColors(isLight).action

    LaunchedEffect(uiState.journalSaveSuccess) {
        if (uiState.journalSaveSuccess) {
            Toast.makeText(context, localizedResources.getString(com.safarparmar.app.R.string.nishtha_journal_saved), Toast.LENGTH_SHORT).show()
            showWriteSheet = false
            // Keep the last form snapshot through the closing animation.
            // Both entry triggers initialize the form on the next opening.
            viewModel.onEvent(NishthaEvent.ClearJournalSuccess)
        }
    }

    LaunchedEffect(uiState.journalError) {
        if (uiState.journalError != null) {
            Toast.makeText(context, uiState.journalError, Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(nishthaWarmColors(isLight).canvas), contentAlignment = Alignment.TopCenter) {
        WarmJournalBody(
            journals = uiState.journals, loading = uiState.isLoadingJournals,
            showAll = showJournals, onShowAll = { showJournals = !showJournals }, isLight = isLight,
            onRefresh = { viewModel.onEvent(NishthaEvent.LoadJournals) },
            onOpen = { selectedJournal = it },
            editorOpen = showWriteSheet,
            onNew = { if (!showWriteSheet) { titleInput = ""; bodyInput = ""; promptContext = null; showWriteSheet = true } },
            onPrompt = { prompt -> promptContext = prompt; titleInput = prompt; bodyInput = ""; showWriteSheet = true },
        )

        // The modal host must survive visible=false so DialogPanel can animate out.
        JournalWriteSheet(
            visible = showWriteSheet,
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
private fun JournalEmptyState(
    hasEntries: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.nishtha_prompt_swipe_hint),
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── macOS Control Center tiles ──────────────────────────────────────────────

@Composable
internal fun PromptGlassCard(
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
internal fun JournalGlassCard(
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
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
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
    visible: Boolean,
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
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val c = nishthaWarmColors(isLight)

    JournalEditorPopup(
        visible = visible,
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.nishtha_new_entry),
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            val maxDialogHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.55f).dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (promptContext != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = promptContext,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontStyle = FontStyle.Italic,
                            color = accent,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Title",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                        fontFamily = JournalMockupFont,
                    )
                    FlatJournalField(
                        value = titleInput,
                        onValueChange = onTitleChange,
                        placeholder = stringResource(R.string.nishtha_entry_title_hint),
                        accent = accent,
                        singleLine = true,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "What's on your mind?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                        fontFamily = JournalMockupFont,
                    )
                    FlatJournalField(
                        value = bodyInput,
                        onValueChange = onBodyChange,
                        placeholder = stringResource(R.string.nishtha_start_writing_hint),
                        accent = accent,
                        singleLine = false,
                        minHeight = 140.dp,
                        minLines = 4,
                    )
                }
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        positiveButton = {
            com.composables.ui.components.Button(
                onClick = onSave,
                style = com.composables.ui.components.ButtonStyle.Primary,
                enabled = bodyInput.isNotBlank() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                com.composables.ui.components.Text(
                    text = if (isSaving) "Saving…" else "Save Entry",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        negativeButton = {
            com.composables.ui.components.Button(
                onClick = onDismiss,
                enabled = !isSaving,
                style = com.composables.ui.components.ButtonStyle.Outlined,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                com.composables.ui.components.Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}

@Composable
private fun JournalDetailSheet(
    entry: JournalEntry,
    accent: Color,
    onDismiss: () -> Unit,
) {
    val c = nishthaWarmColors(MaterialTheme.colorScheme.background.isLightBackground())
    com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
        visible = true, onDismissRequest = onDismiss,
        title = { com.composables.ui.components.Text(journalTitle(entry.content) ?: stringResource(R.string.warm_untitled_entry), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.composables.ui.components.Text(formatJournalDate(entry.timestamp), color = c.muted)
                com.composables.ui.components.Text(journalPlainText(entry.content.replace(Regex("<h[23]>.*?</h[23]>"), "")), color = c.ink)
            }
        },
        positiveButton = {
            com.composables.ui.components.Button(onClick = onDismiss, style = com.composables.ui.components.ButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                com.composables.ui.components.Text(stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
internal fun FlatJournalField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    minLines: Int = 1,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val c = nishthaWarmColors(isLight)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = c.muted,
                fontFamily = JournalMockupFont,
                fontSize = 15.sp,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(
            fontFamily = JournalMockupFont,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            color = c.ink,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.paper,
            unfocusedContainerColor = c.paper,
            focusedBorderColor = accent,
            unfocusedBorderColor = c.line,
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            cursorColor = accent,
            focusedPlaceholderColor = c.muted,
            unfocusedPlaceholderColor = c.muted,
        ),
    )
}

@Composable
internal fun FlatPrimaryButton(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WarmJournalBody(
    journals: List<JournalEntry>, loading: Boolean, showAll: Boolean, onShowAll: () -> Unit,
    isLight: Boolean, onRefresh: () -> Unit, onOpen: (JournalEntry) -> Unit,
    editorOpen: Boolean, onNew: () -> Unit, onPrompt: (String) -> Unit,
) {
    val c = nishthaWarmColors(isLight)
    val pager = rememberPagerState { journalPrompts.size }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxHeight().widthIn(max = 1000.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
        SafarPullRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.nishtha_journal), color = c.ink, fontFamily = JournalMockupFont,
                            fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp)
                    }
                }
                item {
                    val promptSurface = if (isLight) Color(0xFFFFDED5) else c.peach
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(promptSurface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(58.dp).background(c.paper.copy(alpha = .55f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                com.composables.ui.components.Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_journal_quill),
                                    contentDescription = null, tint = c.ink, modifier = Modifier.size(34.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.nishtha_today_prompt).uppercase(java.util.Locale.getDefault()),
                                    color = if (isLight) Color(0xFFE25C29) else Color(0xFFFFB08A), fontFamily = JournalMockupFont,
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
                                    Text(journalPrompts[page], color = c.ink, fontFamily = JournalMockupFont,
                                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp,
                                        modifier = Modifier.padding(end = 4.dp))
                                }
                            }
                        }
                        com.composables.ui.components.Button(onClick = { onPrompt(journalPrompts[pager.currentPage]) },
                            style = com.composables.ui.components.ButtonStyle.Primary, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            com.composables.ui.components.Text(stringResource(R.string.warm_answer_prompt), fontFamily = JournalMockupFont, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            com.composables.ui.components.IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                                enabled = pager.currentPage > 0, style = com.composables.ui.components.ButtonStyle.Ghost,
                                modifier = Modifier.background(c.paper.copy(alpha = .65f), RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp)) {
                                com.composables.ui.components.Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.warm_previous_prompt), tint = c.ink)
                            }
                            Text(stringResource(R.string.warm_prompt_position, pager.currentPage + 1, journalPrompts.size),
                                Modifier.padding(horizontal = 24.dp), color = c.muted, fontFamily = JournalMockupFont, fontSize = 12.sp)
                            com.composables.ui.components.IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                                enabled = pager.currentPage < journalPrompts.lastIndex, style = com.composables.ui.components.ButtonStyle.Ghost,
                                modifier = Modifier.background(c.paper.copy(alpha = .65f), RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp)) {
                                com.composables.ui.components.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.warm_next_prompt), tint = c.ink)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.warm_your_entries), color = c.ink, fontFamily = JournalMockupFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)

                        }
                        if (journals.size > 2) com.composables.ui.components.Button(onClick = onShowAll,
                            style = com.composables.ui.components.ButtonStyle.Outlined, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            com.composables.ui.components.Text(stringResource(if (showAll) R.string.warm_show_less else R.string.warm_view_all), fontFamily = JournalMockupFont, color = c.ink, fontSize = 12.sp)
                            com.composables.ui.components.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (loading && journals.isEmpty()) item { GoalRowSkeleton() }
                else if (journals.isEmpty()) item {
                    WarmNishthaCard(c) {
                        Text(stringResource(R.string.journal_empty_title), color = c.ink, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.journal_empty_subtitle), color = c.muted)
                    }
                }
                items(if (showAll) journals else journals.take(2), key = { it.id }) { entry ->
                    WarmJournalEntry(entry, isLight, journals.indexOf(entry), onOpen)
                }
            }
        }
        JournalComposeFab(
            editorOpen = editorOpen,
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun WarmJournalEntry(entry: JournalEntry, light: Boolean, index: Int, onOpen: (JournalEntry) -> Unit) {
    val c = nishthaWarmColors(light)
    var menuOpen by remember(entry.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.paper)
        .border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(6.dp), verticalAlignment = Alignment.Top) {
        com.composables.ui.components.Button(onClick = { onOpen(entry) }, style = com.composables.ui.components.ButtonStyle.Ghost,
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(10.dp)) {
            com.composables.ui.components.Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_journal_entry),
                contentDescription = null, tint = c.ink,
                modifier = Modifier.size(44.dp).background(if (index % 2 == 0) c.peach else c.lilac, RoundedCornerShape(14.dp)).padding(10.dp))
            Column(Modifier.weight(1f).padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                com.composables.ui.components.Text(journalTitle(entry.content) ?: stringResource(R.string.warm_untitled_entry),
                    color = c.ink, fontFamily = JournalMockupFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                com.composables.ui.components.Text(warmEntryDate(entry.timestamp), color = c.muted, fontFamily = JournalMockupFont, fontSize = 11.sp)
                com.composables.ui.components.Text(journalPlainText(entry.content.replace(Regex("<h[23]>.*?</h[23]>"), "")),
                    color = c.muted, fontFamily = JournalMockupFont, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        }
        SafarComposablesDropdownMenu(expanded = menuOpen, onExpandedChange = { menuOpen = it }, panel = {
            DropdownMenuPanel(backgroundColor = c.paper, contentColor = c.ink) {
                DropdownMenuItem(onClick = { menuOpen = false; onOpen(entry) }) {
                    com.composables.ui.components.Text(stringResource(R.string.warm_open_entry), color = c.ink, fontFamily = JournalMockupFont)
                }
            }
        }) {
            com.composables.ui.components.IconButton(onClick = { menuOpen = !menuOpen }, style = com.composables.ui.components.ButtonStyle.Ghost) {
                com.composables.ui.components.Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.warm_entry_options), tint = c.muted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun warmEntryDate(timestamp: String): String {
    val date = runCatching { ZonedDateTime.parse(timestamp).withZoneSameInstant(IstDateUtils.zone).toLocalDate() }.getOrNull()
    val today = java.time.LocalDate.now(IstDateUtils.zone)
    return when (date) {
        today -> stringResource(R.string.nishtha_today)
        today.minusDays(1) -> stringResource(R.string.warm_yesterday)
        else -> formatJournalDate(timestamp)
    }
}
