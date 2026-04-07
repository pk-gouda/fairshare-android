package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateSettlementRequest(
    @SerialName("amount")        val amount: Double? = null,
    @SerialName("currency")      val currency: String? = null,
    @SerialName("notes")         val notes: String? = null,
    @SerialName("paymentMethod") val paymentMethod: String? = null,
)