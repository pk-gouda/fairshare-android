package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.NotificationResponse
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.model.NotificationType

/**
 * Maps NotificationResponse DTO to Notification domain model.
 *
 * type is stored as String in the DTO to prevent deserialization crashes
 * when the backend adds new notification types.
 * Safe conversion with fallback: unknown type → EXPENSE_ADDED.
 */
fun NotificationResponse.toDomain(): Notification = Notification(
    id             = id,
    title          = title,
    message        = message,
    type           = type.toNotificationTypeSafe(),
    referenceId    = referenceId,
    referenceType  = referenceType,
    isRead         = isRead,
    createdAt      = createdAt,
    groupId        = groupId,
    groupName      = groupName,
    isGroupDeleted = isGroupDeleted,
    isUserMember   = isUserMember,
)

private fun String.toNotificationTypeSafe(): NotificationType =
    try { NotificationType.valueOf(this) } catch (e: IllegalArgumentException) { NotificationType.EXPENSE_ADDED }