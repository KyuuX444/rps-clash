package com.kyuu.rpsclash

import android.app.Application
import com.kyuu.rpsclash.audio.SoundManager
import com.kyuu.rpsclash.data.AppPreferences
import com.kyuu.rpsclash.haptic.HapticManager
import com.kyuu.rpsclash.nativebridge.NativeBridge
import com.kyuu.rpsclash.network.NetworkMonitor

class RPSApplication : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var soundManager: SoundManager
        private set

    lateinit var hapticManager: HapticManager
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = AppPreferences(this)
        hapticManager = HapticManager(this)
        soundManager = SoundManager(this)
        networkMonitor = NetworkMonitor(this)

        // Initialize Native C++ Core with app internal data path
        val storageDir = filesDir.absolutePath
        NativeBridge.initCore(storageDir)

        // Apply saved audio and haptic settings into Native Core
        NativeBridge.setAudioSettings(
            preferences.isMusicEnabled,
            preferences.isSfxEnabled,
            preferences.musicVolume,
            preferences.sfxVolume
        )
        NativeBridge.setHapticSettings(
            preferences.isHapticEnabled,
            preferences.hapticIntensity
        )
    }

    companion object {
        lateinit var instance: RPSApplication
            private set
    }
}
