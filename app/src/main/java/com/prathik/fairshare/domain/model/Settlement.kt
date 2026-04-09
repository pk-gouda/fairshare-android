package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Settlement(
    val id: String,
    val payerId: String,
    val payerName: String,
    val receiverId: String,
    val receiverName: String,
    val amount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    val status: SettlementStatus,
    val notes: String?,
    val paymentMethod: String?,
    val recordedById: String,
    val recordedByName: String,
    val settlementDate: String,
    val completedAt: String?,
    val createdAt: String,
    // Settlement type: ALL, GROUP, NON_GROUP, PARTIAL
    val settleType: String?,
    // True when the full balance in scope was cleared (drives "fully settled" UI)
    val isFullSettle: Boolean,
    // JSON snapshot of per-group balances — only non-null for ALL+isFullSettle
    val groupBalanceSnapshot: String?,
) : Parcelable
