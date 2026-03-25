package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.SettleType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettleRequest(
    @SerialName("otherUserId")       val otherUserId: String,
    @SerialName("type")              val type: SettleType,
    @SerialName("groupId")           val groupId: String? = null,
    @SerialName("amount")            val amount: Double? = null,
    @SerialName("currency")          val currency: String? = null,
    @SerialName("paymentMethod")     val paymentMethod: String? = null,
    @SerialName("paymentProofImage") val paymentProofImage: String? = null,
    @SerialName("notes")             val notes: String? = null,
)
