package com.kyuu.rpsclash.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

    var isSfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SFX_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SFX_ENABLED, value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var musicVolume: Float
        get() = prefs.getFloat(KEY_MUSIC_VOL, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_MUSIC_VOL, value).apply()

    var sfxVolume: Float
        get() = prefs.getFloat(KEY_SFX_VOL, 0.9f)
        set(value) = prefs.edit().putFloat(KEY_SFX_VOL, value).apply()

    var hapticIntensity: Float
        get() = prefs.getFloat(KEY_HAPTIC_INTENSITY, 0.6f)
        set(value) = prefs.edit().putFloat(KEY_HAPTIC_INTENSITY, value).apply()

    var isAnimationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIMATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_ANIMATIONS, value).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "rps_clash_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_SFX_ENABLED = "sfx_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_MUSIC_VOL = "music_volume"
        private const val KEY_SFX_VOL = "sfx_volume"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_ANIMATIONS = "animations_enabled"
        private const val KEY_SERVER_URL = "server_url"

        // Default to localhost/emulator address; configurable in settings dialog
        const val DEFAULT_SERVER_URL = "ws://10.0.2.2:8080"
    }
}
