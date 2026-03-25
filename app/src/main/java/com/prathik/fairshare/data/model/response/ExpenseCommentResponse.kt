package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for a comment on an expense.
 * Maps to ExpenseCommentResponse.java record on the backend.
 */
@Serializable
data class ExpenseCommentResponse(
    val id: String,
    val userId: String,
    val userFullName: String,
    val comment: String,
    val createdAt: String,
)
