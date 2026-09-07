package com.safarparmar.app.feature.live.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.feature.live.model.LiveSession
import java.time.Duration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Deep Royal Purple color system crafted for the Live Classroom experience.
 * High-contrast, elegant, and harmonious across both Light and Dark modes.
 */
object LiveThemeColors {
    // Brand Royal Purple Accents
    val RoyalPurpleLight = Color(0xFF6B21A8) // Deep royal purple 800 (#6B21A8 / #581C87)
    val RoyalPurpleDark = Color(0xFF8B5CF6)  // Vibrant royal violet 500 for dark mode
    val RoyalPurpleContainerLight = Color(0xFFF3E8FF)
    val RoyalPurpleContainerDark = Color(0xFF2E1065)

    // Background Canvas
    val ScreenBgDark = Color(0xFF0F0E17) // Ultra-sleek dark canvas with subtle violet undertone
    val ScreenBgLight = Color(0xFFFAF8FD) // Crisp off-white with warm purple nuance

    // Cards & Surfaces
    val CardBgDark = Color(0xFF181524) // Elevated card background in dark mode
    val CardBgLight = Color(0xFFFFFFFF) // Crisp white card in light mode
    val CardBorderDark = Color(0xFF2B2440) // Subtle dark border
    val CardBorderLight = Color(0xFFECE7F4) // Subtle light border

    // Search Bar
    val SearchBgDark = Color(0xFF14121F)
    val SearchBgLight = Color(0xFFF3EEF9)
    val SearchBorderDark = Color(0xFF2B2440)
    val SearchBorderLight = Color(0xFFE2DCEB)

    // Text & Content Inks
    val TextPrimaryDark = Color(0xFFF6F4FA)
    val TextPrimaryLight = Color(0xFF1E1533)
    val TextSecondaryDark = Color(0xFF9E95B8)
    val TextSecondaryLight = Color(0xFF6B6282)
    val TextTertiaryDark = Color(0xFF6A6185)
    val TextTertiaryLight = Color(0xFF9E95B8)

    // Teacher Avatar
    val AvatarBgDark = Color(0xFF241842)
    val AvatarBgLight = Color(0xFFECE4F7)
    val AvatarTextDark = Color(0xFFC084FC)
    val AvatarTextLight = Color(0xFF581C87)

    // Live Pulse Badge
    val LiveRed = Color(0xFFEF4444)
    val LiveRedContainer = Color(0x33EF4444)

    fun background(isDark: Boolean): Color = if (isDark) ScreenBgDark else ScreenBgLight
    fun card(isDark: Boolean): Color = if (isDark) CardBgDark else CardBgLight
    fun cardBorder(isDark: Boolean): Color = if (isDark) CardBorderDark else CardBorderLight
    fun primary(isDark: Boolean): Color = if (isDark) RoyalPurpleDark else RoyalPurpleLight
    fun primaryContainer(isDark: Boolean): Color = if (isDark) RoyalPurpleContainerDark else RoyalPurpleContainerLight
    fun textPrimary(isDark: Boolean): Color = if (isDark) TextPrimaryDark else TextPrimaryLight
    fun textSecondary(isDark: Boolean): Color = if (isDark) TextSecondaryDark else TextSecondaryLight
    fun textTertiary(isDark: Boolean): Color = if (isDark) TextTertiaryDark else TextTertiaryLight
    fun searchBg(isDark: Boolean): Color = if (isDark) SearchBgDark else SearchBgLight
    fun searchBorder(isDark: Boolean): Color = if (isDark) SearchBorderDark else SearchBorderLight
    fun avatarBg(isDark: Boolean): Color = if (isDark) AvatarBgDark else AvatarBgLight
    fun avatarText(isDark: Boolean): Color = if (isDark) AvatarTextDark else AvatarTextLight
}

/** Filter tabs — maps to Live / Completed. */
enum class LiveSessionFilter(val label: String, val backendStatus: String?) {
    /** Matches web `LiveSessions` (`status=active` → scheduled + live). */
    LIVE("Live", "active"),
    COMPLETED("Completed", "ended"),
}

fun formatLiveScheduledAt(value: String?): String {
    if (value.isNullOrBlank()) return "Not scheduled"
    return try {
        val instant = Instant.parse(value)
        DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (_: DateTimeParseException) {
        value
    }
}

/** Formats the next session teaser subtitle matching the design: "Next session: today, 6:00 PM · Quant". */
fun formatNextSessionSubtitle(scheduledStartAt: String?, title: String?, clock: Clock = Clock.systemDefaultZone()): String {
    if (scheduledStartAt.isNullOrBlank()) {
        return "Next session will be announced soon"
    }
    return try {
        val instant = Instant.parse(scheduledStartAt)
        val sessionZone = clock.zone
        val sessionDate = instant.atZone(sessionZone).toLocalDate()
        val today = LocalDate.now(clock)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val formattedTime = timeFormatter.format(instant.atZone(sessionZone))

        val dayLabel = when (sessionDate) {
            today -> "today"
            today.plusDays(1) -> "tomorrow"
            else -> DateTimeFormatter.ofPattern("EEE, MMM d").format(sessionDate)
        }
        val topic = title?.takeIf { it.isNotBlank() } ?: "Live Class"
        "Next session: $dayLabel, $formattedTime · $topic"
    } catch (_: Exception) {
        val topic = title?.takeIf { it.isNotBlank() } ?: "Live Class"
        "Next session: $scheduledStartAt · $topic"
    }
}

/** Formats relative date & duration: "Yesterday · 48 min", "2 days ago · 52 min", "Today · 30 min". */
fun formatRelativeDateAndDuration(scheduledStartAt: String?, scheduledEndAt: String? = null, clock: Clock = Clock.systemDefaultZone()): String {
    if (scheduledStartAt.isNullOrBlank()) return "Completed"
    return try {
        val startInstant = Instant.parse(scheduledStartAt)
        val sessionZone = clock.zone
        val startDate = startInstant.atZone(sessionZone).toLocalDate()
        val today = LocalDate.now(clock)
        val daysBetween = ChronoUnit.DAYS.between(startDate, today)

        val dateLabel = when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween in 2..6 -> "$daysBetween days ago"
            else -> DateTimeFormatter.ofPattern("MMM d").format(startDate)
        }

        val durationLabel = if (!scheduledEndAt.isNullOrBlank()) {
            try {
                val endInstant = Instant.parse(scheduledEndAt)
                val minutes = Duration.between(startInstant, endInstant).toMinutes()
                if (minutes > 0) " · $minutes min" else ""
            } catch (_: Exception) {
                ""
            }
        } else {
            ""
        }

        "$dateLabel$durationLabel"
    } catch (_: Exception) {
        formatLiveScheduledAt(scheduledStartAt)
    }
}

fun liveSessionsErrorMessage(message: String?, httpCode: Int?): String = when {
    httpCode == 404 -> "Live sessions are not available on this server yet. Deploy the latest SAFAR API, or point the QA build to your dev backend."
    httpCode == 401 -> "Session expired. Please sign in again."
    httpCode == 403 -> "You are not allowed to view live sessions for this course."
    !message.isNullOrBlank() -> message
    else -> "Could not load live sessions"
}

@Composable
fun LiveClassroomErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

fun formatLiveStatusLabel(status: String): String = when (status) {
    "scheduled" -> "Upcoming"
    "ended" -> "Completed"
    "live" -> "Live"
    else -> status.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveClassroomTopBar(
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Live Classroom",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = 12.dp)) {
                com.safarparmar.app.ui.ekagra.focusshield.KavachCircularBackButton(onClick = onBack)
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = { Badge(containerColor = MaterialTheme.colorScheme.error) },
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Modern search input field matching the design mockup:
 * Rounded box, search icon on left, "Search sessions" hint, clear button when text is present.
 */
@Composable
fun LiveSessionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val searchBg = LiveThemeColors.searchBg(isDarkTheme)
    val searchBorder = LiveThemeColors.searchBorder(isDarkTheme)
    val textPrimary = LiveThemeColors.textPrimary(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)
    val primaryColor = LiveThemeColors.primary(isDarkTheme)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(searchBg)
            .border(1.dp, searchBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = textPrimary,
            fontSize = 15.sp,
        ),
        singleLine = true,
        cursorBrush = SolidColor(primaryColor),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textSecondary,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search sessions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp),
                            tint = textSecondary,
                        )
                    }
                }
            }
        },
    )
}

/** Backward-compatible search bar component. */
@Composable
fun LiveClassroomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveSessionSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        isDarkTheme = true,
        modifier = modifier,
    )
}

/**
 * Segmented Tab Buttons: [ Live ] [ Completed ]
 * Selected tab has deep royal purple fill, unselected tab has clean subtle border.
 */
@Composable
fun LiveSessionSegmentedTabs(
    selected: LiveSessionFilter,
    onSelected: (LiveSessionFilter) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = LiveThemeColors.primary(isDarkTheme)
    val cardBg = LiveThemeColors.card(isDarkTheme)
    val cardBorder = LiveThemeColors.cardBorder(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LiveSessionFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) primaryColor else cardBg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) primaryColor else cardBorder,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelected(filter) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else textSecondary,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/** Backward-compatible filter chips. */
@Composable
fun LiveClassroomFilterChips(
    selected: LiveSessionFilter,
    onSelected: (LiveSessionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveSessionSegmentedTabs(
        selected = selected,
        onSelected = onSelected,
        isDarkTheme = true,
        modifier = modifier,
    )
}

/**
 * The center hero card when Parmar sir is not live right now:
 * - Avatar with "PS" (Parmar Sir)
 * - Headline: "Parmar sir isn't live right now"
 * - Subtitle: "Next session: today, 6:00 PM · Quant"
 * - Action button: "Notify me when live" / "Reminder set"
 */
@Composable
fun TeacherNotLiveCard(
    nextSession: LiveSession?,
    isReminderSet: Boolean,
    onToggleReminder: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardBg = LiveThemeColors.card(isDarkTheme)
    val cardBorder = LiveThemeColors.cardBorder(isDarkTheme)
    val primaryColor = LiveThemeColors.primary(isDarkTheme)
    val textPrimary = LiveThemeColors.textPrimary(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)
    val avatarBg = LiveThemeColors.avatarBg(isDarkTheme)
    val avatarText = LiveThemeColors.avatarText(isDarkTheme)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Teacher Avatar circle with "PS"
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(avatarBg)
                    .border(1.5.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "PS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarText,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Headline
            Text(
                text = "Parmar sir isn't live right now",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            val subtitleText = formatNextSessionSubtitle(
                nextSession?.scheduledStartAt,
                nextSession?.title,
            )
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(18.dp))

            // Action Button: "Notify me when live" / "Reminder set"
            Button(
                onClick = onToggleReminder,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isReminderSet) Icons.Default.Check else Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = if (isReminderSet) "Reminder set" else "Notify me when live",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** Live status badge with pulsating dot. */
@Composable
fun LiveStatusBadge(status: String, modifier: Modifier = Modifier) {
    val isLive = status == "live"
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isLive) LiveThemeColors.LiveRed else Color.Black.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
            Text(
                text = formatLiveStatusLabel(status).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

/**
 * Hero session card when Parmar Sir IS broadcasting live:
 * Video thumbnail preview, live indicator, title, and "Join Live Session" button in Deep Royal Purple.
 */
@Composable
fun LiveHeroSessionCard(
    session: LiveSession,
    onPlay: () -> Unit,
    onJoinChat: () -> Unit,
    onShare: () -> Unit = {},
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cardBg = LiveThemeColors.card(isDarkTheme)
    val cardBorder = LiveThemeColors.cardBorder(isDarkTheme)
    val primaryColor = LiveThemeColors.primary(isDarkTheme)
    val textPrimary = LiveThemeColors.textPrimary(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clickable(onClick = onPlay),
            ) {
                if (!session.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = session.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(primaryColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = primaryColor.copy(alpha = 0.6f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(34.dp),
                        tint = Color.White,
                    )
                }

                LiveStatusBadge(
                    status = session.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )

                if (session.status == "live") {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White,
                            )
                            Text(
                                text = "Live now",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = session.title.ifBlank { "Live Classroom Session" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textSecondary,
                    )
                    Text(
                        text = "Started: ${formatLiveScheduledAt(session.scheduledStartAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
                Button(
                    onClick = onJoinChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Join Live Session", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Catch-up row item matching the design mockup:
 * - Square thumbnail with rounded corners on left
 * - Title & relative duration (e.g. "English: comprehension", "Yesterday · 48 min")
 * - Chevron arrow on right
 */
@Composable
fun CompletedSessionCatchUpCard(
    session: LiveSession,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    isNowPlaying: Boolean = false,
) {
    val cardBg = LiveThemeColors.card(isDarkTheme)
    val cardBorder = LiveThemeColors.cardBorder(isDarkTheme)
    val primaryColor = LiveThemeColors.primary(isDarkTheme)
    val textPrimary = LiveThemeColors.textPrimary(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)
    val textTertiary = LiveThemeColors.textTertiary(isDarkTheme)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            width = if (isNowPlaying) 1.5.dp else 1.dp,
            color = if (isNowPlaying) primaryColor else cardBorder,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Square Thumbnail / Icon container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (!session.thumbnailUrl.isNullOrBlank()) {
                            Color.Transparent
                        } else {
                            primaryColor.copy(alpha = if (isDarkTheme) 0.18f else 0.10f)
                        },
                    )
                    .border(
                        1.dp,
                        primaryColor.copy(alpha = if (isDarkTheme) 0.25f else 0.15f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!session.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = session.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = primaryColor,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                if (isNowPlaying) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Text(
                    text = session.title.ifBlank { "Untitled session" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatRelativeDateAndDuration(session.scheduledStartAt, session.scheduledEndAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = textTertiary,
            )
        }
    }
}

/** Backward-compatible completed session card. */
@Composable
fun CompletedSessionCard(
    session: LiveSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNowPlaying: Boolean = false,
) {
    CompletedSessionCatchUpCard(
        session = session,
        onClick = onClick,
        isDarkTheme = true,
        isNowPlaying = isNowPlaying,
        modifier = modifier,
    )
}

/** Up-next list item for scheduled or active queues. */
@Composable
fun LiveUpNextListItem(
    session: LiveSession,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompletedSessionCard(
        session = session,
        onClick = onClick,
        isNowPlaying = selected,
        modifier = modifier,
    )
}

/** Clean empty state matching royal purple styling. */
@Composable
fun LiveClassroomEmptyState(
    title: String,
    subtitle: String?,
    showClearFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
) {
    val cardBg = LiveThemeColors.card(isDarkTheme)
    val cardBorder = LiveThemeColors.cardBorder(isDarkTheme)
    val primaryColor = LiveThemeColors.primary(isDarkTheme)
    val textPrimary = LiveThemeColors.textPrimary(isDarkTheme)
    val textSecondary = LiveThemeColors.textSecondary(isDarkTheme)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
        ),
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = if (isDarkTheme) 0.15f else 0.10f))
                    .border(
                        1.dp,
                        primaryColor.copy(alpha = if (isDarkTheme) 0.30f else 0.22f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = primaryColor,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                textAlign = TextAlign.Center,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            if (showClearFilters) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, primaryColor),
                ) {
                    Text("Clear filters", color = primaryColor)
                }
            }
        }
    }
}
