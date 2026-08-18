package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.ui.ekagra.focusshield.BlockedAppInfo
import com.safarparmar.app.ui.glass.LiquidGlassBackdrop
import com.safarparmar.app.ui.theme.isLightBackground

internal data class EditableApp(
    val packageName: String,
    val label: String,
    val category: AppCategory,
)

/**
 * Lets the student decide what each installed app counts as. Reachable from both
 * Kavach analytics and Kavach setup — the same list drives both, and a change here
 * reclassifies the retained history so past ranges stay consistent.
 */
@Composable
fun AppCategoryEditorScreen(
    onBack: () -> Unit,
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
    appsViewModel: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerState by appsViewModel.pickerState.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { appsViewModel.loadApps() }

    val overrides = remember(state.classifications) {
        state.classifications.associate { it.packageName to AppCategory.fromWire(it.category) }
    }

    val apps = remember(pickerState.allApps, overrides, query) {
        buildEditableApps(pickerState.allApps, overrides, query)
    }

    Box(Modifier.fillMaxSize()) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = isLight)

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 12.dp)
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (!isLight) Color(0xFF3B0764) else Color(0xFF581C87))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "App categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText(isLight),
                    )
                    Text(
                        "Your choices win over SAFAR's defaults and sync with your account.",
                        fontSize = 12.sp,
                        color = secondaryText(isLight),
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search apps") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val context = LocalContext.current
                    val iconDrawable = remember(app.packageName) {
                        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                if (iconDrawable != null) {
                                    AsyncImage(
                                        model = iconDrawable,
                                        contentDescription = app.label,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(secondaryText(isLight).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = secondaryText(isLight),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                app.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText(isLight),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppCategory.PRODUCTIVE to "Productive",
                                AppCategory.DISTRACTING to "Distracting",
                                AppCategory.NEUTRAL to "Neutral",
                            ).forEach { (category, label) ->
                                OutlineChip(
                                    label = label,
                                    accent = KavachCategoryColors.of(category, isLight),
                                    isLight = isLight,
                                    selected = app.category == category,
                                ) { viewModel.setCategory(app.packageName, category, app.label) }
                            }
                        }
                        if (app.category == AppCategory.UNCLASSIFIED) {
                            Text(
                                "Not categorised yet — its time is reported separately, never as a distraction.",
                                fontSize = 11.sp,
                                color = KavachCategoryColors.unclassified(isLight),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(secondaryText(isLight).copy(alpha = 0.10f)),
                        )
                    }
                }
            }
        }
    }
}

/** Pure list assembly, so ordering and search can be unit-tested. */
internal fun buildEditableApps(
    installed: List<BlockedAppInfo>,
    overrides: Map<String, AppCategory>,
    query: String,
): List<EditableApp> {
    val normalised = query.trim().lowercase()
    return installed
        .map { app ->
            EditableApp(
                packageName = app.packageName,
                label = app.appName,
                category = overrides[app.packageName] ?: AppCategory.UNCLASSIFIED,
            )
        }
        .filter { normalised.isEmpty() || it.label.lowercase().contains(normalised) }
        // Unknown apps first: those are the ones whose time is currently unattributed.
        .sortedWith(
            compareBy<EditableApp> { it.category != AppCategory.UNCLASSIFIED }
                .thenBy { it.label.lowercase() },
        )
}
