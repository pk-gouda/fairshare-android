package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ReminderResponse
import com.prathik.fairshare.domain.model.Reminder
import com.prathik.fairshare.domain.model.ReminderFrequency

/**
 * Maps ReminderResponse DTO to Reminder domain model.
 *
 * frequency is stored as String in the DTO.
 * Safe conversion with fallback: unknown frequency → MONTHLY.
 */
fun ReminderResponse.toDomain(): Reminder = Reminder(
    id             = id,
    groupId        = groupId,
    groupName      = groupName,
    createdById    = createdById,
    createdByName  = createdByName,
    frequency      = frequency.toReminderFrequencySafe(),
    dayOfWeek      = dayOfWeek,
    dayOfMonth     = dayOfMonth,
    notifyViaApp   = notifyViaApp,
    notifyViaEmail = notifyViaEmail,
    isActive       = isActive,
    lastSentAt     = lastSentAt,
    createdAt      = createdAt,
)

private fun String.toReminderFrequencySafe(): ReminderFrequency =
    try { ReminderFrequency.valueOf(this) } catch (e: IllegalArgumentException) { ReminderFrequency.MONTHLY }
