package com.safarparmar.app.feature.youtubestudyv2

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A polished "Beta (i)" badge with an interactive tooltip explaining
 * that YouTube Focus is currently undergoing active beta tuning and testing.
 */
@Composable
fun YoutubeFocusBetaBadge(
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    var tooltipExpanded by remember { mutableStateOf(false) }

    val purpleAccent = if (isLight) Color(0xFF6B21A8) else Color(0xFFC084FC)
    val badgeBg = if (isLight) Color(0xFFF3E8FF) else Color(0xFF6B21A8).copy(alpha = 0.22f)
    val badgeBorder = if (isLight) Color(0xFFD8B4FE).copy(alpha = 0.60f) else Color(0xFFC084FC).copy(alpha = 0.35f)
    val badgeShape = RoundedCornerShape(50)

    val surfaceBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF101014)
    val borderClr = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)
    val titleClr = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val bodyClr = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
    val dialogShape = RoundedCornerShape(16.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(badgeShape)
                .background(badgeBg)
                .border(BorderStroke(1.dp, badgeBorder), badgeShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { tooltipExpanded = true },
                )
                .padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        ) {
            Text(
                text = stringResource(R.string.common_beta),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = purpleAccent,
                letterSpacing = 0.2.sp,
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.youtube_focus_beta_about),
                tint = purpleAccent,
                modifier = Modifier.size(11.5.dp),
            )
        }

        DropdownMenu(
            expanded = tooltipExpanded,
            onDismissRequest = { tooltipExpanded = false },
            shape = dialogShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 285.dp)
                    .padding(4.dp),
                shape = dialogShape,
                color = surfaceBg,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, borderClr),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = purpleAccent,
                                modifier = Modifier.size(17.dp),
                            )
                            Text(
                                text = stringResource(R.string.youtube_focus_beta_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = titleClr,
                            )
                        }
                        IconButton(
                            onClick = { tooltipExpanded = false },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_close),
                                tint = bodyClr,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.youtube_focus_beta_body),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = bodyClr,
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { tooltipExpanded = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = purpleAccent.copy(alpha = if (isLight) 0.12f else 0.20f),
                                contentColor = purpleAccent,
                            ),
                            modifier = Modifier.height(28.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.common_got_it),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
