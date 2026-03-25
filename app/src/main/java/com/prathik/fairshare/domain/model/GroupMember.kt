package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupMember(
    val id: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String?,
    val joinedAt: String,
) : Parcelable