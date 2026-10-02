with open('app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraTimerTab.kt', 'r') as f:
    code = f.read()

# 1. Update YouTube Banner bottomBar padding
code = code.replace(
    "modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),",
    "modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),"
)

# 2. Update KavachSummaryPills padding
code = code.replace(
    "modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),",
    "modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 2.dp),"
)

# 3. Update main Column without verticalScroll and with SpaceEvenly
old_column_setup = """        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {"""

new_column_setup = """        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {"""
code = code.replace(old_column_setup, new_column_setup)

# 4. Update Spacers around ModeTabs
code = code.replace("Spacer(Modifier.height(if (isCompactHeight) 8.dp else 12.dp))", "Spacer(Modifier.height(6.dp))")
code = code.replace("Spacer(Modifier.height(if (isCompactHeight) 20.dp else 36.dp))", "Spacer(Modifier.height(12.dp))")
code = code.replace("Spacer(Modifier.height(24.dp))", "Spacer(Modifier.height(10.dp))")

# 5. Update Ring size
code = code.replace("Box(contentAlignment = Alignment.Center, modifier = Modifier.size(EkagraChrome.size(252f)))", "Box(contentAlignment = Alignment.Center, modifier = Modifier.size(EkagraChrome.size(212f)))")

# 6. Update Spacers below ring and controls
code = code.replace("Spacer(Modifier.height(28.dp))", "Spacer(Modifier.height(12.dp))")
code = code.replace("Spacer(Modifier.height(20.dp))", "Spacer(Modifier.height(8.dp))")
code = code.replace("Spacer(Modifier.height(16.dp))", "Spacer(Modifier.height(4.dp))")

# 7. Replace SafarAdaptiveRow for action buttons with Row weight(1f)
old_actions_block = """            // ── Controls — symmetrical horizontal control group ──
            com.safarparmar.app.ui.components.SafarAdaptiveRow {
                if (!isBrowsingOtherMode) {
                    EkagraGhostAction(
                        label = if (timerMode == TimerMode.BREAK) stringResource(R.string.ekagra_end_break) else stringResource(R.string.common_end),
                        ink = ink,
                        onClick = onReset,
                    )
                }
                EkagraPrimaryAction(
                    label = when {
                        isRunning   -> stringResource(R.string.common_pause)
                        hasProgress -> stringResource(R.string.common_resume)
                        else        -> stringResource(R.string.common_start)
                    },
                    accent  = themeAccent,
                    onClick = onPlayPause,
                )
                if (canStartBreak) {
                    EkagraGhostAction(
                        label = stringResource(R.string.ekagra_break),
                        ink   = ink,
                        onClick = onStartBreak,
                    )
                }
            }"""

new_actions_block = """            // ── Controls — symmetrical horizontal control group ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isBrowsingOtherMode) {
                    EkagraGhostAction(
                        label = if (timerMode == TimerMode.BREAK) stringResource(R.string.ekagra_end_break) else stringResource(R.string.common_end),
                        ink = ink,
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                    )
                }
                EkagraPrimaryAction(
                    label = when {
                        isRunning   -> stringResource(R.string.common_pause)
                        hasProgress -> stringResource(R.string.common_resume)
                        else        -> stringResource(R.string.common_start)
                    },
                    accent  = themeAccent,
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f),
                )
                if (canStartBreak) {
                    EkagraGhostAction(
                        label = stringResource(R.string.ekagra_break),
                        ink   = ink,
                        onClick = onStartBreak,
                        modifier = Modifier.weight(1f),
                    )
                }
            }"""
code = code.replace(old_actions_block, new_actions_block)

with open('app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraTimerTab.kt', 'w') as f:
    f.write(code)

