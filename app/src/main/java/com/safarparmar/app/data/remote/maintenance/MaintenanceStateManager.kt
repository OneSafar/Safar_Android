package com.safarparmar.app.data.remote.maintenance

import android.content.Context
import android.util.Log
import com.safarparmar.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceStateManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<MaintenanceInfo?>(null)
    val state = _state.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private val _requiredUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val requiredUpdate = _requiredUpdate.asStateFlow()

    private val updatePreferences = context.getSharedPreferences("safar_update_policy", Context.MODE_PRIVATE)

    private var pollJob: Job? = null

    // Standalone lightweight client for health & maintenance checks
    private val rawClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    init {
        applyCachedUpdatePolicy()
    }

    fun checkSystemStatus() {
        scope.launch {
            _isChecking.value = true
            fetchStatusFromServer()
            _isChecking.value = false
        }
    }

    fun onMaintenanceDetected(info: MaintenanceInfo) {
        _state.value = info
        startPolling()
    }

    fun clearMaintenance() {
        _state.value = null
        stopPolling()
    }

    fun checkStatusManually(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            _isChecking.value = true
            val stillInMaintenance = fetchStatusFromServer()
            _isChecking.value = false
            onComplete?.invoke(stillInMaintenance)
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive && _state.value != null) {
                delay(15_000L) // poll every 15 seconds
                if (!isActive) break
                val inMaintenance = fetchStatusFromServer()
                if (!inMaintenance) {
                    Log.i("MaintenanceState", "Server maintenance has ended, clearing maintenance screen")
                    clearMaintenance()
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * Checks /api/system/status.
     * Returns true if still in maintenance, false if online.
     */
    private fun fetchStatusFromServer(): Boolean {
        return try {
            val base = BuildConfig.BASE_URL.trimEnd('/')
            val statusUrl = if (base.endsWith("/api", ignoreCase = true)) "$base/system/status" else "$base/api/system/status"
            val request = Request.Builder()
                .url(statusUrl)
                .header("Cache-Control", "no-cache")
                .header("Accept", "application/json")
                .build()

            val response = rawClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()
            response.close()

            if (code == 200) {
                val json = JSONObject(body)
                updatePolicyFrom(json.optJSONObject("androidUpdate"))
                val inMaintenance = json.optBoolean("inMaintenance", false)
                if (inMaintenance) {
                    _state.value = MaintenanceInfo(
                        inMaintenance = true,
                        title = json.optString("title", "App Under Maintenance !"),
                        message = json.optString("message", "Check Back Soon......"),
                        detail = json.optString("detail", null),
                        estimatedEndTime = json.optString("estimatedEndTime", null),
                        isDatabaseOperation = json.optBoolean("isDatabaseOperation", true),
                        lastCheckedAt = System.currentTimeMillis(),
                    )
                    true
                } else {
                    false
                }
            } else if (code == 503) {
                runCatching {
                    val json = JSONObject(body)
                    _state.value = MaintenanceInfo(
                        inMaintenance = true,
                        title = json.optString("title", "App Under Maintenance !"),
                        message = json.optString("message", "Check Back Soon......"),
                        detail = json.optString("detail", null),
                        estimatedEndTime = json.optString("estimatedEndTime", null),
                        isDatabaseOperation = json.optBoolean("isDatabaseOperation", true),
                        lastCheckedAt = System.currentTimeMillis(),
                    )
                }
                true
            } else {
                // If server is returning errors, assume maintenance/down
                true
            }
        } catch (e: Exception) {
            Log.w("MaintenanceState", "Failed to check maintenance status: ${e.message}")
            true
        }
    }

    private fun updatePolicyFrom(json: JSONObject?) {
        if (json == null) return
        val minimum = json.optInt("minimumVersionCode", 0).coerceAtLeast(0)
        val latest = json.optString("latestVersionName").takeIf { it.isNotBlank() && it != "null" }
        val title = json.optString("title", "A new Safar update is ready")
        val message = json.optString("message", "Update Safar to continue using the app.")
        val url = json.optString("playStoreUrl", "https://play.google.com/store/apps/details?id=com.safarparmar.app")
        updatePreferences.edit().putInt("minimum_version_code", minimum).putString("latest_version_name", latest)
            .putString("title", title).putString("message", message).putString("play_store_url", url).apply()
        applyUpdatePolicy(minimum, latest, title, message, url)
    }

    private fun applyCachedUpdatePolicy() {
        applyUpdatePolicy(
            updatePreferences.getInt("minimum_version_code", 0),
            updatePreferences.getString("latest_version_name", null),
            updatePreferences.getString("title", null) ?: "A new Safar update is ready",
            updatePreferences.getString("message", null) ?: "Update Safar to continue using the app.",
            updatePreferences.getString("play_store_url", null) ?: "https://play.google.com/store/apps/details?id=com.safarparmar.app",
        )
    }

    private fun applyUpdatePolicy(minimum: Int, latest: String?, title: String, message: String, url: String) {
        _requiredUpdate.value = if (BuildConfig.VERSION_CODE < minimum) AppUpdateInfo(minimum, latest, title, message, url) else null
    }
}
