package com.prathik.fairshare.domain.usecase.notification

import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Fetches the unread notification count.
 * Used to show the badge number on the Activity tab in the bottom nav.
 */
class GetUnreadCountUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): Result<Int> {
        return notificationRepository.getUnreadCount()
    }
}
