package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.SolidColor
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors

/**
 * A chapter's topics get their own dedicated sheet instead of inlining into the subject
 * accordion — a chapter with 50 topics now scrolls in one obvious, unambiguous container
 * (this sheet's own LazyColumn) rather than fighting with the outer subject list's scroll.
 *
 * "Add Topic" is answered with an inline text row inside this same sheet rather than a
 * separate `AlertDialog` — two independent floating Compose surfaces (a dialog window and
 * this sheet's own window) race for top z-order when opened together, and the dialog kept
 * losing that race and rendering underneath the sheet. Keeping the input in this sheet's
 * own composition sidesteps that entirely: there's only one window to stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterTopicsSheet(
    chapterName: String,
    topics: List<StudyTopic>,
    chapter: com.safarparmar.app.domain.model.studyplanner.StudyChapter? = null,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onAddTopic: (String) -> Unit,
    onTopicClick: (StudyTopic) -> Unit,
    onRenameTopic: (StudyTopic) -> Unit,
    onDeleteTopic: (StudyTopic) -> Unit,
    onAssignToday: (StudyTopic) -> Unit,
    canReorder: Boolean,
    onMoveTopicUp: (StudyTopic) -> Unit,
    onMoveTopicDown: (StudyTopic) -> Unit,
    onTopicDragEnd: (StudyTopic) -> Unit,
    onChangeDate: (StudyTopic) -> Unit = {},
    onMarkDoneTopic: (StudyTopic) -> Unit = {},
    onToReviseTopic: (StudyTopic) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var showAddRow by remember { mutableStateOf(false) }
    var newTopicText by remember { mutableStateOf("") }
    // Capped to 60% of the screen (bottom half plus a 10% margin) instead of letting
    // a long topic list stretch the sheet all the way to the top — the topic list
    // scrolls within that cap instead.
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SafarSemanticColors.plannerBackground(),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).imePadding().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = chapterName,
                    fontFamily = LoraFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${topics.size} topics",
                    fontSize = 11.5.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }

            if (showAddRow) {
                fun submitNewTopic() {
                    if (newTopicText.isBlank()) return
                    onAddTopic(newTopicText)
                    newTopicText = ""
                    showAddRow = false
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlanHairline()
                    BasicTextField(
                        value = newTopicText,
                        onValueChange = { newTopicText = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.5.sp,
                            color = PlannerFlatColors.TextDark,
                        ),
                        cursorBrush = SolidColor(PlannerFlatColors.PrimaryAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitNewTopic() }),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        decorationBox = { inner ->
                            if (newTopicText.isEmpty()) {
                                Text(
                                    text = "Topic name — commas add several at once",
                                    fontSize = 13.5.sp,
                                    color = PlannerFlatColors.TextMuted,
                                )
                            }
                            inner()
                        },
                    )
                    PlanHairline()
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Add",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.PrimaryAccent,
                            modifier = Modifier.clickable { submitNewTopic() },
                        )
                        Text(
                            text = "Cancel",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.TextMuted,
                            modifier = Modifier.clickable { showAddRow = false; newTopicText = "" },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Topics",
                        fontFamily = LoraFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = PlannerFlatColors.TextDark,
                        modifier = Modifier.weight(1f),
                    )
                    SyllabusGlassAddButton(
                        onClick = { showAddRow = true },
                        contentDescription = "Add topic",
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (topics.isEmpty()) {
                Text(
                    text = "No topics yet. Add one above — commas add several at once.",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    itemsIndexed(topics, key = { _, topic -> topic.id }) { _, topic ->
                        key(topic.id) {
                            PlanHairline(alpha = 0.6f)
                            SyllabusMagazineTopicRow(
                                topic = topic,
                                chapter = chapter,
                                onRename = { onRenameTopic(topic) },
                                onDelete = { onDeleteTopic(topic) },
                                onAssignToday = { onAssignToday(topic) },
                                onChangeDate = { onChangeDate(topic) },
                                onMarkDone = { onMarkDoneTopic(topic) },
                                onToRevise = { onToReviseTopic(topic) },
                                canReorder = canReorder,
                                onMoveUp = { onMoveTopicUp(topic) },
                                onMoveDown = { onMoveTopicDown(topic) },
                                onDragEnd = { onTopicDragEnd(topic) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}
