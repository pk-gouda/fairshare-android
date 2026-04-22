package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettlementChangeLogResponse(
    @SerialName("id")             val id: String,
    @SerialName("settlementId")   val settlementId: String,
    @SerialName("changedById")    val changedById: String,
    @SerialName("changedByName")  val changedByName: String,
    @SerialName("changedAt")      val changedAt: String,
    @SerialName("fieldName")      val fieldName: String,
    @SerialName("oldValue")       val oldValue: String? = null,
    @SerialName("newValue")       val newValue: String? = null,
)