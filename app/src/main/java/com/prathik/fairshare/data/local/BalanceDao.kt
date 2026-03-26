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

    @Query("DELETE FROM balances WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM balances")
    suspend fun deleteAll()
}
