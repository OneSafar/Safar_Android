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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.isLightBackground
import kotlinx.coroutines.delay

internal enum class MehfilTab(val label: String, val icon: ImageVector) {
    COMMUNITY("Community", Icons.Default.Groups),
    SAVED("Saved", Icons.Default.Bookmark),
    ANALYTICS("My Activity", Icons.Default.BarChart),
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
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
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
                FlatTopIconChip(onClick = onTourClick) {
                    Image(
                        painter = painterResource(R.drawable.ic_butterfly_tour),
                        contentDescription = "Guide",
                        modifier = Modifier.size(20.dp),
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

                        // Flat accent FAB (not Material FAB)
                        if (selectedTab == MehfilTab.COMMUNITY) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp)
                                    .size(56.dp)
                                    .shadow(
                                        elevation = if (isLight) 6.dp else 12.dp,
                                        shape = CircleShape,
                                        spotColor = MehfilFlatColors.Primary.copy(alpha = 0.35f),
                                        ambientColor = MehfilFlatColors.Primary.copy(alpha = 0.2f),
                                    )
                                    .clip(CircleShape)
                                    .background(MehfilFlatColors.Primary)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onCreatePostClick,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Create post",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
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
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, MehfilFlatColors.Hairline, CircleShape)
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
 * Same macOS glass bottom-bar recipe as Exam Planner / Nishtha.
 */
@Composable
private fun MehfilBottomBar(
    selectedTab: MehfilTab,
    pendingCount: Int,
    onTabSelected: (MehfilTab) -> Unit,
) {
    val tabs = MehfilTab.entries
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()
    val isDark = !isLight

    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "mehfilMacOSGlassTabSlide",
    )

    val glassBodyColor = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E).copy(alpha = 0.65f)
    val glassBorderBrush = if (!isLight) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)),
        )
    }
    val shadowElevation = if (isLight) 6.dp else 14.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.85f)
    val barShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val topBorderBrush = if (!isLight) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color.Transparent),
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = barShape,
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            .border(
                width = 0.5.dp,
                brush = topBorderBrush,
                shape = barShape,
            ),
        color = scheme.surface,
        shape = barShape,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 6.dp),
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / tabs.size
            val pillShape = RoundedCornerShape(20.dp)

            Box(
                modifier = Modifier
                    .offset(x = itemWidth * animatedIndex)
                    .width(itemWidth)
                    .height(56.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .shadow(
                        elevation = shadowElevation,
                        shape = pillShape,
                        spotColor = shadowColor,
                        ambientColor = shadowColor,
                    )
                    .clip(pillShape)
                    .background(glassBodyColor)
                    .border(
                        width = 0.5.dp,
                        brush = glassBorderBrush,
                        shape = pillShape,
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabAccent = mehfilTabAccent(tab, isDark)
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            tabAccent
                        } else {
                            scheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        animationSpec = tween(200),
                        label = "mehfilTabContentColor",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(pillShape)
                            .clickable { onTabSelected(tab) },
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
    }
}

private fun mehfilTabAccent(tab: MehfilTab, isDark: Boolean): Color = when (tab) {
    MehfilTab.COMMUNITY -> if (isDark) Color(0xFFC084FC) else Color(0xFF6D28D9)
    MehfilTab.SAVED -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    MehfilTab.ANALYTICS -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    MehfilTab.CONNECTIONS -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
}
