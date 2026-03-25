package com.prathik.fairshare.domain.model


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
)