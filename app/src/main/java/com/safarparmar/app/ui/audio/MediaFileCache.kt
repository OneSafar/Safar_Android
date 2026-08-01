package com.safarparmar.app.ui.audio

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * On-disk cache for the remote music and theme-video files.
 *
 * Every player in the app used to hand MediaPlayer the bucket URL directly.
 * MediaPlayer keeps no disk cache and only holds a small buffer window, so a
 * looping track was re-downloaded from the start on *every* loop — a 4-minute,
 * 8 MB track looped through a 2-hour focus session pulled ~230 MB, and the same
 * user paid it again the next day. That was the bulk of our object-storage
 * egress bill.
 *
 * These files never change, so they only ever need to be fetched once per
 * device. [uriFor] returns the local copy when we have it and the remote URL
 * when we don't — so the first play still starts instantly by streaming, while
 * a background download makes every later play local and free.
 */
object MediaFileCache {

    private const val CACHE_DIR_NAME = "media_cache"
    private const val MAX_CACHE_BYTES = 300L * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    /** URLs currently downloading, so concurrent players don't fetch the same file twice. */
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    /**
     * Local copy if we have it, otherwise the remote URL — and in that case a
     * background download is started so the next play is local.
     *
     * Non-http URIs (android.resource://, file://) are returned untouched: they
     * are already on the device and cost nothing.
     */
    fun uriFor(context: Context, url: String): Uri {
        if (!isRemote(url)) return Uri.parse(url)

        val cached = cachedFileOrNull(context, url)
        if (cached != null) return Uri.fromFile(cached)

        prefetch(context, url)
        return Uri.parse(url)
    }

    /** Downloads [url] into the cache if it isn't there already. Safe to call repeatedly. */
    fun prefetch(context: Context, url: String) {
        if (!isRemote(url)) return
        if (cachedFileOrNull(context, url) != null) return
        if (!inFlight.add(url)) return

        val appContext = context.applicationContext
        Thread {
            try {
                download(appContext, url)
                trimToMaxSize(appContext)
            } catch (_: Exception) {
                // A failed prefetch is not worth surfacing — playback already
                // fell back to streaming the remote URL, so nothing is broken.
                // The next play tries again.
            } finally {
                inFlight.remove(url)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun isRemote(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

    private fun cacheDir(context: Context): File =
        File(context.filesDir, CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun fileFor(context: Context, url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val extension = url.substringAfterLast('.', "").substringBefore('?').take(4)
        val name = if (extension.isNotEmpty()) "$digest.$extension" else digest
        return File(cacheDir(context), name)
    }

    private fun cachedFileOrNull(context: Context, url: String): File? {
        val file = fileFor(context, url)
        if (!file.exists() || file.length() == 0L) return null
        // Touch on read so trimToMaxSize evicts genuinely unused files, not
        // simply the ones downloaded longest ago.
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    private fun download(context: Context, url: String) {
        val target = fileFor(context, url)
        if (target.exists() && target.length() > 0L) return

        // Download to a temp file and rename only on success, so an interrupted
        // download can never leave a truncated file that plays as a corrupt track.
        val temp = File(target.absolutePath + ".part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) return
            val expectedBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (expectedBytes > 0 && temp.length() != expectedBytes) {
                temp.delete()
                return
            }
            if (!temp.renameTo(target)) temp.delete()
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    /** Keeps the cache bounded, evicting least-recently-used files first. */
    private fun trimToMaxSize(context: Context) {
        val files = cacheDir(context).listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return

        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}
