package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SettlementDao {

    // ── Reads ──────────────────────────────────────────────────────────────────

    /** All settlements for a group, newest first. */
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY settlementDate DESC, createdAt DESC")
    suspend fun getByGroupId(groupId: String): List<SettlementEntity>

    /**
     * All settlements where the given user was payer OR receiver.
     * Used as the cache for getHistory(otherUserId) — returns the full set;
     * caller filters to the relevant counterparty pair.
     */
    @Query("""
        SELECT * FROM settlements
        WHERE (payerId = :userId OR receiverId = :userId)
        ORDER BY settlementDate DESC, createdAt DESC
    """)
    suspend fun getByUserId(userId: String): List<SettlementEntity>

    /** Direct (non-group) settlements between two specific users. */
    @Query("""
        SELECT * FROM settlements
        WHERE groupId IS NULL
          AND ((payerId = :userId AND receiverId = :otherUserId)
               OR (payerId = :otherUserId AND receiverId = :userId))
        ORDER BY settlementDate DESC, createdAt DESC
    """)
    suspend fun getDirectBetween(userId: String, otherUserId: String): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SettlementEntity?

    // ── Writes ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settlements: List<SettlementEntity>)

    /** Scoped delete for GROUP settlements — called before re-inserting fresh list. */
    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    /**
     * Scoped delete for direct settlements between exactly two users.
     * Used before re-inserting fresh history for one friend pair — does NOT
     * affect other friends' cached settlements.
     */
    @Query("""
        DELETE FROM settlements
        WHERE groupId IS NULL
          AND ((payerId = :userId AND receiverId = :otherUserId)
               OR (payerId = :otherUserId AND receiverId = :userId))
    """)
    suspend fun deleteDirectBetween(userId: String, otherUserId: String)

    /** Remove one settlement by id — used after delete/cancel mutations. */
    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteById(settlementId: String)

    /** Full wipe — only for logout / account deletion. */
    @Query("DELETE FROM settlements")
    suspend fun deleteAll()
}