package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettlementResponse(
    @SerialName("id")                    val id: String,
    @SerialName("payerId")               val payerId: String,
    @SerialName("payerName")             val payerName: String,
    @SerialName("receiverId")            val receiverId: String,
    @SerialName("receiverName")          val receiverName: String,
    @SerialName("amount")                val amount: Double,
    @SerialName("currency")              val currency: String,
    @SerialName("groupId")               val groupId: String? = null,
    @SerialName("groupName")             val groupName: String? = null,
    @SerialName("status")                val status: String,
    @SerialName("notes")                 val notes: String? = null,
    @SerialName("paymentMethod")         val paymentMethod: String? = null,
    @SerialName("paymentProofImage")     val paymentProofImage: String? = null,
    @SerialName("recordedById")          val recordedById: String,
    @SerialName("recordedByName")        val recordedByName: String,
    @SerialName("settlementDate")        val settlementDate: String,
    @SerialName("completedAt")           val completedAt: String? = null,
    @SerialName("createdAt")             val createdAt: String,
    // Settlement type: ALL, GROUP, NON_GROUP, PARTIAL
    @SerialName("settleType")            val settleType: String? = null,
    // True when the full balance in scope was cleared
    @SerialName("isFullSettle")          val isFullSettle: Boolean = false,
    // JSON snapshot of per-group balances at time of ALL settle
    @SerialName("groupBalanceSnapshot")  val groupBalanceSnapshot: String? = null,
    // Populated when status == CANCELLED
    @SerialName("cancelledAt")           val cancelledAt: String? = null,
    @SerialName("cancelledByName")       val cancelledByName: String? = null,
    @SerialName("cancelledById")         val cancelledById: String? = null,
)