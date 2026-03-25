package com.prathik.fairshare.domain.usecase.notification

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Marks all notifications as read.
 * Clears the unread badge on the bottom nav.
 */
class MarkAllReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): ApiResult<Unit> {
        return notificationRepository.markAllRead()
    }
}
