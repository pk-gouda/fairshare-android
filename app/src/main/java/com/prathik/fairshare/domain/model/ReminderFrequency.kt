package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * How often a group reminder is sent to members.
 */
@Serializable
enum class ReminderFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}