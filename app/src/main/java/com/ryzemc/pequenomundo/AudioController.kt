package com.ryzemc.pequenomundo

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/** Plays only WAV files shipped in assets/audio. No Android TTS engine or network is used. */
class AudioController(private val context: Context, private val store: LocalStore) {
    private var narration: MediaPlayer? = null
    private var background: MediaPlayer? = null
    private var generation = 0
    var foreground = false

    private fun player(key: String): MediaPlayer? =
        runCatching {
                val media = MediaPlayer()
                try {
                    media.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    context.assets.openFd("audio/$key.wav").use {
                        media.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                    media.prepare()
                    media
                } catch (e: Exception) {
                    media.release()
                    throw e
                }
            }
            .getOrNull()

    fun say(vararg keys: String) {
        stopVoice()
        if (!store.voice || !foreground) return
        val token = generation
        val queue = ArrayDeque(keys.toList())
        fun next() {
            if (token != generation || !foreground) return
            if (queue.isEmpty()) {
                background?.setVolume(.055f, .055f)
                return
            }
            val media = player(queue.removeFirst()) ?: return
            narration = media
            background?.setVolume(.018f, .018f)
            media.setOnCompletionListener {
                if (narration === it) narration = null
                it.release()
                next()
            }
            media.setOnErrorListener { mp, _, _ ->
                if (narration === mp) narration = null
                mp.release()
                background?.setVolume(.055f, .055f)
                true
            }
            media.start()
        }
        next()
    }

    fun updateMusic() {
        if (!foreground || !store.music) {
            background?.release()
            background = null
            return
        }
        if (background == null)
            background =
                player("music")?.apply {
                    isLooping = true
                    setVolume(.055f, .055f)
                    start()
                }
    }

    fun stopVoice() {
        generation++
        narration?.release()
        narration = null
        background?.setVolume(.055f, .055f)
    }

    fun pause() {
        foreground = false
        stopVoice()
        background?.release()
        background = null
    }

    fun resume() {
        foreground = true
        updateMusic()
    }
}
