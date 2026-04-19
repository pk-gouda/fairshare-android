package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The type of notification — determines icon, color,
 * and deep link destination when tapped.
 */
@Serializable
enum class NotificationType {
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    EXPENSE_RESTORED,
    SETTLEMENT_RECEIVED,
    SETTLEMENT_CONFIRMED,
    SETTLEMENT_CANCELLED,
    FRIEND_REQUEST_RECEIVED,
    FRIEND_REQUEST_ACCEPTED,
    GROUP_INVITE_RECEIVED,
    GROUP_MEMBER_JOINED,
    GROUP_MEMBER_REMOVED,
    GROUP_DELETED,
    SETTLE_UP_REMINDER
}