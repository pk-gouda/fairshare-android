package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GroupMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<GroupMemberEntity>)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: String): List<GroupMemberEntity>

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}