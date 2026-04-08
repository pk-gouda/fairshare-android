package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImportRequest(
    @SerialName("type")            val type: String,
    @SerialName("groupName")       val groupName: String? = null,
    @SerialName("csvContent")      val csvContent: String,
    @SerialName("importerCsvName") val importerCsvName: String? = null,
)

@Serializable
data class AssignPlaceholderRequest(
    @SerialName("placeholderUserId") val placeholderUserId: String,
    @SerialName("friendUserId")      val friendUserId: String,
)

@Serializable
data class ClaimIdentityRequest(
    @SerialName("placeholderUserId") val placeholderUserId: String,
)

@Serializable
data class UnclaimIdentityRequest(
    @SerialName("wrongClaimerUserId") val wrongClaimerUserId: String,
    @SerialName("originalCsvName")    val originalCsvName: String,
)