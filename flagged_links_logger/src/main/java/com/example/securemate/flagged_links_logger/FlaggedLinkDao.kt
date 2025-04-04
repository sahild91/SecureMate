package com.example.securemate.flagged_links_logger

import androidx.room.*

@Dao
interface FlaggedLinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: FlaggedLink)

    @Query("SELECT * FROM flagged_links ORDER BY timestamp DESC")
    suspend fun getAll(): List<FlaggedLink>

    @Query("DELETE FROM flagged_links")
    suspend fun clearAll()
}