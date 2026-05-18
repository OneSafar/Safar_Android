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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.safar.app.R

@Composable
fun KavachAboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasUsage by remember { mutableStateOf(FocusShieldPermissionHelper.hasUsageStatsPermission(context)) }
    var hasA11y by remember { mutableStateOf(FocusShieldPermissionHelper.hasAccessibilityService(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasA11y = FocusShieldPermissionHelper.hasAccessibilityService(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KavachDesign.Background),
    ) {
        KavachStitchBackHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(KavachDesign.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = KavachDesign.Primary,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.kavach_about_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KavachDesign.TextMain,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.kavach_about_description),
                fontSize = 16.sp,
                color = KavachDesign.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth(0.88f),
            )

            Spacer(Modifier.height(32.dp))

            KavachStitchSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = KavachDesign.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.kavach_about_privacy_card),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = KavachDesign.TextMuted,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.kavach_info_permissions_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KavachDesign.TextMain,
                )

                Spacer(Modifier.height(16.dp))

                KavachAboutPermissionRow(
                    title = stringResource(R.string.kavach_permission_usage_title),
                    granted = hasUsage,
                )
                HorizontalDivider(color = KavachDesign.Surface, thickness = 1.dp)
                KavachAboutPermissionRow(
                    title = stringResource(R.string.kavach_permission_accessibility_title),
                    granted = hasA11y,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun KavachAboutPermissionRow(
    title: String,
    granted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = KavachDesign.TextMain,
            modifier = Modifier.weight(1f),
        )
        KavachStitchStatusChip(granted = granted)
    }
}
