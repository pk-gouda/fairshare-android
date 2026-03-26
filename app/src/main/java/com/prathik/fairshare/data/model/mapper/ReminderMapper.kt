package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ReminderResponse
import com.prathik.fairshare.domain.model.Reminder

/**
 * Maps ReminderResponse DTO to Reminder domain model.
 *
 * dayOfWeek  — only populated when frequency = WEEKLY (1=Monday, 7=Sunday)
 * dayOfMonth — only populated when frequency = MONTHLY (1-31)
 * lastSentAt — null if the reminder has never been triggered yet
 */
fun ReminderResponse.toDomain(): Reminder = Reminder(
    id            = id,
    groupId       = groupId,
    groupName     = groupName,
    createdById   = createdById,
    createdByName = createdByName,
    frequency     = frequency,
    dayOfWeek     = dayOfWeek,
    dayOfMonth    = dayOfMonth,
    notifyViaApp  = notifyViaApp,
    notifyViaEmail = notifyViaEmail,
    isActive      = isActive,
    lastSentAt    = lastSentAt,
    createdAt     = createdAt,
)