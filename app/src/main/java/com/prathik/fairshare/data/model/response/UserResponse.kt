package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.AuthProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName("id")                  val id: String,
    @SerialName("email")               val email: String,
    @SerialName("fullName")            val fullName: String,
    @SerialName("phoneNumber")         val phoneNumber: String? = null,
    @SerialName("profilePictureUrl")   val profilePictureUrl: String? = null,
    @SerialName("authProvider")        val authProvider: AuthProvider,
    @SerialName("accountStatus")       val accountStatus: AccountStatus,
    @SerialName("preferredCurrency")   val preferredCurrency: String,
    @SerialName("language")            val language: String,
    @SerialName("notificationEnabled") val notificationEnabled: Boolean,
    @SerialName("createdAt")           val createdAt: String,
)
