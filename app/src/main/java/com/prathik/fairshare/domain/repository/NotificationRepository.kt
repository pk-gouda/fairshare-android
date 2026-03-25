package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.Notification

/**
 * Contract for notification-related operations.
 * Implementation lives in data/repository/impl/NotificationRepositoryImpl.kt
 */
interface NotificationRepository {

    /**
     * Fetches all notifications for the current user.
     */
    suspend fun getAll(): Result<List<Notification>>

    /**
     * Fetches only unread notifications.
     */
    suspend fun getUnread(): Result<List<Notification>>

    /**
     * Fetches the unread notification count.
     * Used for the badge on the Activity tab.
     */
    suspend fun getUnreadCount(): Result<Int>

    /**
     * Marks a single notification as read.
     */
    suspend fun markRead(notificationId: String): Result<Unit>

    /**
     * Marks all notifications as read.
     */
    suspend fun markAllRead(): Result<Unit>
}