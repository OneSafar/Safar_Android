package com.safarparmar.app.ui.dhyan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * Editorial Dhyan Courses content matching the website layout:
 * - Header: Amber Sparkles + "Dhyan Course"
 * - Card: Cover artwork (dhyan_course.webp), title, enrollment badge, bullet checklist,
 *   and dynamic action button ("Enroll Now" vs "Go to Dhyan Live →").
 */
@Composable
fun DhyanCoursesContent(
    isDarkTheme: Boolean,
    isPremiumActive: Boolean,
    onNavigate: (String) -> Unit,
    onGoToLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Section Header: Sparkles + "Dhyan Course"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFF59E0B), // Amber sparkles
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Dhyan Course",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = DhyanFlatColors.Text,
            )
        }

        // Featured Course Card: SAFAR Yoga and Meditation Course
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = DhyanFlatColors.BorderHairline,
                    shape = RoundedCornerShape(24.dp),
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DhyanFlatColors.CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Course Cover Artwork (dhyan_course.webp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(if (isDarkTheme) Color(0xFF1B1117) else Color(0xFFFCE7F3)),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dhyan_course),
                        contentDescription = "SAFAR Yoga and Meditation Course",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                // Card Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Title and Enrollment Status Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "SAFAR Yoga and Meditation Course",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DhyanFlatColors.Text,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            lineHeight = 24.sp,
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(DhyanFlatColors.EmeraldBg)
                                .border(0.5.dp, DhyanFlatColors.EmeraldBorder, RoundedCornerShape(999.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (isPremiumActive) "ENROLLED & ACTIVE" else "AVAILABLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DhyanFlatColors.Emerald,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    // Subtitle
                    Text(
                        text = "Every morning join Parmar Sir for Meditation and Yoga sessions.",
                        fontSize = 13.sp,
                        color = DhyanFlatColors.Muted,
                        lineHeight = 18.sp,
                    )

                    // Checklist Benefits
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CourseBenefitRow(
                            text = "Guided Morning Meditation & Yoga",
                            isDark = isDarkTheme,
                        )
                        CourseBenefitRow(
                            text = "Diaphragmatic & Pranayama Breathing",
                            isDark = isDarkTheme,
                        )
                        CourseBenefitRow(
                            text = "Daily Structure & 6 Months Access",
                            isDark = isDarkTheme,
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    PlanHairline(alpha = 0.5f)

                    // Bottom Access & Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = if (isPremiumActive) "YOUR ACCESS" else "CHOOSE YOUR PLAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DhyanFlatColors.Muted,
                                letterSpacing = 0.5.sp,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                if (isPremiumActive) {
                                    Text(
                                        text = "Enrolled",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DhyanFlatColors.Text,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(DhyanFlatColors.Emerald),
                                    )
                                    Text(
                                        text = "Active",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = DhyanFlatColors.Emerald,
                                    )
                                } else {
                                    Text(
                                        text = "₹49",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = DhyanFlatColors.Text,
                                    )
                                    Text(
                                        text = "or from ₹29 for active Premium",
                                        fontSize = 11.sp,
                                        color = DhyanFlatColors.Muted,
                                    )
                                }
                            }
                        }

                        // CTA Button: "Enroll Now" vs "Go to Dhyan Live →"
                        Button(
                            onClick = {
                                if (isPremiumActive) {
                                    onGoToLive()
                                } else {
                                    onNavigate(Routes.PREMIUM)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DhyanFlatColors.Primary,
                                contentColor = if (isDarkTheme) Color(0xFF27141E) else Color.White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        ) {
                            Text(
                                text = if (isPremiumActive) "Go to Dhyan Live →" else "Enroll Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseBenefitRow(
    text: String,
    isDark: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DhyanFlatColors.Emerald,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = DhyanFlatColors.Muted,
        )
    }
}

/** Standalone screen wrapper maintaining compatibility with Routes.COURSES */
@Composable
fun DhyanCoursesScreen(
    currentRoute: String = Routes.COURSES,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    isPremiumActive: Boolean = false,
) {
    DhyanScreen(
        initialTab = DhyanTab.COURSES,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
    )
}
