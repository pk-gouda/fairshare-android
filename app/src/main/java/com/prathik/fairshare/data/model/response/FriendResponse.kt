package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendResponse(
    @SerialName("id")                val id: String,
    @SerialName("fullName")          val fullName: String,
    @SerialName("email")             val email: String,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
)
