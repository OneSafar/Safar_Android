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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
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
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SyllabusRowSkeleton

@Composable
fun AppPickerScreen(
    onBack: () -> Unit,
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    androidx.activity.compose.BackHandler {
        onBack()
    }

    val state by viewModel.pickerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val filteredApps = remember(state.allApps, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.allApps
        else state.allApps.filter {
            it.appName.contains(state.searchQuery, ignoreCase = true) ||
                it.packageName.contains(state.searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
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
                    .padding(16.dp),
            ) {
                KavachStitchPrimaryButton(
                    text = stringResource(R.string.kavach_save_configuration),
                    onClick = onBack,
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
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
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
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
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

            // Bulk selection buttons
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.selectDistractingApps() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KavachDesign.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Select Distracting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.selectAllApps() },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = KavachDesign.TextMain
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.Border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Select All", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.deselectAllApps() },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = KavachDesign.TextMain
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.Border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

            if (state.isLoading && filteredApps.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(8) { SyllabusRowSkeleton() }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                    ) { app ->
                        ShieldConfigAppRow(
                            app = app,
                            onToggle = { viewModel.toggleApp(app.packageName) },
                        )
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
