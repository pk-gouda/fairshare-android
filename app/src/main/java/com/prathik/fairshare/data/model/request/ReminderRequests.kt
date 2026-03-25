package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.data.model.enums.ReminderFrequency
import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/groups/{groupId}/reminders
 *
 * dayOfWeek  — required when frequency = WEEKLY (1=Monday through 7=Sunday)
 * dayOfMonth — required when frequency = MONTHLY (1-31)
 */
@Serializable
data class CreateReminderRequest(
    val groupId: String,
    val frequency: ReminderFrequency,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val notifyViaApp: Boolean? = null,
    val notifyViaEmail: Boolean? = null,
)

/**
 * Request body for PUT /api/reminders/{reminderId}
 * All fields are optional — only non-null fields are updated.
 * isActive can be used to toggle a reminder on/off without deleting it.
 */
@Serializable
data class UpdateReminderRequest(
    val frequency: ReminderFrequency? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val notifyViaApp: Boolean? = null,
    val notifyViaEmail: Boolean? = null,
    val isActive: Boolean? = null,
)
