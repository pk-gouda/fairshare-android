package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching expense data locally.
 * Stores lightweight version — payers/splits stored as JSON strings.
 * Full nested objects fetched from network when needed.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val description: String,
    val totalAmount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    val addedById: String,
    val addedByName: String,
    val splitType: String,
    val category: String?,
    val notes: String?,
    val expenseDate: String,
    val isDeleted: Boolean,
    val commentCount: Int,
    val itemCount: Int,
    val yourPaid: Double,
    val yourShare: Double,
    val yourBalance: Double,
    val createdAt: String,
    val updatedAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
