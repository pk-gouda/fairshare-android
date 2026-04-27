package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    /** All cached notifications, newest first. */
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    suspend fun getAll(): List<NotificationEntity>

    /** Replace entire cache with a fresh list (called after every successful fetch). */
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}