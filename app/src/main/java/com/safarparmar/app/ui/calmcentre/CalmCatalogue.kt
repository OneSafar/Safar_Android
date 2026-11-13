package com.safarparmar.app.ui.calmcentre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text as UiText

internal data class CalmCatalogueItem(val id: String, val title: String, val summary: String,
    val practice: CalmPractice, val technique: CalmBreathTechnique? = null)

internal val calmBreathingItems = CalmBreathTechnique.entries.map {
    CalmCatalogueItem("breath-${it.name}", it.title, it.pattern, CalmPractice.BREATHE, it)
}
internal val calmGuidedItems = listOf(CalmPractice.KINDNESS, CalmPractice.GROUND, CalmPractice.REST).map {
    CalmCatalogueItem(it.name, it.title, it.category, it)
}

@Composable
internal fun CalmCatalogue(recommendation: CalmRecommendation, minutes: Int, colors: CalmColors,
    hasCheckIn: Boolean, onMinutes: (Int) -> Unit, onStart: (CalmCatalogueItem) -> Unit, onCheckIn: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val suggested = CalmCatalogueItem("suggested", recommendation.practice.title, if (hasCheckIn) recommendation.reason else "A gentle place to begin. Or explore a different rhythm from the collection below.",
        recommendation.practice, if (recommendation.practice == CalmPractice.BREATHE) CalmBreathTechnique.GENTLE else null)
    LazyColumn(Modifier.fillMaxSize().testTag("calm-catalogue"), verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your calm, your way.", color = colors.ink, style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("A little breathing room. A practice that meets you here.", color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Breathe", "Ground", "Kindness", "Rest")) { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Time for you", color = colors.muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                listOf(1, 2, 5).forEach { value ->
                    FilterChip(selected = minutes == value, onClick = { onMinutes(value) }, label = { Text("$value min") })
                }
            }
        }
        if (filter == "All") item {
            Card(colors = CardDefaults.cardColors(containerColor = colors.stage(suggested.practice)),
                shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FOR THIS MOMENT", color = colors.muted, style = MaterialTheme.typography.labelSmall)
                    CalmArtwork(suggested.practice, Modifier.fillMaxWidth().height(175.dp))
                    Text(suggested.title, color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("${suggested.practice.category} · $minutes min", color = colors.muted, style = MaterialTheme.typography.labelLarge)
                    Text(suggested.summary, color = colors.ink, style = MaterialTheme.typography.bodyMedium)
                    UiButton(onClick = { onStart(suggested) }, style = ButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) {
                        UiText("Begin practice", color = colors.onPlum)
                    }
                }
            }
        }
        if (filter == "All" || filter == "Breathe") item {
            CalmCatalogueShelf("Find your breathing rhythm", "Choose a pattern. Follow the shape.", calmBreathingItems,
                minutes, colors, onStart)
        }
        val guided = calmGuidedItems.filter { filter == "All" || when (it.practice) {
            CalmPractice.KINDNESS -> filter == "Kindness"
            CalmPractice.GROUND -> filter == "Ground"
            CalmPractice.REST -> filter == "Rest"
            else -> false
        } }
        if (guided.isNotEmpty()) item {
            CalmCatalogueShelf("Guided moments", "A softer inner voice, a little presence, or a quieter evening.", guided,
                minutes, colors, onStart)
        }
        item {
            Text("Go at your own pace. Every practice is optional; you can pause or choose something else at any time.",
                color = colors.muted, style = MaterialTheme.typography.bodySmall)
            UiButton(onClick = onCheckIn, style = ButtonStyle.Ghost, modifier = Modifier.heightIn(min = 48.dp)) {
                UiText(if (hasCheckIn) "Change my check-in" else "Find my recommendation", color = colors.plum)
            }
        }
    }
}

@Composable
private fun CalmCatalogueShelf(title: String, subtitle: String, entries: List<CalmCatalogueItem>, minutes: Int,
    colors: CalmColors, onStart: (CalmCatalogueItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = colors.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        LazyRow(Modifier.testTag(if (title == "Guided moments") "calm-guided-shelf" else "calm-breathing-shelf"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries, key = { it.id }) { entry ->
                Card(onClick = { onStart(entry) }, modifier = Modifier.width(220.dp), shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                    Box(Modifier.fillMaxWidth().height(150.dp).background(colors.stage(entry.practice))) {
                        CalmArtwork(entry.practice, Modifier.fillMaxSize(), technique = entry.technique ?: CalmBreathTechnique.GENTLE,
                            motionEnabled = false)
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(entry.title, color = colors.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(entry.summary, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                        Text("$minutes min · ${if (entry.technique != null) "Breathing practice" else "Guided animation"}",
                            color = colors.plum, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
