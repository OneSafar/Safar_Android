package com.safarparmar.app.ui.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.data.remote.maintenance.AppUpdateInfo
import com.safarparmar.app.ui.theme.SafarSemanticColors

@Composable
fun ForceUpdateScreen(info: AppUpdateInfo, modifier: Modifier = Modifier) {
    BackHandler(enabled = true) { }
    val context = LocalContext.current
    val accent = SafarSemanticColors.brandPurple()
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(104.dp).background(accent.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(72.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SystemUpdateAlt, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.size(28.dp))
        Text(info.title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.size(10.dp))
        Text(info.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
        info.latestVersionName?.let { Spacer(Modifier.size(10.dp)); Text("Latest version: $it", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.size(30.dp))
        Button(onClick = { openPlayStore(context, info.playStoreUrl) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
            Text("Update Safar", modifier = Modifier.padding(vertical = 7.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Text("This update is required to continue.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private fun openPlayStore(context: Context, webUrl: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
