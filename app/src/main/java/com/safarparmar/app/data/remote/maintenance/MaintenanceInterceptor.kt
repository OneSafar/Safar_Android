package com.safarparmar.app.data.remote.maintenance

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceInterceptor @Inject constructor(
    private val maintenanceStateManager: MaintenanceStateManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val isMaintenanceHeader = response.header("X-Safar-Maintenance") == "1"
        val is503 = response.code == 503

        if (is503 || isMaintenanceHeader) {
            val contentType = response.body?.contentType()
            val rawBody = response.body?.string().orEmpty()

            var detectedMaintenance = isMaintenanceHeader
            var title = "App Under Maintenance !"
            var message = "Check Back Soon......"
            var detail: String? = null
            var estimatedEnd: String? = null
            var isDbOp = true

            if (rawBody.isNotBlank()) {
                runCatching {
                    val json = JSONObject(rawBody)
                    if (json.optString("error") == "maintenance_mode" || json.optBoolean("inMaintenance", false)) {
                        detectedMaintenance = true
                        title = json.optString("title", title)
                        message = json.optString("message", message)
                        detail = json.optString("detail", null)
                        estimatedEnd = json.optString("estimatedEndTime", null)
                        isDbOp = json.optBoolean("isDatabaseOperation", true)
                    }
                }
            }

            if (detectedMaintenance) {
                maintenanceStateManager.onMaintenanceDetected(
                    MaintenanceInfo(
                        inMaintenance = true,
                        title = title,
                        message = message,
                        detail = detail,
                        estimatedEndTime = estimatedEnd,
                        isDatabaseOperation = isDbOp,
                        lastCheckedAt = System.currentTimeMillis(),
                    )
                )
            }

            // Rebuild the response so downstream consumers can still read it if needed
            return response.newBuilder()
                .body(rawBody.toResponseBody(contentType))
                .build()
        } else if (response.isSuccessful && maintenanceStateManager.state.value != null) {
            // If we received a successful response on a regular route, server is back online
            val path = request.url.encodedPath
            if (!path.contains("/api/system/")) {
                maintenanceStateManager.clearMaintenance()
            }
        }

        return response
    }
}
