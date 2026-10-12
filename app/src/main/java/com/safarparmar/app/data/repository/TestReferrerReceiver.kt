package com.safarparmar.app.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safarparmar.app.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TestReferrerReceiver : BroadcastReceiver() {
    @Inject lateinit var referralManager: ReferralManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (!BuildConfig.DEBUG) {
            Log.w("TestReferrerReceiver", "Ignored: test broadcast only active in debug builds")
            return
        }
        val referrer = intent?.getStringExtra("referrer") ?: intent?.getStringExtra("ref")
        if (!referrer.isNullOrBlank()) {
            Log.d("TestReferrerReceiver", "Received test referrer via broadcast: $referrer")
            referralManager.simulateReferrerForTesting(referrer)
        }
    }
}
