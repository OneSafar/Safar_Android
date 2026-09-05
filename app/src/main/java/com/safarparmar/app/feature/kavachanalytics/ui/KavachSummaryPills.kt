package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground

@Composable
fun TealLivePulseDot(
    modifier: Modifier = Modifier,
    size: Int = 18,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tealLiveDotPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tealPulseProgress"
    )

    val tealGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF5EEAD4), Color(0xFF2DD4BF), Color(0xFF14B8A6))
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = (size.dp.toPx() / 2f)
            val maxExtra = 6.dp.toPx()
            val extraRadius = maxExtra * pulseProgress
            val strokeWidth = (2.dp.toPx() * (1f - pulseProgress)).coerceAtLeast(0f)

            if (strokeWidth > 0.1f) {
                drawCircle(
                    brush = tealGradient,
                    radius = baseRadius + extraRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                    alpha = (1f - pulseProgress).coerceIn(0f, 1f),
                )
            }
            drawCircle(
                brush = tealGradient,
                radius = baseRadius,
                center = center,
            )
        }
    }
}

@Composable
fun KavachSummaryPills(
    modifier: Modifier = Modifier,
    ink: com.safarparmar.app.ui.ekagra.EkagraInk? = null,
    themeAccent: Color = MaterialTheme.colorScheme.primary,
    onOpenAnalytics: () -> Unit = {},
    myCircles: List<StudyCircleSummaryDto> = emptyList(),
    selectedCircle: StudyCircleSummaryDto? = null,
    onSelectCircle: (StudyCircleSummaryDto) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.refresh()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val report = state.report
    val distractingSeconds = report?.allDay?.distractingSeconds ?: 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Left Box: Distracting (Red warning icon, 3x size)
        SummaryPill(
            label = "Distracting",
            value = if (distractingSeconds > 0) {
                KavachAnalyticsFormat.duration(distractingSeconds)
            } else {
                "0m"
            },
            icon = Icons.Outlined.WarningAmber,
            accent = themeAccent,
            iconColor = Color(0xFFEF4444),
            ink = ink,
            isLight = isLight,
            modifier = Modifier.weight(1f),
            onClick = onOpenAnalytics,
        )

        // Right Box: Study Group Live Focus Box (Light Teal pulsating dot, 3x size)
        StudyGroupLivePill(
            myCircles = myCircles,
            selectedCircle = selectedCircle,
            onSelectCircle = onSelectCircle,
            onNavigate = onNavigate,
            accent = themeAccent,
            ink = ink,
            isLight = isLight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    iconColor: Color,
    ink: com.safarparmar.app.ui.ekagra.EkagraInk?,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(20.dp)
    val cardColor by animateColorAsState(
        targetValue = when {
            isPressed -> accent.copy(alpha = 0.90f)
            else -> accent
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 600f),
        label = "summaryChipColor",
    )
    val borderColor = Color.White.copy(alpha = 0.35f)
    val textColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.82f)

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(cardColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 3x Larger Icon in Red
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 3 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 3 })
                    },
                    label = "summaryChipValue",
                ) { animatedValue ->
                    Text(
                        text = animatedValue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudyGroupLivePill(
    myCircles: List<StudyCircleSummaryDto>,
    selectedCircle: StudyCircleSummaryDto?,
    onSelectCircle: (StudyCircleSummaryDto) -> Unit,
    onNavigate: (String) -> Unit,
    accent: Color,
    ink: com.safarparmar.app.ui.ekagra.EkagraInk?,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)

    val cardColor by animateColorAsState(
        targetValue = when {
            isPressed -> accent.copy(alpha = 0.90f)
            else -> accent
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 600f),
        label = "groupLiveCardColor",
    )
    val borderColor = Color.White.copy(alpha = 0.35f)
    val textColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.85f)

    val activeCircle = selectedCircle ?: myCircles.firstOrNull()
    val groupName = activeCircle?.name ?: "Study Group"
    val liveCount = activeCircle?.focusingCount ?: 0
    val displayValue = if (myCircles.isEmpty()) {
        "Join Group"
    } else {
        if (liveCount == 1) "1 Live" else "$liveCount Live"
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(cardColor)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (activeCircle != null) {
                        onNavigate(Routes.STUDY_CIRCLE_DETAIL.replace("{circleId}", activeCircle.id))
                    } else {
                        onNavigate(Routes.STUDY_CIRCLES)
                    }
                },
                onLongClick = {
                    showDropdown = true
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 3x Larger Light Teal Pulsating Live Dot
            TealLivePulseDot(size = 18, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = groupName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (myCircles.size > 1) {
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Group",
                            tint = secondaryColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                AnimatedContent(
                    targetState = displayValue,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 3 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 3 })
                    },
                    label = "groupLiveValue",
                ) { animatedValue ->
                    Text(
                        text = animatedValue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }

        // Dropdown Menu when tapping & holding or selecting
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
        ) {
            Text(
                text = "YOUR STUDY GROUPS (${myCircles.size}/5)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

            if (myCircles.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No groups joined yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    onClick = {
                        showDropdown = false
                        onNavigate(Routes.STUDY_CIRCLES)
                    },
                )
            } else {
                myCircles.forEach { circle ->
                    val isSelected = circle.id == (selectedCircle?.id ?: myCircles.firstOrNull()?.id)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = circle.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    TealLivePulseDot(size = 8, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${circle.focusingCount} live · ${circle.memberCount} members",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                if (circle.visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        onClick = {
                            onSelectCircle(circle)
                            showDropdown = false
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = {
                    Text(
                        "Explore Study Circles",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                onClick = {
                    showDropdown = false
                    onNavigate(Routes.STUDY_CIRCLES)
                },
            )
        }
    }
}
