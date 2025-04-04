package com.example.securemate.sms_scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.securemate.flagged_links_logger.LoggerHelper
import com.example.securemate.threat_model.ThreatChecker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val messageBody = msg.messageBody
                val sender = msg.originatingAddress ?: "Unknown"
                val timestamp = msg.timestampMillis

                val urls = SmsParser.extractLinks(messageBody)
                for (url in urls) {
                    val result = ThreatChecker.checkThreat(url)
                    if (result.isThreat) {
                        LoggerHelper.logSuspiciousLink(
                            context = context,
                            url = url,
                            sender = sender,
                            time = timestamp,
                            reason = result.reason,
                            level = result.level,
                            messageBody = messageBody
                        )

                        val channelId = "suspicious_sms_alert"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(channelId, "SMS Alerts", NotificationManager.IMPORTANCE_HIGH)
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.createNotificationChannel(channel)
                        }

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.stat_sys_warning)
                            .setContentTitle("⚠️ $sender sent suspicious link")
                            .setContentText("$url (Level: ${result.level})")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)

                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(url.hashCode(), builder.build())
                    }
                }
            }
        }
    }
}
