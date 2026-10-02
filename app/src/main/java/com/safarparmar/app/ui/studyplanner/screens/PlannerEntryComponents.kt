package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.*
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SafarButton
import com.safarparmar.app.ui.components.SafarOverflowMenu
import com.safarparmar.app.ui.components.SafarMenuAction

@Composable
internal fun PlannerPosterEntry(onClick: () -> Unit) {
    SafarButton(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Image(painterResource(R.drawable.parmar_tracker_poster), contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            Row(Modifier.fillMaxWidth().background(Color(0xFF17212B)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Parmar Topper Batch Tracker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Lectures, revision and progress", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
internal fun PlannerEntryAction(title: String, subtitle: String, icon: ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    val coral = Color(0xFFFF6555)
    val ink = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    SafarButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (primary) coral else MaterialTheme.colorScheme.surface, contentColor = ink),
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(16.dp)) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(if (primary) Color.White else Color(0xFF6255D9).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (primary) coral else Color(0xFF6255D9), modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = if (primary) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun PlannerExamEntry(title: String, subtitle: String, badge: String, upcoming: Boolean, active: Boolean,
    onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val green = if (MaterialTheme.colorScheme.background.red > 0.5f) Color(0xFF00685D) else Color(0xFF69DCC6)
    androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) green else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onOpen, style = ButtonStyle.Ghost, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).background(Color(0xFF6255D9).copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF8275EA), modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (badge.isNotBlank()) Text(badge, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (upcoming) green else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.background(if (upcoming) green.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
            SafarOverflowMenu(expanded, { expanded = it }, listOf(
                SafarMenuAction("Rename plan", onRename, Icons.Default.Edit),
                SafarMenuAction("Delete plan", onDelete, Icons.Default.Delete, destructive = true),
            )) {
                IconButton(onClick = { expanded = !expanded }, style = ButtonStyle.Ghost) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options for $title", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
