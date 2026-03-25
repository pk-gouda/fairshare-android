package com.prathik.fairshare.data.model.enums

/**
 * The type of notification — determines the icon, color,
 * and deep link destination when the notification is tapped.
 */
enum class NotificationType {
    // Expense events
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,

    // Settlement events
    SETTLEMENT_RECEIVED,
    SETTLEMENT_CONFIRMED,
    SETTLEMENT_CANCELLED,

    // Friend events
    FRIEND_REQUEST_RECEIVED,
    FRIEND_REQUEST_ACCEPTED,

    // Group events
    GROUP_INVITE_RECEIVED,
    GROUP_MEMBER_JOINED,
    GROUP_DELETED,

    // Reminders
    SETTLE_UP_REMINDER
}