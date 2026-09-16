package com.kyuu.rpsclash.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.kyuu.rpsclash.RPSApplication
import com.kyuu.rpsclash.nativebridge.NativeBridge
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val sampleRate = 44100
    private val sfxExecutor = Executors.newFixedThreadPool(2)
    private val pcmCache = ConcurrentHashMap<Int, ShortArray>()

    @Volatile
    private var isBgmRunning = false
    private var bgmThread: Thread? = null

    init {
        // Pre-render sound buffers asynchronously
        sfxExecutor.execute {
            pcmCache[NativeBridge.SFX_TAP] = generateTapSound()
            pcmCache[NativeBridge.SFX_SELECT] = generateSelectSound()
            pcmCache[NativeBridge.SFX_COUNTDOWN] = generateCountdownSound()
            pcmCache[NativeBridge.SFX_MATCH_START] = generateMatchStartSound()
            pcmCache[NativeBridge.SFX_WIN] = generateWinSound()
            pcmCache[NativeBridge.SFX_LOSE] = generateLoseSound()
            pcmCache[NativeBridge.SFX_DRAW] = generateDrawSound()
            pcmCache[NativeBridge.SFX_MATCH_END] = generateMatchEndSound()
        }
    }

    fun playSfx(sfxId: Int) {
        val prefs = RPSApplication.instance.preferences
        if (!prefs.isSfxEnabled) return

        sfxExecutor.execute {
            val pcm = pcmCache[sfxId] ?: when (sfxId) {
                NativeBridge.SFX_TAP -> generateTapSound()
                NativeBridge.SFX_SELECT -> generateSelectSound()
                NativeBridge.SFX_COUNTDOWN -> generateCountdownSound()
                NativeBridge.SFX_MATCH_START -> generateMatchStartSound()
                NativeBridge.SFX_WIN -> generateWinSound()
                NativeBridge.SFX_LOSE -> generateLoseSound()
                NativeBridge.SFX_DRAW -> generateDrawSound()
                NativeBridge.SFX_MATCH_END -> generateMatchEndSound()
                else -> generateTapSound()
            }
            playPcm(pcm, prefs.sfxVolume)
        }
    }

    private fun playPcm(pcm: ShortArray, volume: Float) {
        try {
            val audioTrack = AudioTrack.Builder()
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

            audioTrack.setVolume(volume.coerceIn(0f, 1f))
            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.play()

            // Release track after completion
            val durationMs = (pcm.size * 1000L) / sampleRate + 50
            sfxExecutor.execute {
                try {
                    Thread.sleep(durationMs)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun startBgm() {
        val prefs = RPSApplication.instance.preferences
        if (!prefs.isMusicEnabled || isBgmRunning) return

        isBgmRunning = true
        bgmThread = Thread {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()

            // Ambient lo-fi chord progression: Am7 -> Fmaj7 -> Cmaj7 -> G
            val chordFreqs = arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 392.0), // Am7
                doubleArrayOf(174.61, 220.0, 261.63, 329.63), // Fmaj7
                doubleArrayOf(130.81, 164.81, 196.0, 246.94), // Cmaj7
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)  // G
            )

            val samplesPerChord = sampleRate * 2 // 2 seconds per chord
            val tempBuffer = ShortArray(1024)
            var totalSampleIdx = 0L

            try {
                while (isBgmRunning) {
                    val currentVol = prefs.musicVolume.coerceIn(0f, 1f)
                    track.setVolume(currentVol)

                    for (i in tempBuffer.indices) {
                        val chordIdx = ((totalSampleIdx / samplesPerChord) % chordFreqs.size).toInt()
                        val freqs = chordFreqs[chordIdx]

                        var sampleVal = 0.0
                        for (f in freqs) {
                            sampleVal += sin(2.0 * PI * f * totalSampleIdx / sampleRate)
                        }
                        sampleVal /= freqs.size

                        // Subtle warm low-pass softening
                        val envelope = 0.25
                        tempBuffer[i] = (sampleVal * envelope * 32767.0).toInt().toShort()
                        totalSampleIdx++
                    }
                    track.write(tempBuffer, 0, tempBuffer.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        }.apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
            start()
        }
    }

    fun stopBgm() {
        isBgmRunning = false
        bgmThread = null
    }

    fun onPause() {
        stopBgm()
    }

    fun onResume() {
        val prefs = RPSApplication.instance.preferences
        if (prefs.isMusicEnabled) {
            startBgm()
        }
    }

    // --- Procedural Sound Waveform Generators ---

    private fun generateTapSound(): ShortArray {
        val samples = (sampleRate * 0.04).toInt() // 40ms
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 80.0)
            val wave = sin(2.0 * PI * 880.0 * t) * decay
            buffer[i] = (wave * 28000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateSelectSound(): ShortArray {
        val samples = (sampleRate * 0.08).toInt() // 80ms
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val freq = 587.0 + (t / 0.08) * 350.0 // sweep up
            val decay = exp(-t * 25.0)
            val wave = sin(2.0 * PI * freq * t) * decay
            buffer[i] = (wave * 30000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateCountdownSound(): ShortArray {
        val samples = (sampleRate * 0.06).toInt() // 60ms
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 45.0)
            val wave = sin(2.0 * PI * 440.0 * t) * decay
            buffer[i] = (wave * 32000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateMatchStartSound(): ShortArray {
        val duration = 0.35 // 350ms
        val samples = (sampleRate * duration).toInt()
        val buffer = ShortArray(samples)
        val freqs = doubleArrayOf(440.0, 554.37, 659.25) // A Major triad
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 6.0)
            var wave = 0.0
            for (f in freqs) {
                wave += sin(2.0 * PI * f * t)
            }
            wave = (wave / freqs.size) * decay
            buffer[i] = (wave * 30000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateWinSound(): ShortArray {
        val duration = 0.65 // 650ms
        val samples = (sampleRate * duration).toInt()
        val buffer = ShortArray(samples)
        // Fanfare: C5 -> E5 -> G5 -> C6
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDur = duration / notes.size
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val noteT = t - (noteIdx * noteDur)
            val decay = exp(-noteT * 10.0)
            val wave = (sin(2.0 * PI * notes[noteIdx] * noteT) +
                    0.3 * sin(4.0 * PI * notes[noteIdx] * noteT)) * decay
            buffer[i] = (wave * 26000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateLoseSound(): ShortArray {
        val duration = 0.55 // 550ms
        val samples = (sampleRate * duration).toInt()
        val buffer = ShortArray(samples)
        // Descending minor: Eb4 -> C4 -> Ab3
        val notes = doubleArrayOf(311.13, 261.63, 207.65)
        val noteDur = duration / notes.size
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val noteT = t - (noteIdx * noteDur)
            val decay = exp(-noteT * 7.0)
            val wave = sin(2.0 * PI * notes[noteIdx] * noteT) * decay
            buffer[i] = (wave * 28000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateDrawSound(): ShortArray {
        val duration = 0.40 // 400ms
        val samples = (sampleRate * duration).toInt()
        val buffer = ShortArray(samples)
        // Dual bells: 523.25 & 587.33
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 9.0)
            val wave = (sin(2.0 * PI * 523.25 * t) + sin(2.0 * PI * 587.33 * t)) * 0.5 * decay
            buffer[i] = (wave * 28000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateMatchEndSound(): ShortArray {
        val duration = 0.45 // 450ms
        val samples = (sampleRate * duration).toInt()
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 8.0)
            val wave = sin(2.0 * PI * 392.0 * t) * decay
            buffer[i] = (wave * 28000.0).toInt().toShort()
        }
        return buffer
    }
}
