package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.BalanceResponse
import com.prathik.fairshare.domain.model.Balance

/**
 * Maps BalanceResponse DTO to Balance domain model.
 *
 * Sign convention is preserved exactly from the backend:
 * Positive amount = otherUser owes you (show in green)
 * Negative amount = you owe otherUser (show in orange)
 */
fun BalanceResponse.toDomain(): Balance = Balance(
    userId        = userId,
    otherUserId   = otherUserId,
    otherUserName = otherUserName,
    amount        = amount,
    currency      = currency,
    groupId       = groupId,
    groupName     = groupName,
)
