package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.NotificationResponse
import com.prathik.fairshare.domain.model.Notification

/**
 * Maps NotificationResponse DTO to Notification domain model.
 *
 * referenceId and referenceType are used for deep linking —
 * they tell the app which screen to navigate to when
 * the notification is tapped in the Activity feed.
 */
fun NotificationResponse.toDomain(): Notification = Notification(
    id            = id,
    title         = title,
    message       = message,
    type          = type,
    referenceId   = referenceId,
    referenceType = referenceType,
    isRead        = isRead,
    createdAt     = createdAt,
)
