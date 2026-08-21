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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.ui.theme.SafarSemanticColors
import kotlinx.coroutines.delay

internal enum class MehfilTab(val label: String, val icon: ImageVector) {
    COMMUNITY("Community", Icons.Default.Groups),
    SAVED("Saved", Icons.Default.Bookmark),
    ANALYTICS("Activity", Icons.Default.BarChart),
    CONNECTIONS("Chats", Icons.Default.PersonAdd),
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
) {
    val searchFocusRequester = remember { FocusRequester() }
    SafarDrawerScaffold(
        title = "Mehfil",
        subtitle = "SAFAR",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = SafarSemanticColors.plannerBackground(),
        topBarActions = {
                FlatTopIconChip(
                    onClick = { onSearchActiveChange(!searchActive) },
                ) {
                    Icon(
                        if (searchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MehfilFlatColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                FlatTopIconChip(onClick = onGuidelinesClick) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Guidelines",
                        tint = MehfilFlatColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = MehfilFlatColors.Bg,
                    contentWindowInsets = WindowInsets.safeDrawing,
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
                                    bottom = innerPadding.calculateBottomPadding(),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CircularProgressIndicator(color = MehfilFlatColors.Primary)
                                Text(
                                    "Setting up Mehfil...",
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
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                    onCreatePostClick = onCreatePostClick,
                                    onViewStudyCircles = { onNavigate(com.safarparmar.app.ui.navigation.Routes.STUDY_CIRCLES) },
                                    onOpenStudyCircle = { circleId ->
                                        onNavigate("${com.safarparmar.app.ui.navigation.Routes.STUDY_CIRCLES}/$circleId")
                                    },
                                )
                                MehfilTab.SAVED -> SavedTab(
                                    uiState = uiState,
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

@Composable
private fun FlatTopIconChip(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val chipShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(chipShape)
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, chipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text("Search posts or names...", fontSize = 13.sp, color = MehfilFlatColors.Muted)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MehfilFlatColors.Muted,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearSearch),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MehfilFlatColors.Muted,
                        )
                    }
                }
            },
            modifier = modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MehfilFlatColors.Primary,
                unfocusedBorderColor = MehfilFlatColors.Hairline,
                focusedTextColor = MehfilFlatColors.Text,
                unfocusedTextColor = MehfilFlatColors.Text,
                cursorColor = MehfilFlatColors.Primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            textStyle = TextStyle(fontSize = 13.sp),
        )
    }
}

/**
 * Modern Flat Bottom Navigation Bar with Centered Overlapping FAB
 */
@Composable
private fun MehfilBottomBar(
    selectedTab: MehfilTab,
    pendingCount: Int,
    onTabSelected: (MehfilTab) -> Unit,
    onCreatePostClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val leftTabs = listOf(MehfilTab.COMMUNITY, MehfilTab.SAVED)
    val rightTabs = listOf(MehfilTab.ANALYTICS, MehfilTab.CONNECTIONS)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MehfilFlatColors.Hairline,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ),
            color = scheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Tabs
                leftTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val contentColor = if (isSelected) MehfilFlatColors.Primary else MehfilFlatColors.Muted

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(tab)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Center gap reserved for the overlapping FAB
                Spacer(Modifier.width(64.dp))

                // Right Tabs
                rightTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val contentColor = if (isSelected) MehfilFlatColors.Primary else MehfilFlatColors.Muted

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(tab)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp),
                            )
                            if (tab == MehfilTab.CONNECTIONS && pendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MehfilFlatColors.Like),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (pendingCount > 9) "9+" else pendingCount.toString(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Center Floating Action Button (Overlapping half on bottom bar, half above)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(MehfilFlatColors.Primary)
                .border(3.5.dp, MehfilFlatColors.Bg, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreatePostClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create post",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
