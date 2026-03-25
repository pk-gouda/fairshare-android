package com.prathik.fairshare.data.model.enums

/**
 * How often a group reminder is sent to members.
 *
 * DAILY   — every day at the configured time
 * WEEKLY  — every week on the configured day
 * MONTHLY — every month on the configured date
 */
enum class ReminderFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}