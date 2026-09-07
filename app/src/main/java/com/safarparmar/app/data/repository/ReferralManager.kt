package com.safarparmar.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.ReferralApi
import com.safarparmar.app.data.remote.dto.AttributeInstallRequest
import com.safarparmar.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferralManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: SafarDataStore,
    private val referralApi: ReferralApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "ReferralManager"
    }

    /**
     * Check if install referrer needs to be captured from Google Play,
     * or if already captured, ensure it is synced to backend.
     */
    fun checkAndCaptureInstallReferrer() {
        scope.launch {
            try {
                val isSynced = dataStore.referralSynced.first() &&
                    dataStore.referralSyncedUser.first() == (dataStore.userId.first() ?: "")
                val existingSource = dataStore.referralUtmSource.first()

                if (isSynced && !existingSource.isNullOrBlank()) {
                    Log.d(TAG, "Install referrer already captured and synced: $existingSource")
                    return@launch
                }

                if (!existingSource.isNullOrBlank() && !isSynced) {
                    // Referrer was saved earlier but backend sync failed; retry sync now
                    syncAttributionToBackend()
                    return@launch
                }

                // Query Google Play Install Referrer API
                connectAndQueryPlayReferrer()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking install referrer", e)
            }
        }
    }

    private fun connectAndQueryPlayReferrer() {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        scope.launch {
                            try {
                                val response: ReferrerDetails = referrerClient.installReferrer
                                val referrerUrl = response.installReferrer
                                Log.d(TAG, "Google Play Install Referrer retrieved: $referrerUrl")

                                parseAndSaveReferrerString(referrerUrl)
                                syncAttributionToBackend()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to read install referrer response", e)
                            } finally {
                                runCatching { referrerClient.endConnection() }
                            }
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Log.w(TAG, "Play Store install referrer feature not supported on this device")
                        runCatching { referrerClient.endConnection() }
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Log.w(TAG, "Play Store install referrer service unavailable, will retry next launch")
                        runCatching { referrerClient.endConnection() }
                    }
                    else -> {
                        Log.w(TAG, "Play Store install referrer returned code: $responseCode")
                        runCatching { referrerClient.endConnection() }
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Log.w(TAG, "Install referrer service disconnected")
            }
        })
    }

    /**
     * Parse raw referrer query string e.g.
     * "utm_source=creator_amit&utm_medium=youtube&utm_campaign=safar_v4"
     * or standard URL format
     */
    suspend fun parseAndSaveReferrerString(rawString: String?) {
        if (rawString.isNullOrBlank()) return

        var source: String? = null
        var medium: String? = null
        var campaign: String? = null

        val pairs = rawString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = runCatching { URLDecoder.decode(pair.substring(0, idx), "UTF-8") }.getOrDefault("")
                val value = runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrDefault("")

                when (key.lowercase()) {
                    "utm_source", "ref", "referrer" -> if (source == null) source = value
                    "utm_medium" -> medium = value
                    "utm_campaign" -> campaign = value
                }
            }
        }

        if (source.isNullOrBlank() && !rawString.contains("=") && rawString.length < 50) {
            // Raw creator tag passed directly
            source = rawString.trim()
        }

        Log.d(TAG, "Parsed referrer: source=$source, medium=$medium, campaign=$campaign")
        dataStore.saveReferralInfo(source, medium, campaign, rawString)
    }

    /**
     * Send device attribution to the backend API.
     */
    // An anonymous first-open acknowledgement does not acknowledge the account link.
    suspend fun onUserAuthenticated() {
        dataStore.setReferralSynced(false)
        scope.launch { syncAttributionToBackend() }
    }

    suspend fun syncAttributionToBackend() = syncMutex.withLock {
        try {
            val syncUserId = dataStore.userId.first()
            val deviceId = dataStore.getOrCreateDeviceInstallId()
            val source = dataStore.referralUtmSource.first()
            val medium = dataStore.referralUtmMedium.first()
            val campaign = dataStore.referralUtmCampaign.first()
            val raw = dataStore.referralRaw.first()

            val req = AttributeInstallRequest(
                deviceId = deviceId,
                utmSource = source,
                utmMedium = medium,
                utmCampaign = campaign,
                rawReferrer = raw
            )

            val res = referralApi.attributeInstall(req)
            if (res.isSuccessful && res.body()?.success == true) {
                Log.d(TAG, "Attribution synced to backend successfully")
                dataStore.setReferralSynced(true, syncUserId)
            } else {
                Log.w(TAG, "Attribution sync response failed: ${res.code()} - ${res.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Attribution sync network error", e)
        }
    }

    /**
     * Handle incoming deep link (e.g. safar://referral?ref=creator1)
     */
    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            try {
                val ref = uri.getQueryParameter("ref")
                    ?: uri.getQueryParameter("utm_source")
                    ?: uri.getQueryParameter("creator")
                    ?: uri.lastPathSegment

                if (!ref.isNullOrBlank()) {
                    val medium = uri.getQueryParameter("utm_medium") ?: "deeplink"
                    val campaign = uri.getQueryParameter("utm_campaign")
                    dataStore.saveReferralInfo(ref, medium, campaign, uri.toString())
                    syncAttributionToBackend()
                    Log.d(TAG, "Handled deep link referral: $ref")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling referral deep link", e)
            }
        }
    }

    /**
     * Debug testing method to simulate Play Store install referrer via ADB or test UI.
     */
    fun simulateReferrerForTesting(testReferrer: String) {
        scope.launch {
            Log.d(TAG, "Simulating referrer for testing: $testReferrer")
            parseAndSaveReferrerString(testReferrer)
            syncAttributionToBackend()
        }
    }
}
