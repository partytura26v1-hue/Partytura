package com.example.domain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class MetronomeEngine {
    private val sampleRate = 22050
    private val clickSamplesAccent = synthesizeClick(frequencyHz = 1150.0, durationMs = 28)
    private val clickSamplesRegular = synthesizeClick(frequencyHz = 820.0, durationMs = 24)

    fun playBeatClick(isAccent: Boolean) {
        val samples = if (isAccent) clickSamplesAccent else clickSamplesRegular
        try {
            val bufferSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.release()
                    } catch (_: Exception) {
                    }
                }

                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Exception) {
            // AudioTrack may be unavailable in headless test environments
        }
    }

    private fun synthesizeClick(frequencyHz: Double, durationMs: Int): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 95.0)
            val wave = sin(2.0 * PI * frequencyHz * t) * envelope
            (wave * Short.MAX_VALUE * 0.45).toInt().toShort()
        }
    }
}
