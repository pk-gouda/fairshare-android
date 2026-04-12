package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(friends: List<FriendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: FriendEntity)

    @Query("SELECT * FROM friends ORDER BY fullName ASC")
    suspend fun getAll(): List<FriendEntity>

    @Query("DELETE FROM friends WHERE id = :friendId")
    suspend fun deleteById(friendId: String)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()
}