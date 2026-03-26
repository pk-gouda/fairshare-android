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

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
