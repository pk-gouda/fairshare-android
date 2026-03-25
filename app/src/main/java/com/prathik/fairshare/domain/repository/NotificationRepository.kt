package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Notification

/**
 * Contract for notification-related operations.
 * Implementation lives in data/repository/impl/NotificationRepositoryImpl.kt
 */
interface NotificationRepository {

    /**
     * Fetches all notifications for the current user.
     * Ordered by date descending — most recent first.
     */
    suspend fun getAll(): ApiResult<List<Notification>>

    /**
     * Fetches only unread notifications.
     * Used to populate the Activity tab feed.
     */
    suspend fun getUnread(): ApiResult<List<Notification>>

    /**
     * Fetches the unread notification count.
     * Used for the badge number on the Activity tab in the bottom nav.
     */
    suspend fun getUnreadCount(): ApiResult<Int>

    /**
     * Marks a single notification as read.
     */
    suspend fun markRead(notificationId: String): ApiResult<Unit>

    /**
     * Marks all notifications as read.
     * Called when user taps "Mark all read" in the Activity tab.
     * Clears the unread badge on the bottom nav.
     */
    suspend fun markAllRead(): ApiResult<Unit>
}