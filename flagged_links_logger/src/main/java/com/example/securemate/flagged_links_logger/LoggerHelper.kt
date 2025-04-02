package com.example.securemate.flagged_links_logger

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LoggerHelper {
    fun logSuspiciousLink(context: Context, url: String, reason: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = FlaggedLinkDatabase.getInstance(context).linkDao()
            dao.insert(FlaggedLink(url = url, timestamp = System.currentTimeMillis(), reason = reason))
        }
    }
}