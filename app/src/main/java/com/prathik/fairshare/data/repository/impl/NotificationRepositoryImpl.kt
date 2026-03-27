package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.NotificationApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationService: NotificationApiService,
) : NotificationRepository {

    override suspend fun getAll(): ApiResult<List<Notification>> =
        safeApiCall { notificationService.getAll() }
            .mapSuccess { list -> list.map { it.toDomain() } }

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
}
