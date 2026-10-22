with open('app/src/main/java/com/safarparmar/app/ui/nishtha/checkin/CheckInScreen.kt', 'r') as f:
    code = f.read()

# 1. Add imports if needed
if "import androidx.compose.material3.Surface" not in code:
    code = code.replace(
        "import androidx.compose.material3.MaterialTheme",
        "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Surface\nimport androidx.compose.foundation.BorderStroke"
    )

# 2. Replace HistoryMoodRow
row_start = "@OptIn(ExperimentalLayoutApi::class)\n@Composable\nprivate fun HistoryMoodRow(mood: Mood) {"
row_end = "private fun moodEmoji(mood: String): String"

new_history = """private data class ParsedMoodDetails(
    val userNote: String?,
    val reasons: List<String>,
    val causedByText: String?,
)

private fun parseMoodNotes(rawNotes: String?): ParsedMoodDetails {
    if (rawNotes.isNullOrBlank()) return ParsedMoodDetails(null, emptyList(), null)

    var text = rawNotes.trim()
    var extractedDueTo: String? = null
    var extractedCausedBy: String? = null
    var extractedTags: List<String> = emptyList()

    // 1. Check for "Tags: ..." line
    val tagsMatch = Regex("(?i)Tags:\\s*([^\\n]+)").find(text)
    if (tagsMatch != null) {
        extractedTags = tagsMatch.groupValues[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
        text = text.replace(tagsMatch.value, "").trim()
    }

    // 2. Check for "Caused by: ..." line
    val causedMatch = Regex("(?i)Caused by:\\s*([^\\n]+)").find(text)
    if (causedMatch != null) {
        extractedCausedBy = causedMatch.groupValues[1].trim()
        text = text.replace(causedMatch.value, "").trim()
    }

    // 3. Check for "Due to: ..." or "Due to" (case-insensitive)
    val dueToMatch = Regex("(?i)Due to:\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(text)
    if (dueToMatch != null) {
        extractedDueTo = dueToMatch.groupValues[1].trim()
        text = text.substring(0, dueToMatch.range.first).trim()
    }

    // Parse all reasons into a combined list of tag strings
    val allReasons = mutableListOf<String>()
    if (!extractedDueTo.isNullOrBlank()) {
        allReasons.addAll(extractedDueTo.split(",").map { it.trim() }.filter { it.isNotEmpty() })
    }
    allReasons.addAll(extractedTags)

    val cleanNote = text.replace(Regex("\\n+"), " ").trim().ifBlank { null }
    val cleanCausedBy = extractedCausedBy?.ifBlank { null }

    return ParsedMoodDetails(
        userNote = cleanNote,
        reasons = allReasons.distinct(),
        causedByText = cleanCausedBy
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryMoodRow(mood: Mood) {
    val details = remember(mood.notes) { parseMoodNotes(mood.notes) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    
    // Distinct vibrant Indigo/Purple color scheme for Reasons
    val reasonBadgeBg = if (!isLight) Color(0xFF311E52) else Color(0xFFF3E8FF)
    val reasonTextTint = if (!isLight) Color(0xFFD8B4FE) else Color(0xFF7C3AED)
    val reasonLabelColor = if (!isLight) Color(0xFFC084FC) else Color(0xFF6B21A8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Top Row: Emoji + Mood Name & Intensity + Timestamp ──────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PlannerFlatColors.BorderSoft.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(moodEmoji(mood.mood), fontSize = 22.sp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mood.mood.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PlannerFlatColors.TextDark,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PlannerFlatColors.BorderSoft.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = stringResource(R.string.checkin_intensity_value, mood.intensity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = mood.timestamp.take(10),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PlannerFlatColors.TextMuted,
            )
        }

        // ── User's Custom Note (if any) ─────────────────────────────────────
        if (!details.userNote.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PlannerFlatColors.BorderSoft.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp),
            ) {
                Text(
                    text = details.userNote,
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextDark,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // ── Caused By / Explanation (if any) ─────────────────────────────────
        if (!details.causedByText.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(start = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = reasonLabelColor,
                )
                Text(
                    text = details.causedByText,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── "Due to" Reasons / Tags (Highlighted in distinct reason color!) ───
        if (details.reasons.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Due to:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = reasonLabelColor,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details.reasons.forEach { reason ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = reasonBadgeBg,
                            border = BorderStroke(0.8.dp, reasonTextTint.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = reason,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = reasonTextTint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

"""

s_idx = code.find(row_start)
e_idx = code.find(row_end, s_idx)
if s_idx != -1 and e_idx != -1:
    code = code[:s_idx] + new_history + code[e_idx:]

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/checkin/CheckInScreen.kt', 'w') as f:
    f.write(code)

