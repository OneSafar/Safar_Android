package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    onBack: () -> Unit,
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.pickerState.collectAsState()
    val shieldState by viewModel.shieldState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val filteredApps = remember(state.allApps, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.allApps
        else state.allApps.filter {
            it.appName.contains(state.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(state.searchQuery, ignoreCase = true)
        }
    }

    val selectedCount = state.allApps.count { it.isBlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Choose Apps to Block", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "$selectedCount app${if (selectedCount != 1) "s" else ""} selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedCount > 0) {
                        TextButton(onClick = {
                            state.allApps.filter { it.isBlocked }.forEach {
                                viewModel.toggleApp(it.packageName)
                            }
                        }) {
                            Text("Clear All", fontSize = 13.sp)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Quick-select popular distractors
            if (state.searchQuery.isBlank()) {
                val distractors = listOf(
                    "com.instagram.android",
                    "com.google.android.youtube",
                    "com.twitter.android",
                    "com.whatsapp",
                    "com.snapchat.android",
                    "com.zhiliaoapp.musically", // TikTok
                    "com.facebook.katana",
                )
                val distractorApps = state.allApps.filter { it.packageName in distractors }
                if (distractorApps.isNotEmpty()) {
                    Text(
                        "POPULAR DISTRACTORS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    distractorApps.forEach { app ->
                        AppRow(
                            app = app,
                            onToggle = { viewModel.toggleApp(app.packageName) },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "ALL APPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                    ) { app ->
                        AppRow(
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
private fun AppRow(app: BlockedAppInfo, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // App icon
        app.icon?.let { drawable ->
            Image(
                bitmap = drawable.toBitmap(width = 96, height = 96).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } ?: Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(24.dp))
        }

        // App name + package
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                app.packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        // Toggle
        Switch(
            checked = app.isBlocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFE53935),
            ),
        )
    }
}
