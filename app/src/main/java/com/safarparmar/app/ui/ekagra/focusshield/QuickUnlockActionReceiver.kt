package com.safarparmar.app.ui.ekagra.focusshield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Ends only the YouTube Study Mode unlock represented by this notification. */
@AndroidEntryPoint
class QuickUnlockActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: FocusShieldRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != QuickUnlockNotification.ACTION_END_UNLOCK) return
        repository.endYoutubeQuickUnlock(
            intent.getLongExtra(QuickUnlockNotification.EXTRA_GRACE_UNTIL, 0L),
        )
    }
}
