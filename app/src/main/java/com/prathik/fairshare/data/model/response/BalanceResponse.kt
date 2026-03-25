package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(
    @SerialName("userId")        val userId: String,
    @SerialName("otherUserId")   val otherUserId: String,
    @SerialName("otherUserName") val otherUserName: String,
    @SerialName("amount")        val amount: Double,
    @SerialName("currency")      val currency: String,
    @SerialName("groupId")       val groupId: String? = null,
    @SerialName("groupName")     val groupName: String? = null,
)
