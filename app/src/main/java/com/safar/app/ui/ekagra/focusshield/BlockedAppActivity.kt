package com.safar.app.ui.ekagra.focusshield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.MainActivity
import com.safar.app.R
import com.safar.app.ui.theme.SafarTheme

class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_BEAST_MODE = "beast_mode"
        const val EXTRA_UNLOCKS_REMAINING = "unlocks_remaining"
        const val EXTRA_UNLOCK_SECONDS = "unlock_seconds"

        private val BLOCK_COPY = listOf(
            "One scroll can wait. Your future can't.",
            "You set this boundary. Honour it.",
            "Five more focused minutes change everything.",
            "Distractions are loud. Discipline is louder.",
            "Right now \u2014 the only goal is your goal.",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val appName = labelForPackage(blockedPackage)
        val strict = intent.getBooleanExtra(EXTRA_BEAST_MODE, false) ||
            FocusShieldRepository.ShieldPrefs.isStrict(this)
        val initialUnlocks = intent.getIntExtra(EXTRA_UNLOCKS_REMAINING, -1).let {
            if (it >= 0) it else FocusShieldRepository.ShieldPrefs.getUnlocksRemaining(this)
        }
        val unlockSeconds = intent.getIntExtra(EXTRA_UNLOCK_SECONDS, 0).let {
            if (it > 0) it else FocusShieldRepository.ShieldPrefs.getUnlockSeconds(this)
        }

        setContent {
            SafarTheme {
                BlockedAppScreen(
                    appName = appName,
                    motivationalCopy = remember { BLOCK_COPY.random() },
                    strict = strict,
                    initialUnlocksRemaining = initialUnlocks,
                    unlockSeconds = unlockSeconds,
                    onReturnToFocus = ::returnToFocus,
                    onEmergencyUnlock = ::handleEmergencyUnlock,
                )
            }
        }
    }

    private fun labelForPackage(packageName: String): String {
        if (packageName.isBlank()) return "This app"
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault("This app")
    }

    private fun returnToFocus() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                data = Uri.parse("safar://ekagra")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        finish()
    }

    private fun handleEmergencyUnlock(): Int? {
        if (FocusShieldRepository.ShieldPrefs.isStrict(this)) return null
        val limit = FocusShieldRepository.ShieldPrefs.getUnlockLimit(this)
        val used = FocusShieldRepository.ShieldPrefs.getUnlocksUsed(this)
        if (limit <= 0 || used >= limit) return 0

        val seconds = FocusShieldRepository.ShieldPrefs.getUnlockSeconds(this).coerceAtLeast(5)
        val graceUntilMs = System.currentTimeMillis() + seconds * 1000L
        val newUsed = used + 1
        FocusShieldRepository.ShieldPrefs.applyEmergencyUnlock(this, graceUntilMs, newUsed)
        finish()
        return (limit - newUsed).coerceAtLeast(0)
    }
}

@Composable
private fun BlockedAppScreen(
    appName: String,
    motivationalCopy: String,
    strict: Boolean,
    initialUnlocksRemaining: Int,
    unlockSeconds: Int,
    onReturnToFocus: () -> Unit,
    onEmergencyUnlock: () -> Int?,
) {
    BackHandler(onBack = onReturnToFocus)

    var unlocksRemaining by remember { mutableIntStateOf(initialUnlocksRemaining) }
    val showUnlock = !strict && initialUnlocksRemaining >= 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KavachDesign.Primary)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                "Stay focused",
                modifier = Modifier.padding(top = 22.dp),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                appName,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                motivationalCopy,
                modifier = Modifier.padding(top = 16.dp),
                color = KavachDesign.ActiveSessionStatus,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onReturnToFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = KavachDesign.Primary,
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.kavach_block_return), fontWeight = FontWeight.Bold)
            }

            if (showUnlock) {
                Spacer(Modifier.height(10.dp))
                val enabled = unlocksRemaining > 0
                OutlinedButton(
                    onClick = {
                        val result = onEmergencyUnlock()
                        if (result != null) unlocksRemaining = result
                        else unlocksRemaining = 0
                    },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = if (enabled) 0.35f else 0.2f),
                    ),
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (enabled) Color.White else KavachDesign.ActiveSessionStatus,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                                Text(
                                    if (enabled)
                                        stringResource(
                                            R.string.kavach_block_unlock,
                                            unlocksRemaining,
                                            unlockSeconds,
                                        )
                                    else
                                        stringResource(R.string.kavach_block_unlock_exhausted),
                        color = if (enabled) Color.White else KavachDesign.ActiveSessionStatus,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (strict) {
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.kavach_block_beast_footer),
                    color = Color(0xFFFECACA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}
