package com.safar.app

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.data.local.SafarDataStore
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

    companion object {
        const val EXTRA_NAVIGATE_EKAGRA = "navigate_to_ekagra"
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

        // Bind (and start) the TimerService so it survives navigation
        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            val isNightMode by themeViewModel.isNightMode.collectAsState()
            val currentLanguage by themeViewModel.language.collectAsState()

            SafarTheme(darkTheme = isDarkTheme, nightMode = isNightMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
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

    fun resetNavigateToEkagra() { navigateToEkagra = false }

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
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_EKAGRA, false)) {
            navigateToEkagra = true
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
                    .setAspectRatio(Rational(16, 9))
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
        if (isInPictureInPictureMode) {
            // Just entered PiP — navigate to Ekagra so PiP shows it
            navigateToEkagra = true
        } else {
            // Exiting PiP (restore) — also navigate to Ekagra
            navigateToEkagra = true
        }
    }

    // Required so Ekagra PiP overlay renders correctly
}