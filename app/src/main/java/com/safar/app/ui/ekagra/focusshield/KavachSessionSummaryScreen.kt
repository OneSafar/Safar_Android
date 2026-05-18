package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.safar.app.R

data class KavachBlockedAttempt(
    val appName: String,
    val attemptCount: Int,
    val icon: android.graphics.drawable.Drawable? = null,
)

@Composable
fun KavachSessionSummaryScreen(
    focusedMinutes: Int,
    blockedAttempts: List<KavachBlockedAttempt>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    Scaffold(
        containerColor = KavachDesign.Background,
        topBar = {
            KavachStitchBackHeader(
                onBack = onBack,
                title = stringResource(R.string.kavach_session_summary_title),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KavachDesign.Background.copy(alpha = 0f),
                                KavachDesign.Background,
                                KavachDesign.Background,
                            ),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                KavachStitchPrimaryButton(
                    text = stringResource(R.string.kavach_session_summary_done),
                    onClick = onDone,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(KavachDesign.SurfaceHighlight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = KavachDesign.Primary,
                    modifier = Modifier.size(72.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.kavach_session_summary_heading),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KavachDesign.TextMain,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.kavach_session_summary_subtitle),
                fontSize = 16.sp,
                color = KavachDesign.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f),
            )

            Spacer(Modifier.height(24.dp))

            KavachStitchSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.kavach_session_time_focused_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = KavachDesign.TextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$focusedMinutes",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = KavachDesign.Primary,
                        )
                        Text(
                            text = stringResource(R.string.kavach_session_minutes_unit),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = KavachDesign.Primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(KavachDesign.CardWhite)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = KavachDesign.SuccessText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.kavach_session_deep_work),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = KavachDesign.TextMain,
                        )
                    }
                }
            }

            if (blockedAttempts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                KavachStitchSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = KavachDesign.TextMuted,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.kavach_session_blocked_attempts),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KavachDesign.TextMain,
                            )
                        }

                        blockedAttempts.forEach { attempt ->
                            KavachBlockedAttemptRow(attempt)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun KavachBlockedAttemptRow(attempt: KavachBlockedAttempt) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KavachDesign.CardWhite)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KavachDesign.SurfaceHighlight),
                contentAlignment = Alignment.Center,
            ) {
                val drawable = attempt.icon
                if (drawable != null) {
                    androidx.compose.foundation.Image(
                        bitmap = drawable.toBitmap(80, 80).asImageBitmap(),
                        contentDescription = attempt.appName,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Text(
                        text = attempt.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = KavachDesign.Primary,
                    )
                }
            }
            Text(
                text = attempt.appName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = KavachDesign.TextMain,
            )
        }
        Text(
            text = stringResource(R.string.kavach_session_attempts_count, attempt.attemptCount),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KavachDesign.Surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMuted,
        )
    }
}
