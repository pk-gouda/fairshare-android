package com.prathik.fairshare.domain.usecase.settlement

import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject

/**
 * Settles balances with another user.
 *
 * type options:
 * - "ALL"       — settles everything between two users across all groups
 * - "GROUP"     — settles only a specific group's balance (requires groupId)
 * - "NON_GROUP" — settles only non-group direct expenses
 * - "PARTIAL"   — settles a custom amount (requires amount + currency)
 *
 * Settlements are immediately COMPLETED — no confirmation step needed.
 */
class SettleUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(
        otherUserId: String,
        type: String,
        groupId: String?,
        amount: Double?,
        currency: String?,
        paymentMethod: String?,
        notes: String?,
    ): Result<List<Settlement>> {
        if (otherUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        if (type == "GROUP" && groupId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Group ID is required for GROUP settlement"))
        }
        if (type == "PARTIAL") {
            if (amount == null || amount <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be greater than 0 for partial settlement"))
            }
            if (currency.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Currency is required for partial settlement"))
            }
        }
        return settlementRepository.settle(
            otherUserId   = otherUserId,
            type          = type,
            groupId       = groupId,
            amount        = amount,
            currency      = currency,
            paymentMethod = paymentMethod,
            notes         = notes?.trim(),
        )
    }
}
