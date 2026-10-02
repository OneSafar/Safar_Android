with open('app/src/main/java/com/safarparmar/app/ui/nishtha/journal/JournalScreen.kt', 'r') as f:
    code = f.read()

# 1. Add ModalBottomSheet imports
if "import androidx.compose.material3.ModalBottomSheet" not in code:
    code = code.replace(
        "import androidx.compose.material3.MaterialTheme",
        "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.ModalBottomSheet\nimport androidx.compose.material3.rememberModalBottomSheetState"
    )

# 2. Replace JournalInlineSheetScaffold
scaffold_start = "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun JournalInlineSheetScaffold("
if scaffold_start not in code:
    scaffold_start = "@Composable\nprivate fun JournalInlineSheetScaffold("

scaffold_end = "}\n\n@Composable\nprivate fun JournalWriteSheet("

new_scaffold = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalInlineSheetScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlannerFlatColors.BgCream,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlannerFlatColors.BorderSoft),
                )
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            content = content,
        )
    }
}

@Composable
private fun JournalWriteSheet("""

s_idx = code.find(scaffold_start)
e_idx = code.find(scaffold_end, s_idx)
if s_idx != -1 and e_idx != -1:
    code = code[:s_idx] + new_scaffold + code[e_idx + len(scaffold_end):]

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/journal/JournalScreen.kt', 'w') as f:
    f.write(code)

