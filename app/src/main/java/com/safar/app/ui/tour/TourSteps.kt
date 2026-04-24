package com.safar.app.ui.tour

import com.safar.app.ui.butterfly.ButterflyTourStep
import com.safar.app.ui.butterfly.TooltipSide

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
        anchorX = 0.12f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Journal ✍️",
        message = "Tap Journal to write freely. Your entries are private and stay on your device.",
        anchorX = 0.35f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Goals 🎯",
        message = "Set meaningful daily or weekly goals here. Completing them feeds your streak.",
        anchorX = 0.57f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Streaks 🔥",
        message = "Your current streak lives here. Consistency is the magic — even one small action counts.",
        anchorX = 0.75f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Analytics 📊",
        message = "See your weekly patterns, mood trends, and progress charts over time.",
        anchorX = 0.92f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
)

/** Tour steps shown on the Ekagra screen. */
val ekagraTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Ekagra 🎯",
        message = "Ekagra means 'one-pointed focus'. Use it for deep work sessions, Pomodoro timers, and flow tracking.",
        anchorX = 0.5f, anchorY = 0.28f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    ButterflyTourStep(
        title   = "Focus Timer",
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
        title   = "Navigation Menu ☰",
        message = "Swipe from the left or tap the menu icon to switch between Safar modules at any time.",
        anchorX = 0.07f, anchorY = 0.06f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
)

/** Tour steps shown on the Mehfil screen. */
val mehfilTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Mehfil 🌐",
        message = "Mehfil is Safar's community space. Share thoughts, find like-minded peers, and grow together.",
        anchorX = 0.5f, anchorY = 0.3f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    ButterflyTourStep(
        title   = "Community Posts",
        message = "Browse posts in Academic and Reflective spaces. Like, comment, or save anything that resonates.",
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Sandesh 📢",
        message = "The banner at the top is a Sandesh — a community-wide message. Tap it to react and comment.",
        anchorX = 0.5f, anchorY = 0.18f,
        tooltipSide = TooltipSide.BOTTOM,
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
        title   = "Create a Post ✍️",
        message = "Tap the + button to share your thoughts with the community. Choose Academic or Reflective space.",
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
    ),
)

/** Tour steps shown on the Dhyan screen. */
val dhyanTourSteps = listOf(
    ButterflyTourStep(
        title   = "Welcome to Dhyan 🧘",
        message = "Dhyan means meditation in Sanskrit. This space is for breathing exercises, calm, and guided courses.",
        anchorX = 0.5f, anchorY = 0.28f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    ButterflyTourStep(
        title   = "Breathe with Me",
        message = "Switch to the Breathing tab and tap 'Breathe with me' to begin a guided breathing session.",
        anchorX = 0.25f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Techniques 🌬️",
        message = "Tap the air-icon FAB to switch between Diaphragmatic, Box, 4-7-8 and 6-7-8 techniques.",
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
    ),
    ButterflyTourStep(
        title   = "Courses 📚",
        message = "Switch to the Courses tab for structured meditation tracks — including the SAFAR 30-Day Journey.",
        anchorX = 0.75f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = "Ambient Sound 🎵",
        message = "Tap the music icon in the top bar to add Rain, Forest, Ocean, or Binaural Beats to your session.",
        anchorX = 0.88f, anchorY = 0.06f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
)
