package com.prathik.fairshare.domain.model

enum class NotificationType {
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    SETTLEMENT_RECEIVED,
    SETTLEMENT_CONFIRMED,
    SETTLEMENT_CANCELLED,
    FRIEND_REQUEST_RECEIVED,
    FRIEND_REQUEST_ACCEPTED,
    GROUP_INVITE_RECEIVED,
    GROUP_MEMBER_JOINED,
    GROUP_DELETED,
    SETTLE_UP_REMINDER
}

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val referenceId: String?,
    val referenceType: String?,
    val isRead: Boolean,
    val createdAt: String,
)