package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Balance(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    /** ISO datetime of the group's last activity — used for sorting group balance rows in timelines. */
    val groupLastActivity: String?,
) : Parcelable

/**
 * Per-currency net breakdown for multi-currency balance bars.
 * Single canonical definition — imported by GroupsViewModel, FriendsViewModel, AccountViewModel.
 */
data class BalanceCurrencyEntry(
    val currency: String,
    val owedToMe: Double,
    val youOwe: Double,
    val net: Double,   // positive = others owe you, negative = you owe
)