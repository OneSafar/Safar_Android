with open('app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchDashboard.kt', 'r') as f:
    code = f.read()

# 1. Add Spacer and sp imports if not present
if "import androidx.compose.foundation.layout.Spacer" not in code:
    code = code.replace(
        "import androidx.compose.foundation.layout.PaddingValues",
        "import androidx.compose.foundation.layout.PaddingValues\nimport androidx.compose.foundation.layout.Spacer\nimport androidx.compose.ui.unit.sp"
    )

# 2. Update displayName() and subjects list to ensure all 4 subjects (english, reasoning, mathematics, gk) are included
old_display = 'internal fun BatchSubject.displayName() = if (key == "mathematics") "Maths" else name'
new_display = 'internal fun BatchSubject.displayName() = if (key == "mathematics") "Maths" else if (key == "gk") "GK" else name'
code = code.replace(old_display, new_display)

old_subjects = 'val subjects = subjectOrder.mapNotNull { key -> overview.subjects.firstOrNull { it.key == key } }'
new_subjects = 'val subjects = subjectOrder.map { key -> overview.subjects.firstOrNull { it.key == key } ?: BatchSubject(id = key, key = key, name = if (key == "mathematics") "Maths" else if (key == "gk") "GK" else key.replaceFirstChar { it.uppercase() }) }'
code = code.replace(old_subjects, new_subjects)

# 3. Replace SubjectWatchGrid and SubjectWatchChip
old_grid_start = "@Composable\ninternal fun SubjectWatchGrid("
new_grid = """@Composable
internal fun SubjectWatchGrid(
    subjects: List<BatchSubject>,
    overview: BatchOverview,
    vm: ToppersBatchViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        subjects.chunked(2).forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SubjectWatchChip(
                        subject = subject,
                        overview = overview,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.focusTodaySubject(subject.id) }
                    )
                }
                if (rowSubjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectWatchChip(
    subject: BatchSubject,
    overview: BatchOverview,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val progress = overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }
    val subjectDone = progress?.completed ?: 0
    val subjectTotal = progress?.total ?: 0
    val fraction = if (subjectTotal == 0) 0f else (subjectDone.toFloat() / subjectTotal).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    val accent = when (subject.key) {
        "english" -> Color(0xFFE11D48)
        "reasoning" -> Color(0xFF7C3AED)
        "mathematics" -> Color(0xFFF59E0B)
        "gk" -> Color(0xFF10B981)
        else -> Color(0xFF16A34A)
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (subject.key == "gk") "GK" else subject.displayName().take(1),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (subject.key == "gk") 13.sp else 16.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = subject.displayName(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$percent%",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
"""

grid_idx = code.find(old_grid_start)
if grid_idx != -1:
    code = code[:grid_idx] + new_grid

with open('app/src/main/java/com/safarparmar/app/feature/toppersbatch/ToppersBatchDashboard.kt', 'w') as f:
    f.write(code)

