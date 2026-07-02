package com.safarparmar.app

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.ui.ekagra.LocalTimerService
import com.safarparmar.app.ui.ekagra.TimerService
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldBlockPrompt
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldBlockedBottomSheet
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safarparmar.app.ui.navigation.SafarNavGraph
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.analytics.StudyPlannerAnalytics
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.safarparmar.app.ui.premium.PaymentEventBus

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var dataStore: SafarDataStore

    private var timerService by mutableStateOf<TimerService?>(null)
    /** Set to true when PiP is restored — NavGraph observes this to navigate to Ekagra. */
    var navigateToEkagra by mutableStateOf(false)
        private set
    var isInPipMode by mutableStateOf(false)
        private set
    var notificationRoute by mutableStateOf<String?>(null)
        private set
    var focusShieldBlockPrompt by mutableStateOf<FocusShieldBlockPrompt?>(null)
        private set
    private var pendingTimerPipFromNotification = false
    private var pipRequestPosted = false

    companion object {
        const val EXTRA_NAVIGATE_EKAGRA = "navigate_to_ekagra"
        const val EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE = "focus_shield_blocked_package"
        const val EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME = "focus_shield_blocked_app_name"
        const val EXTRA_FOCUS_SHIELD_STRICT = "focus_shield_strict"
        const val EXTRA_FOCUS_SHIELD_ALWAYS_ON = "focus_shield_always_on"
        const val EXTRA_FOCUS_SHIELD_UNLOCKS_REMAINING = "focus_shield_unlocks_remaining"
        const val EXTRA_FOCUS_SHIELD_UNLOCK_SECONDS = "focus_shield_unlock_seconds"
        const val EXTRA_FOCUS_SHIELD_OPEN_EKAGRA = "focus_shield_open_ekagra"
        private const val TABLET_SMALLEST_WIDTH_DP = 600
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerBinder).getService()
            enterTimerPipFromNotificationIfReady()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()

        // Bind (and start) the TimerService so it survives navigation
        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
        }
        consumeTimerPipIntent(intent)
        consumeNotificationIntent(intent)
        consumeFocusShieldBlockIntent(intent)

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current

            // Clamp font scale globally across the app to prevent broken layouts on large display settings
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = androidx.compose.ui.unit.Density(
                density = currentDensity.density,
                fontScale = (currentDensity.fontScale * 1.10f).coerceIn(0.75f, 1.25f) // 10% larger, still clamped
            )

            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides customDensity) {
                SafarTheme(darkTheme = isDarkTheme) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isTablet = configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
                        val isTabletLandscape = isTablet && maxWidth > maxHeight
                        val appContentModifier =
                            if (isTabletLandscape) {
                                Modifier
                                    .width(maxHeight * 9f / 16f)
                                    .fillMaxHeight()
                            } else {
                                Modifier.fillMaxSize()
                            }

                        Surface(modifier = appContentModifier) {
                            CompositionLocalProvider(LocalTimerService provides timerService) {
                                SafarNavGraph(
                                    dataStore = dataStore,
                                    isDarkTheme = isDarkTheme,
                                    onToggleDarkTheme = { themeViewModel.toggleDarkTheme() },
                                )
                            }
                        }
                        focusShieldBlockPrompt?.let { prompt ->
                            FocusShieldBlockedBottomSheet(
                                prompt = prompt,
                                isEkagraTimerRunning = TimerService.isFocusTimerRunning(this@MainActivity),
                                onDismiss = ::dismissFocusShieldBlockPrompt,
                                onQuickUnlock = { minutes, pauseTimer -> quickUnlockBlockedApp(prompt, minutes, pauseTimer) },
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetNavigateToEkagra() { navigateToEkagra = false }
    fun resetNotificationRoute() { notificationRoute = null }
    fun dismissFocusShieldBlockPrompt() { focusShieldBlockPrompt = null }

    private fun applyOrientationPolicy() {
        val currentRequest = requestedOrientation
        if (currentRequest == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
            currentRequest == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
            currentRequest == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ||
            currentRequest == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
            return
        }

        val configuration = resources.configuration
        val isTablet = configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
        if (!isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /** Called on logout — stops PiP and prevents any pending Ekagra navigation. */
    fun onLogout() {
        navigateToEkagra = false
        // Disable PiP auto-enter so the next minimize doesn't re-trigger it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                setPictureInPictureParams(
                    PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                )
            } catch (_: Exception) {}
        }
        // If PiP overlay is currently visible, move the task to back so it dismisses
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            moveTaskToBack(false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
        }
        consumeTimerPipIntent(intent)
        consumeNotificationIntent(intent)
        consumeFocusShieldBlockIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        enterTimerPipFromNotificationIfReady()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterTimerPipFromNotificationIfReady()
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(NotificationDeepLinkHandler.EXTRA_ROUTE)
            ?: intent?.dataString?.let(NotificationDeepLinkHandler::routeFor)
        if (!route.isNullOrBlank()) {
            if (route.substringBefore("?") == Routes.STUDY_PLANNER) {
                StudyPlannerAnalytics.track(StudyPlannerAnalytics.PLANNER_NOTIFICATION_OPENED)
            }
            notificationRoute = route
        }
    }

    private fun consumeFocusShieldBlockIntent(intent: Intent?) {
        val blockedPackage = intent
            ?.getStringExtra(EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE)
            ?.takeIf { it.isNotBlank() }
            ?: return
        val appName = intent.getStringExtra(EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: blockedPackage
        val openEkagra = intent.getBooleanExtra(EXTRA_FOCUS_SHIELD_OPEN_EKAGRA, false)

        focusShieldBlockPrompt = FocusShieldBlockPrompt(
            packageName = blockedPackage,
            appName = appName,
            strict = intent.getBooleanExtra(EXTRA_FOCUS_SHIELD_STRICT, false),
            alwaysOn = intent.getBooleanExtra(EXTRA_FOCUS_SHIELD_ALWAYS_ON, false) && !openEkagra,
            unlocksRemaining = intent.getIntExtra(EXTRA_FOCUS_SHIELD_UNLOCKS_REMAINING, -1),
        )

        if (openEkagra) {
            navigateToEkagra = true
        }
    }

    private fun quickUnlockBlockedApp(prompt: FocusShieldBlockPrompt, minutes: Int, pauseTimer: Boolean) {
        if (pauseTimer) {
            pauseEkagraTimerIfRunning()
        }
        val unlockMinutes = minutes.coerceIn(1, 60)
        val graceUntilMs = System.currentTimeMillis() + unlockMinutes * 60_000L
        val used = FocusShieldRepository.ShieldPrefs.getUnlocksUsed(this) + 1
        FocusShieldRepository.ShieldPrefs.applyEmergencyUnlock(this, graceUntilMs, used)
        dismissFocusShieldBlockPrompt()
        packageManager.getLaunchIntentForPackage(prompt.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { runCatching { startActivity(it) } }
    }

    private fun pauseEkagraTimerIfRunning() {
        timerService?.takeIf { it.isRunning.value }?.pause() ?: run {
            if (TimerService.isFocusTimerRunning(this)) {
                startService(Intent(this, TimerService::class.java).apply {
                    action = TimerService.ACTION_PAUSE
                })
            }
        }
    }

    /**
     * PiP can only be entered by a foreground activity. The notification first restores
     * this activity, then this delayed hand-off renders Ekagra and returns it to PiP.
     */
    private fun consumeTimerPipIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationDeepLinkHandler.EXTRA_ENTER_TIMER_PIP, false) != true) return
        navigateToEkagra = true
        pendingTimerPipFromNotification = true
        enterTimerPipFromNotificationIfReady()
    }

    private fun enterTimerPipFromNotificationIfReady() {
        if (!pendingTimerPipFromNotification || pipRequestPosted || !hasWindowFocus()) return
        pipRequestPosted = true
        Handler(Looper.getMainLooper()).postDelayed({
            pipRequestPosted = false
            if (!pendingTimerPipFromNotification || isFinishing || isDestroyed) return@postDelayed
            pendingTimerPipFromNotification = false
            enterTimerPipIfRunning()
        }, 300L)
    }

    private fun buildTimerPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(1, 1))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(timerService?.isRunning?.value == true)
                    setSeamlessResizeEnabled(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setTitle("SAFAR Ekagra Timer")
                    setSubtitle("Ekagra timer running")
                }
            }
            .build()
    }

    private fun enterTimerPipIfRunning(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val service = timerService ?: return false
        if (!service.isRunning.value || isFinishing || isDestroyed) return false
        return try {
            buildTimerPipParams()?.let(::enterPictureInPictureMode) ?: false
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    // ── PiP: enter when user presses Home while timer is running ──────────────
    // onUserLeaveHint fires ONLY on app minimize — never on in-app navigation.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterTimerPipIfRunning()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // Just entered PiP — navigate to Ekagra so PiP shows it
            navigateToEkagra = true
        } else {
            // Exiting PiP (restore) — also navigate to Ekagra
            navigateToEkagra = true
        }
    }

    override fun onStop() {
        super.onStop()
        // Focus Shield is owned by TimerService, not the PiP window. Do not
        // recreate PiP after the user dismisses it.
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    // Required so Ekagra PiP overlay renders correctly

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        PaymentEventBus.postSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        PaymentEventBus.postError(code, description, paymentData)
    }
}
