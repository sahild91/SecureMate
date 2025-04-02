package com.example.securemate.flagged_links_logger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FlaggedLink::class], version = 1, exportSchema = false)
abstract class FlaggedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): FlaggedLinkDao

    companion object {
        @Volatile private var INSTANCE: FlaggedLinkDatabase? = null

        fun getInstance(context: Context): FlaggedLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlaggedLinkDatabase::class.java,
                    "flagged_links_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}