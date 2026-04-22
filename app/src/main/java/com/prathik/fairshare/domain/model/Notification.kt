package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val referenceId: String?,
    val referenceType: String?,
    val isRead: Boolean,
    val createdAt: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val isGroupDeleted: Boolean = false,
) : Parcelable