package com.safar.app.util

object YoutubeUrls {
    const val DEFAULT_MEDITATION_VIDEO_URL =
        "https://youtu.be/i65MjKQCWUE?si=XbBv4pq0N5vXHNkw"
    const val SAFAR_CHANNEL_URL =
        "https://youtube.com/@safarparmar?si=Mvs6U5JaSGojIzSM"
    const val VISUAL_GUIDANCE_PLAYLIST_URL =
        "https://www.youtube.com/playlist?list=PLriBGSFKTHVY1YKUDRrQiSjXE2-XE31u8"

    private val VIDEO_ID_REGEX = Regex(
        """(?:youtube\.com/(?:watch\?v=|shorts/)|youtu\.be/)([a-zA-Z0-9_-]{11})""",
    )

    fun extractVideoId(url: String): String? =
        VIDEO_ID_REGEX.find(url.trim())?.groupValues?.getOrNull(1)

    fun isValidVideoUrl(url: String): Boolean = extractVideoId(url) != null

    fun watchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun thumbnailUrls(videoId: String): List<String> = listOf(
        "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
        "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
    )

    fun safeVideoUrl(url: String?): String =
        url?.trim()?.takeIf { isValidVideoUrl(it) } ?: DEFAULT_MEDITATION_VIDEO_URL
}
