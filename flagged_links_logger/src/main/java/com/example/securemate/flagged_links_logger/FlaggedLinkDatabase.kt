package com.example.securemate.flagged_links_logger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FlaggedLink::class], version = 2, exportSchema = false)
abstract class FlaggedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): FlaggedLinkDao

    companion object {
        @Volatile private var INSTANCE: FlaggedLinkDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flagged_links ADD COLUMN message TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): FlaggedLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlaggedLinkDatabase::class.java,
                    "flagged_links_db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}