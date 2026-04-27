package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching notification rows locally.
 *
 * Populated when [NotificationRepositoryImpl.getAll] succeeds. On network
 * failure the cached rows are returned so Activity shows recent notifications
 * offline — in particular [NotificationType.EXPENSE_DELETED] rows, which give
 * the user access to the queue-backed restore flow via ExpenseDetailScreen.
 *
 * All fields mirror [NotificationResponse] exactly so no data is lost.
 * [type] is stored as a String for forward-compatibility with new backend types.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id           : String,
    val title          : String,
    val message        : String,
    val type           : String,
    val referenceId    : String?,
    val referenceType  : String?,
    val isRead         : Boolean,
    val createdAt      : String,
    val groupId        : String?,
    val groupName      : String?,
    val isGroupDeleted : Boolean,
    val isUserMember   : Boolean,
    val cachedAt       : Long = System.currentTimeMillis(),
)