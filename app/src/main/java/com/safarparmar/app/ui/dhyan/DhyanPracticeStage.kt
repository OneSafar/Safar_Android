package com.safarparmar.app.ui.dhyan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.*

/** Pure presentation: no BreathingTab, MediaPlayer, countdown, preferences, or session tracking. */
@Composable
internal fun DhyanPracticeStage(scene: PracticeScene, dark: Boolean) {
    Box(Modifier.fillMaxSize()) {
        DhyanMockBackdrop()
        Column(Modifier.fillMaxSize()) {
            DhyanTopNavigation(DhyanTab.DHYAN, {}, Modifier.fillMaxWidth().padding(16.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DhyanMeditationOrb(dark) {
                    Image(painterResource(R.drawable.meditation_transparent_background), stringResource(R.string.dhyan_meditate), Modifier.fillMaxSize(.88f))
                }
                Text(if (scene == PracticeScene.DHYAN_PAUSE || scene == PracticeScene.DHYAN_RESET) "04:30" else "05:00", fontSize = 52.sp, color = DhyanFlatColors.Text)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dhyan_session_length), color = DhyanFlatColors.Text)
                    Text(stringResource(R.string.common_minutes_short, 5), color = DhyanFlatColors.Text)
                }
                DhyanSessionSlider(5f, {}, dark, Modifier.fillMaxWidth().tutorialTarget("dhyan.length"))
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.tutorialTarget("dhyan.reset")) { DhyanControlButton(Icons.Default.Refresh, dark, {}, 48.dp, DhyanControlStyle.Reset, stringResource(R.string.common_reset)) }
                    Box(Modifier.tutorialTarget(if (scene == PracticeScene.DHYAN_PAUSE) "dhyan.pause" else "dhyan.start")) {
                        DhyanControlButton(if (scene == PracticeScene.DHYAN_PAUSE) Icons.Default.Pause else Icons.Default.PlayArrow, dark, {}, 72.dp, DhyanControlStyle.Play,
                            stringResource(if (scene == PracticeScene.DHYAN_PAUSE) R.string.common_pause else R.string.common_start))
                    }
                    Box(Modifier.tutorialTarget("dhyan.volume")) { DhyanControlButton(if (scene == PracticeScene.DHYAN_VOLUME) Icons.Default.VolumeOff else Icons.Default.VolumeUp, dark, {}, 48.dp, DhyanControlStyle.Volume, stringResource(R.string.common_mute)) }
                }
                Spacer(Modifier.height(22.dp))
                DhyanGuidanceSheet(dark, null, {}, {})
            }
        }
    }
}
