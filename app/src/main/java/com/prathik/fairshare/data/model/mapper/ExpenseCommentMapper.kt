package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ExpenseCommentResponse
import com.prathik.fairshare.domain.model.ExpenseComment

/**
 * Maps ExpenseCommentResponse DTO to ExpenseComment domain model.
 */
fun ExpenseCommentResponse.toDomain(): ExpenseComment = ExpenseComment(
    id           = id,
    userId       = userId,
    userFullName = userFullName,
    comment      = comment,
    createdAt    = createdAt,
)