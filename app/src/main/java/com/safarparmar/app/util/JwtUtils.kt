package com.safarparmar.app.util

import android.util.Base64
import org.json.JSONObject

fun decodeIsAdminClaim(jwtToken: String?): Boolean {
    if (jwtToken.isNullOrBlank()) return false
    val parts = jwtToken.split(".")
    if (parts.size < 2) return false
    return try {
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        JSONObject(payload).optBoolean("isAdmin", false)
    } catch (_: Exception) {
        false
    }
}
