package com.kyuu.rpsclash.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kyuu.rpsclash.RPSApplication
import com.kyuu.rpsclash.nativebridge.NativeBridge

class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun trigger(pattern: Int) {
        val prefs = RPSApplication.instance.preferences
        if (!prefs.isHapticEnabled) return

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val intensity = prefs.hapticIntensity.coerceIn(0.1f, 1.0f)
        val amplitude = (255 * intensity).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (pattern) {
                NativeBridge.HAPTIC_BUTTON_PRESS -> {
                    vib.vibrate(VibrationEffect.createOneShot(12, amplitude / 2))
                }
                NativeBridge.HAPTIC_MOVE_SELECTION -> {
                    vib.vibrate(VibrationEffect.createOneShot(28, amplitude))
                }
                NativeBridge.HAPTIC_COUNTDOWN -> {
                    vib.vibrate(VibrationEffect.createOneShot(18, amplitude))
                }
                NativeBridge.HAPTIC_WIN -> {
                    val timings = longArrayOf(0, 50, 40, 60, 40, 100)
                    val amplitudes = intArrayOf(0, amplitude, 0, amplitude, 0, amplitude)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                NativeBridge.HAPTIC_LOSE -> {
                    val timings = longArrayOf(0, 90, 60, 140)
                    val amplitudes = intArrayOf(0, amplitude, 0, amplitude)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                NativeBridge.HAPTIC_DRAW -> {
                    val timings = longArrayOf(0, 40, 50, 40)
                    val amplitudes = intArrayOf(0, (amplitude * 0.7f).toInt(), 0, (amplitude * 0.7f).toInt())
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                NativeBridge.HAPTIC_MATCH_RESULT -> {
                    val timings = longArrayOf(0, 70, 40, 110, 50, 160)
                    val amplitudes = intArrayOf(0, amplitude, 0, amplitude, 0, amplitude)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                else -> {
                    vib.vibrate(VibrationEffect.createOneShot(20, amplitude))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            when (pattern) {
                NativeBridge.HAPTIC_BUTTON_PRESS -> vib.vibrate(12)
                NativeBridge.HAPTIC_MOVE_SELECTION -> vib.vibrate(28)
                NativeBridge.HAPTIC_COUNTDOWN -> vib.vibrate(18)
                NativeBridge.HAPTIC_WIN -> vib.vibrate(longArrayOf(0, 50, 40, 60, 40, 100), -1)
                NativeBridge.HAPTIC_LOSE -> vib.vibrate(longArrayOf(0, 90, 60, 140), -1)
                NativeBridge.HAPTIC_DRAW -> vib.vibrate(longArrayOf(0, 40, 50, 40), -1)
                NativeBridge.HAPTIC_MATCH_RESULT -> vib.vibrate(longArrayOf(0, 70, 40, 110, 50, 160), -1)
                else -> vib.vibrate(20)
            }
        }
    }
}
