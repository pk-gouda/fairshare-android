package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    @SerialName("fullName")            val fullName: String? = null,
    @SerialName("phoneNumber")         val phoneNumber: String? = null,
    @SerialName("preferredCurrency")   val preferredCurrency: String? = null,
    @SerialName("language")            val language: String? = null,
    @SerialName("notificationEnabled") val notificationEnabled: Boolean? = null,
    @SerialName("timezone")             val timezone: String? = null,
)