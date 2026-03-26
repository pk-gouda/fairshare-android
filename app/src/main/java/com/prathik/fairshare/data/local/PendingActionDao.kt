package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingActionEntity)

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_actions SET retryCount = :count, lastAttemptAt = :attemptAt WHERE id = :id")
    suspend fun updateRetry(id: String, count: Int, attemptAt: Long)

    @Query("DELETE FROM pending_actions")
    suspend fun deleteAll()
}
