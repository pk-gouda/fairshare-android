package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupResponse(
    @SerialName("id")               val id: String,
    @SerialName("name")             val name: String,
    @SerialName("groupImage")       val groupImage: String? = null,
    @SerialName("type")             val type: String,
    @SerialName("createdById")      val createdById: String,
    @SerialName("createdByName")    val createdByName: String,
    @SerialName("tripStartDate")    val tripStartDate: String? = null,
    @SerialName("tripEndDate")      val tripEndDate: String? = null,
    @SerialName("simplifyDebts")    val simplifyDebts: Boolean,
    @SerialName("inviteCode")       val inviteCode: String,
    @SerialName("groupNotes")       val groupNotes: String? = null,
    @SerialName("lastActivityDate") val lastActivityDate: String? = null,
    @SerialName("isArchived")       val isArchived: Boolean,
    @SerialName("memberCount")      val memberCount: Int,
    @SerialName("createdAt")        val createdAt: String,
    @SerialName("lastRemainderIndex") val lastRemainderIndex: Int = 0,
    @SerialName("defaultCurrency")   val defaultCurrency: String = "USD",
)