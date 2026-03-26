package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.SettlementResponse
import com.prathik.fairshare.domain.model.Settlement

/**
 * Maps SettlementResponse DTO to Settlement domain model.
 *
 * Note: In the current backend, settlements are immediately COMPLETED.
 * completedAt will equal settlementDate in most cases.
 */
fun SettlementResponse.toDomain(): Settlement = Settlement(
    id              = id,
    payerId         = payerId,
    payerName       = payerName,
    receiverId      = receiverId,
    receiverName    = receiverName,
    amount          = amount,
    currency        = currency,
    groupId         = groupId,
    groupName       = groupName,
    status          = status,
    notes           = notes,
    paymentMethod   = paymentMethod,
    recordedById    = recordedById,
    recordedByName  = recordedByName,
    settlementDate  = settlementDate,
    completedAt     = completedAt,
    createdAt       = createdAt,
)
