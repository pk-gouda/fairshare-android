package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Balance(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    /** ISO datetime of the group's last activity — used for sorting group balance rows in timelines. */
    val groupLastActivity: String?,
) : Parcelable
