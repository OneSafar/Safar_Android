package com.safarparmar.app.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.safarparmar.app.ui.theme.SafarTheme

class NotificationTestPanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafarTheme {
                NotificationTestPanel(onBack = { finish() })
            }
        }
    }
}
