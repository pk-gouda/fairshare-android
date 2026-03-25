package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    @SerialName("success")   val success: Boolean,
    @SerialName("message")   val message: String? = null,
    @SerialName("data")      val data: T? = null,
    @SerialName("errors")    val errors: List<FieldError>? = null,
    @SerialName("timestamp") val timestamp: String? = null,
) {
    @Serializable
    data class FieldError(
        @SerialName("field")         val field: String,
        @SerialName("message")       val message: String,
        @SerialName("rejectedValue") val rejectedValue: String? = null,
    )
}
