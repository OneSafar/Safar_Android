package com.safarparmar.app.ui.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.safarparmar.app.R

enum class AudioCategory(@StringRes val displayNameRes: Int) {
    ORIGINALS(R.string.audio_category_originals),
    RAGA(R.string.audio_category_raga),
    AMBIENT(R.string.audio_category_ambient),
    NATURE(R.string.audio_category_nature),
    MEDITATION(R.string.audio_category_meditation)
}

data class AudioTrack(
    val id: String,
    val name: String,
    val url: String,
    val category: AudioCategory?,
    @StringRes val descriptionRes: Int? = null,
    val description: String? = null,
    @StringRes val nameRes: Int? = null,
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
        descriptionRes = R.string.audio_desc_silence,
        nameRes = R.string.audio_none_name,
        isLocal = true,
        localResId = null
    )

    val DEFAULT_DHYAN_TRACK = AudioTrack(
        id = "dhyan-default",
        name = "Dhyan",
        url = "android.resource://com.safarparmar.app/${R.raw.dhyan_processed}",
        category = AudioCategory.MEDITATION,
        descriptionRes = R.string.audio_desc_default_dhyan,
        nameRes = R.string.audio_dhyan_name,
        isLocal = true,
        localResId = R.raw.dhyan_processed
    )

    val TRACKS: List<AudioTrack> = listOf(
        NONE_TRACK,
        DEFAULT_DHYAN_TRACK,
        // ── SAFAR Originals ────────────────────────────────────────────────────────
        AudioTrack(
            id = "safar-original-bhairav-surya-stuti",
            name = "Raga Bhairav: Surya Stuti",
            url = "$BASE_URL/safar-original-bhairav-surya-stuti.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Serene morning raga celebration with contemplative flute and tanpura"
        ),
        AudioTrack(
            id = "safar-original-bhairav-dhyan-dhun",
            name = "Raga Bhairav: Dhyan Dhun",
            url = "$BASE_URL/safar-original-bhairav-dhyan-dhun.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Soothing classical melody designed for centered study and stillness"
        ),
        AudioTrack(
            id = "safar-original-bhairav-brahma-muhurta",
            name = "Raga Bhairav: Brahma Muhurta",
            url = "$BASE_URL/safar-original-bhairav-brahma-muhurta.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Sacred morning tranquility invoking pure awareness and mental clarity"
        ),
        AudioTrack(
            id = "safar-original-bhairav-antar-man",
            name = "Raga Bhairav: Antar Man",
            url = "$BASE_URL/safar-original-bhairav-antar-man.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Introspective sitar and woodwind textures grounding intense study"
        ),
        AudioTrack(
            id = "safar-original-bhairav-madhur-venu",
            name = "Raga Bhairav: Madhur Venu",
            url = "$BASE_URL/safar-original-bhairav-madhur-venu.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Sweet flute melodies over gentle drone for unbroken concentration"
        ),
        AudioTrack(
            id = "safar-original-bhairav-chetna-pravah",
            name = "Raga Bhairav: Chetna Pravah",
            url = "$BASE_URL/safar-original-bhairav-chetna-pravah.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Flowing classical soundscape harmonizing focus and inner peace"
        ),
        AudioTrack(
            id = "safar-original-nisha-bansuri",
            name = "Nisha Bansuri",
            url = "$BASE_URL/safar-original-nisha-bansuri.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Velvet bamboo flute woven with nocturnal raga tones for deep calm"
        ),
        AudioTrack(
            id = "safar-original-prana-tarang",
            name = "Prana Tarang",
            url = "$BASE_URL/safar-original-prana-tarang.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Harmonious acoustic resonance balancing energy and steady focus"
        ),
        AudioTrack(
            id = "safar-original-shanti-dhwani",
            name = "Shanti Dhwani",
            url = "$BASE_URL/safar-original-shanti-dhwani.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Resonant meditative strings and singing bowls for unwinding the mind"
        ),
        AudioTrack(
            id = "safar-original-ushas-alap",
            name = "Ushas Alap",
            url = "$BASE_URL/safar-original-ushas-alap.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Crisp morning flute and sitar harmonics greeting a productive day"
        ),
        AudioTrack(
            id = "safar-original-venu-smriti",
            name = "Venu Smriti",
            url = "$BASE_URL/safar-original-venu-smriti.mp3",
            category = AudioCategory.ORIGINALS,
            description = "Rhythmic breathwork cadence guided by soulful bansuri phrasing"
        ),

        // ── Indian Classical Ragas ─────────────────────────────────────────────────
        AudioTrack(
            id = "bageshree-for-focus",
            name = "Bageshree for Focus",
            url = "$BASE_URL/boopul-bansuri-sitar-amp-tabla-bageshree-raga-for-focus-538696.mp3",
            category = AudioCategory.RAGA,
            descriptionRes = R.string.audio_desc_bageshree_focus
        ),
        AudioTrack(
            id = "bhairav-deep-sleep",
            name = "Bhairav Deep Sleep",
            url = "$BASE_URL/boopul-singing-bowls-amp-sitar-bhairav-raga-for-deep-sleep-538666.mp3",
            category = AudioCategory.RAGA,
            descriptionRes = R.string.audio_desc_bhairav_sleep
        ),
        AudioTrack(
            id = "bhairav-morning",
            name = "Bhairav Morning",
            url = "$BASE_URL/boopul-sitar-amp-tabla-bhairav-raga-for-morning-meditation-538702.mp3",
            category = AudioCategory.RAGA,
            descriptionRes = R.string.audio_desc_bhairav_morning
        ),
        AudioTrack(
            id = "bhupali-pranayama",
            name = "Bhupali Pranayama",
            url = "$BASE_URL/boopul-sitar-amp-tabla-meditation-pranayama-bhupali-raga-538689.mp3",
            category = AudioCategory.RAGA,
            descriptionRes = R.string.audio_desc_bhupali_pranayama
        ),
        AudioTrack(
            id = "bageshree-deep-sleep",
            name = "Bageshree Deep Sleep",
            url = "$BASE_URL/boopul-sitar-amp-tanpura-bageshree-raga-for-deep-sleep-538712.mp3",
            category = AudioCategory.RAGA,
            descriptionRes = R.string.audio_desc_bageshree_sleep
        ),

        // ── Ambient / Solitude ───────────────────────────────────────────────────
        AudioTrack(
            id = "nastelbom-meditation",
            name = "Nastelbom Meditation",
            url = "$BASE_URL/nastelbom-meditation-463389.mp3",
            category = AudioCategory.AMBIENT,
            descriptionRes = R.string.audio_desc_solitude
        ),
        AudioTrack(
            id = "quietphase-ambient",
            name = "Quiet Phase Ambient",
            url = "$BASE_URL/quietphase-meditation-ambient-484356.mp3",
            category = AudioCategory.AMBIENT,
            descriptionRes = R.string.audio_desc_reading_focus
        ),
        AudioTrack(
            id = "sigma-meditation",
            name = "Sigma Meditation",
            url = "$BASE_URL/sigmamusicart-meditation-meditation-music-514539.mp3",
            category = AudioCategory.AMBIENT,
            descriptionRes = R.string.audio_desc_calm_studying
        ),
        AudioTrack(
            id = "quietphase-meditation",
            name = "Quiet Phase",
            url = "$BASE_URL/quietphase-meditation-meditation-482096.mp3",
            category = AudioCategory.AMBIENT,
            descriptionRes = R.string.audio_desc_slow_ambient
        ),

        // ── Nature / Healing ─────────────────────────────────────────────────────
        AudioTrack(
            id = "pure-birds-morning",
            name = "Pure Birds Morning",
            url = "$BASE_URL/meditativetiger-pure-birds-good-morning-music-to-wake-up-to-the-perfect-alarm-481958.mp3",
            category = AudioCategory.NATURE,
            descriptionRes = R.string.audio_desc_bird_mornings
        ),
        AudioTrack(
            id = "healing-vibrations",
            name = "Healing Vibrations",
            url = "$BASE_URL/meditativetiger-healing-vibrations-the-shamans-rest-forest-of-tigers-481962.mp3",
            category = AudioCategory.NATURE,
            descriptionRes = R.string.audio_desc_shamanic_rest
        ),
        AudioTrack(
            id = "healing-waves",
            name = "Healing Waves",
            url = "$BASE_URL/light_music-healing-waves-179881.mp3",
            category = AudioCategory.NATURE,
            descriptionRes = R.string.audio_desc_healing_waves
        ),

        // ── Deep Meditation / Energy ─────────────────────────────────────────────
        AudioTrack(
            id = "balance-of-energy",
            name = "Balance of Energy",
            url = "$BASE_URL/grand_project-deep-meditation-music-balance-of-energy-477861.mp3",
            category = AudioCategory.MEDITATION,
            descriptionRes = R.string.audio_desc_balance_energy
        ),
        AudioTrack(
            id = "anxiety-relief",
            name = "Anxiety Relief",
            url = "$BASE_URL/petrushkasound-anxiety-relief-amp-sleep-background-433174.mp3",
            category = AudioCategory.MEDITATION,
            descriptionRes = R.string.audio_desc_anxiety_relief
        ),
        AudioTrack(
            id = "meditation-music",
            name = "Meditation Music",
            url = "$BASE_URL/ikoliks_aj-meditation-music-322801.mp3",
            category = AudioCategory.MEDITATION,
            descriptionRes = R.string.audio_desc_classic_meditation
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
