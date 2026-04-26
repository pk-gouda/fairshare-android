package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpensePayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payers: List<ExpensePayerEntity>)

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getByExpenseId(expenseId: String): List<ExpensePayerEntity>

    @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)
}