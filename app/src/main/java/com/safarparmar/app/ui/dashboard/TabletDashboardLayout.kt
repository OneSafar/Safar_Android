package com.safarparmar.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Reflow the same live cards; scrolling remains available for large fonts and short windows. */
@Composable
internal fun TabletDashboardLayout(
    modifier: Modifier = Modifier,
    landscape: Boolean,
    welcome: @Composable (Modifier) -> Unit,
    title: @Composable (Modifier) -> Unit,
    progress: @Composable (Modifier) -> Unit,
    shortcuts: @Composable (Boolean) -> Unit,
    report: (@Composable (Modifier) -> Unit)?,
    mood: (@Composable (Modifier) -> Unit)?,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 1440.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (landscape) {
            item(key = "overview") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    welcome(Modifier.weight(0.38f).fillMaxHeight().heightIn(min = 224.dp))
                    title(Modifier.weight(0.30f).fillMaxHeight().heightIn(min = 224.dp))
                    progress(Modifier.weight(0.32f).fillMaxHeight().heightIn(min = 224.dp))
                }
            }
        } else {
            item(key = "welcome") { welcome(Modifier.fillMaxWidth()) }
            item(key = "title_progress") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    title(Modifier.weight(1f).fillMaxHeight().heightIn(min = 224.dp))
                    progress(Modifier.weight(1f).fillMaxHeight().heightIn(min = 224.dp))
                }
            }
        }
        item(key = "shortcuts") { shortcuts(landscape) }
        if (landscape && report != null && mood != null) {
            item(key = "insights") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    report(Modifier.weight(1f).fillMaxHeight())
                    mood(Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else {
            if (report != null) item(key = "report") { report(Modifier.fillMaxWidth()) }
            if (mood != null) item(key = "mood") { mood(Modifier.fillMaxWidth()) }
        }
    }
}
