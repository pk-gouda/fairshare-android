package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<BalanceEntity>)

    // ── Scoped reads ──────────────────────────────────────────────────────────
    // Each method reads ONLY the rows it owns. groupId constraints close the
    // remaining loophole: a badly-shaped API response with unexpected groupId
    // fields cannot contaminate total-balance rows.

    /** ALL_BALANCES scope, top-level total rows only (groupId must be empty). */
    @Query("SELECT * FROM balances WHERE userId = :userId AND cacheScope = 'ALL_BALANCES' AND groupId = ''")
    suspend fun getAllBalanceRows(userId: String): List<BalanceEntity>

    /** FRIEND_NET scope — total relationship balance with one friend (groupId = ''). */
    @Query("SELECT * FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND cacheScope = 'FRIEND_NET' AND groupId = ''")
    suspend fun getFriendNetRows(userId: String, otherUserId: String): List<BalanceEntity>

    /** FRIEND_BREAKDOWN scope — per-group component rows for a friend (groupId must be non-empty). */
    @Query("SELECT * FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND cacheScope = 'FRIEND_BREAKDOWN' AND groupId != ''")
    suspend fun getFriendBreakdownRows(userId: String, otherUserId: String): List<BalanceEntity>

    /** GROUP_BALANCE scope — per-member balances within one group. */
    @Query("SELECT * FROM balances WHERE userId = :userId AND groupId = :groupId AND cacheScope = 'GROUP_BALANCE'")
    suspend fun getGroupBalanceRows(userId: String, groupId: String): List<BalanceEntity>

    // ── Scoped deletes ────────────────────────────────────────────────────────

    /** Replace FRIEND_NET rows for a friend before reinserting fresh data. */
    @Query("DELETE FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND cacheScope = 'FRIEND_NET' AND groupId = ''")
    suspend fun deleteNetBalanceForFriend(userId: String, otherUserId: String)

    /** Replace FRIEND_BREAKDOWN rows for a friend before reinserting fresh data. */
    @Query("DELETE FROM balances WHERE userId = :userId AND otherUserId = :otherUserId AND cacheScope = 'FRIEND_BREAKDOWN'")
    suspend fun deleteBreakdownForFriend(userId: String, otherUserId: String)

    /**
     * Delete FRIEND_BREAKDOWN rows for a specific group across ALL friends.
     * Called after group delete/leave so the per-group breakdown in FriendDetail
     * does not show a deleted group's balance row in the offline fallback.
     * GROUP_BALANCE rows for the group are handled by deleteByGroupId separately.
     */
    @Query("DELETE FROM balances WHERE userId = :userId AND groupId = :groupId AND cacheScope = 'FRIEND_BREAKDOWN'")
    suspend fun deleteBreakdownByGroupId(userId: String, groupId: String)

    /** Replace GROUP_BALANCE rows for a group before reinserting fresh data. */
    @Query("DELETE FROM balances WHERE userId = :userId AND groupId = :groupId AND cacheScope = 'GROUP_BALANCE'")
    suspend fun deleteByGroupId(userId: String, groupId: String)

    // ── Full-user operations (logout / cache clear only) ──────────────────────

    /**
     * Delete only ALL_BALANCES scope rows before reinserting fresh backend data.
     * Leaves FRIEND_NET, FRIEND_BREAKDOWN, and GROUP_BALANCE rows untouched.
     */
    @Query("DELETE FROM balances WHERE userId = :userId AND cacheScope = 'ALL_BALANCES'")
    suspend fun deleteAllBalanceRows(userId: String)

    /**
     * Delete all balance rows for this user across all scopes.
     * Use only for logout or a deliberate full cache reset — never for
     * per-screen refresh (that would re-introduce the double-count bug).
     */
    @Query("DELETE FROM balances WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    /** Delete the entire balance table — used for testing/factory reset only. */
    @Query("DELETE FROM balances")
    suspend fun deleteAll()
}