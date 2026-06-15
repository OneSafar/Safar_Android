package com.safarparmar.app.ui.dhyan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes

@Composable
fun DhyanCoursesScreen(
    currentRoute: String = Routes.COURSES,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
) {
    var showYoutubeModal by remember { mutableStateOf(false) }

    val dhyanVm: DhyanViewModel = hiltViewModel()
    val meditationVideoUrl by dhyanVm.meditationVideoUrl.collectAsStateWithLifecycle()
    val isLoadingVideo by dhyanVm.isLoadingVideo.collectAsStateWithLifecycle()
    val videoError by dhyanVm.videoError.collectAsStateWithLifecycle()

    if (showYoutubeModal) {
        DhyanYoutubePromotionDialog(
            videoUrl = meditationVideoUrl,
            onDismiss = { showYoutubeModal = false },
        )
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        SafarDrawerScaffold(
            title = "Courses",
            subtitle = "SAFAR",
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val err = videoError
                    when {
                        isLoadingVideo -> StatCardSkeleton(modifier = Modifier.height(180.dp))
                        err != null -> SafarErrorState(message = err, onRetry = { dhyanVm.loadMeditationVideo() })
                        else -> DhyanLatestVideoCard(
                            videoUrl = meditationVideoUrl,
                            onOpenModal = { showYoutubeModal = true },
                        )
                    }
                    Text("Dhyan Learning Tracks", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Deepen your meditation journey with guided courses, daily structure, and progress checkpoints.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    val context = LocalContext.current
                    val courseUrl = "https://www.parmaracademy.in/courses/75-safar-30"
                    Card(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(courseUrl))
                                context.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardDefaults.outlinedCardBorder(),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primary.copy(0.1f)), contentAlignment = Alignment.Center) { 
                                    Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person_standing), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) 
                                }
                                Text("SAFAR 30-Day Meditation Course", fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                            }
                            Text("A 30-day guided meditation journey to build a consistent practice, reduce stress, and deepen self-awareness.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
                                    Text("Available", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                                Text("Buy now →", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
