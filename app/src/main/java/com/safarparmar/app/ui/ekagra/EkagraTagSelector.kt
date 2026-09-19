package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EkagraTagSelector(
    availableTags: List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    onAddTag: ((String) -> Unit)? = null,
    onDeleteTag: ((String) -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    val unselectedBg = if (isDark) Color(0xFF1E2129) else Color(0xFFF1F3F6)
    val unselectedBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val unselectedText = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF424750)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Add custom tag button
            if (onAddTag != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, unselectedBorder),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            newTagInput = ""
                            showAddDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.ekagra_add_subject),
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.ekagra_new_subject),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                        )
                    }
                }
            }

            // Tag chips
            availableTags.forEach { tag ->
                val isSelected = tag.equals(selectedTag, ignoreCase = true)
                val chipBg = if (isSelected) accentColor.copy(alpha = 0.18f) else unselectedBg
                val chipBorder = if (isSelected) accentColor else unselectedBorder
                val chipTextColor = if (isSelected) accentColor else unselectedText

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipBg)
                        .border(BorderStroke(1.dp, chipBorder), RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = {
                                if (isSelected) {
                                    onSelectTag(null)
                                } else {
                                    onSelectTag(tag)
                                }
                            },
                            onLongClick = {
                                if (onDeleteTag != null) {
                                    tagToDelete = tag
                                }
                            }
                        )
                        .padding(start = 10.dp, end = if (onDeleteTag != null) 4.dp else 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = chipTextColor,
                        )
                        if (onDeleteTag != null) {
                            IconButton(
                                onClick = { tagToDelete = tag },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.ekagra_delete_subject_named, tag),
                                    tint = chipTextColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Tag Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.ekagra_add_subject), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text(stringResource(R.string.ekagra_subject_name)) },
                    placeholder = { Text(stringResource(R.string.ekagra_subject_examples)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTagInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddTag?.invoke(trimmed)
                            onSelectTag(trimmed)
                        }
                        showAddDialog = false
                    },
                    enabled = newTagInput.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.common_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Delete Tag Confirmation Dialog
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text(stringResource(R.string.ekagra_remove_subject), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(stringResource(R.string.ekagra_remove_subject_body, tag), fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag?.invoke(tag)
                        if (selectedTag == tag) {
                            onSelectTag(null)
                        }
                        tagToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
