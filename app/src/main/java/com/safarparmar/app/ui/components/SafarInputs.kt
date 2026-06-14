package com.safarparmar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarCustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
    placeholder: String? = null,
) {
    val hasError = !errorText.isNullOrBlank()
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background == Color(0xFF0F1115)

    val containerBg = if (enabled) {
        scheme.surfaceVariant
    } else {
        scheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val borderColor = if (hasError) {
        scheme.error
    } else {
        scheme.outlineVariant
    }

    val labelColor = if (hasError) {
        scheme.error
    } else {
        scheme.onSurfaceVariant
    }

    val textColor = if (enabled) {
        scheme.onSurface
    } else {
        scheme.onSurface.copy(alpha = 0.6f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(containerBg)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.05.sp
                        ),
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(scheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                if (!enabled && label.contains("EMAIL")) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Read-only",
                        tint = labelColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (hasError) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText!!,
                color = scheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (helperText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = scheme.outline,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarCustomDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background == Color(0xFF0F1115)

    val containerBg = scheme.surfaceVariant
    val borderColor = scheme.outlineVariant
    val labelColor = scheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerBg)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.05.sp
                            ),
                            color = labelColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedOption,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = scheme.onSurface,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = labelColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(scheme.surface),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
