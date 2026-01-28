package com.nomyek.myapplication.data.dao

import androidx.room.*
import com.nomyek.myapplication.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    
    /**
     * Insert history
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long
    
    /**
     * Get all history (sorted by created time)
     */
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
}

