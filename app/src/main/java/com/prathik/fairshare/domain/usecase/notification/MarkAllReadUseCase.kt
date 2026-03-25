package com.prathik.fairshare.domain.usecase.notification

import com.prathik.fairshare.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Marks all notifications as read.
 * Called when user taps "Mark all read" in the Activity tab.
 * Clears the unread badge on the bottom nav.
 */
class MarkAllReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        return notificationRepository.markAllRead()
    }
}
