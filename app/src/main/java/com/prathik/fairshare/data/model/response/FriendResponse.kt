package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.AccountStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendResponse(
    @SerialName("id")                val id: String,
    @SerialName("fullName")          val fullName: String,
    @SerialName("email")             val email: String? = null,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("accountStatus")     val accountStatus: AccountStatus = AccountStatus.ACTIVE,
)