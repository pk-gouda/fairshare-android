package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ExpenseItemResponse
import com.prathik.fairshare.domain.model.ExpenseItem

/**
 * Maps ExpenseItemResponse DTO to ExpenseItem domain model.
 *
 * assignedTo defaults to empty list if null —
 * items start unassigned after receipt scanning.
 * Users assign them on the ItemAssignmentScreen.
 */
fun ExpenseItemResponse.toDomain(): ExpenseItem = ExpenseItem(
    id           = id,
    name         = name,
    price        = price,
    quantity     = quantity,
    totalPrice   = totalPrice,
    taxCode      = taxCode,
    taxRate      = taxRate,
    taxAmount    = taxAmount,
    totalWithTax = totalWithTax,
    assignedTo   = assignedTo?.map { it.toDomain() } ?: emptyList(),
)

/**
 * Maps AssignedUser DTO to domain AssignedUser.
 */
fun ExpenseItemResponse.AssignedUser.toDomain(): ExpenseItem.AssignedUser =
    ExpenseItem.AssignedUser(
        userId   = userId,
        fullName = fullName,
    )