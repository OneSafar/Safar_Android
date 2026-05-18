package com.safar.app.ui.mehfil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.domain.model.MehfilPost
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.theme.Blue500
import com.safar.app.ui.theme.Violet500
import kotlinx.coroutines.delay

internal enum class MehfilTab(val label: String, val icon: ImageVector) {
    COMMUNITY("Community", Icons.Default.Groups),
    SAVED("Saved", Icons.Default.Bookmark),
    ANALYTICS("Activity", Icons.Default.BarChart),
    CONNECTIONS("Connections", Icons.Default.PersonAdd),
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
    onLanguageClick: () -> Unit,
    onTabSelected: (MehfilTab) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTourClick: () -> Unit,
    onGuidelinesClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onLoadPosts: (Boolean) -> Unit,
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
) {
    val searchFocusRequester = remember { FocusRequester() }

    SafarDrawerScaffold(
        title = "Mehfil",
        subtitle = "SAFAR",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarActions = {
            IconButton(onClick = { onSearchActiveChange(!searchActive) }) {
                Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onTourClick) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Guide")
            }
            IconButton(onClick = onGuidelinesClick) {
                Icon(Icons.Default.Info, contentDescription = "Guidelines")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    MehfilBottomBar(
                        selectedTab = selectedTab,
                        pendingCount = uiState.pendingDmRequests.size,
                        onTabSelected = onTabSelected,
                    )
                },
                floatingActionButton = {
                    if (selectedTab == MehfilTab.COMMUNITY) {
                        FloatingActionButton(
                            onClick = onCreatePostClick,
                            containerColor = when (uiState.selectedSpace) {
                                "ACADEMIC" -> Blue500
                                "REFLECTIVE" -> Violet500
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                },
            ) { innerPadding ->
                if (uiState.isInitializing) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Setting up Mehfil...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    return@Scaffold
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                ) {
                    AnimatedVisibility(
                        visible = searchActive && selectedTab == MehfilTab.COMMUNITY,
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
                            onCommentClick = onCommentClick,
                            onConnect = onConnect,
                            onLoadPosts = onLoadPosts,
                            onJoinRoom = onJoinRoom,
                            onReactSandesh = onReactSandesh,
                            onLikePost = onLikePost,
                            onSavePost = onSavePost,
                        )
                        MehfilTab.SAVED -> SavedTab(
                            uiState = uiState,
                            onLikePost = onLikePost,
                            onCommentClick = onCommentClick,
                            onUnsavePost = onUnsavePost,
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

@Composable
private fun MehfilSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search posts or names...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            textStyle = TextStyle(fontSize = 13.sp),
        )
    }
}

@Composable
private fun MehfilBottomBar(
    selectedTab: MehfilTab,
    pendingCount: Int,
    onTabSelected: (MehfilTab) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        MehfilTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == MehfilTab.CONNECTIONS && pendingCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (pendingCount > 9) "9+" else pendingCount.toString(), fontSize = 9.sp) } }) {
                            Icon(tab.icon, contentDescription = null)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        tab.label,
                        fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 10.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
