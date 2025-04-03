package com.example.securemate.settings

import android.content.Context
import android.content.SharedPreferences

object ScanPreferencesHelper {
    private const val PREF_NAME = "auto_scan_prefs"
    private const val KEY_LAST_SCAN_TIME = "last_scan_time"
    private const val KEY_SCAN_ENABLED = "scan_enabled"
    private const val KEY_SCAN_FREQUENCY = "scan_frequency"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isScanEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCAN_ENABLED, false)
    }

    fun setScanEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCAN_ENABLED, enabled).apply()
    }

    fun getLastScanTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SCAN_TIME, 0L)
    }

    fun setLastScanTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SCAN_TIME, time).apply()
    }

    fun getScanFrequencyDays(context: Context): Int {
        return getPrefs(context).getInt(KEY_SCAN_FREQUENCY, 1)
    }

    fun setScanFrequencyDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_SCAN_FREQUENCY, days).apply()
    }
}