package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseItemResponse(
    @SerialName("id")           val id: String,
    @SerialName("name")         val name: String,
    @SerialName("price")        val price: Double,
    @SerialName("quantity")     val quantity: Int? = null,
    @SerialName("totalPrice")   val totalPrice: Double,
    @SerialName("taxCode")      val taxCode: String? = null,
    @SerialName("taxRate")      val taxRate: Double? = null,
    @SerialName("taxAmount")    val taxAmount: Double? = null,
    @SerialName("totalWithTax") val totalWithTax: Double? = null,
    @SerialName("assignedTo")   val assignedTo: List<AssignedUser>? = null,
) {
    @Serializable
    data class AssignedUser(
        @SerialName("userId")   val userId: String,
        @SerialName("fullName") val fullName: String,
    )
}
