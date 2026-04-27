package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<BalanceEntity>)

    @Query("SELECT * FROM balances WHERE userId = :userId")
    suspend fun getByUserId(userId: String): List<BalanceEntity>

    /** Cached net balance rows for a specific friend (non-group, groupId = ''). */
    @Query("SELECT * FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND groupId = ''")
    suspend fun getByOtherUserId(userId: String, otherUserId: String): List<BalanceEntity>

    /** Cached group balance rows for a specific group. */
    @Query("SELECT * FROM balances WHERE userId = :userId AND groupId = :groupId")
    suspend fun getByGroupId(userId: String, groupId: String): List<BalanceEntity>

    /** Delete cached group balance rows for a specific group before re-inserting fresh data. */
    @Query("DELETE FROM balances WHERE userId = :userId AND groupId = :groupId")
    suspend fun deleteByGroupId(userId: String, groupId: String)

    /** Delete cached net-balance rows for a specific friend (non-group, groupId = ''). */
    @Query("DELETE FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND groupId = ''")
    suspend fun deleteNetBalanceForFriend(userId: String, otherUserId: String)

    /** Delete cached group-breakdown rows for a specific friend (non-empty groupId). */
    @Query("DELETE FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND groupId != ''")
    suspend fun deleteBreakdownForFriend(userId: String, otherUserId: String)

    @Query("DELETE FROM balances WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM balances")
    suspend fun deleteAll()
}