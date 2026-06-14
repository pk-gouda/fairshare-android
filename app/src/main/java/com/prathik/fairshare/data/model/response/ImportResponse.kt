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

/**
 * Response shape for GET /api/import/groups/{groupId}/unclaimed.
 *
 * The backend returns each unclaimed PLACEHOLDER as a map with these exact keys
 * (SplitWiseImportService.getUnclaimedMembers) — NOT a GroupMemberResponse. The
 * only fields the claim/assign UI consumes are placeholderUserId (used as the
 * claim/assign target) and csvName (the display label); the totals are available
 * for an optional balance preview. All fields default so a partial/extended
 * backend response never fails deserialization.
 */
@Serializable
data class UnclaimedMemberResponse(
    @SerialName("placeholderUserId")   val placeholderUserId: String = "",
    @SerialName("csvName")             val csvName: String = "",
    @SerialName("expenseInvolvement")  val expenseInvolvement: Int = 0,
    @SerialName("totalPaid")           val totalPaid: Double = 0.0,
    @SerialName("totalOwed")           val totalOwed: Double = 0.0,
    @SerialName("settlementPaid")      val settlementPaid: Double = 0.0,
    @SerialName("settlementReceived")  val settlementReceived: Double = 0.0,
    @SerialName("netBalance")          val netBalance: Double = 0.0,
)