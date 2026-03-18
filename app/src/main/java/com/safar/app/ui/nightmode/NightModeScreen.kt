package com.safar.app.ui.nightmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.ui.components.SafarCard
import com.safar.app.ui.components.SafarTopBar
import com.safar.app.ui.theme.ThemeViewModel

@Composable
fun NightModeScreen(onBack: () -> Unit, themeViewModel: ThemeViewModel = hiltViewModel()) {
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    val isNight by themeViewModel.isNightMode.collectAsState()

    Scaffold(topBar = { SafarTopBar("Display & Theme", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SafarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                                Text("Easier on the eyes in low light", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                            Switch(checked = isDark, onCheckedChange = { themeViewModel.toggleDarkTheme() })
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightlightRound, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Night Mode", style = MaterialTheme.typography.bodyLarge)
                                Text("Ultra dark theme for late night use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                            Switch(checked = isNight, onCheckedChange = { themeViewModel.setNightMode(it) })
                        }
                    }
                }
            }

            item {
                SafarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Night Mode Tips", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "🌙" to "Reduces blue light for better sleep",
                            "👁️" to "Reduces eye strain during late study sessions",
                            "🔋" to "May improve battery life on OLED screens",
                            "🧘" to "Creates a calm, focused atmosphere"
                        ).forEach { (emoji, tip) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.width(8.dp))
                                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}
