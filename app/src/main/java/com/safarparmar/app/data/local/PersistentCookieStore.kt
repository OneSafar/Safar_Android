package com.safarparmar.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentCookieStore @Inject constructor(
    @ApplicationContext context: Context,
) : CookieStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val cookies = linkedMapOf<String, StoredCookie>()

    init {
        synchronized(lock) {
            readPersistedCookies()
            pruneExpiredLocked()
        }
    }

    override fun add(uri: URI?, cookie: HttpCookie?) {
        if (cookie == null) return
        synchronized(lock) {
            if (cookie.maxAge == 0L) {
                cookies.remove(cookie.key(uri))
            } else {
                cookies[cookie.key(uri)] = StoredCookie.from(uri, cookie)
            }
            persistLocked()
        }
    }

    override fun get(uri: URI?): MutableList<HttpCookie> = synchronized(lock) {
        pruneExpiredLocked()
        cookies.values
            .filter { it.matches(uri) }
            .mapNotNull { it.toHttpCookie() }
            .toMutableList()
    }

    override fun getCookies(): MutableList<HttpCookie> = synchronized(lock) {
        pruneExpiredLocked()
        cookies.values.mapNotNull { it.toHttpCookie() }.toMutableList()
    }

    override fun getURIs(): MutableList<URI> = synchronized(lock) {
        pruneExpiredLocked()
        cookies.values
            .mapNotNull { stored ->
                val host = stored.domain?.removePrefix(".") ?: stored.uriHost
                host?.let { runCatching { URI("https://$it") }.getOrNull() }
            }
            .distinct()
            .toMutableList()
    }

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean = synchronized(lock) {
        val removed = cookie != null && cookies.remove(cookie.key(uri)) != null
        if (removed) persistLocked()
        removed
    }

    override fun removeAll(): Boolean = synchronized(lock) {
        val hadCookies = cookies.isNotEmpty()
        cookies.clear()
        prefs.edit().remove(KEY_COOKIES).apply()
        hadCookies
    }

    private fun readPersistedCookies() {
        val raw = prefs.getString(KEY_COOKIES, null).orEmpty()
        if (raw.isBlank()) return
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val stored = StoredCookie.fromJson(array.getJSONObject(index))
                if (!stored.isExpired()) {
                    cookies[stored.key] = stored
                }
            }
        }
    }

    private fun pruneExpiredLocked() {
        val before = cookies.size
        cookies.entries.removeAll { it.value.isExpired() }
        if (before != cookies.size) persistLocked()
    }

    private fun persistLocked() {
        val array = JSONArray()
        cookies.values.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_COOKIES, array.toString()).apply()
    }

    private fun HttpCookie.key(uri: URI?): String {
        val host = domain?.lowercase()?.removePrefix(".") ?: uri?.host?.lowercase().orEmpty()
        val safePath = path?.ifBlank { "/" } ?: "/"
        return "${name.lowercase()}|$host|$safePath"
    }

    private data class StoredCookie(
        val key: String,
        val name: String,
        val value: String,
        val domain: String?,
        val uriHost: String?,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val version: Int,
        val expiresAtMs: Long?,
    ) {
        fun toHttpCookie(): HttpCookie? {
            if (isExpired()) return null
            return HttpCookie(name, value).apply {
                domain?.let { this.domain = it }
                path = this@StoredCookie.path
                secure = this@StoredCookie.secure
                isHttpOnly = httpOnly
                version = this@StoredCookie.version
                maxAge = expiresAtMs?.let {
                    ((it - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                } ?: -1L
            }
        }

        fun matches(uri: URI?): Boolean {
            if (uri == null || isExpired()) return false
            if (secure && uri.scheme != "https") return false

            val requestHost = uri.host?.lowercase() ?: return false
            val cookieHost = (domain ?: uriHost)?.lowercase()?.removePrefix(".") ?: return false
            val hostMatches = requestHost == cookieHost || requestHost.endsWith(".$cookieHost")
            if (!hostMatches) return false

            val requestPath = uri.path?.ifBlank { "/" } ?: "/"
            return requestPath.startsWith(path)
        }

        fun isExpired(): Boolean = expiresAtMs?.let { it <= System.currentTimeMillis() } == true

        fun toJson(): JSONObject = JSONObject()
            .put("key", key)
            .put("name", name)
            .put("value", value)
            .put("domain", domain)
            .put("uriHost", uriHost)
            .put("path", path)
            .put("secure", secure)
            .put("httpOnly", httpOnly)
            .put("version", version)
            .put("expiresAtMs", expiresAtMs)

        companion object {
            fun from(uri: URI?, cookie: HttpCookie): StoredCookie {
                val path = cookie.path?.ifBlank { "/" } ?: "/"
                val domain = cookie.domain?.lowercase()
                val uriHost = uri?.host?.lowercase()
                val host = domain?.removePrefix(".") ?: uriHost.orEmpty()
                val key = "${cookie.name.lowercase()}|$host|$path"
                val expiresAt = when {
                    cookie.maxAge < 0L -> null
                    else -> System.currentTimeMillis() + cookie.maxAge * 1000L
                }
                return StoredCookie(
                    key = key,
                    name = cookie.name,
                    value = cookie.value,
                    domain = domain,
                    uriHost = uriHost,
                    path = path,
                    secure = cookie.secure,
                    httpOnly = cookie.isHttpOnly,
                    version = cookie.version,
                    expiresAtMs = expiresAt,
                )
            }

            fun fromJson(json: JSONObject): StoredCookie = StoredCookie(
                key = json.getString("key"),
                name = json.getString("name"),
                value = json.getString("value"),
                domain = json.optString("domain").takeIf { it.isNotBlank() && it != "null" },
                uriHost = json.optString("uriHost").takeIf { it.isNotBlank() && it != "null" },
                path = json.optString("path", "/"),
                secure = json.optBoolean("secure", false),
                httpOnly = json.optBoolean("httpOnly", false),
                version = json.optInt("version", 1),
                expiresAtMs = if (json.isNull("expiresAtMs")) null else json.optLong("expiresAtMs"),
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "safar_http_cookies"
        const val KEY_COOKIES = "cookies_json"
    }
}
