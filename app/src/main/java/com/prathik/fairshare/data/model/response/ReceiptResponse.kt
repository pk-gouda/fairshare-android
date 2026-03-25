package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptResponse(
    @SerialName("id")              val id: String,
    @SerialName("expenseId")       val expenseId: String? = null,
    @SerialName("scannedById")     val scannedById: String,
    @SerialName("imageUrl")        val imageUrl: String? = null,
    @SerialName("merchantName")    val merchantName: String? = null,
    @SerialName("merchantAddress") val merchantAddress: String? = null,
    @SerialName("subtotal")        val subtotal: Double? = null,
    @SerialName("taxAmount")       val taxAmount: Double? = null,
    @SerialName("tipAmount")       val tipAmount: Double? = null,
    @SerialName("totalAmount")     val totalAmount: Double,
    @SerialName("currency")        val currency: String? = null,
    @SerialName("paymentMethod")   val paymentMethod: String? = null,
    @SerialName("receiptDate")     val receiptDate: String? = null,
    @SerialName("scanConfidence")  val scanConfidence: String? = null,
    @SerialName("itemCount")       val itemCount: Int? = null,
    @SerialName("createdAt")       val createdAt: String,
)
