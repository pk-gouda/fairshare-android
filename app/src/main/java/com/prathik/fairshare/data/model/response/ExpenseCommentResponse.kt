package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseCommentResponse(
    @SerialName("id")           val id: String,
    @SerialName("userId")       val userId: String,
    @SerialName("userFullName") val userFullName: String,
    @SerialName("comment")      val comment: String,
    @SerialName("createdAt")    val createdAt: String,
)
