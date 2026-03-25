package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.ReminderFrequency
import kotlinx.serialization.Serializable

/**
 * API response DTO for a group reminder.
 * Maps to ReminderResponse.java record on the backend.
 *
 * dayOfWeek is used when frequency = WEEKLY (1=Monday through 7=Sunday)
 * dayOfMonth is used when frequency = MONTHLY (1-31)
 * lastSentAt is null if the reminder has never been sent yet.
 */
@Serializable
data class ReminderResponse(
    val id: String,
    val groupId: String,
    val groupName: String,
    val createdById: String,
    val createdByName: String,
    val frequency: ReminderFrequency,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val notifyViaApp: Boolean,
    val notifyViaEmail: Boolean,
    val isActive: Boolean,
    val lastSentAt: String? = null,
    val createdAt: String,
)
