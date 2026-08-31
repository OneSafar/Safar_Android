package com.safarparmar.app.feature.youtubeinsights

/** Legacy analytics key helpers. This contains no accessibility-screen parsing. */
object YoutubeChannelIdentity {
    fun normalize(value: String): String = value.trim().lowercase()
        .removePrefix("@")
        .replace(Regex("\\s+"), " ")

    fun identityKey(value: String): String = normalize(value)
        .filter { it.isLetterOrDigit() }
}
