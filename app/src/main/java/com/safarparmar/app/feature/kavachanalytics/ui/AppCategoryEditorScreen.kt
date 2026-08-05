package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryText(isLight),
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    GlassCard(isLight) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                app.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText(isLight),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    AppCategory.PRODUCTIVE to "Productive",
                                    AppCategory.DISTRACTING to "Distracting",
                                    AppCategory.NEUTRAL to "Neutral",
                                ).forEach { (category, label) ->
                                    CategoryPill(
                                        label = label,
                                        color = KavachCategoryColors.of(category, isLight),
                                        selected = app.category == category,
                                        isLight = isLight,
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
                        }
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
