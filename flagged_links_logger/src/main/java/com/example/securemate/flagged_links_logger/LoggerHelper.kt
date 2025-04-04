package com.example.securemate.flagged_links_logger

import android.content.Context
import com.example.securemate.threat_model.ThreatLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LoggerHelper {
    fun logSuspiciousLink(
        context: Context,
        url: String,
        sender: String,
        time: Long,
        reason: String,
        level: ThreatLevel,
        messageBody: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = FlaggedLinkDatabase.getInstance(context).linkDao()
            dao.insert(
                FlaggedLink(
                    url = url,
                    sender = sender,
                    timestamp = time,
                    reason = reason,
                    threatLevel = level.name,
                    message = messageBody
                )
            )
        }
    }
}
