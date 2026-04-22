package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseResponse(
    @SerialName("id")            val id: String,
    @SerialName("description")   val description: String,
    @SerialName("totalAmount")   val totalAmount: Double,
    @SerialName("currency")      val currency: String,
    @SerialName("groupId")       val groupId: String? = null,
    @SerialName("groupName")     val groupName: String? = null,
    @SerialName("addedById")     val addedById: String,
    @SerialName("addedByName")   val addedByName: String,
    @SerialName("updatedById")   val updatedById: String? = null,
    @SerialName("updatedByName") val updatedByName: String? = null,
    @SerialName("splitType")     val splitType: String,
    @SerialName("category")      val category: String? = null,
    @SerialName("receipt")       val receipt: ReceiptSummary? = null,
    @SerialName("notes")         val notes: String? = null,
    @SerialName("expenseDate")   val expenseDate: String,
    @SerialName("isDeleted")     val isDeleted: Boolean,
    @SerialName("deletedByName")  val deletedByName: String? = null,
    @SerialName("deletedAt")      val deletedAt: String? = null,
    @SerialName("payers")        val payers: List<PayerDetail>? = null,
    @SerialName("splits")        val splits: List<SplitDetail>? = null,
    @SerialName("commentCount")  val commentCount: Int,
    @SerialName("itemCount")     val itemCount: Int,
    @SerialName("yourPaid")      val yourPaid: Double,
    @SerialName("yourShare")     val yourShare: Double,
    @SerialName("yourBalance")   val yourBalance: Double,
    @SerialName("createdAt")     val createdAt: String,
    @SerialName("updatedAt")       val updatedAt: String,
    @SerialName("repeatInterval")  val repeatInterval: String? = null,
    @SerialName("nextRepeatDate")  val nextRepeatDate: String? = null,
    @SerialName("isRecurring")     val isRecurring: Boolean = false,
    @SerialName("isTemplate")      val isTemplate: Boolean = false,
    @SerialName("canEdit")         val canEdit: Boolean = false,
) {
    @Serializable
    data class PayerDetail(
        @SerialName("userId")     val userId: String,
        @SerialName("fullName")   val fullName: String,
        @SerialName("amountPaid") val amountPaid: Double,
    )

    @Serializable
    data class SplitDetail(
        @SerialName("userId")     val userId: String,
        @SerialName("fullName")   val fullName: String,
        @SerialName("amountOwed") val amountOwed: Double,
        @SerialName("percentage") val percentage: Double? = null,
        @SerialName("shares")     val shares: Int? = null,
        @SerialName("isSettled")  val isSettled: Boolean,
    )

    @Serializable
    data class ReceiptSummary(
        @SerialName("receiptId")      val receiptId: String,
        @SerialName("imageUrl")       val imageUrl: String? = null,
        @SerialName("merchantName")   val merchantName: String? = null,
        @SerialName("totalAmount")    val totalAmount: Double,
        @SerialName("scanConfidence") val scanConfidence: String? = null,
        @SerialName("itemCount")      val itemCount: Int? = null,
        @SerialName("receiptDate")    val receiptDate: String? = null,
    )
}