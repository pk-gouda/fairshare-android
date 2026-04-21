package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Expense(
    val id: String,
    val description: String,
    val totalAmount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    val addedById: String,
    val addedByName: String,
    val splitType: SplitType,
    val category: ExpenseCategory?,
    val notes: String?,
    val expenseDate: String,
    val isDeleted: Boolean,
    val deletedByName: String? = null,
    val deletedAt: String? = null,
    val payers: List<PayerDetail>,
    val splits: List<SplitDetail>,
    val commentCount: Int,
    val itemCount: Int,
    val yourPaid: Double,
    val yourShare: Double,
    val yourBalance: Double,
    val createdAt: String,
    val updatedAt: String,
    val receipt: ReceiptSummary? = null,
    val repeatInterval: String? = null,
    val nextRepeatDate: String? = null,
    val isRecurring: Boolean = false,
    val isTemplate: Boolean = false,
) : Parcelable {

    @Parcelize
    data class ReceiptSummary(
        val receiptId     : String,
        val imageUrl      : String?,
        val merchantName  : String?,
        val totalAmount   : Double,
        val scanConfidence: String?,
        val itemCount     : Int?,
        val receiptDate   : String?,
    ) : Parcelable

    @Parcelize
    data class PayerDetail(
        val userId: String,
        val fullName: String,
        val amountPaid: Double,
    ) : Parcelable

    @Parcelize
    data class SplitDetail(
        val userId: String,
        val fullName: String,
        val amountOwed: Double,
        val percentage: Double?,
        val shares: Int?,
        val isSettled: Boolean,
    ) : Parcelable
}