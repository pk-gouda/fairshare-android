package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val id: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String?,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
) : Parcelable {
    val isPlaceholder: Boolean get() = accountStatus == AccountStatus.PLACEHOLDER
    val isInvited: Boolean get() = accountStatus == AccountStatus.INVITED
    val isActive: Boolean get() = accountStatus == AccountStatus.ACTIVE
}

@Parcelize
data class Friendship(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val receiverId: String,
    val receiverName: String,
    val receiverEmail: String? = null,
    val receiverAccountStatus: AccountStatus = AccountStatus.ACTIVE,
    val status: FriendStatus,
    val friendshipType: FriendshipType,
    val createdAt: String,
) : Parcelable