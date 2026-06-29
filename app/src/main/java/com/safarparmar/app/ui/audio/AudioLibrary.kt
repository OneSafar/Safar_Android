package com.safarparmar.app.ui.audio

import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.R

enum class AudioCategory(val displayName: String) {
    RAGA("Indian Classical"),
    AMBIENT("Ambient"),
    NATURE("Nature"),
    MEDITATION("Meditation")
}

data class AudioTrack(
    val id: String,
    val name: String,
    val url: String,
    val category: AudioCategory?,
    val description: String? = null,
    val isLocal: Boolean = false,
    val localResId: Int? = null
)

object AudioLibrary {
    private const val BASE_URL = "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music"
    private const val PREFS_NAME = "safar_audio_prefs"
    private const val PREF_KEY_TRACK_ID = "selected_audio_track_id"

    val NONE_TRACK = AudioTrack(
        id = "none-track",
        name = "None (Silent)",
        url = "",
        category = null,
        description = "Meditate in silence",
        isLocal = true,
        localResId = null
    )

    val DEFAULT_DHYAN_TRACK = AudioTrack(
        id = "dhyan-default",
        name = "Dhyan",
        url = "android.resource://com.safarparmar.app/${R.raw.dhyan_processed}",
        category = AudioCategory.MEDITATION,
        description = "Default ambient meditation sound",
        isLocal = true,
        localResId = R.raw.dhyan_processed
    )

    val TRACKS: List<AudioTrack> = listOf(
        NONE_TRACK,
        DEFAULT_DHYAN_TRACK,
        // ── Indian Classical Ragas ─────────────────────────────────────────────────
        AudioTrack(
            id = "bageshree-for-focus",
            name = "Bageshree for Focus",
            url = "$BASE_URL/boopul-bansuri-sitar-amp-tabla-bageshree-raga-for-focus-538696.mp3",
            category = AudioCategory.RAGA,
            description = "Bansuri, sitar & tabla — Bageshree raga for deep focus"
        ),
        AudioTrack(
            id = "bhairav-deep-sleep",
            name = "Bhairav Deep Sleep",
            url = "$BASE_URL/boopul-singing-bowls-amp-sitar-bhairav-raga-for-deep-sleep-538666.mp3",
            category = AudioCategory.RAGA,
            description = "Singing bowls & sitar — Bhairav raga for deep sleep"
        ),
        AudioTrack(
            id = "bhairav-morning",
            name = "Bhairav Morning",
            url = "$BASE_URL/boopul-sitar-amp-tabla-bhairav-raga-for-morning-meditation-538702.mp3",
            category = AudioCategory.RAGA,
            description = "Sitar & tabla — Bhairav raga for morning meditation"
        ),
        AudioTrack(
            id = "bhupali-pranayama",
            name = "Bhupali Pranayama",
            url = "$BASE_URL/boopul-sitar-amp-tabla-meditation-pranayama-bhupali-raga-538689.mp3",
            category = AudioCategory.RAGA,
            description = "Sitar & tabla — Bhupali raga for pranayama"
        ),
        AudioTrack(
            id = "bageshree-deep-sleep",
            name = "Bageshree Deep Sleep",
            url = "$BASE_URL/boopul-sitar-amp-tanpura-bageshree-raga-for-deep-sleep-538712.mp3",
            category = AudioCategory.RAGA,
            description = "Sitar & tanpura — Bageshree raga for deep sleep"
        ),

        // ── Ambient / Solitude ───────────────────────────────────────────────────
        AudioTrack(
            id = "nastelbom-meditation",
            name = "Nastelbom Meditation",
            url = "$BASE_URL/nastelbom-meditation-463389.mp3",
            category = AudioCategory.AMBIENT,
            description = "Deep continuous drone for ultimate solitude"
        ),
        AudioTrack(
            id = "quietphase-ambient",
            name = "Quiet Phase Ambient",
            url = "$BASE_URL/quietphase-meditation-ambient-484356.mp3",
            category = AudioCategory.AMBIENT,
            description = "Soft ambient waves for reading & focusing"
        ),
        AudioTrack(
            id = "sigma-meditation",
            name = "Sigma Meditation",
            url = "$BASE_URL/sigmamusicart-meditation-meditation-music-514539.mp3",
            category = AudioCategory.AMBIENT,
            description = "Gentle synths for calm studying"
        ),
        AudioTrack(
            id = "quietphase-meditation",
            name = "Quiet Phase",
            url = "$BASE_URL/quietphase-meditation-meditation-482096.mp3",
            category = AudioCategory.AMBIENT,
            description = "Slow pacing ambient music"
        ),

        // ── Nature / Healing ─────────────────────────────────────────────────────
        AudioTrack(
            id = "pure-birds-morning",
            name = "Pure Birds Morning",
            url = "$BASE_URL/meditativetiger-pure-birds-good-morning-music-to-wake-up-to-the-perfect-alarm-481958.mp3",
            category = AudioCategory.NATURE,
            description = "Forest bird sounds for gentle mornings"
        ),
        AudioTrack(
            id = "healing-vibrations",
            name = "Healing Vibrations",
            url = "$BASE_URL/meditativetiger-healing-vibrations-the-shamans-rest-forest-of-tigers-481962.mp3",
            category = AudioCategory.NATURE,
            description = "Shamanic rest in the forest of tigers"
        ),
        AudioTrack(
            id = "healing-waves",
            name = "Healing Waves",
            url = "$BASE_URL/light_music-healing-waves-179881.mp3",
            category = AudioCategory.NATURE,
            description = "Gentle water waves and healing frequencies"
        ),

        // ── Deep Meditation / Energy ─────────────────────────────────────────────
        AudioTrack(
            id = "balance-of-energy",
            name = "Balance of Energy",
            url = "$BASE_URL/grand_project-deep-meditation-music-balance-of-energy-477861.mp3",
            category = AudioCategory.MEDITATION,
            description = "Deep bass frequencies to balance energy"
        ),
        AudioTrack(
            id = "anxiety-relief",
            name = "Anxiety Relief",
            url = "$BASE_URL/petrushkasound-anxiety-relief-amp-sleep-background-433174.mp3",
            category = AudioCategory.MEDITATION,
            description = "Sleep background noise for anxiety relief"
        ),
        AudioTrack(
            id = "meditation-music",
            name = "Meditation Music",
            url = "$BASE_URL/ikoliks_aj-meditation-music-322801.mp3",
            category = AudioCategory.MEDITATION,
            description = "Classic meditative bells and pads"
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPersistedTrackId(context: Context): String {
        return getPrefs(context).getString(PREF_KEY_TRACK_ID, DEFAULT_DHYAN_TRACK.id) ?: DEFAULT_DHYAN_TRACK.id
    }

    fun getPersistedTrack(context: Context): AudioTrack {
        val id = getPersistedTrackId(context)
        return TRACKS.find { it.id == id } ?: DEFAULT_DHYAN_TRACK
    }

    fun persistTrackId(context: Context, trackId: String) {
        getPrefs(context).edit().putString(PREF_KEY_TRACK_ID, trackId).apply()
    }
}
