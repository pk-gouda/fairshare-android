package com.prathik.fairshare.domain.model

data class Balance(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,       // negative = you owe, positive = they owe you
    val currency: String,
    val groupId: String?,
    val groupName: String?,
)