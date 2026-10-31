package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.*
import com.safarparmar.app.R

@Composable
fun KavachActiveSessionScreen(secondsLeft: Int, blockedCount: Int, onBack: () -> Unit,
    onEndSession: () -> Unit, modifier: Modifier = Modifier) {
    KavachCommandTheme {
        Column(modifier.fillMaxSize().background(KavachDesign.Background).safeDrawingPadding()) {
            Column(Modifier.weight(1f).fillMaxWidth().background(KavachDesign.PrimaryDark,
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, style = ButtonStyle.Ghost, contentColor = Color.White) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_previous), tint = Color.White)
                    }
                    Text(stringResource(R.string.nav_focus_shield), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(100.dp).background(Color(0xFFFFCBBE), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = KavachDesign.PrimaryDark, modifier = Modifier.size(58.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.kavach_command_protected), color = Color.White,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("%02d:%02d".format(secondsLeft.coerceAtLeast(0) / 60, secondsLeft.coerceAtLeast(0) % 60),
                    color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 48.sp)
                Spacer(Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.kavach_active_status, blockedCount), color = KavachDesign.SuccessText,
                    modifier = Modifier.background(KavachDesign.SuccessBg, CircleShape).padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.kavach_command_session_body), color = KavachDesign.TextMuted, textAlign = TextAlign.Center)
                Button(onClick = onEndSession, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.kavach_end_session), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
