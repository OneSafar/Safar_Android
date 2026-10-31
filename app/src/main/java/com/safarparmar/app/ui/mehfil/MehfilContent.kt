package com.safarparmar.app.ui.mehfil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.ui.theme.SafarSemanticColors
import kotlinx.coroutines.delay

internal enum class MehfilTab(val labelRes: Int, val icon: ImageVector) {
    COMMUNITY(R.string.mehfil_tab_community, Icons.Default.Groups),
    SAVED(R.string.mehfil_tab_saved, Icons.Default.Bookmark),
    ANALYTICS(R.string.mehfil_tab_activity, Icons.Default.BarChart),
    CONNECTIONS(R.string.mehfil_tab_chats, Icons.Default.PersonAdd),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MehfilContent(
    uiState: MehfilUiState,
    selectedTab: MehfilTab,
    currentRoute: String,
    isDarkTheme: Boolean,
    searchActive: Boolean,
    searchQuery: String,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onTabSelected: (MehfilTab) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTourClick: () -> Unit,
    onGuidelinesClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onLoadPosts: (Boolean) -> Unit,
    onLoadSavedPage: (Int) -> Unit,
    onJoinRoom: (String) -> Unit,
    onReactSandesh: (String) -> Unit,
    onLikePost: (MehfilPost) -> Unit,
    onSavePost: (String) -> Unit,
    onUnsavePost: (String) -> Unit,
    onCommentClick: (MehfilPost) -> Unit,
    onSandeshCommentClick: (String) -> Unit,
    onConnect: (MehfilPost) -> Unit,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
    onOpenDmChat: () -> Unit,
    dataStore: com.safarparmar.app.data.local.SafarDataStore? = null,
    onOpenSandesh: () -> Unit = {},
) {
    val searchFocusRequester = remember { FocusRequester() }
    val tablet = com.safarparmar.app.ui.adaptive.rememberSafarAdaptiveState().isTablet
    // Inherit SafarTheme's density/font-scale clamp, exactly as Home does.
    // A feature-local native-density override makes the entire sidebar and feed oversized.
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMehfilTabletStyle provides tablet,
        com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides MehfilFlatColors.Primary,
    ) {
    com.safarparmar.app.ui.theme.SafarComposablesTheme {
    androidx.compose.material3.ProvideTextStyle(
        MaterialTheme.typography.bodyLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, letterSpacing = 0.sp,
        ),
    ) {
    SafarDrawerScaffold(
        title = stringResource(R.string.mehfil_title),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = MehfilFlatColors.Bg,
        topBarContentColor = MehfilFlatColors.Text,
        showTopBarTitle = false,
        showTutorialButton = !tablet,
        topBarActions = {
                if (tablet) {
                    val controller = com.safarparmar.app.ui.tutorial.LocalTitliController.current
                    com.safarparmar.app.ui.tutorial.TitliGuideButton(onClick = if (controller == null) onTourClick else null)
                }
                if (tablet) {
                    MehfilLibrarySearch(searchQuery, onSearchQueryChange, onClearSearch, Modifier.width(360.dp))
                } else FlatTopIconChip(
                    onClick = { onSearchActiveChange(!searchActive) },
                ) {
                    Icon(
                        if (searchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(R.string.common_search),
                        tint = MehfilFlatColors.Muted,
                        modifier = Modifier.size(if (tablet) 24.dp else 18.dp),
                    )
                }
                FlatTopIconChip(onClick = onGuidelinesClick) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.mehfil_guidelines),
                        tint = MehfilFlatColors.Muted,
                        modifier = Modifier.size(if (tablet) 24.dp else 18.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = MehfilFlatColors.Bg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        MehfilBottomBar(
                            selectedTab = selectedTab,
                            pendingCount = uiState.pendingDmRequests.size,
                            onTabSelected = onTabSelected,
                            onCreatePostClick = onCreatePostClick,
                        )
                    },
                ) { innerPadding ->
                    if (uiState.isInitializing) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    top = padding.calculateTopPadding(),
                                    bottom = innerPadding.calculateBottomPadding() + 6.dp,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CircularProgressIndicator(color = MehfilFlatColors.Primary)
                                Text(
                                    stringResource(R.string.mehfil_setting_up),
                                    fontSize = 13.sp,
                                    color = MehfilFlatColors.Muted,
                                )
                            }
                        }
                        return@Scaffold
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding() + 6.dp,
                            ),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = !tablet && searchActive && selectedTab == MehfilTab.COMMUNITY,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                LaunchedEffect(searchActive) {
                                    if (searchActive) {
                                        delay(80)
                                        searchFocusRequester.requestFocus()
                                    }
                                }
                                MehfilSearchBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onClearSearch = onClearSearch,
                                    modifier = Modifier.focusRequester(searchFocusRequester),
                                )
                            }

                            when (selectedTab) {
                                MehfilTab.COMMUNITY -> CommunityTab(
                                    uiState = uiState,
                                    searchQuery = searchQuery,
                                    onClearSearch = onClearSearch,
                                    onSandeshCommentClick = onSandeshCommentClick,
                                    onOpenSandesh = onOpenSandesh,
                                    onCommentClick = onCommentClick,
                                    onConnect = onConnect,
                                    onLoadPosts = onLoadPosts,
                                    onJoinRoom = onJoinRoom,
                                    onReactSandesh = onReactSandesh,
                                    onLikePost = onLikePost,
                                    onSavePost = onSavePost,
                                    onCreatePostClick = onCreatePostClick,
                                    onViewStudyCircles = { onNavigate(com.safarparmar.app.ui.navigation.Routes.STUDY_CIRCLES) },
                                    onOpenStudyCircle = { circleId ->
                                        onNavigate("${com.safarparmar.app.ui.navigation.Routes.STUDY_CIRCLES}/$circleId")
                                    },
                                )
                                MehfilTab.SAVED -> SavedTab(
                                    uiState = uiState,
                                    onLoadPage = onLoadSavedPage,
                                    onLikePost = onLikePost,
                                    onCommentClick = onCommentClick,
                                    onUnsavePost = onUnsavePost,
                                    onConnect = onConnect,
                                )
                                MehfilTab.ANALYTICS -> AnalyticsTab(uiState = uiState)
                                MehfilTab.CONNECTIONS -> ConnectionsTab(
                                    uiState = uiState,
                                    onNavigateToDmChat = onOpenDmChat,
                                    onAcceptDm = onAcceptDm,
                                    onDeclineDm = onDeclineDm,
                                )
                            }
                        }

                    }
                }
            }
        }
}
}
}
}

@Composable
private fun FlatTopIconChip(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    com.composables.ui.components.IconButton(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 3.dp).size(44.dp),
        content = content,
    )
}

@Composable
private fun MehfilSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MehfilLibrarySearch(searchQuery, onSearchQueryChange, onClearSearch,
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun MehfilLibrarySearch(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val state = androidx.compose.foundation.text.input.rememberTextFieldState(value)
    val callback by androidx.compose.runtime.rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.text.toString() }.collect { callback(it) }
    }
    com.composables.ui.components.TextField(
        state = state, modifier = modifier, shape = RoundedCornerShape(18.dp),
        backgroundColor = MehfilFlatColors.Surface,
        contentColor = MehfilFlatColors.Text, borderColor = MehfilFlatColors.Hairline,
        accessibilityLabel = stringResource(R.string.common_search),
        placeholder = { com.composables.ui.components.Text(stringResource(R.string.mehfil_search_placeholder), fontSize = 14.sp) },
        leading = { com.composables.ui.components.Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailing = if (value.isNotEmpty()) ({
            com.composables.ui.components.IconButton(onClick = onClear, style = com.composables.ui.components.ButtonStyle.Ghost) {
                com.composables.ui.components.Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_search), modifier = Modifier.size(18.dp))
            }
        }) else null,
    )
}

/**
 * Modern Flat Bottom Navigation Bar with Centered Overlapping FAB
 */
@Composable
internal fun MehfilBottomBar(
    selectedTab: MehfilTab,
    pendingCount: Int,
    onTabSelected: (MehfilTab) -> Unit,
    onCreatePostClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val primary = MehfilFlatColors.Primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            val dockShape = RoundedCornerShape(30.dp)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = dockShape,
                color = MehfilFlatColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MehfilFlatColors.Primary.copy(alpha = 0.12f)),
            ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MehfilTab.entries.forEachIndexed { index, tab ->
                    if (index == 2) Spacer(Modifier.width(56.dp))
                    com.composables.ui.components.NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                            .background(if (selectedTab == tab) MehfilFlatColors.Lavender else Color.Transparent, RoundedCornerShape(18.dp))
                            .tutorialTarget("mehfil.${tab.name.lowercase(java.util.Locale.ROOT)}"),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val tint = if (selectedTab == tab) primary else MehfilFlatColors.Muted
                            Box {
                                com.composables.ui.components.Icon(tab.icon, contentDescription = stringResource(tab.labelRes), modifier = Modifier.size(24.dp), tint = tint)
                                if (tab == MehfilTab.CONNECTIONS && pendingCount > 0) {
                                    Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-2).dp).size(8.dp).background(MehfilFlatColors.Like, CircleShape))
                                }
                            }
                            com.composables.ui.components.Text(stringResource(tab.labelRes), fontSize = 11.sp, color = tint, maxLines = 1)
                            Box(Modifier.width(22.dp).height(3.dp).background(if (selectedTab == tab) primary else Color.Transparent, CircleShape))
                        }
                    }
                }
            }
            }
            com.composables.ui.components.IconButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCreatePostClick() },
                style = com.composables.ui.components.ButtonStyle.Ghost,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp).size(62.dp)
                    .shadow(16.dp, CircleShape, clip = false,
                        ambientColor = primary.copy(alpha = 0.22f), spotColor = primary.copy(alpha = 0.32f))
                    .background(primary, CircleShape)
                    .border(2.dp, MehfilFlatColors.Surface, CircleShape).tutorialTarget("mehfil.post"),
                contentColor = Color.White,
            ) {
                com.composables.ui.components.Icon(Icons.Default.Add, contentDescription = stringResource(R.string.mehfil_create_post), modifier = Modifier.size(30.dp), tint = Color.White)
            }
        }
    }
}
