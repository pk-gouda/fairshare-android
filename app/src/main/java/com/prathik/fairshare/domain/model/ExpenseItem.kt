package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseItem(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int?,
    val totalPrice: Double,
    val taxCode: String?,
    val taxRate: Double?,
    val taxAmount: Double?,
    val totalWithTax: Double?,
    val assignedTo: List<AssignedUser>,
) : Parcelable {

    @Parcelize
    data class AssignedUser(
        val userId: String,
        val fullName: String,
    ) : Parcelable
}