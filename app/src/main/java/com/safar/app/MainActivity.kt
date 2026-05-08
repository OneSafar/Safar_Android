package com.safar.app

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.data.local.SafarDataStore
import com.google.firebase.messaging.FirebaseMessaging
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.ui.ekagra.LocalTimerService
import com.safar.app.ui.ekagra.TimerService
import com.safar.app.ui.navigation.SafarNavGraph
import com.safar.app.ui.theme.SafarTheme
import com.safar.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

    companion object {
        const val EXTRA_NAVIGATE_EKAGRA = "navigate_to_ekagra"
        private const val TABLET_SMALLEST_WIDTH_DP = 600
        private val PHONE_VIEWPORT_MAX_WIDTH_DP = 430.dp
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
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener {
                Log.d("SAFAR_FCM", "TOKEN = $it")
            }
            .addOnFailureListener {
                Log.e("SAFAR_FCM", "TOKEN fetch failed", it)
            }
            .addOnCompleteListener { task ->
                Log.d("SAFAR_FCM", "TOKEN task complete. success=${task.isSuccessful}")
            }

        // Bind (and start) the TimerService so it survives navigation
        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
        }
        consumeNotificationIntent(intent)

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            val isNightMode by themeViewModel.isNightMode.collectAsState()
            val currentLanguage by themeViewModel.language.collectAsState()

            SafarTheme(darkTheme = isDarkTheme, nightMode = isNightMode) {
                val configuration = LocalConfiguration.current
                val isTablet = configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .then(
                                if (isTablet) Modifier.widthIn(max = PHONE_VIEWPORT_MAX_WIDTH_DP)
                                else Modifier.fillMaxSize()
                            )
                    ) {
                        // Provide TimerService to the entire composition tree
                        CompositionLocalProvider(LocalTimerService provides timerService) {
                            SafarNavGraph(
                                dataStore          = dataStore,
                                isDarkTheme        = isDarkTheme,
                                isNightMode        = isNightMode,
                                onToggleDarkTheme  = { themeViewModel.toggleDarkTheme() },
                                onToggleNightMode  = {},
                                onLanguageToggle   = {
                                    val next = if (currentLanguage == "en") "hi" else "en"
                                    themeViewModel.setLanguage(next)
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(next)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetNavigateToEkagra() { navigateToEkagra = false }
    fun resetNotificationRoute() { notificationRoute = null }

    private fun applyOrientationPolicy() {
        requestedOrientation =
            if (resources.configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(NotificationDeepLinkHandler.EXTRA_ROUTE)
            ?: intent?.dataString?.let(NotificationDeepLinkHandler::routeFor)
        if (!route.isNullOrBlank()) {
            notificationRoute = route
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val service = timerService ?: return
            if (!service.isRunning.value) return
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(1, 1))
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setAutoEnterEnabled(true)
                            setSeamlessResizeEnabled(true)
                        }
                    }
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    // Required so Ekagra PiP overlay renders correctly

}
