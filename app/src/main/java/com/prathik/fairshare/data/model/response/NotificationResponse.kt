package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.NotificationType
import kotlinx.serialization.Serializable

/**
 * API response DTO for a notification.
 * Maps to NotificationResponse.java record on the backend.
 *
 * referenceId and referenceType are used for deep linking:
 * - referenceType "EXPENSE" + referenceId → navigate to ExpenseDetail
 * - referenceType "GROUP"   + referenceId → navigate to GroupDetail
 * - referenceType "FRIEND"  + referenceId → navigate to FriendDetail
 */
@Serializable
data class NotificationResponse(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val isRead: Boolean,
    val createdAt: String,
)
