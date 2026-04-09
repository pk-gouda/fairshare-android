package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.SettlementResponse
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettlementStatus

/**
 * Maps SettlementResponse DTO to Settlement domain model.
 */
fun SettlementResponse.toDomain(): Settlement = Settlement(
    id                   = id,
    payerId              = payerId,
    payerName            = payerName,
    receiverId           = receiverId,
    receiverName         = receiverName,
    amount               = amount,
    currency             = currency,
    groupId              = groupId,
    groupName            = groupName,
    status               = status.toSettlementStatusSafe(),
    notes                = notes,
    paymentMethod        = paymentMethod,
    recordedById         = recordedById,
    recordedByName       = recordedByName,
    settlementDate       = settlementDate,
    completedAt          = completedAt,
    createdAt            = createdAt,
    settleType           = settleType,
    isFullSettle         = isFullSettle,
    groupBalanceSnapshot = groupBalanceSnapshot,
)

private fun String.toSettlementStatusSafe(): SettlementStatus =
    try { SettlementStatus.valueOf(this) } catch (e: IllegalArgumentException) { SettlementStatus.PENDING }
