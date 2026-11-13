package com.safarparmar.app.feature.kavachanalytics.ui
import com.composables.ui.components.HorizontalSeparator as HorizontalDivider

import com.safarparmar.app.performance.decorativeFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.safarparmar.app.ui.components.popups.SafarDropdownMenu as DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.tutorial.tutorialTarget
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
    val pulseProgress by infiniteTransition.decorativeFloat(
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
    isDarkTheme: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
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

    KavachSummaryPillsContent(
        distractingSeconds = distractingSeconds, modifier = modifier, ink = ink,
        isDarkTheme = isDarkTheme, themeAccent = themeAccent, onOpenAnalytics = onOpenAnalytics,
        myCircles = myCircles, selectedCircle = selectedCircle, onSelectCircle = onSelectCircle,
        onNavigate = onNavigate,
    )
}

/** Presentation shared with isolated feature tutorials; no ViewModel or lifecycle work. */
@Composable
fun KavachSummaryPillsContent(
    distractingSeconds: Int,
    modifier: Modifier = Modifier,
    ink: com.safarparmar.app.ui.ekagra.EkagraInk? = null,
    isDarkTheme: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
    themeAccent: Color = MaterialTheme.colorScheme.primary,
    onOpenAnalytics: () -> Unit = {},
    myCircles: List<StudyCircleSummaryDto> = emptyList(),
    selectedCircle: StudyCircleSummaryDto? = null,
    onSelectCircle: (StudyCircleSummaryDto) -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Two aligned summaries; the circle gets more room for its name.
        SummaryPill(
            label = "Distracting",
            value = if (distractingSeconds > 0) {
                KavachAnalyticsFormat.duration(distractingSeconds)
            } else {
                "0m"
            },
            icon = Icons.Outlined.WarningAmber,
            modifier = Modifier.weight(.42f),
            onClick = onOpenAnalytics,
        )

        // Circle selection has a separate, discoverable dropdown action.
        StudyGroupLivePill(
            myCircles = myCircles,
            selectedCircle = selectedCircle,
            onSelectCircle = onSelectCircle,
            onNavigate = onNavigate,
            modifier = Modifier.weight(.58f).tutorialTarget("ekagra.circles"),
        )
    }
}
@Composable
private fun SummaryPill(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    com.composables.ui.components.Button(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = shape,
        contentColor = scheme.onSurface,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 68.dp)
            .background(scheme.surface, shape).border(1.dp, scheme.outlineVariant, shape),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            com.composables.ui.components.Text(label, color = scheme.onSurfaceVariant,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.composables.ui.components.Icon(icon, contentDescription = null,
                    tint = scheme.primary, modifier = Modifier.size(18.dp))
                com.composables.ui.components.Text(value, color = scheme.onSurface,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StudyGroupLivePill(
    myCircles: List<StudyCircleSummaryDto>,
    selectedCircle: StudyCircleSummaryDto?,
    onSelectCircle: (StudyCircleSummaryDto) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDropdown by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val activeCircle = selectedCircle ?: myCircles.firstOrNull()
    val groupName = activeCircle?.name ?: "Study Group"
    val liveCount = activeCircle?.focusingCount ?: 0
    val displayValue = if (myCircles.isEmpty()) {
        "Join Group"
    } else {
        if (liveCount == 1) "1 Live" else "$liveCount Live"
    }

    Box(modifier) {
        com.composables.ui.components.Button(
            onClick = {
                onNavigate(activeCircle?.let { Routes.STUDY_CIRCLE_DETAIL.replace("{circleId}", it.id) }
                    ?: Routes.STUDY_CIRCLES)
            },
            style = com.composables.ui.components.ButtonStyle.Ghost,
            shape = shape,
            contentColor = scheme.onSurface,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
                .background(scheme.surface, shape).border(1.dp, scheme.outlineVariant, shape),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.composables.ui.components.Text(groupName, color = scheme.onSurfaceVariant,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(end = if (myCircles.size > 1) 30.dp else 0.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // A small live indicator keeps the count as the visual focus.
                    TealLivePulseDot(size = 8, modifier = Modifier.size(14.dp))
                    com.composables.ui.components.Text(displayValue, color = scheme.onSurface,
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        if (myCircles.size > 1) {
            com.composables.ui.components.IconButton(
                onClick = { showDropdown = true },
                style = com.composables.ui.components.ButtonStyle.Ghost,
                contentColor = scheme.onSurface,
                modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
            ) {
                com.composables.ui.components.Icon(Icons.Default.ArrowDropDown,
                    contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.kavach_select_group),
                    tint = scheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }

        // Dropdown Menu when tapping & holding or selecting
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.kavach_your_study_groups, myCircles.size),
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
                                    contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_selected),
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
