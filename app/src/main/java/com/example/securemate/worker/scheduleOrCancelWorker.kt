package com.example.securemate.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleOrCancelWorker(context: Context, enable: Boolean, intervalDays: Int) {
    val workManager = WorkManager.getInstance(context)
    val workRequest = PeriodicWorkRequestBuilder<SmsAutoScanWorker>(
        intervalDays.toLong(), TimeUnit.DAYS
    ).build()

    if (enable) {
        workManager.enqueueUniquePeriodicWork(
            "sms_auto_scan",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    } else {
        workManager.cancelUniqueWork("sms_auto_scan")
    }
}