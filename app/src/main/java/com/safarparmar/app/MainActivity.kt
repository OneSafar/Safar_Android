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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldEntryPoint
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import dagger.hilt.android.EntryPointAccessors
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

    @Inject
    lateinit var maintenanceStateManager: com.safarparmar.app.data.remote.maintenance.MaintenanceStateManager

    @Inject
    lateinit var youtubeStudyV2HealthMonitor: com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2HealthMonitor

    private var timerService by mutableStateOf<TimerService?>(null)
    var navigateToEkagra by mutableStateOf(false)
        private set
    var notificationRoute by mutableStateOf<String?>(null)
        private set

    companion object {
        const val EXTRA_NAVIGATE_EKAGRA = "navigate_to_ekagra"
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

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current

            val maintenanceInfo by maintenanceStateManager.state.collectAsStateWithLifecycle()
            val isCheckingMaintenance by maintenanceStateManager.isChecking.collectAsStateWithLifecycle()

            // Anchor font scale globally across the entire app so all screens, dialogs, and sheets maintain intended typography and layout
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = androidx.compose.ui.unit.Density(
                density = currentDensity.density,
                fontScale = 1.0f // Strictly anchored to 1.0f
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
                            androidx.compose.animation.AnimatedContent(
                                targetState = maintenanceInfo?.takeIf { it.inMaintenance },
                                transitionSpec = {
                                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) togetherWith
                                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250))
                                },
                                label = "maintenance_switch",
                            ) { activeMaintenance ->
                                if (activeMaintenance != null) {
                                    com.safarparmar.app.ui.maintenance.MaintenanceScreen(
                                        info = activeMaintenance,
                                        isChecking = isCheckingMaintenance,
                                        onCheckStatus = { maintenanceStateManager.checkStatusManually() },
                                    )
                                } else {
                                    CompositionLocalProvider(LocalTimerService provides timerService) {
                                        SafarNavGraph(
                                            dataStore = dataStore,
                                            isDarkTheme = isDarkTheme,
                                            onToggleDarkTheme = { themeViewModel.toggleDarkTheme() },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun resetNavigateToEkagra() { navigateToEkagra = false }
    fun resetNotificationRoute() { notificationRoute = null }

    override fun onResume() {
        super.onResume()
        youtubeStudyV2HealthMonitor.checkOnAppResume(this)
    }

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
