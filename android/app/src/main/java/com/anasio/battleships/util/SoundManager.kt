package com.anasio.battleships.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

object SoundManager {
    var enabled = true

    private const val SAMPLE_RATE = 44100

    /** Bounded thread pool prevents unbounded thread creation on rapid sound events */
    private val executor = Executors.newScheduledThreadPool(4)

    private fun tone(freq: Float, durationMs: Int, volume: Float = 0.3f) {
        if (!enabled) return
        executor.execute {
            try {
                val numSamples = (SAMPLE_RATE * durationMs / 1000)
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val envelope = (1f - t / (durationMs / 1000f)).coerceAtLeast(0.001f)
                    buffer[i] = (Short.MAX_VALUE * volume * envelope * sin(2.0 * PI * freq * t)).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val dataSize = buffer.size * 2
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufSize = maxOf(dataSize, if (minBufSize > 0) minBufSize else dataSize)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                try {
                    track.write(buffer, 0, buffer.size)
                    track.play()
                    Thread.sleep(durationMs.toLong() + 50)
                } finally {
                    track.release()
                }
            } catch (_: Exception) {}
        }
    }

    fun playHit() {
        tone(800f, 150, 0.2f)
        executor.schedule({ tone(600f, 100, 0.15f) }, 80, TimeUnit.MILLISECONDS)
    }

    fun playMiss() = tone(300f, 250, 0.12f)

    fun playSunk() {
        tone(200f, 300, 0.2f)
        executor.schedule({ tone(150f, 400, 0.25f) }, 150, TimeUnit.MILLISECONDS)
    }

    fun playVictory() {
        listOf(523f, 659f, 784f, 1047f).forEachIndexed { i, f ->
            executor.schedule({ tone(f, 300, 0.25f) }, i * 150L, TimeUnit.MILLISECONDS)
        }
    }

    fun playDefeat() {
        listOf(400f, 350f, 300f, 250f).forEachIndexed { i, f ->
            executor.schedule({ tone(f, 350, 0.2f) }, i * 200L, TimeUnit.MILLISECONDS)
        }
    }

    fun playChat() = tone(1200f, 50, 0.08f)

    fun playTurn() {
        tone(880f, 100, 0.15f)
        executor.schedule({ tone(1100f, 150, 0.15f) }, 100, TimeUnit.MILLISECONDS)
    }

    fun playPlace() = tone(500f, 80, 0.1f)
}
