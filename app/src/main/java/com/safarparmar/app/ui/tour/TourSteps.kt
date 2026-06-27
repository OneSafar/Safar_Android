package com.safarparmar.app.ui.tour

import com.safarparmar.app.ui.butterfly.ButterflyTourStep
import com.safarparmar.app.ui.butterfly.TooltipSide
import androidx.compose.ui.unit.dp

/** Tour steps shown on the Nishtha screen. */
val nishthaTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Nishtha 🌱",
        message = "This is your personal growth hub — track habits, moods, journals and streaks all in one place.",
        anchorX = 0.5f, anchorY = 0.25f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    ButterflyTourStep(
        title   = "Daily Check-In",
        message = "Start here every day. Log your mood and set an intention. Small moments of awareness add up!",
        anchorX = 0.10f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Journal",
        message = "Tap Journal to write freely. Your entries are private and stay on your device.",
        anchorX = 0.30f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_pencil_simple_line
    ),
    ButterflyTourStep(
        title   = "Goals",
        message = "Set meaningful daily or weekly goals here. Completing them feeds your streak.",
        anchorX = 0.50f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    ButterflyTourStep(
        title   = "Streaks",
        message = "Your current streak lives here. Consistency is the magic — even one small action counts.",
        anchorX = 0.70f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_flame
    ),
    ButterflyTourStep(
        title   = "Analytics",
        message = "See your weekly patterns, mood trends, and progress charts over time.",
        anchorX = 0.90f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_chart_bar
    ),
)

/** Tour steps shown on the Ekagra screen. */
val ekagraTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Ekagra",
        message = "Ekagra means 'one-pointed ekagra'. Use it for deep work sessions, Pomodoro timers, and flow tracking.",
        anchorX = 0.5f, anchorY = 0.28f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    ButterflyTourStep(
        title   = "Ekagra Timer",
        message = "Set a session length and press Start. The timer keeps you accountable without distraction.",
        anchorX = 0.5f, anchorY = 0.55f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Session Settings",
        message = "Customize your work and break durations to match your natural rhythm.",
        anchorX = 0.5f, anchorY = 0.75f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Navigation Menu",
        message = "Swipe from the left or tap the menu icon to switch between SAFAR modules at any time.",
        anchorX = 0.07f, anchorY = 0.06f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
)

/** Tour steps shown on the Mehfil screen. */
val mehfilTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Mehfil",
        message = "Mehfil is SAFAR's community space. Share thoughts, find like-minded peers, and grow together.",
        anchorX = 0.5f, anchorY = 0.3f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_globe
    ),
    ButterflyTourStep(
        title   = "Community Posts",
        message = "Browse posts in Academic and Reflective spaces. Like, comment, or save anything that resonates.",
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Sandesh",
        message = "The banner at the top is a Sandesh — a community-wide message. Tap it to react and comment.",
        anchorX = 0.5f, anchorY = 0.18f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_megaphone
    ),
    ButterflyTourStep(
        title   = "Connect with Someone",
        message = "See the person-icon on a post? Tap it to send a private connection request for an ephemeral chat.",
        anchorX = 0.88f, anchorY = 0.45f,
        tooltipSide = TooltipSide.LEFT,
    ),
    ButterflyTourStep(
        title   = "Connections Tab",
        message = "Manage incoming requests and open chats from the Connections tab in the bottom bar.",
        anchorX = 0.88f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Create a Post",
        message = "Tap the + button to share your thoughts with the community. Choose Academic or Reflective space.",
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
        iconRes = com.safarparmar.app.R.drawable.ic_pencil_simple_line
    ),
)

/** Tour steps shown on the Dhyan screen. */
val dhyanTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Dhyan",
        message = "Dhyan means meditation in Sanskrit. This space is for breathing exercises, calm, and guided courses.",
        anchorX = 0.5f, anchorY = 0.28f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_person_standing
    ),
    ButterflyTourStep(
        title   = "Breathe with Me",
        message = "Switch to the Breathing tab and tap 'Breathe with me' to begin a guided breathing session.",
        anchorX = 0.25f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Techniques",
        message = "Tap the air-icon FAB to switch between Diaphragmatic, Box, 4-7-8 and 6-7-8 techniques.",
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
        iconRes = com.safarparmar.app.R.drawable.ic_wind
    ),
    ButterflyTourStep(
        title   = "Courses",
        message = "Switch to the Courses tab for structured meditation tracks — including the SAFAR 30-Day Journey.",
        anchorX = 0.75f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_library
    ),
    ButterflyTourStep(
        title   = "Ambient Sound",
        message = "Tap the music icon in the top bar to add Rain, Forest, Ocean, or Binaural Beats to your session.",
        anchorX = 0.88f, anchorY = 0.06f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_music_notes_simple
    ),
)

/** Tour steps shown on the Study Planner screen. */
val studyPlannerTourSteps = listOf(
    // Step 0 (YOUR_EXAMS)
    ButterflyTourStep(
        title   = "Welcome to Study Planner 📚",
        message = "This is your exam list. Open any exam to see its plan.",
        anchorX = 0.5f, anchorY = 0.15f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_library
    ),
    // Step 1 (YOUR_EXAMS)
    ButterflyTourStep(
        title   = "Plan More Exams",
        message = "Tap here to choose another exam template.",
        anchorX = 0.88f, anchorY = 0.36f,
        tooltipSide = TooltipSide.LEFT,
    ),
    // Step 2 (YOUR_EXAMS) - Custom Plan
    ButterflyTourStep(
        title   = "Build Your Planner",
        message = "Tap Custom Plan if your exam is not in templates.",
        anchorX = 0.88f, anchorY = 0.45f,
        tooltipSide = TooltipSide.LEFT,
    ),
    // Step 3 (PLAN)
    ButterflyTourStep(
        title   = "Exam Status",
        message = "See days left and how much syllabus is done.",
        anchorX = 0.5f, anchorY = 0.12f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 4 (PLAN)
    ButterflyTourStep(
        title   = "Task Tabs",
        message = "Use these tabs to see today, overdue, upcoming, and done topics.",
        anchorX = 0.5f, anchorY = 0.27f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 5 (PLAN)
    ButterflyTourStep(
        title   = "Start Study Flow",
        message = "Use Study Flow to finish today’s topics one by one.",
        anchorX = 0.45f, anchorY = 0.48f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    // Step 6 (PLAN)
    ButterflyTourStep(
        title   = "Today's Agenda",
        message = "These are today’s topics. Mark them done after studying.",
        anchorX = 0.5f, anchorY = 0.65f,
        tooltipSide = TooltipSide.TOP,
    ),
    // Step 7 (SYLLABUS)
    ButterflyTourStep(
        title   = "Syllabus Setup",
        message = "Add or import your subjects, chapters, and topics here.",
        anchorX = 0.28f, anchorY = 0.67f,
        tooltipSide = TooltipSide.TOP,
    ),
    // Step 8 (SYLLABUS)
    ButterflyTourStep(
        title   = "Build Planner",
        message = "After topics are ready, tap this. It creates your daily plan.",
        anchorX = 0.74f, anchorY = 0.68f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 9 (CALENDAR)
    ButterflyTourStep(
        title   = "Exam Countdown",
        message = "This shows how many days are left for your exam.",
        anchorX = 0.5f, anchorY = 0.19f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 10 (CALENDAR)
    ButterflyTourStep(
        title   = "Change Month",
        message = "Use arrows to see your plan for other months.",
        anchorX = 0.5f, anchorY = 0.36f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 11 (CALENDAR)
    ButterflyTourStep(
        title   = "Study Dots",
        message = "Dots show study days. Tap a date to see topics.",
        anchorX = 0.76f, anchorY = 0.66f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 12 (CALENDAR)
    ButterflyTourStep(
        title   = "Calendar Legend",
        message = "These colours show planned, done, overdue, and off days.",
        anchorX = 0.5f, anchorY = 0.79f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 13 (INSIGHTS)
    ButterflyTourStep(
        title   = "Insights",
        message = "See where you are lagging and what to do next.",
        anchorX = 0.5f, anchorY = 0.3f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_chart_bar
    ),
    // Step 14 (PLAN)
    ButterflyTourStep(
        title   = "Bottom Menu",
        message = "Use this menu to move between Exam, Plan, Syllabus, Calendar, and Insights.",
        anchorX = 0.5f, anchorY = 0.95f,
        tooltipSide = TooltipSide.TOP,
    ),
)
