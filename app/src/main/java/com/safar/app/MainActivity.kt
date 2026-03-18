package com.safar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.ui.navigation.SafarNavGraph
import com.safar.app.ui.theme.SafarTheme
import com.safar.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            val isNightMode by themeViewModel.isNightMode.collectAsState()

            SafarTheme(darkTheme = isDarkTheme, nightMode = isNightMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafarNavGraph()
                }
            }
        }
    }
}
