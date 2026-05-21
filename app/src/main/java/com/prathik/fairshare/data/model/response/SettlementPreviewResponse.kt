package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from GET /api/settlements/preview.
 * Shows what allocations would be created without committing.
 */
@Serializable
data class SettlementPreviewResponse(
    @SerialName("totalAmount")        val totalAmount       : Double,
    @SerialName("currency")           val currency          : String,
    @SerialName("allocations")        val allocations       : List<AllocationPreview>,
    @SerialName("overpaymentAmount")  val overpaymentAmount : Double? = null,
    @SerialName("overpaymentMessage") val overpaymentMessage: String? = null,
) {
    @Serializable
    data class AllocationPreview(
        @SerialName("contextType")          val contextType         : String,
        @SerialName("groupId")              val groupId             : String? = null,
        @SerialName("groupName")            val groupName           : String? = null,
        @SerialName("effectivePayerId")     val effectivePayerId    : String,
        @SerialName("effectivePayerName")   val effectivePayerName  : String,
        @SerialName("effectiveReceiverId")  val effectiveReceiverId : String,
        @SerialName("effectiveReceiverName")val effectiveReceiverName: String,
        @SerialName("amount")               val amount              : Double,
        @SerialName("currency")             val currency            : String,
        @SerialName("isOverpaymentCredit")  val isOverpaymentCredit : Boolean = false,
    )
}