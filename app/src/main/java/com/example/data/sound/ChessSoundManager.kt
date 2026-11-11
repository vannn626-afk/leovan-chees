package com.example.data.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Subtle, high-quality procedural acoustic sound synthesizer for chess interactions.
 * Produces warm wooden piece placement taps, crisp capture snaps, check chimes,
 * checkmate chords, and optional ambient background harmonies without external audio bloat.
 */
object ChessSoundManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var soundEnabled: Boolean = true
    private var musicEnabled: Boolean = false
    private var musicJob: Job? = null

    fun updatePreferences(soundOn: Boolean, musicOn: Boolean) {
        soundEnabled = soundOn
        if (musicEnabled != musicOn) {
            musicEnabled = musicOn
            if (musicEnabled) startAmbientMusic() else stopAmbientMusic()
        }
    }

    fun playMoveSound() {
        if (!soundEnabled) return
        // Warm wooden thud (low resonant tap + soft click)
        scope.launch {
            playSynthesizedTone(freqHz = 165.0, secondaryHz = 330.0, durationMs = 55, decayRate = 45.0, volume = 0.28f)
        }
    }

    fun playCaptureSound() {
        if (!soundEnabled) return
        // Crisp wooden snap
        scope.launch {
            playSynthesizedTone(freqHz = 240.0, secondaryHz = 520.0, durationMs = 70, decayRate = 38.0, volume = 0.34f)
        }
    }

    fun playCheckSound() {
        if (!soundEnabled) return
        // Resonant alert chime
        scope.launch {
            playSynthesizedTone(freqHz = 587.33, secondaryHz = 880.0, durationMs = 160, decayRate = 14.0, volume = 0.30f)
        }
    }

    fun playGameStartSound() {
        if (!soundEnabled) return
        scope.launch {
            playSynthesizedTone(freqHz = 440.0, secondaryHz = 659.25, durationMs = 120, decayRate = 18.0, volume = 0.22f)
        }
    }

    fun playGameEndSound(isVictory: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            if (isVictory) {
                playSynthesizedTone(freqHz = 523.25, secondaryHz = 659.25, durationMs = 130, decayRate = 14.0, volume = 0.25f)
                delay(110)
                playSynthesizedTone(freqHz = 659.25, secondaryHz = 783.99, durationMs = 240, decayRate = 10.0, volume = 0.28f)
            } else {
                playSynthesizedTone(freqHz = 392.0, secondaryHz = 293.66, durationMs = 220, decayRate = 12.0, volume = 0.24f)
            }
        }
    }

    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            playSynthesizedTone(freqHz = 420.0, secondaryHz = 840.0, durationMs = 24, decayRate = 80.0, volume = 0.14f)
        }
    }

    fun playPuzzleSolvedSound() {
        playGameEndSound(true)
    }

    fun playIllegalOrBlunderSound() {
        playCheckSound()
    }

    private fun startAmbientMusic() {
        musicJob?.cancel()
        musicJob = scope.launch {
            val chordFrequencies = listOf(
                220.0 to 329.63, // A2 + E4
                196.0 to 293.66, // G2 + D4
                174.61 to 261.63, // F2 + C4
                164.81 to 246.94  // E2 + B3
            )
            var idx = 0
            while (isActive && musicEnabled) {
                val (f1, f2) = chordFrequencies[idx % chordFrequencies.size]
                playSynthesizedTone(freqHz = f1, secondaryHz = f2, durationMs = 900, decayRate = 2.8, volume = 0.05f)
                delay(3200)
                idx++
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playSynthesizedTone(
        freqHz: Double,
        secondaryHz: Double,
        durationMs: Int,
        decayRate: Double,
        volume: Float
    ) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            if (numSamples <= 0) return
            val pcm = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-decayRate * t)
                val wave = 0.7 * sin(2.0 * PI * freqHz * t) + 0.3 * sin(2.0 * PI * secondaryHz * t)
                val sample = (wave * envelope * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[i] = sample.toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // AudioTrack may be unavailable in headless Robolectric unit tests
        }
    }
}
