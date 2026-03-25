package com.prathik.fairshare.domain.model



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
    val payers: List<PayerDetail>,
    val splits: List<SplitDetail>,
    val commentCount: Int,
    val itemCount: Int,
    val yourPaid: Double,
    val yourShare: Double,
    val yourBalance: Double,
    val createdAt: String,
    val updatedAt: String,
) {
    data class PayerDetail(
        val userId: String,
        val fullName: String,
        val amountPaid: Double,
    )

    data class SplitDetail(
        val userId: String,
        val fullName: String,
        val amountOwed: Double,
        val percentage: Double?,
        val shares: Int?,
        val isSettled: Boolean,
    )
}