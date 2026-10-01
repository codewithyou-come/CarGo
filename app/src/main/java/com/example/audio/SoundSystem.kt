package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Self-contained synthesized arcade sound effects using AudioTrack.
 * Generates crisp 8-bit / 16-bit arcade tones on demand with zero external asset dependencies.
 */
class SoundSystem(private var soundEnabled: Boolean = true) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    /**
     * Crisp two-tone coin collect chime (880Hz to 1320Hz)
     */
    fun playCoinSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration1 = 0.06f
            val duration2 = 0.10f
            val samples1 = (sampleRate * duration1).toInt()
            val samples2 = (sampleRate * duration2).toInt()
            val totalSamples = samples1 + samples2
            val buffer = ShortArray(totalSamples)

            // Note 1: A5 (880 Hz)
            val freq1 = 880.0
            for (i in 0 until samples1) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / samples1) * 0.4
                val sample = sin(2.0 * PI * freq1 * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
            }

            // Note 2: E6 (1318.5 Hz)
            val freq2 = 1318.5
            for (i in 0 until samples2) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / samples2)
                val sample = sin(2.0 * PI * freq2 * t) * envelope
                buffer[samples1 + i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Ascending fast sweep for Turbo / Nitro activation
     */
    fun playTurboSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration = 0.25f
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                // Frequency sweeps from 300Hz up to 1200Hz
                val freq = 300.0 + (progress * progress) * 900.0
                val t = i.toDouble() / sampleRate
                val envelope = if (progress < 0.2) progress / 0.2 else (1.0 - progress)
                // Add a bit of harmonic distortion for engine punch
                val sample = (sin(2.0 * PI * freq * t) * 0.7 + sin(4.0 * PI * freq * t) * 0.3) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Crash explosion: burst of white noise with low-frequency rumble
     */
    fun playCrashSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration = 0.45f
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)
            var lastNoise = 0.0

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                val envelope = (1.0 - progress) * (1.0 - progress)
                // White noise filtered
                val rawNoise = (Math.random() * 2.0 - 1.0)
                // Low-pass filter for explosion crunch
                lastNoise = lastNoise * 0.7 + rawNoise * 0.3
                // Add low frequency sub-bass
                val subBass = sin(2.0 * PI * (60.0 * (1.0 - progress * 0.5)) * (i.toDouble() / sampleRate))
                val combined = (lastNoise * 0.75 + subBass * 0.25) * envelope
                buffer[i] = (combined.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.85).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Near miss whoosh sound
     */
    fun playNearMissSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration = 0.18f
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                // Fast Doppler pitch drop: 650Hz down to 350Hz
                val freq = 650.0 - (progress * 300.0)
                val t = i.toDouble() / sampleRate
                val envelope = sin(PI * progress) // smooth bell envelope
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.5).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Countdown beeps (high pitch for GO, lower for 3-2-1)
     */
    fun playCountdownBeep(isFinal: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            val duration = if (isFinal) 0.28f else 0.12f
            val freq = if (isFinal) 880.0 else 440.0
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                val envelope = 1.0 - (progress * 0.7)
                val sample = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * UI Click tone
     */
    fun playClickSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration = 0.04f
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                val envelope = 1.0 - progress
                val sample = sin(2.0 * PI * 1050.0 * (i.toDouble() / sampleRate)) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.4).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Shield collect / protect sound
     */
    fun playShieldSound() {
        if (!soundEnabled) return
        scope.launch {
            val duration = 0.22f
            val totalSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                val freq = 500.0 + sin(progress * 15.0) * 120.0
                val t = i.toDouble() / sampleRate
                val envelope = sin(PI * progress)
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.55).toInt().toShort()
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    /**
     * Game Over sad descending motif
     */
    fun playGameOverSound() {
        if (!soundEnabled) return
        scope.launch {
            val notes = doubleArrayOf(493.88, 440.0, 392.0, 329.63) // B4 -> A4 -> G4 -> E4
            val noteDuration = 0.12f
            val samplesPerNote = (sampleRate * noteDuration).toInt()
            val totalSamples = samplesPerNote * notes.size
            val buffer = ShortArray(totalSamples)

            for (n in notes.indices) {
                val freq = notes[n]
                val offset = n * samplesPerNote
                for (i in 0 until samplesPerNote) {
                    val progress = i.toDouble() / samplesPerNote
                    val envelope = 1.0 - progress
                    val sample = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
                    buffer[offset + i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
                }
            }

            playPcmBuffer(buffer, sampleRate)
        }
    }

    private fun playPcmBuffer(buffer: ShortArray, rate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufSize, buffer.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            // Release after playing completes
            val playTimeMs = (buffer.size.toFloat() / rate * 1000f).toLong() + 50L
            Thread.sleep(playTimeMs)
        } catch (_: Exception) {
            // Ignore audio generation failures gracefully
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
