package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InvitedFriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: InvitedFriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(friends: List<InvitedFriendEntity>)

    @Query("SELECT * FROM invited_friends ORDER BY invitedAt DESC")
    suspend fun getAll(): List<InvitedFriendEntity>

    @Query("SELECT * FROM invited_friends WHERE emailOrPhone = :email LIMIT 1")
    suspend fun findByEmail(email: String): InvitedFriendEntity?

    @Query("SELECT * FROM invited_friends WHERE displayName = :name AND isPlaceholder = 1 LIMIT 1")
    suspend fun findPlaceholderByName(name: String): InvitedFriendEntity?

    @Query("DELETE FROM invited_friends WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invited_friends")
    suspend fun deleteAll()
}