package com.safarparmar.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var dataStore: SafarDataStore

    private var timerService by mutableStateOf<TimerService?>(null)
    var navigateToEkagra by mutableStateOf(false)
        private set
    var notificationRoute by mutableStateOf<String?>(null)
        private set
    var focusShieldBlockPrompt by mutableStateOf<FocusShieldBlockPrompt?>(null)
        private set

    companion object {
        const val EXTRA_NAVIGATE_EKAGRA = "navigate_to_ekagra"
        const val EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE = "focus_shield_blocked_package"
        const val EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME = "focus_shield_blocked_app_name"
        const val EXTRA_FOCUS_SHIELD_STRICT = "focus_shield_strict"
        const val EXTRA_FOCUS_SHIELD_ALWAYS_ON = "focus_shield_always_on"
        const val EXTRA_FOCUS_SHIELD_OPEN_EKAGRA = "focus_shield_open_ekagra"
        private const val TABLET_SMALLEST_WIDTH_DP = 600
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerBinder).getService()
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
            try {
                startService(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to start TimerService: ${e.message}")
            }
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
        }
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

                        Surface(
                            modifier = appContentModifier.imePadding(),
                        ) {
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

    /** Called on logout — prevents any pending Ekagra navigation. */
    fun onLogout() {
        navigateToEkagra = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
        }
        consumeNotificationIntent(intent)
        consumeFocusShieldBlockIntent(intent)
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
            alwaysOn = intent.getBooleanExtra(EXTRA_FOCUS_SHIELD_ALWAYS_ON, false),
        )

        if (openEkagra) {
            navigateToEkagra = true
        }
    }

    private fun quickUnlockBlockedApp(prompt: FocusShieldBlockPrompt, minutes: Int, _pauseTimer: Boolean) {
        pauseEkagraTimerIfRunning()
        val unlockMinutes = minutes.coerceIn(1, 60)
        val graceUntilMs = System.currentTimeMillis() + unlockMinutes * 60_000L
        FocusShieldRepository.ShieldPrefs.applyEmergencyUnlock(this, graceUntilMs)
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

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        PaymentEventBus.postSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        PaymentEventBus.postError(code, description, paymentData)
    }
}
