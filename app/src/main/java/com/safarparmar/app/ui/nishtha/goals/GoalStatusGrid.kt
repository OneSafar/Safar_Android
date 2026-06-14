package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.GoalSubtask
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun StatusGrid(goals: List<Goal>, ekagraAnalytics: com.safarparmar.app.domain.model.EkagraAnalyticsStats) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = standardGoals.filter { !it.completed && it.lifecycleStatus !in listOf("abandoned", "rolled_over") && !it.isDormant(todayKey) }
    val scheduled = standardGoals.filter { !it.completed && it.isDormant(todayKey) }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val doneToday = manualCompletedGoals.count { it.completedDateKey() == todayKey }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatInfoCard("Done Today", doneToday.toString(), "Manual goals completed on today's date.", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            StatInfoCard("Open Now", pending.size.toString(), "Active manual goals available to work on right now.", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        }
        StatInfoCard("Scheduled Ahead", scheduled.size.toString(), "Future goals parked until their scheduled day arrives.", Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primary)
    }
}
