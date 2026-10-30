package com.safarparmar.app.ui.mehfil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.domain.model.Sandesh
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.SafarTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/** Offline visual QA entry point. Reference copy is fixture data, never added to the live feed. */
@AndroidEntryPoint
class MehfilTabletPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeVm = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.theme.ThemeViewModel>()
            val dark by themeVm.isDarkTheme.collectAsState()
            LaunchedEffect(Unit) {
                val persisted = themeVm.dataStore.isDarkTheme.first()
                themeVm.isDarkTheme.first { it == persisted }
                if (persisted != intent.getBooleanExtra("dark", false)) themeVm.toggleDarkTheme()
            }
            SafarTheme(darkTheme = dark) {
                var selectedTab by remember { mutableStateOf(MehfilTab.COMMUNITY) }
                var room by remember { mutableStateOf("ALL") }
                var search by remember { mutableStateOf(false) }
                var query by remember { mutableStateOf("") }
                var composer by remember { mutableStateOf(false) }
                var guidelines by remember { mutableStateOf(false) }
                var commentPost by remember { mutableStateOf<MehfilPost?>(null) }
                var posts by remember { mutableStateOf(referencePosts()) }
                var saved by remember { mutableStateOf(emptySet<String>()) }
                var reacted by remember { mutableStateOf(setOf("library", "wellbeing")) }
                val announcements = remember { referenceAnnouncements() }
                val state = MehfilUiState(
                    isInitializing = false, posts = posts.filter { room == "ALL" || it.space == room },
                    selectedSpace = room, sandeshes = announcements, savedPostIds = saved,
                    savedPosts = posts.filter { it.id in saved }, savedHasMore = false,
                    reactedSandeshIds = reacted, currentUserId = "preview-viewer", mehfilDm = true,
                    isLoadingPremiumFeatures = false,
                )
                MehfilContent(
                    uiState = state, selectedTab = selectedTab, currentRoute = Routes.MEHFIL,
                    isDarkTheme = dark, searchActive = search, searchQuery = query,
                    onNavigate = {}, onToggleDarkTheme = { themeVm.toggleDarkTheme() }, onTabSelected = { selectedTab = it },
                    onSearchActiveChange = { search = it }, onSearchQueryChange = { query = it }, onClearSearch = { query = "" },
                    onTourClick = {}, onGuidelinesClick = { guidelines = true }, onCreatePostClick = { composer = true },
                    onLoadPosts = {}, onLoadSavedPage = {}, onJoinRoom = { room = it },
                    onReactSandesh = { reacted = if (it in reacted) reacted - it else reacted + it },
                    onLikePost = { target -> posts = posts.map { if (it.id == target.id) it.copy(userLiked = !it.userLiked, reactionCount = it.reactionCount + if (it.userLiked) -1 else 1) else it } },
                    onSavePost = { saved = if (it in saved) saved - it else saved + it }, onUnsavePost = { saved = saved - it },
                    onCommentClick = { commentPost = it }, onSandeshCommentClick = {}, onConnect = { selectedTab = MehfilTab.CONNECTIONS },
                    onAcceptDm = {}, onDeclineDm = {}, onOpenDmChat = {},
                )
                if (composer) CreatePostSheet(room, false, { _, _, _ -> composer = false }, { composer = false })
                if (guidelines) GuidelinesSheet { guidelines = false }
                commentPost?.let { post -> CommentsBottomSheet(post, emptyList(), false, false, false, false, null, "", {}, {}, {}, { commentPost = null }) }
            }
        }
    }
}

private fun referencePosts() = listOf(
    MehfilPost(
        id = "history", userId = "aarushi", authorName = "Aarushi K.", space = "ACADEMIC",
        createdAt = ZonedDateTime.now().minusHours(2).toString(), reactionCount = 24, commentCount = 8, userLiked = true,
        content = "How are you revising Modern History this week?\n\nI’m a bit overwhelmed with the vast timeline and so many movements. Would love to hear how you’re structuring your revision — are you using notes, timelines, PYQs or any specific resources that are working well for you? Any tips would be really helpful!",
    ),
    MehfilPost(
        id = "progress", userId = "neha", authorName = "Neha S.", space = "REFLECTIVE",
        createdAt = ZonedDateTime.now().minusDays(1).toString(), reactionCount = 37, commentCount = 5, userLiked = true,
        content = "Small progress still counts. Keep showing up.\n\nSome days feel harder than others, and it’s easy to feel like you’re not doing enough. But even a little bit of focused study, a short walk, or just being kind to yourself is progress. Proud of everyone here who keeps showing up, even on the tough days. You’re doing better than you think! 💜",
    ),
)

private fun referenceAnnouncements() = listOf(
    Sandesh(id = "library", createdAt = ZonedDateTime.now().minusDays(1).toString(), reactionCount = 56, commentCount = 12,
        content = "Library extended hours this week\n\nThe central library will remain open till 10 PM from Oct 28 – Nov 3 to support your exam preparation. Please carry your institute ID card for entry."),
    Sandesh(id = "wellbeing", createdAt = ZonedDateTime.now().minusDays(3).toString(), reactionCount = 42, commentCount = 9,
        content = "Mindful Mondays – Group Session\n\nJoin our online session on stress management and building healthy routines, this Monday at 6 PM. Open to all students. Take a moment for your well-being! 🌿"),
)
