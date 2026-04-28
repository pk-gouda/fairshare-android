package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingBalanceImpactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(impact: PendingBalanceImpactEntity)

    /** All active UPDATE impacts for a specific group — used by GroupDetail/GroupsHome overlay. */
    @Query("SELECT * FROM pending_balance_impact WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: String): List<PendingBalanceImpactEntity>

    /** All active UPDATE impacts for a specific direct-friend context. */
    @Query("SELECT * FROM pending_balance_impact WHERE otherUserId = :friendId AND groupId IS NULL")
    suspend fun getByFriendId(friendId: String): List<PendingBalanceImpactEntity>

    /** Delete impact row when the op is SYNCED or CANCELLED/FAILED_PERMANENT. */
    @Query("DELETE FROM pending_balance_impact WHERE operationId = :operationId")
    suspend fun deleteByOperationId(operationId: String)

    /** Delete all impact rows for an expense (e.g. on full expense deletion). */
    @Query("DELETE FROM pending_balance_impact WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)

    /** All impact rows — used by GroupsHome to compute optimistic deltas across all groups. */
    @Query("SELECT * FROM pending_balance_impact")
    suspend fun getAll(): List<PendingBalanceImpactEntity>
}