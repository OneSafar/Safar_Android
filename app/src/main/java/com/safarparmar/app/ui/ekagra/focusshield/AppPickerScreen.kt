package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.R
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Parser
import com.safarparmar.app.ui.components.SyllabusRowSkeleton

@Composable
fun AppPickerScreen(
    onBack: () -> Unit,
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    fun finishAndReturn() {
        val activated = viewModel.saveAndEnableShieldIfReady()
        if (activated) {
            android.widget.Toast.makeText(context, context.getString(R.string.kavach_active_ready), android.widget.Toast.LENGTH_SHORT).show()
        }
        onBack()
    }

    androidx.activity.compose.BackHandler {
        finishAndReturn()
    }

    val state by viewModel.pickerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadApps() }

    AppPickerContent(
        apps = state.allApps, searchQuery = state.searchQuery, isLoading = state.isLoading,
        onSearchChange = viewModel::setSearchQuery, onToggleApp = viewModel::toggleApp,
        onSelectDistracting = viewModel::selectDistractingApps, onSelectAll = viewModel::selectAllApps,
        onClearAll = viewModel::deselectAllApps, onBack = ::finishAndReturn, onSave = ::finishAndReturn,
    )
}

/** Presentation only: installed-app loading, persistence and activation stay in AppPickerScreen. */
@Composable
internal fun AppPickerContent(
    apps: List<BlockedAppInfo>, searchQuery: String, isLoading: Boolean,
    onSearchChange: (String) -> Unit, onToggleApp: (String) -> Unit,
    onSelectDistracting: () -> Unit, onSelectAll: () -> Unit, onClearAll: () -> Unit,
    onBack: () -> Unit, onSave: () -> Unit,
    modifier: Modifier = Modifier, animateRows: Boolean = true,
) {
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = KavachDesign.Background,
        topBar = {
            KavachStitchBackHeader(
                onBack = onBack,
                title = stringResource(R.string.kavach_shield_configuration_title),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KavachDesign.Background.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                KavachStitchPrimaryButton(
                    text = stringResource(R.string.kavach_save_configuration),
                    onClick = onSave,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        stringResource(R.string.kavach_search_apps),
                        color = KavachDesign.SearchHint,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = KavachDesign.SearchHint)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = KavachDesign.SearchHint)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = KavachDesign.SearchFieldBg,
                    unfocusedContainerColor = KavachDesign.SearchFieldBg,
                    disabledContainerColor = KavachDesign.SearchFieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = KavachDesign.Primary,
                    focusedTextColor = KavachDesign.TextMain,
                    unfocusedTextColor = KavachDesign.TextMain,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (apps.any { it.packageName == YoutubeStudyV2Parser.YOUTUBE_PACKAGE && it.isBlocked }) {
                Text(
                    text = stringResource(R.string.youtube_focus_kavach_priority),
                    color = KavachDesign.TextMain,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Bulk selection buttons
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    androidx.compose.material3.Button(
                        onClick = onSelectDistracting,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KavachDesign.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(R.string.kavach_select_distracting), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onSelectAll,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = KavachDesign.TextMain
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.Border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(R.string.common_select_all), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onClearAll,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = KavachDesign.TextMain
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.Border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(R.string.common_clear_all), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

            if (isLoading && filteredApps.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(8) { SyllabusRowSkeleton() }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().tutorialTarget("ekagra.blockedApps"),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(
                        items = filteredApps,
                        key = { index, item -> item.packageName },
                    ) { index, app ->
                        if (animateRows) StaggeredAppPickerEntranceBox(index = index) {
                            ShieldConfigAppRow(app, onToggle = { onToggleApp(app.packageName) })
                        } else ShieldConfigAppRow(app, onToggle = { onToggleApp(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldConfigAppRow(
    app: BlockedAppInfo,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        app.icon?.let { drawable ->
            Image(
                bitmap = drawable.toBitmap(width = 96, height = 96).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } ?: Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KavachDesign.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Android, contentDescription = null, tint = KavachDesign.TextMuted)
        }

        Text(
            app.appName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = KavachDesign.TextMain,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )

        KavachStitchPillToggle(
            checked = app.isBlocked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun StaggeredAppPickerEntranceBox(
    index: Int,
    content: @Composable () -> Unit,
) {
    var isVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else (16 + (index % 12) * 8).dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 280,
            delayMillis = (index % 12) * 25,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "appPickerStaggeredOffset",
    )
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 240,
            delayMillis = (index % 12) * 25,
        ),
        label = "appPickerStaggeredAlpha",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}
