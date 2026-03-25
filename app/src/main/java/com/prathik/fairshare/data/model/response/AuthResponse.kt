package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("accessToken")  val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("tokenType")    val tokenType: String,
    @SerialName("expiresIn")    val expiresIn: Long,
    @SerialName("user")         val user: UserResponse? = null,
)
