package com.example.securemate.flagged_links_logger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flagged_links")
data class FlaggedLink(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val timestamp: Long,
    val reason: String
)
