package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0 ORDER BY expenseDate DESC")
    suspend fun getByGroupId(groupId: String): List<ExpenseEntity>

    /** All expenses for a group including soft-deleted rows — used to distinguish
     * 'empty because all offline-deleted' from 'empty because never cached'. */
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY expenseDate DESC")
    suspend fun getByGroupIdIncludingDeleted(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    /** Optimistically update the isDeleted flag for offline delete/restore. */
    @Query("UPDATE expenses SET isDeleted = :isDeleted WHERE id = :expenseId")
    suspend fun updateLocalDeletedStatus(expenseId: String, isDeleted: Boolean)

    @Query("DELETE FROM expenses WHERE otherUserId = :otherUserId")
    suspend fun deleteByOtherUserId(otherUserId: String)

    /** All non-deleted direct expenses with a given friend, newest first. */
    @Query("SELECT * FROM expenses WHERE otherUserId = :otherUserId AND isDeleted = 0 ORDER BY expenseDate DESC")
    suspend fun getByOtherUserId(otherUserId: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    /**
     * Delete group expenses while PRESERVING unsynced local CREATE_EXPENSE placeholders.
     * Uses a cross-table NOT IN subquery so active pending creates are not wiped during
     * a server-side refresh. The subquery references pending_operations in the same DB.
     */
    @Query("""
        DELETE FROM expenses
        WHERE groupId = :groupId
          AND id NOT IN (
              SELECT localResourceId FROM pending_operations
              WHERE operationType = 'CREATE_EXPENSE'
                AND status IN ('PENDING', 'SYNCING', 'FAILED_RETRYABLE')
                AND localResourceId IS NOT NULL
          )
    """)
    suspend fun deleteByGroupIdExcludingPendingCreates(groupId: String)

    /**
     * Delete direct friend expenses while preserving unsynced placeholders.
     */
    @Query("""
        DELETE FROM expenses
        WHERE otherUserId = :otherUserId
          AND id NOT IN (
              SELECT localResourceId FROM pending_operations
              WHERE operationType = 'CREATE_EXPENSE'
                AND status IN ('PENDING', 'SYNCING', 'FAILED_RETRYABLE')
                AND localResourceId IS NOT NULL
          )
    """)
    suspend fun deleteByOtherUserIdExcludingPendingCreates(otherUserId: String)

    /** Set otherUserId on a cached expense — used to propagate it from placeholder to server expense. */
    @Query("UPDATE expenses SET otherUserId = :otherUserId WHERE id = :expenseId")
    suspend fun updateOtherUserId(expenseId: String, otherUserId: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}