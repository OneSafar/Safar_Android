package com.safarparmar.app.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

/** Owns media audio focus for both Ekagra music and library previews. */
internal class FocusAwareMusicPlayer(context: Context) {
    private val context = context.applicationContext
    private val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var state: MusicPlaybackState? = null
    private var volume = 0.7f

    fun play(uri: Uri, looping: Boolean, volume: Float, onCompletion: () -> Unit = {}, onError: () -> Unit = {}) {
        release()
        this.volume = volume.coerceIn(0f, 1f)
        val playback = MusicPlaybackState()
        val media = MediaPlayer()
        player = media
        state = playback
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            // Ringing and duck requests must pause study music, never merely lower it.
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ change ->
                if (player === media) {
                    when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            playback.gainedFocus()
                            startIfAllowed(media, playback)
                        }
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            val permanent = change == AudioManager.AUDIOFOCUS_LOSS
                            playback.lostFocus(permanent)
                            runCatching { if (media.isPlaying) media.pause() }
                            if (permanent) abandonFocus()
                        }
                    }
                }
            }, Handler(Looper.getMainLooper()))
            .build()
        focusRequest = request
        try {
            when (audioManager.requestAudioFocus(request)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> playback.gainedFocus()
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Unit
                else -> { release(); onError(); return }
            }
            media.setAudioAttributes(attributes)
            media.setDataSource(context, uri)
            media.isLooping = looping
            media.setVolume(this.volume, this.volume)
            media.setOnPreparedListener {
                if (player === media) {
                    playback.prepared()
                    startIfAllowed(media, playback)
                }
            }
            media.setOnCompletionListener {
                if (player === media) { release(); onCompletion() }
            }
            media.setOnErrorListener { _, _, _ ->
                if (player === media) { release(); onError() }
                true
            }
            media.prepareAsync()
        } catch (_: Exception) {
            release()
            onError()
        }
    }

    private fun startIfAllowed(media: MediaPlayer, playback: MusicPlaybackState) {
        if (player !== media || !playback.canPlay) return
        runCatching {
            media.setVolume(volume, volume)
            media.start()
        }
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        player?.let { runCatching { it.setVolume(volume, volume) } }
    }

    private fun abandonFocus() {
        val request = focusRequest
        focusRequest = null
        request?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
    }

    fun release() {
        state?.released()
        state = null
        val oldPlayer = player
        player = null
        oldPlayer?.let { runCatching { if (it.isPlaying) it.pause() } }
        abandonFocus()
        oldPlayer?.let {
            kotlin.concurrent.thread {
                runCatching { it.release() }
            }
        }
    }
}
