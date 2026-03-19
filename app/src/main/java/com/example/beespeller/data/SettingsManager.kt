package com.example.beespeller.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("beespeller_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CHALLENGE_LIMIT = "challenge_limit"
        const val KEY_TIME_WINDOW_HOURS = "time_window_hours"
        const val KEY_MAX_ATTEMPTS_PER_WINDOW = "max_attempts_per_window"
        const val KEY_LAST_WINDOW_START = "last_window_start"
        const val KEY_WINDOW_ATTEMPTS_COUNT = "window_attempts_count"
        
        const val ADMIN_PASSWORD = "BeeSafePassword2026"
    }

    var challengeLimit: Int
        get() = prefs.getInt(KEY_CHALLENGE_LIMIT, 3)
        set(value) = prefs.edit().putInt(KEY_CHALLENGE_LIMIT, value).apply()

    var timeWindowHours: Int
        get() = prefs.getInt(KEY_TIME_WINDOW_HOURS, 1)
        set(value) = prefs.edit().putInt(KEY_TIME_WINDOW_HOURS, value).apply()

    var maxAttemptsPerWindow: Int
        get() = prefs.getInt(KEY_MAX_ATTEMPTS_PER_WINDOW, 0)
        set(value) = prefs.edit().putInt(KEY_MAX_ATTEMPTS_PER_WINDOW, value).apply()

    private var lastWindowStart: Long
        get() = prefs.getLong(KEY_LAST_WINDOW_START, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_WINDOW_START, value).apply()

    private var windowAttemptsCount: Int
        get() = prefs.getInt(KEY_WINDOW_ATTEMPTS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_WINDOW_ATTEMPTS_COUNT, value).apply()

    fun canUseAi(): Boolean {
        if (maxAttemptsPerWindow <= 0) return false
        
        val now = System.currentTimeMillis()
        val windowMillis = timeWindowHours * 60 * 60 * 1000L
        
        if (now - lastWindowStart > windowMillis) {
            // New window
            lastWindowStart = now
            windowAttemptsCount = 0
        }
        
        return windowAttemptsCount < maxAttemptsPerWindow
    }

    fun incrementAiUsage() {
        windowAttemptsCount++
    }
}
