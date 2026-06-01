package com.safarparmar.app.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safarparmar.app.BuildConfig

class NotificationDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val command = intent?.getStringExtra(EXTRA_COMMAND).orEmpty().ifBlank { COMMAND_SHOW_PANEL }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Debug notification command=$command")
        }
        NotificationDebugActions.runCommand(context, command)
    }

    companion object {
        const val ACTION_TRIGGER_DEBUG_NOTIFICATION = "com.safarparmar.app.ACTION_TRIGGER_DEBUG_NOTIFICATION"
        const val EXTRA_COMMAND = "command"
        const val COMMAND_SHOW_PANEL = "show_panel"
        private const val TAG = "SAFAR_NOTIF_DEBUG"
    }
}
