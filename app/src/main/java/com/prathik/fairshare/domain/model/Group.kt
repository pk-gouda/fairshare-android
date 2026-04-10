package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val createdById: String,
    val createdByName: String,
    val inviteCode: String,
    val simplifyDebts: Boolean,
    val isArchived: Boolean,
    val memberCount: Int,
    val groupNotes: String?,
    val groupImage: String?,
    val lastActivityDate: String?,
    val tripStartDate: String?,
    val tripEndDate: String?,
    val createdAt: String,
    val lastRemainderIndex: Int = 0,
) : Parcelable