package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImportResponse(
    @SerialName("type")               val type: String? = null,
    @SerialName("groupId")            val groupId: String? = null,
    @SerialName("groupName")          val groupName: String? = null,
    @SerialName("inviteCode")         val inviteCode: String? = null,
    @SerialName("expensesCreated")    val expensesCreated: Int = 0,
    @SerialName("settlementsCreated") val settlementsCreated: Int = 0,
    @SerialName("rowsSkipped")        val rowsSkipped: Int = 0,
    @SerialName("totalRows")          val totalRows: Int = 0,
    @SerialName("members")            val members: List<ImportMemberEntry> = emptyList(),
    @SerialName("removedNames")       val removedNames: List<String> = emptyList(),
    @SerialName("warnings")           val warnings: List<String> = emptyList(),
)

@Serializable
data class ImportMemberEntry(
    @SerialName("placeholderUserId") val placeholderUserId: String = "",
    @SerialName("status")            val status: String = "",
    @SerialName("csvName")           val csvName: String = "",
)

@Serializable
data class ImportActionResponse(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("message") val message: String? = null,
)