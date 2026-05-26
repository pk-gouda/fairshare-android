package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.GroupType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    @SerialName("name")           val name: String,
    @SerialName("type")           val type: GroupType,
    @SerialName("description")    val description: String? = null,
    @SerialName("tripStartDate")  val tripStartDate: String? = null,
    @SerialName("tripEndDate")    val tripEndDate: String? = null,
)

@Serializable
data class UpdateGroupRequest(
    @SerialName("name")            val name: String? = null,
    @SerialName("description")     val description: String? = null,
    @SerialName("simplifyDebts")   val simplifyDebts: Boolean? = null,
    @SerialName("defaultCurrency") val defaultCurrency: String? = null,
    @SerialName("tripStartDate")   val tripStartDate: String? = null,
    @SerialName("tripEndDate")     val tripEndDate: String? = null,
)

@Serializable
data class JoinGroupRequest(
    @SerialName("inviteCode") val inviteCode: String,
)

@Serializable
data class AddMemberRequest(
    @SerialName("userId") val userId: String,
)