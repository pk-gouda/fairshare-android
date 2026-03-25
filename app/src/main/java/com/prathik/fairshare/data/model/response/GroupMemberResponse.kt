package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberResponse(
    @SerialName("id")                val id: String,
    @SerialName("userId")            val userId: String,
    @SerialName("fullName")          val fullName: String,
    @SerialName("email")             val email: String,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("joinedAt")          val joinedAt: String,
)
