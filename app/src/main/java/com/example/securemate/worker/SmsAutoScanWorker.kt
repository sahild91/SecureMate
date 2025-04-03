package com.example.securemate.worker

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.securemate.settings.ScanPreferencesHelper
import com.example.securemate.flagged_links_logger.LoggerHelper
import com.example.securemate.sms_scanner.SmsParser
import com.example.securemate.threat_model.ThreatChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SmsAutoScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!ScanPreferencesHelper.isScanEnabled(applicationContext)) {
            return@withContext Result.success()
        }

        val lastScan = ScanPreferencesHelper.getLastScanTime(applicationContext)
        val fromDate = Date(lastScan)

        val uri: Uri = android.provider.Telephony.Sms.Inbox.CONTENT_URI
        val cursor = applicationContext.contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            "date>=?",
            arrayOf(fromDate.time.toString()),
            "date DESC"
        )

        var flagged = 0
        cursor?.use {
            while (it.moveToNext()) {
                val sender = it.getString(0) ?: "Unknown"
                val message = it.getString(1) ?: ""
                val date = it.getLong(2)

                val links = SmsParser.extractLinks(message)
                for (url in links) {
                    val result = ThreatChecker.checkThreat(url)
                    if (result.isThreat) {
                        LoggerHelper.logSuspiciousLink(
                            applicationContext,
                            url,
                            sender,
                            date,
                            result.reason,
                            result.level,
                            message
                        )
                        flagged++
                    }
                }
            }
        }

        ScanPreferencesHelper.setLastScanTime(applicationContext, System.currentTimeMillis())

        if (flagged > 0) {
            notifyUser(flagged)
        }

        Result.success()
    }

    private fun notifyUser(count: Int) {
        val builder = NotificationCompat.Builder(applicationContext, "auto_scan_channel")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("SecureMate: Scan Complete")
            .setContentText("$count suspicious link(s) found in background scan.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Check permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(applicationContext, permission)

            if (granted == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(99, builder.build())
            }
        } else {
            notificationManager.notify(99, builder.build())
        }
    }
}