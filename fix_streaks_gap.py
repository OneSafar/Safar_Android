with open('app/src/main/java/com/safarparmar/app/ui/nishtha/StreaksScreen.kt', 'r') as f:
    code = f.read()

# Replace padding top
code = code.replace(".padding(top = 12.dp, bottom = 32.dp)", ".padding(top = 4.dp, bottom = 32.dp)")

# Replace Toolbar block with clean Row
toolbar_start = "        // ── Composables UI Toolbar ───────────────────────────────────────────\n        Toolbar("
toolbar_end = "            },\n        )"

new_row = """        // ── Header Row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StreaksPalette.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.ic_flame),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = StreaksPalette.Primary,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.streaks_header_title),
                    fontFamily = LoraFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme[colors][onBackgroundColor],
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                alignment = DropdownMenuAlignment.End,
                panel = {
                    DropdownMenuPanel {
                        DropdownMenuItem(
                            onClick = {
                                menuExpanded = false
                                viewModel.loadTab(NishthaTab.STREAKS)
                            },
                            leading = {
                                ComposablesIcon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        ) {
                            Text("Refresh streaks")
                        }
                        DropdownMenuItem(
                            onClick = {
                                menuExpanded = false
                                showRulesDialog = true
                            },
                            leading = {
                                ComposablesIcon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        ) {
                            Text("Streak guide")
                        }
                    }
                },
                anchor = {
                    IconButton(
                        onClick = { menuExpanded = true },
                        style = ButtonStyle.Ghost,
                        buttonSize = ButtonSize.Small,
                    ) {
                        ComposablesIcon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }"""

tb_s = code.find(toolbar_start)
tb_e = code.find(toolbar_end, tb_s)
if tb_s != -1 and tb_e != -1:
    code = code[:tb_s] + new_row + code[tb_e + len(toolbar_end):]

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/StreaksScreen.kt', 'w') as f:
    f.write(code)

