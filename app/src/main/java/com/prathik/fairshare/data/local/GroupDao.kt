package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE isArchived = 0 ORDER BY cachedAt DESC")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getById(groupId: String): GroupEntity?

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}
