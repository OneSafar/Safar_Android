package com.safarparmar.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.drawer.drawerItems
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.components.popups.SafarModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllToolsSheet(onDismiss: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val entries = (drawerItems.filter { !it.requiresAdmin && it.route != Routes.HOME }
        .map { context.getString(it.labelRes) to it.route } + listOf(
        "Achievements" to Routes.ACHIEVEMENTS,
        "Analytics" to Routes.ANALYTICS,
        "GK 5.0" to Routes.GK5_BATCH,
        "Toppers Batch" to Routes.TOPPERS_BATCH,
        "Calm Centre" to Routes.CALM_CENTRE,
        "Courses" to Routes.COURSES,
        "Habit Insights" to Routes.HABIT_INSIGHTS,
        "Habit Tracker" to Routes.HABIT_TRACKER,
        "Timetable" to Routes.TIMETABLE,
        "YouTube Focus" to Routes.YOUTUBE_STUDY_MODE_V2,
    )).distinctBy { it.second }.sortedBy { it.first.lowercase(java.util.Locale.getDefault()) }
    SafarModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("All tools & features", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(entries, key = { it.second }) { (label, route) ->
                ListItem(headlineContent = { Text(label) }, modifier = Modifier.clickable {
                    onNavigate(Routes.normalizeFeatureRoute(route))
                }.padding(horizontal = 8.dp))
            }
        }
    }
}
