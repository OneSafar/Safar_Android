package com.safarparmar.app.ui.studycircle

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.data.remote.dto.PublicStudyCircleDto
import com.safarparmar.app.data.remote.dto.StudyCircleDetailDto
import com.safarparmar.app.data.remote.dto.StudyCircleLeaderboardEntryDto
import com.safarparmar.app.data.remote.dto.StudyCircleMemberDto
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CircleIndigo: Color
    @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFFC084FC)
    } else {
        Color(0xFF581C87)
    }
private val CircleAmber = Color(0xFFF59E0B)

val RoyalPurpleLight = Color(0xFFA855F7)
val RoyalPurpleMid   = Color(0xFF7C3AED)
val RoyalPurpleDark  = Color(0xFF581C87)

val RoyalPurpleGradient = Brush.linearGradient(
    colors = listOf(RoyalPurpleLight, RoyalPurpleMid, RoyalPurpleDark)
)

@Composable
fun LivePulsingAvatarRing(
    isLive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!isLive) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "avatarLiveRing")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatarPulseProgress"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.padding(3.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = size.minDimension / 2f
            val extraRadius = 4.dp.toPx() * pulseProgress
            val strokeWidth = (2.dp.toPx() * (1f - pulseProgress)).coerceAtLeast(0f)

            if (strokeWidth > 0.1f) {
                drawCircle(
                    brush = RoyalPurpleGradient,
                    radius = baseRadius + extraRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                    alpha = (1f - pulseProgress).coerceIn(0f, 1f)
                )
            }
            // Inner crisp accent ring
            drawCircle(
                brush = RoyalPurpleGradient,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
                alpha = 0.9f
            )
        }
        content()
    }
}

@Composable
fun LivePulseDot(modifier: Modifier = Modifier, size: Int = 8) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveDotPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPulseProgress"
    )

    Box(
        modifier = modifier.size((size + 6).dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = (size.dp.toPx() / 2f)
            val extraRadius = 3.dp.toPx() * pulseProgress
            val strokeWidth = (1.5.dp.toPx() * (1f - pulseProgress)).coerceAtLeast(0f)

            if (strokeWidth > 0.1f) {
                drawCircle(
                    brush = RoyalPurpleGradient,
                    radius = baseRadius + extraRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                    alpha = (1f - pulseProgress).coerceIn(0f, 1f),
                )
            }
            drawCircle(
                brush = RoyalPurpleGradient,
                radius = baseRadius,
                center = center,
            )
        }
    }
}

@Composable
fun LiveFocusingBadge(modifier: Modifier = Modifier) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Surface(
        modifier = modifier,
        color = (if (isLight) RoyalPurpleDark else RoyalPurpleLight).copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, (if (isLight) RoyalPurpleDark else RoyalPurpleLight).copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LivePulseDot(size = 6)
            Text(
                "FOCUSING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (isLight) RoyalPurpleDark else RoyalPurpleLight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCircleScreen(
    currentRoute: String = Routes.STUDY_CIRCLES,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: StudyCircleViewModel = hiltViewModel(),
) {
    val state by viewModel.hub.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CircleDialog?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(CircleListTab.All) }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    SafarDrawerScaffold(
        title = "Study Circle",
        subtitle = null,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        topBarActions = {
            IconButton(onClick = { viewModel.loadHub(refresh = true) }, enabled = !state.refreshing) {
                if (state.refreshing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, "Refresh circles")
            }
        },
        useGlassTopBar = false,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val atLimit = state.circles.size >= 5
        val joinedIds = remember(state.circles) { state.circles.mapTo(mutableSetOf()) { it.id } }
        val availablePublicCircles = remember(state.publicCircles, joinedIds) {
            state.publicCircles.filterNot { it.id in joinedIds }
        }
        var searchQuery by rememberSaveable { mutableStateOf("") }
        val filteredCircles = remember(state.circles, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) state.circles
            else state.circles.filter { it.name.lowercase().contains(q) }
        }
        val filteredPublicCircles = remember(availablePublicCircles, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) availablePublicCircles
            else availablePublicCircles.filter { it.name.lowercase().contains(q) }
        }
        val allCount = state.circles.size + availablePublicCircles.size
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StudyCircleTopButtons(
                    atLimit = atLimit,
                    onJoin = { dialog = CircleDialog.Join },
                    onCreate = { dialog = CircleDialog.Create },
                )
            }
            item { CircleLimitInfoRow(state.circles.size, atLimit) }
            item {
                CircleTabsHeader(
                    selectedTab = selectedTab,
                    allCount = allCount,
                    yoursCount = state.circles.size,
                    onSelectTab = { selectedTab = it },
                )
            }
            if (selectedTab == CircleListTab.All) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search public groups by name", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedBorderColor = CircleIndigo,
                        ),
                    )
                }
            }
            if (state.loading) {
                item { CircleLoadingState() }
            } else if (state.error != null) {
                item { CircleErrorState(state.error.orEmpty(), viewModel::loadHub) }
            } else {
                if (selectedTab == CircleListTab.All) {
                    if (allCount == 0) {
                        item { DashedStyleEmptyCard("No circles are available yet.") }
                    } else if (searchQuery.isNotBlank() && filteredCircles.isEmpty() && filteredPublicCircles.isEmpty()) {
                        item { DashedStyleEmptyCard("No circles match \"$searchQuery\".") }
                    } else {
                        items(filteredCircles, key = { "joined-${it.id}" }) { circle ->
                            MyCircleCard(circle) { onNavigate(Routes.studyCircleDetail(circle.id)) }
                        }
                        items(filteredPublicCircles, key = { "available-${it.id}" }) { circle ->
                            PublicCircleCard(
                                circle = circle,
                                enabled = !atLimit,
                                busy = state.busyId == circle.id,
                                onClick = { viewModel.joinPublic(circle) { onNavigate(Routes.studyCircleDetail(it)) } },
                            )
                        }
                    }
                } else {
                    if (state.circles.isEmpty()) item { DashedStyleEmptyCard("You have not joined a circle yet.") }
                    items(state.circles, key = { "yours-${it.id}" }) { circle ->
                        MyCircleCard(circle) { onNavigate(Routes.studyCircleDetail(circle.id)) }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    dialog?.let { mode ->
        StudyCircleInputDialog(
            mode = mode,
            busy = state.busyId != null,
            onDismiss = { dialog = null },
            onSubmit = { value, visibility ->
                val open: (String) -> Unit = { id -> dialog = null; onNavigate(Routes.studyCircleDetail(id)) }
                if (mode == CircleDialog.Create) viewModel.createCircle(value, visibility, open)
                else viewModel.joinWithCode(value, open)
            },
        )
    }
}

private enum class CircleDialog { Create, Join }

private enum class CircleListTab { All, Yours }

@Composable
private fun StudyCircleTopButtons(
    atLimit: Boolean,
    onJoin: () -> Unit,
    onCreate: () -> Unit,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val primaryBg = if (isLight) Color(0xFF581C87) else Color(0xFF6B21A8)
    val outlineClr = CircleIndigo

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onJoin,
            enabled = !atLimit,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, outlineClr),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = outlineClr,
                disabledContentColor = outlineClr.copy(alpha = 0.38f),
            ),
        ) {
            Icon(Icons.Default.Key, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Join with code", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Button(
            onClick = onCreate,
            enabled = !atLimit,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBg,
                contentColor = Color.White,
                disabledContainerColor = primaryBg.copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.38f),
            ),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create circle", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CircleLimitInfoRow(count: Int, atLimit: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (atLimit) "You're in 5 of 5 circles. Private circles hold up to 100 members."
            else "You're in $count of 5 circles. Private circles hold up to 100 members.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun CircleTabsHeader(
    selectedTab: CircleListTab,
    allCount: Int,
    yoursCount: Int,
    onSelectTab: (CircleListTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isAll = selectedTab == CircleListTab.All
        Column(
            modifier = Modifier.clickable { onSelectTab(CircleListTab.All) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "All $allCount",
                fontSize = 15.sp,
                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                color = if (isAll) CircleIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(44.dp)
                    .height(2.5.dp)
                    .background(if (isAll) CircleIndigo else Color.Transparent, RoundedCornerShape(2.dp))
            )
        }

        val isYours = selectedTab == CircleListTab.Yours
        Column(
            modifier = Modifier.clickable { onSelectTab(CircleListTab.Yours) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Yours $yoursCount",
                fontSize = 15.sp,
                fontWeight = if (isYours) FontWeight.Bold else FontWeight.Medium,
                color = if (isYours) CircleIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(44.dp)
                    .height(2.5.dp)
                    .background(if (isYours) CircleIndigo else Color.Transparent, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun MyCircleCard(circle: StudyCircleSummaryDto, onClick: () -> Unit) {
    FlatCircleCard(Modifier.clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircleIcon(public = circle.visibility == "public", size = 46)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val memberText = if (circle.visibility == "public") {
                    "${circle.memberCount} members, public"
                } else {
                    "${circle.memberCount} of ${circle.maxMembers ?: 100} members, private"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(memberText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    if (circle.focusingCount > 0) {
                        Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        val isLight = MaterialTheme.colorScheme.background.isLightBackground()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(size = 6)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${circle.focusingCount} live",
                                color = if (isLight) RoyalPurpleDark else RoyalPurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (circle.role == "owner") {
                OwnerPillBadge()
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open circle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PublicCircleCard(circle: PublicStudyCircleDto, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    FlatCircleCard(
        modifier = Modifier.clickable(enabled = enabled && !busy, onClick = onClick),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircleIcon(public = true, size = 46)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val subText = if (circle.joined) {
                    "${circle.memberCount} members, public"
                } else {
                    "Created by ${circle.ownerName}, ${circle.memberCount} ${if (circle.memberCount == 1) "member" else "members"}"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(subText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (circle.focusingCount > 0) {
                        Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        val isLight = MaterialTheme.colorScheme.background.isLightBackground()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(size = 6)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${circle.focusingCount} live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) RoyalPurpleDark else RoyalPurpleLight,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (circle.joined) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open circle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled && !busy,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, CircleIndigo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CircleIndigo),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = CircleIndigo)
                    else Text("Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun OwnerPillBadge() {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Surface(
        color = if (isLight) Color(0xFFFEF3C7) else Color(0xFF3B2E10),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "OWNER",
            color = if (isLight) Color(0xFFB45309) else Color(0xFFFDE68A),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun FlatCircleCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp),
    ) { Column(Modifier.fillMaxWidth().padding(contentPadding), content = content) }
}

@Composable
private fun CircleIcon(public: Boolean, size: Int = 46) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(CircleIndigo.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (public) Icons.Default.Public else Icons.Default.Lock, null, tint = CircleIndigo, modifier = Modifier.size((size * .44f).dp))
    }
}

@Composable
private fun CircleLoadingState() = com.safarparmar.app.ui.components.StudyCircleSkeleton()

@Composable
private fun CircleErrorState(message: String, retry: () -> Unit) = FlatCircleCard {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(10.dp)); Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = retry) { Text("Try again") }
    }
}

@Composable
private fun DashedStyleEmptyCard(text: String) = Surface(
    modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
    shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) { Text(text, Modifier.padding(28.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }

@Composable
private fun StudyCircleInputDialog(mode: CircleDialog, busy: Boolean, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var value by remember(mode) { mutableStateOf("") }
    var visibility by remember(mode) { mutableStateOf("private") }
    val isCreate = mode == CircleDialog.Create
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(if (isCreate) Icons.Default.GroupAdd else Icons.Default.Key, null, tint = CircleIndigo) },
        title = { Text(if (isCreate) "Create a Study Circle" else "Join a Study Circle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (isCreate) "Choose a simple name and decide who can join." else "Enter the six-character code shared by a private circle owner.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = if (isCreate) it.take(50) else it.uppercase().filter(Char::isLetterOrDigit).take(6)
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(if (isCreate) "Circle name" else "Join code") },
                    placeholder = { Text(if (isCreate) "JEE Morning Group" else "ABC234") },
                    keyboardOptions = KeyboardOptions(capitalization = if (isCreate) KeyboardCapitalization.Sentences else KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(14.dp),
                )
                if (isCreate) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisibilityChoice("private", visibility, Modifier.weight(1f)) { visibility = it }
                        VisibilityChoice("public", visibility, Modifier.weight(1f)) { visibility = it }
                    }
                    Text(if (visibility == "public") "Anyone signed in to Safar can find and join." else "Only people with the join code can join.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            val isLight = MaterialTheme.colorScheme.background.isLightBackground()
            val btnBg = if (isLight) Color(0xFF581C87) else Color(0xFF6B21A8)
            Button(
                onClick = { onSubmit(value.trim(), visibility) },
                enabled = !busy && value.trim().length >= (if (isCreate) 3 else 6),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnBg,
                    contentColor = Color.White,
                    disabledContainerColor = btnBg.copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.38f),
                ),
            ) { if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Color.White) else Text(if (isCreate) "Create" else "Join") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
        shape = RoundedCornerShape(24.dp),
    )
}

@Composable
private fun VisibilityChoice(value: String, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    FilterChip(
        selected = value == selected, onClick = { onSelect(value) }, modifier = modifier,
        label = { Text(value.replaceFirstChar { it.uppercase() }) },
        leadingIcon = { Icon(if (value == "public") Icons.Default.Public else Icons.Default.Lock, null, Modifier.size(16.dp)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCircleDetailScreen(
    circleId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: StudyCircleViewModel = hiltViewModel(),
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val mehfilDm by viewModel.mehfilDm.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var renameDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(circleId) { viewModel.loadDetail(circleId) }
    LaunchedEffect(circleId) { while (true) { delay(30_000); viewModel.loadDetail(circleId, refresh = true) } }
    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.consumeMessage() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Study Circle", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDetail(circleId, refresh = true) }, enabled = !state.refreshing) {
                        if (state.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, "Refresh")
                    }
                    // Owner-only: Edit name / Delete / Leave in the top overflow menu.
                    // Normal members leave via the ⋯ button on their own member card.
                    if (state.circle?.role == "owner") {
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit group name") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = CircleIndigo,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        renameDialogOpen = true
                                    },
                                    enabled = !state.actionInProgress,
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        confirm = ConfirmAction.Delete
                                    },
                                    enabled = !state.actionInProgress,
                                )
                                DropdownMenuItem(
                                    text = { Text("Leave circle", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        confirm = ConfirmAction.Leave
                                    },
                                    enabled = !state.actionInProgress,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.loading -> com.safarparmar.app.ui.components.StudyCircleSkeleton(Modifier.padding(padding))
            state.error != null || state.circle == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Circle not found", textAlign = TextAlign.Center); Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.loadDetail(circleId) }) { Text("Try again") }
                }
            }
            else -> DetailContent(
                circle = state.circle!!,
                entries = state.leaderboard?.entries.orEmpty(),
                padding = padding,
                busy = state.actionInProgress,
                currentUserId = currentUserId.orEmpty(),
                hasDmAccess = mehfilDm || isPremium,
                onEditName = { renameDialogOpen = true },
                onToggleVisibility = viewModel::toggleVisibility,
                onConnectMember = { member ->
                    val circleName = state.circle?.name.orEmpty()
                    viewModel.connectWithMember(
                        targetUserId = member.userId,
                        targetUserName = member.name,
                        circleName = circleName,
                        onRequirePremium = { onNavigate(Routes.MEHFIL) },
                        onConnected = { onNavigate(Routes.MEHFIL) },
                    )
                },
                onRemove = { userId, name -> confirm = ConfirmAction.Remove(userId, name) },
                onLeave = { confirm = ConfirmAction.Leave },
            )
        }
    }

    if (renameDialogOpen && state.circle != null) {
        val originalName = state.circle!!.name
        var newName by rememberSaveable(originalName) { mutableStateOf(originalName) }
        val canSave = newName.trim().length in 3..50 && newName.trim() != originalName
        val canUndo = newName != originalName

        AlertDialog(
            onDismissRequest = { if (!state.actionInProgress) renameDialogOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Edit Group Name", fontWeight = FontWeight.Bold)
                    if (canUndo) {
                        TextButton(
                            onClick = { newName = originalName },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a new name for your study group (3–50 characters).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 50) newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. UPSC Champions 2026") },
                        trailingIcon = {
                            if (newName.isNotBlank()) {
                                IconButton(onClick = { newName = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                        supportingText = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("${newName.trim().length}/50", fontSize = 11.sp)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedName = newName.trim()
                        renameDialogOpen = false
                        viewModel.renameCircle(savedName) { previousName ->
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Renamed to \"$savedName\"",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.renameCircle(previousName)
                                }
                            }
                        }
                    },
                    enabled = canSave && !state.actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = CircleIndigo),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { renameDialogOpen = false },
                    enabled = !state.actionInProgress,
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    confirm?.let { action ->
        val circle = state.circle ?: return@let
        val title = when (action) {
            ConfirmAction.Delete -> "Delete Study Group?"
            is ConfirmAction.Remove -> "Remove ${action.name}?"
            ConfirmAction.Leave -> when { circle.memberCount == 1 -> "Leave and archive this circle?"; circle.role == "owner" -> "Leave and transfer ownership?"; else -> "Leave this circle?" }
        }
        val description = when (action) {
            ConfirmAction.Delete -> "Are you sure you want to delete \"${circle.name}\"? All members will be removed and this action cannot be undone."
            is ConfirmAction.Remove -> "This person will lose access to the member list and leaderboard."
            ConfirmAction.Leave -> when { circle.memberCount == 1 -> "You are the last member, so the circle will be archived."; circle.role == "owner" -> "The oldest active member will become the new owner."; else -> "You will lose access to this circle and its leaderboard." }
        }
        AlertDialog(
            onDismissRequest = { if (!state.actionInProgress) confirm = null },
            title = { Text(title, fontWeight = FontWeight.Bold) }, text = { Text(description) },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            ConfirmAction.Delete -> viewModel.deleteCircle { confirm = null; onBack() }
                            is ConfirmAction.Remove -> { viewModel.removeMember(action.userId, action.name); confirm = null }
                            ConfirmAction.Leave -> viewModel.leaveCircle { confirm = null; onBack() }
                        }
                    },
                    enabled = !state.actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(
                        when (action) {
                            ConfirmAction.Delete -> "Delete group"
                            is ConfirmAction.Remove -> "Remove member"
                            ConfirmAction.Leave -> "Leave circle"
                        }
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }, enabled = !state.actionInProgress) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

private sealed interface ConfirmAction {
    data object Leave : ConfirmAction
    data object Delete : ConfirmAction
    data class Remove(val userId: String, val name: String) : ConfirmAction
}

@Composable
private fun DetailContent(
    circle: StudyCircleDetailDto,
    entries: List<StudyCircleLeaderboardEntryDto>,
    padding: PaddingValues,
    busy: Boolean,
    currentUserId: String,
    hasDmAccess: Boolean,
    onEditName: () -> Unit,
    onToggleVisibility: () -> Unit,
    onConnectMember: (StudyCircleMemberDto) -> Unit,
    onRemove: (String, String) -> Unit,
    onLeave: () -> Unit,
) {
    val entryByUser = remember(entries) { entries.associateBy { it.userId } }
    val sortedMembers = remember(circle.members, entryByUser) {
        circle.members.sortedBy { member ->
            entryByUser[member.userId]?.rank?.takeIf { it > 0 } ?: 999
        }
    }
    val ownerName = remember(circle) { circle.members.firstOrNull { it.role == "owner" }?.name ?: "Organizer" }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val owner = circle.role == "owner"
    val joinCode = circle.joinCode
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            FlatCircleCard(contentPadding = PaddingValues(16.dp)) {
                // Top row with Avatar, Title, Rename, and Owner Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CircleIndigo.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = circle.name.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CircleIndigo,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = circle.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        if (owner) {
                            Text(
                                text = "Tap to rename",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(enabled = !busy, onClick = onEditName),
                            )
                        } else {
                            Text(
                                text = "Created by $ownerName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (owner) {
                        val isLight = MaterialTheme.colorScheme.background.isLightBackground()
                        Surface(
                            color = if (isLight) Color(0xFFFEF3C7) else Color(0xFF3B2E10),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (isLight) Color(0xFFFDE68A) else Color(0xFF78350F)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Star, null, tint = CircleAmber, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "OWNER",
                                    color = if (isLight) Color(0xFFB45309) else Color(0xFFFDE68A),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Spacer(Modifier.height(10.dp))

                // Stats row with divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Group, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        val memberCountDisplay = if (circle.visibility == "public") {
                            "${circle.memberCount} members"
                        } else {
                            "${circle.memberCount}/${circle.maxMembers ?: 100} members"
                        }
                        Text(memberCountDisplay, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }

                    Box(
                        Modifier
                            .height(16.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Icon(
                            if (circle.visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (circle.visibility == "public") "Public circle" else "Private, needs code",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Live status row (only if someone is focusing)
                if (circle.focusingCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulseDot(size = 8)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (circle.focusingCount == 1) "1 member focusing right now" else "${circle.focusingCount} members focusing right now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CircleIndigo,
                        )
                    }
                }

                // Every active member of a private circle can share its invite code.
                if (circle.visibility == "private" && !joinCode.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "INVITE CODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    joinCode.chunked(1).joinToString(" "),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "Copy and share this invite code with your friends to join this study group.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboard.setText(AnnotatedString(joinCode))
                                        Toast.makeText(context, "Join code copied", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.ContentCopy, null, tint = CircleIndigo, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", color = CircleIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Make circle public / private button (if owner)
                if (owner) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onToggleVisibility,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Icon(if (circle.visibility == "public") Icons.Default.Lock else Icons.Default.Public, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (circle.visibility == "public") "Make circle private" else "Make circle public",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // GROUP RANKING Section header
        item {
            Text(
                "GROUP RANKING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        // Ranking members
        items(sortedMembers, key = { it.userId }) { member ->
            LeaderboardMemberCard(
                member = member,
                entry = entryByUser[member.userId],
                currentUserId = currentUserId,
                hasDmAccess = hasDmAccess,
                owner = owner,
                busy = busy,
                onConnect = onConnectMember,
                onRemove = onRemove,
                onLeave = onLeave,
            )
        }

    }
}

@Composable
private fun LeaderboardMemberCard(
    member: StudyCircleMemberDto,
    entry: StudyCircleLeaderboardEntryDto?,
    currentUserId: String,
    hasDmAccess: Boolean,
    owner: Boolean,
    busy: Boolean,
    onConnect: (StudyCircleMemberDto) -> Unit,
    onRemove: (String, String) -> Unit,
    onLeave: () -> Unit,
) {
    val isLive = member.isFocusing || (entry?.isFocusing == true)
    val rank = entry?.rank?.takeIf { it > 0 }
    val isSelf = member.userId == currentUserId
    val canConnect = member.userId.isNotBlank() && !isSelf
    // Connect button is locked (greyed out) if the viewing user has no DM access,
    // OR if the target member is non-premium (they would see the paywall on tap).
    val isConnectLocked = canConnect && (!hasDmAccess || !member.isPremium)
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    var selfMenuExpanded by remember { mutableStateOf(false) }

    val liveGreen = if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80)
    val cardBg = if (isLive) {
        if (isLight) Color(0xFFF8F5FC) else Color(0xFF1E172E)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val cardBorder = if (isSelf) {
        BorderStroke(2.5.dp, CircleIndigo)
    } else if (isLive) {
        BorderStroke(1.dp, CircleIndigo.copy(alpha = if (isLight) 0.25f else 0.40f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }

    FlatCircleCard(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        containerColor = cardBg,
        border = cardBorder,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, if (isLive || rank == 1) CircleIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rank?.toString() ?: "–",
                        fontWeight = if (isLive || rank == 1) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isLive || rank == 1) CircleIndigo else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            // Avatar with solid green dot on bottom-right corner with background stroke cutout
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!member.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = member.avatar,
                        contentDescription = member.name,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isLive) CircleIndigo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isLive) CircleIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (isLive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(11.dp)
                            .background(cardBg, CircleShape)
                            .padding(1.5.dp)
                            .background(liveGreen, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSelf) {
                        Spacer(Modifier.width(4.dp))
                        Text("(you)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (member.role == "owner") {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = "Owner", tint = CircleAmber, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                if (isLive) {
                    val focusMins = entry?.totalFocusMinutes ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(liveGreen, CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (focusMins > 0) {
                                "Live · ${formatFocusMinutes(focusMins)} focus time"
                            } else {
                                "Focusing now"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = liveGreen,
                            maxLines = 1,
                        )
                    }
                } else {
                    val focusMins = entry?.totalFocusMinutes ?: 0
                    val sessionCount = entry?.sessionCount ?: 0
                    val subtext = if (focusMins > 0) {
                        "${formatFocusMinutes(focusMins)} focus time · $sessionCount ${if (sessionCount == 1) "session" else "sessions"}"
                    } else {
                        "0m focus time · Not focused today"
                    }
                    Text(
                        text = subtext,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isSelf) {
                    // Self card: horizontal three-dot (⋯) opens a "Leave circle" option.
                    Box {
                        IconButton(
                            onClick = { selfMenuExpanded = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = selfMenuExpanded,
                            onDismissRequest = { selfMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Leave circle", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    selfMenuExpanded = false
                                    onLeave()
                                },
                                enabled = !busy,
                            )
                        }
                    }
                } else if (canConnect) {
                    // Other members: Connect button.
                    // Black = premium member (can connect directly after tap).
                    // Greyed-out = non-premium member (paywall shown on tap).
                    val connectContainerColor = when {
                        isConnectLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        member.isPremium -> Color(0xFF111827)   // Solid near-black for premium
                        else             -> CircleIndigo
                    }
                    val connectBorderColor = when {
                        isConnectLocked -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        member.isPremium -> Color(0xFF111827)
                        else             -> CircleIndigo
                    }
                    val connectIconTint = if (isConnectLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    val connectTextColor = if (isConnectLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    val connectIcon = if (isConnectLocked) Icons.Default.Lock else Icons.Default.PersonAdd

                    Surface(
                        onClick = { onConnect(member) },
                        shape = RoundedCornerShape(8.dp),
                        color = connectContainerColor,
                        border = BorderStroke(1.dp, connectBorderColor),
                        shadowElevation = if (isConnectLocked) 0.dp else 2.dp,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Icon(
                                imageVector = connectIcon,
                                contentDescription = "Connect",
                                modifier = Modifier.size(13.dp),
                                tint = connectIconTint,
                            )
                            Text(
                                text = "Connect",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = connectTextColor,
                            )
                        }
                    }
                }
                // Owner-only: Remove member button (shown for non-owner members)
                if (owner && member.role != "owner") {
                    IconButton(
                        onClick = { onRemove(member.userId, member.name) },
                        enabled = !busy,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Default.PersonRemove,
                            "Remove ${member.name}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatFocusMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
