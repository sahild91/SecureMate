package com.example.securemate.sms_scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.securemate.flagged_links_logger.LoggerHelper

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val messageBody = msg.messageBody
                Log.d("SecureMate", "Received SMS: $messageBody")

                val urls = SmsParser.extractLinks(messageBody)
                for (url in urls) {
                    val (isThreat, reason) = ThreatChecker.checkThreat(url)
                    if (isThreat) {
                        LoggerHelper.logSuspiciousLink(context, url, reason ?: "Unknown")

                        val channelId = "suspicious_sms_alert"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(channelId, "SMS Alerts", NotificationManager.IMPORTANCE_HIGH)
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.createNotificationChannel(channel)
                        }

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.stat_sys_warning)
                            .setContentTitle("⚠️ Suspicious Link Detected")
                            .setContentText(url)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)

                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(url.hashCode(), builder.build())
                    }
                }
            }
        }
    }
}
