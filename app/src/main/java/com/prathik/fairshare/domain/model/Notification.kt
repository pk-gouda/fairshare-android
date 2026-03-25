package com.prathik.fairshare.domain.model



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