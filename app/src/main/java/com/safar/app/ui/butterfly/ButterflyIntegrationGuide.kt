package com.safar.app.ui.butterfly

/**
 * ════════════════════════════════════════════════════════════
 *  BUTTERFLY GUIDED TOUR — INTEGRATION GUIDE
 *  com.safar.app.ui.butterfly
 * ════════════════════════════════════════════════════════════
 *
 *  FILES ADDED
 *  ───────────
 *  ButterflyTourStep.kt        ← data model for each stop
 *  ButterflyTourState.kt       ← state holder (start / next / dismiss)
 *  rememberButterflyTourState.kt ← Compose remember helpers
 *  ButterflyDrawing.kt         ← animated canvas butterfly (wings flap!)
 *  GlitterSystem.kt            ← sparkle trail particles
 *  ButterflyOverlay.kt         ← full-screen tour overlay composable
 *  ButterflyNudge.kt           ← single-nudge banner composable
 *
 * ════════════════════════════════════════════════════════════
 *  QUICK START  (edit MainActivity.kt or your root composable)
 * ════════════════════════════════════════════════════════════
 *
 * ```kotlin
 * @Composable
 * fun RootContent() {
 *
 *     // 1. Define your steps (anchorX / anchorY are 0f–1f screen fractions)
 *     val tourState = rememberButterflyTourState(
 *         ButterflyTourStep(
 *             title   = "Welcome to Safar 🦋",
 *             message = "I'll show you around. Follow me!",
 *             anchorX = 0.15f, anchorY = 0.20f,
 *         ),
 *         ButterflyTourStep(
 *             title   = "Your Feed",
 *             message = "Scroll here for today's Mehfil sessions.",
 *             anchorX = 0.50f, anchorY = 0.45f,
 *         ),
 *         ButterflyTourStep(
 *             title   = "Navigation Bar",
 *             message = "Tap any icon to switch sections.",
 *             anchorX = 0.50f, anchorY = 0.93f,
 *         ),
 *         ButterflyTourStep(
 *             title   = "Profile",
 *             message = "Your Dhyan streaks and settings live here.",
 *             anchorX = 0.88f, anchorY = 0.93f,
 *         ),
 *     )
 *
 *     // 2. Wrap your nav graph + overlay in a Box
 *     Box(Modifier.fillMaxSize()) {
 *         SafarNavGraph()
 *
 *         // 3. Drop the overlay — invisible until state.isVisible == true
 *         ButterflyOverlay(
 *             state = tourState,
 *             wingColor = Color(0xFF1CB1F2),   // match your brand
 *             autoAdvanceMs = 3200L,            // 0 = manual-only
 *         )
 *     }
 *
 *     // 4. Start on first launch (use DataStore / SharedPreferences to gate)
 *     val context = LocalContext.current
 *     LaunchedEffect(Unit) {
 *         val prefs = context.getSharedPreferences("safar_prefs", Context.MODE_PRIVATE)
 *         if (!prefs.getBoolean("tour_done", false)) {
 *             tourState.start()
 *             prefs.edit().putBoolean("tour_done", true).apply()
 *         }
 *     }
 * }
 * ```
 *
 * ════════════════════════════════════════════════════════════
 *  CONTEXTUAL NUDGE (no tour, just a hint banner)
 * ════════════════════════════════════════════════════════════
 *
 * ```kotlin
 * var showNudge by remember { mutableStateOf(true) }
 *
 * Box(Modifier.fillMaxSize()) {
 *     YourScreenContent()
 *
 *     ButterflyNudge(
 *         message  = "✨ Long-press a card to pin it!",
 *         visible  = showNudge,
 *         onDismiss = { showNudge = false },
 *         modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
 *     )
 * }
 * ```
 *
 * ════════════════════════════════════════════════════════════
 *  CONTROLLING THE TOUR PROGRAMMATICALLY
 * ════════════════════════════════════════════════════════════
 *
 *  tourState.start()    → begin from step 0
 *  tourState.next()     → advance one step (or dismiss if last)
 *  tourState.dismiss()  → hide overlay immediately
 *  tourState.restart()  → go back to step 0 and show again
 *
 *  You can expose tourState via your ViewModel if you want to
 *  trigger the tour from a menu item / help button:
 *
 * ```kotlin
 * // In ViewModel
 * val tourState = ButterflyTourState(steps = mySteps)
 * fun onHelpClicked() = tourState.start()
 * ```
 *
 * ════════════════════════════════════════════════════════════
 *  CUSTOMISATION
 * ════════════════════════════════════════════════════════════
 *
 *  ButterflyOverlay parameters
 *  ───────────────────────────
 *  wingColor      Color  — wing fill (default cyan #1CB1F2)
 *  bodyColor      Color  — body/antennae fill
 *  butterflySize  Dp     — overall size (default 88.dp)
 *  dimColor       Color  — scrim behind everything (default 0x55000000)
 *  autoAdvanceMs  Long   — 0 disables auto-advance; user must tap Next
 *
 *  ButterflyTourStep fields
 *  ────────────────────────
 *  title          String — bold label in tooltip
 *  message        String — italic body text
 *  anchorX/Y      Float  — 0f–1f fraction of screen width/height
 *  tooltipSide    TooltipSide — AUTO (recommended) | LEFT | RIGHT | TOP | BOTTOM
 */
object ButterflyIntegrationGuide
