package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseSplitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<ExpenseSplitEntity>)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getByExpenseId(expenseId: String): List<ExpenseSplitEntity>

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)
}