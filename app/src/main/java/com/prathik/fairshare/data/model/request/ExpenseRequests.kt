package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExpenseRequest(
    @SerialName("groupId")        val groupId: String?,
    @SerialName("description")    val description: String,
    @SerialName("totalAmount")    val totalAmount: Double,
    @SerialName("currency")       val currency: String,
    @SerialName("splitType")      val splitType: SplitType? = null,
    @SerialName("category")       val category: ExpenseCategory? = null,
    @SerialName("notes")          val notes: String? = null,
    @SerialName("expenseDate")    val expenseDate: String? = null,
    @SerialName("payerData")      val payerData: Map<String, Double>? = null,
    @SerialName("splitData")      val splitData: Map<String, Double>? = null,
    @SerialName("idempotencyKey") val idempotencyKey: String? = null,
    @SerialName("receiptId")      val receiptId: String? = null,
)

@Serializable
data class UpdateExpenseRequest(
    @SerialName("description")    val description: String? = null,
    @SerialName("totalAmount")    val totalAmount: Double? = null,
    @SerialName("currency")       val currency: String? = null,
    @SerialName("splitType")      val splitType: SplitType? = null,
    @SerialName("category")       val category: ExpenseCategory? = null,
    @SerialName("notes")          val notes: String? = null,
    @SerialName("expenseDate")    val expenseDate: String? = null,
    @SerialName("payerData")      val payerData: Map<String, Double>? = null,
    @SerialName("splitData")      val splitData: Map<String, Double>? = null,
    @SerialName("idempotencyKey") val idempotencyKey: String? = null,
)

@Serializable
data class AddCommentRequest(
    @SerialName("comment") val comment: String,
)

@Serializable
data class ItemAssignmentRequest(
    @SerialName("assignments") val assignments: Map<String, List<String>>,
)