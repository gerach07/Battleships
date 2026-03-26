package com.anasio.battleships.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

object MusicManager {
    private const val FADE_DURATION_MS = 400L
    private const val FADE_STEPS = 16
    private const val VOLUME = 0.35f

    var enabled = true
        set(value) {
            field = value
            if (!value) stopMusic()
            else resumeMusic()
        }

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackResId: Int? = null
    private var currentContext: java.lang.ref.WeakReference<Context>? = null
    private var currentLoop: Boolean = true
    private var pausedPosition: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null

    private fun cancelFade() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun playTrack(context: Context, resId: Int, loop: Boolean = true) {
        if (!enabled) {
            currentTrackResId = resId
            currentContext = java.lang.ref.WeakReference(context)
            currentLoop = loop
            return
        }
        if (currentTrackResId == resId && mediaPlayer?.isPlaying == true) return

        // Fade out old track, then start new one
        val oldPlayer = mediaPlayer
        if (oldPlayer != null && oldPlayer.isPlaying) {
            fadeOutAndRelease(oldPlayer) {
                startNewTrack(context, resId, loop)
            }
            mediaPlayer = null
        } else {
            stopMusic()
            startNewTrack(context, resId, loop)
        }
    }

    private fun startNewTrack(context: Context, resId: Int, loop: Boolean) {
        currentTrackResId = resId
        currentContext = java.lang.ref.WeakReference(context)
        currentLoop = loop
        pausedPosition = 0

        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = loop
                setVolume(VOLUME, VOLUME)
                start()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error playing music track $resId: ${e.message}")
        }
    }

    private fun fadeOutAndRelease(player: MediaPlayer, onDone: () -> Unit) {
        cancelFade()
        val stepMs = FADE_DURATION_MS / FADE_STEPS
        var step = 0
        fadeRunnable = object : Runnable {
            override fun run() {
                step++
                val vol = VOLUME * (1f - step.toFloat() / FADE_STEPS)
                try {
                    player.setVolume(vol.coerceAtLeast(0f), vol.coerceAtLeast(0f))
                } catch (_: Exception) {}
                if (step >= FADE_STEPS) {
                    try {
                        player.stop()
                        player.release()
                    } catch (_: Exception) {}
                    fadeRunnable = null
                    onDone()
                } else {
                    handler.postDelayed(this, stepMs)
                }
            }
        }
        handler.postDelayed(fadeRunnable!!, stepMs)
    }

    fun resumeMusic() {
        if (!enabled) return
        if (mediaPlayer?.isPlaying == true) return

        if (mediaPlayer != null) {
            try { mediaPlayer?.start(); return } catch (_: Exception) {}
        }

        val ctx = currentContext?.get()
        val resId = currentTrackResId
        
        if (ctx != null && resId != null) {
            try {
                try { mediaPlayer?.release() } catch (_: Exception) {}
                mediaPlayer = null

                mediaPlayer = MediaPlayer.create(ctx.applicationContext, resId)?.apply {
                    isLooping = currentLoop
                    setVolume(VOLUME, VOLUME)
                    setOnPreparedListener { mp ->
                        if (pausedPosition > 0) mp.seekTo(pausedPosition)
                        mp.start()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicManager", "Error resuming music: ${e.message}")
            }
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                pausedPosition = mediaPlayer?.currentPosition ?: 0
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error pausing music: ${e.message}")
        }
    }

    fun stopMusic() {
        cancelFade()
        try {
            mediaPlayer?.apply {
                try { if (isPlaying) stop() } catch (_: IllegalStateException) {}
                release()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error stopping music: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    // --- Specific phase triggers ---
    // These functions use reflection/identifier lookup to safely fail if the file isn't present yet.

    private fun playIfAvailable(context: Context, filename: String, loop: Boolean = true) {
        val resId = context.resources.getIdentifier(filename, "raw", context.packageName)
        if (resId != 0) {
            playTrack(context, resId, loop)
        } else {
            Log.d("MusicManager", "Track $filename not found in res/raw. Skipping.")
            stopMusic() // Stop current track if the new phase has no music
            currentTrackResId = null
        }
    }

    fun playMenuMusic(context: Context) = playIfAvailable(context, "bgm_menu", true)
    fun playPlacementMusic(context: Context) = playIfAvailable(context, "bgm_placement", true)
    fun playBattleMusic(context: Context) = playIfAvailable(context, "bgm_battle", true)
    fun playVictoryMusic(context: Context) = playIfAvailable(context, "bgm_victory", false)
    fun playDefeatMusic(context: Context) = playIfAvailable(context, "bgm_defeat", false)
}
