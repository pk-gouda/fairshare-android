package com.prathik.fairshare.domain.usecase.notification

import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Fetches all notifications for the current user.
 * Ordered by date descending — most recent first.
 */
class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): Result<List<Notification>> {
        return notificationRepository.getAll()
    }
}
