package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettlementStatus

/**
 * Room entity for caching settlement data locally.
 *
 * Cache scopes:
 *   groupId != null  → GROUP settlement, indexed by groupId
 *   groupId == null  → DIRECT/NON_GROUP settlement between two users
 *
 * Indexed by groupId and both participant IDs so GroupDetail and FriendDetail
 * can each query their slice without a full table scan.
 *
 * Cache replacement strategy: delete by scope then re-insert (same as BalanceEntity).
 *   - getGroupSettlements  → deletes by groupId, then inserts fresh list
 *   - getHistory(userId)   → deletes by (payerId=userId OR receiverId=userId), then inserts
 *
 * Does not cache paymentProofImage blobs — those are fetched on demand.
 */
@Entity(
    tableName = "settlements",
    indices = [
        Index("groupId"),
        Index("payerId"),
        Index("receiverId"),
    ],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val payerId           : String,
    val payerName         : String,
    val receiverId        : String,
    val receiverName      : String,
    val amount            : Double,
    val currency          : String,
    val groupId           : String?,
    val groupName         : String?,
    val status            : String,   // SettlementStatus.name
    val notes             : String?,
    val paymentMethod     : String?,
    val recordedById      : String,
    val recordedByName    : String,
    val settlementDate    : String,
    val completedAt       : String?,
    val createdAt         : String,
    val settleType        : String?,
    val isFullSettle      : Boolean,
    val groupBalanceSnapshot: String?,
    val cancelledAt       : String?,
    val cancelledByName   : String?,
    val cancelledById     : String?,
) {
    fun toDomain(): Settlement = Settlement(
        id                   = id,
        payerId              = payerId,
        payerName            = payerName,
        receiverId           = receiverId,
        receiverName         = receiverName,
        amount               = amount,
        currency             = currency,
        groupId              = groupId,
        groupName            = groupName,
        status               = try { SettlementStatus.valueOf(status) }
        catch (_: IllegalArgumentException) { SettlementStatus.PENDING },
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
        cancelledAt          = cancelledAt,
        cancelledByName      = cancelledByName,
        cancelledById        = cancelledById,
    )
}

fun Settlement.toEntity(): SettlementEntity = SettlementEntity(
    id                   = id,
    payerId              = payerId,
    payerName            = payerName,
    receiverId           = receiverId,
    receiverName         = receiverName,
    amount               = amount,
    currency             = currency,
    groupId              = groupId,
    groupName            = groupName,
    status               = status.name,
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
    cancelledAt          = cancelledAt,
    cancelledByName      = cancelledByName,
    cancelledById        = cancelledById,
)