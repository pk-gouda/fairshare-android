package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reminder(
    val id: String,
    val groupId: String,
    val groupName: String,
    val createdById: String,
    val createdByName: String,
    val frequency: ReminderFrequency,
    val dayOfWeek: Int?,
    val dayOfMonth: Int?,
    val notifyViaApp: Boolean,
    val notifyViaEmail: Boolean,
    val isActive: Boolean,
    val lastSentAt: String?,
    val createdAt: String,
) : Parcelable