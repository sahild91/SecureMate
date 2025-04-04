package com.example.securemate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SecureMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "auto_scan_channel",
                "Auto Scan Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for background SMS scan results"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}