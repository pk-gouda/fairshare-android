package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.AuthProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("email")             val email: String,
    @SerialName("fullName")          val fullName: String,
    @SerialName("password")          val password: String,
    @SerialName("phoneNumber")       val phoneNumber: String? = null,
    @SerialName("preferredCurrency") val preferredCurrency: String? = null,
    @SerialName("language")          val language: String? = null,
)

@Serializable
data class LoginRequest(
    @SerialName("email")    val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class OAuthLoginRequest(
    @SerialName("email")             val email: String,
    @SerialName("fullName")          val fullName: String,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("authProvider")      val authProvider: AuthProvider,
)

@Serializable
data class ForgotPasswordRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class ResetPasswordRequest(
    @SerialName("token")       val token: String,
    @SerialName("newPassword") val newPassword: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("currentPassword") val currentPassword: String,
    @SerialName("newPassword")     val newPassword: String,
)
