package com.safarparmar.app.ui.mehfil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.ui.tutorial.*

@Composable
internal fun MehfilPracticeStage(scene: PracticeScene) {
    if (scene == PracticeScene.MEHFIL_WRITE || scene == PracticeScene.MEHFIL_SHARE) {
        CreatePostSheet("ACADEMIC", false, { _, _, _ -> }, {}, inlinePreview = true,
            initialContent = stringResource(R.string.practice_sample_post))
        return
    }
    val saved = scene == PracticeScene.MEHFIL_SAVED
    val post = MehfilPost(id = "practice-post", userId = "practice-author", content = stringResource(R.string.practice_sample_post),
        authorName = stringResource(R.string.practice_sample_author), space = "ACADEMIC", createdAt = "2026-01-01T10:00:00Z",
        reactionCount = 3, commentCount = 1, isSaved = saved)
    val state = MehfilUiState(isInitializing = false, posts = listOf(post), selectedSpace = "ACADEMIC",
        savedPosts = listOf(post), savedPostIds = if (saved) setOf(post.id) else emptySet(), savedHasMore = false,
        isLoadingPremiumFeatures = false,
        pendingDmRequests = if (scene == PracticeScene.MEHFIL_CONNECTIONS) listOf(PendingDmRequest("sample-person", stringResource(R.string.practice_sample_author), "sample-request")) else emptyList())
    val tab = when (scene) {
        PracticeScene.MEHFIL_SAVED -> MehfilTab.SAVED
        PracticeScene.MEHFIL_CONNECTIONS -> MehfilTab.CONNECTIONS
        else -> MehfilTab.COMMUNITY
    }
    // SafarDrawerScaffold resolves live account and Premium ViewModels. Use only the
    // model-free body and real bottom bar here; none of those dependencies enter practice.
    Scaffold(containerColor = MehfilFlatColors.Bg, contentWindowInsets = WindowInsets(0),
        topBar = { Text(stringResource(R.string.mehfil_title), style = MaterialTheme.typography.titleLarge,
            color = MehfilFlatColors.Text, modifier = Modifier.padding(20.dp)) },
        bottomBar = { MehfilBottomBar(tab, state.pendingDmRequests.size, {}, {}) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                MehfilTab.COMMUNITY -> CommunityTab(state, "", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
                MehfilTab.SAVED -> SavedTab(state, {}, {}, {}, {}, {})
                MehfilTab.CONNECTIONS -> ConnectionsTab(state, {}, {}, {})
                else -> Unit
            }
        }
    }
}
