package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.NotificationDao
import com.prathik.fairshare.data.local.NotificationEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.NotificationApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.model.NotificationType
import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationService: NotificationApiService,
    private val notificationDao    : NotificationDao,
) : NotificationRepository {

    override suspend fun getCachedNotifications(): List<Notification> =
        notificationDao.getAll().map { it.toDomain() }

    override suspend fun getAll(): ApiResult<List<Notification>> {
        val result = safeApiCall { notificationService.getAll() }
        if (result is ApiResult.Success) {
            // Replace cache with fresh data so Activity is up-to-date when online.
            notificationDao.deleteAll()
            notificationDao.insertAll(result.data.map { it.toEntity() })
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        // Network failed — return cached notifications so EXPENSE_DELETED rows
        // remain reachable offline, allowing the user to navigate to ExpenseDetail
        // and trigger the queue-backed restore flow.
        val cached = notificationDao.getAll()
        if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getUnread(): ApiResult<List<Notification>> =
        safeApiCall { notificationService.getUnread() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getUnreadCount(): ApiResult<Int> =
        safeApiCall { notificationService.getUnreadCount() }
            .mapSuccess { map -> map["unreadCount"] ?: 0 }

    override suspend fun markRead(notificationId: String): ApiResult<Unit> =
        safeApiCall { notificationService.markRead(notificationId) }.mapSuccess { }

    override suspend fun markAllRead(): ApiResult<Unit> =
        safeApiCall { notificationService.markAllRead() }.mapSuccess { }


    private fun com.prathik.fairshare.data.model.response.NotificationResponse.toEntity() =
        NotificationEntity(
            id             = id,
            title          = title,
            message        = message,
            type           = type,
            referenceId    = referenceId,
            referenceType  = referenceType,
            isRead         = isRead,
            createdAt      = createdAt,
            groupId        = groupId,
            groupName      = groupName,
            isGroupDeleted = isGroupDeleted,
            isUserMember   = isUserMember,
        )

    private fun NotificationEntity.toDomain() = Notification(
        id             = id,
        title          = title,
        message        = message,
        type           = try { NotificationType.valueOf(type) }
        catch (e: Exception) { NotificationType.EXPENSE_ADDED },
        referenceId    = referenceId,
        referenceType  = referenceType,
        isRead         = isRead,
        createdAt      = createdAt,
        groupId        = groupId,
        groupName      = groupName,
        isGroupDeleted = isGroupDeleted,
        isUserMember   = isUserMember,
    )
}