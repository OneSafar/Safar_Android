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

val ekagraTourSteps = listOf(
    // Step 0 — What is Ekagra Screen
    ButterflyTourStep(
        title   = "Ekagra Screen ⏱",
        message = "study with no distractions.",
        anchorX = 0.5f, anchorY = 0.47f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),

    // Step 1 — Kavach
    ButterflyTourStep(
        title   = "Kavach Shield",
        message = "don't forget to activate Kavach for distraction-free study.",
        anchorX = 0.5f, anchorY = 0.26f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 2 — Modes
    ButterflyTourStep(
        title   = "Timer Modes",
        message = "choose your session type - Pomodoro timer , break or Stopwatch.",
        anchorX = 0.5f, anchorY = 0.32f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 3 — Settings
    ButterflyTourStep(
        title   = "Timer Settings",
        message = "set duration for both focus and break sessions. Choose how you want to study.",
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 4 — Start Pomodoro Session
    ButterflyTourStep(
        title   = "Start Pomodoro",
        message = "you can start a pomodoro session by setting how many times you need to repeat the session",
        anchorX = 0.5f, anchorY = 0.80f,
        tooltipSide = TooltipSide.TOP,
    ),

    // Step 5 — Floating pip
    ButterflyTourStep(
        title   = "Floating Timer",
        message = "You can enable the floating timer and Kavach Always On from this three dots menu.",
        anchorX = 0.91f, anchorY = 0.13f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 6 — Session History
    ButterflyTourStep(
        title   = "Session History",
        message = "you can either link a goal or name your own sessions and save them.",
        anchorX = 0.5f, anchorY = 0.45f,
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
        message = "The banner at the top is a sandesh from Parmar sir. ",
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
        message = "This space is for breathing exercises, calm, and guided courses.",
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
        message = "You can select from any of the breathing techniques. ",
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
        iconRes = com.safarparmar.app.R.drawable.ic_wind
    ),
    ButterflyTourStep(
        title   = "Ambient Sound",
        message = "Tap the music icon to open the audio library. ",
        anchorX = 0.84f, anchorY = 0.16f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_music_notes_simple
    ),
)

/** Tour steps shown on the Exam Planner screen. */
val studyPlannerTourSteps = listOf(
    // Step 0 (YOUR_EXAMS)
    ButterflyTourStep(
        title   = "Aapke Exam Plans",
        message = "Yahan aapke sabhi exam plans milenge. Kisi plan ko kholkar uski preparation manage kar sakte hain.",
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_library
    ),
    // Step 1 (YOUR_EXAMS) - Create Plan
    ButterflyTourStep(
        title   = "Naya Exam Plan",
        message = "Naye Exam Plan ke liye Diye gye Iss CREATE YOUR NEW PLAN  pe Tap Kariye .",
        anchorX = 0.5f, anchorY = 0.85f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    // Step 2 (PLAN) - Dashboard
    ButterflyTourStep(
        title   = "Plan Overview",
        message = "Yahan exam tak bache din, overall progress aur aaj ki padhai ka clear overview milta hai.",
        anchorX = 0.5f, anchorY = 0.15f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 3 (PLAN) - Quick Filters
    ButterflyTourStep(
        title   = "Preparation Status",
        message = "Today , Upcoming , Missed aur Completed topics ko Dekhkar Apni Preparation Track Kar skte hain .",
        anchorX = 0.5f, anchorY = 0.35f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 4 (PLAN) - Daily Todo
    ButterflyTourStep(
        title   = "Daily To-Do",
        message = "Roz Repeat hone waali  Daily Topics ko yahan pe Add Karskte hain taaki Daily Practice Hoti rahe.",
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.TOP,
    ),
    // Step 5 (PLAN) - Today's Mission
    ButterflyTourStep(
        title   = "Todays Study Plan",
        message = "Aaj kya aur kitne topics Padhne hain , Woh Yahan Milega . ",
        anchorX = 0.5f, anchorY = 0.55f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    // Step 6 (SYLLABUS)
    ButterflyTourStep(
        title   = "Syllabus",
        message = "Yahan subjects, chapters aur topics ko organize karein, unki priority set karein aur preparation status dekhein.",
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 7 (CALENDAR)
    ButterflyTourStep(
        title   = "Study Calendar",
        message = "Har date ka study schedule yahan dikhega. Isse aage ka workload aur pending work samajh sakte hain.",
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 8 (CALENDAR) - Revision & Missed Topics Buttons
    ButterflyTourStep(
        title   = "Revision & Missed Topics",
        message = "Jin topic ko Revision Ke liye set kiya hai aur Jo bhi topics missed hogye hain Woh yaha pe Dikh jayenge . ",
        anchorX = 0.5f, anchorY = 0.85f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 9 (INSIGHTS)
    ButterflyTourStep(
        title   = "Progress Insights",
        message = "Yahan study speed, syllabus progress aur weak areas samajhkar apni preparation ko better bana sakte hain.",
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_chart_bar
    ),
)
