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
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.data.local.SafarDataStore
import com.google.firebase.messaging.FirebaseMessaging
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.ui.ekagra.LocalTimerService
import com.safar.app.ui.ekagra.TimerService
import com.safar.app.ui.navigation.SafarNavGraph
import com.safar.app.ui.theme.SafarTheme
import com.safar.app.ui.theme.ThemeViewModel
import com.safar.app.ui.debug.DebugFontScaleOverlay
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
                if (BuildConfig.DEBUG) Log.d("SAFAR_FCM", "FCM token fetched")
            }
            .addOnFailureListener {
                if (BuildConfig.DEBUG) Log.e("SAFAR_FCM", "FCM token fetch failed", it)
            }
            .addOnCompleteListener { task ->
                if (BuildConfig.DEBUG) Log.d("SAFAR_FCM", "FCM token task complete. success=${task.isSuccessful}")
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
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isNightMode by themeViewModel.isNightMode.collectAsStateWithLifecycle()
            val currentLanguage by themeViewModel.language.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current

            SafarTheme(darkTheme = isDarkTheme, nightMode = isNightMode) {
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
                        // Provide TimerService to the entire composition tree
                        CompositionLocalProvider(LocalTimerService provides timerService) {
                            DebugFontScaleOverlay {
                                SafarNavGraph(
                                    dataStore = dataStore,
                                    isDarkTheme = isDarkTheme,
                                    isNightMode = isNightMode,
                                    onToggleDarkTheme = { themeViewModel.toggleDarkTheme() },
                                    onToggleNightMode = { themeViewModel.toggleNightMode() },
                                    onLanguageToggle = {
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
                    setTitle("SAFAR Focus Timer")
                    setSubtitle("Focus timer running")
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

}
