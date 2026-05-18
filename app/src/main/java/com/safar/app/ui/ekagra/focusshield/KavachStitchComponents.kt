package com.safar.app.ui.ekagra.focusshield

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachStitchBackHeader(
    onBack: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    backTint: Color = KavachDesign.TextMain,
) {
    if (title == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_previous),
                    tint = backTint,
                )
            }
        }
    } else {
        TopAppBar(
            modifier = modifier.height(56.dp),
            title = {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.TextMain,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_previous),
                        tint = KavachDesign.TextMain,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = KavachDesign.Background),
        )
    }
}

@Composable
fun KavachStitchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KavachDesign.Primary,
            contentColor = Color.White,
            disabledContainerColor = KavachDesign.Primary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun KavachStitchStatusChip(
    granted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (granted) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KavachDesign.SuccessBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = KavachDesign.SuccessText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.kavach_permission_granted),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = KavachDesign.SuccessText,
            )
        }
    } else {
        Text(
            text = stringResource(R.string.kavach_permission_required),
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KavachDesign.Primary)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
fun KavachStitchPillToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 2f,
        label = "pill_thumb",
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) KavachDesign.Primary else KavachDesign.SearchFieldBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color.Black.copy(alpha = 0.04f), CircleShape),
        )
    }
}

@Composable
fun KavachStitchSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(KavachDesign.Surface),
    ) {
        content()
    }
}
