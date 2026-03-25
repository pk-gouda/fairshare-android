package com.prathik.fairshare.domain.usecase.settlement

import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject

/**
 * Fetches the settlement breakdown between the current user and another user.
 * Shows what is owed broken down by group and non-group expenses.
 * Used by the Settle Up screen to show per-group amounts.
 */
class GetSettlementBreakdownUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(otherUserId: String): Result<Map<String, Any>> {
        if (otherUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return settlementRepository.getBreakdown(otherUserId)
    }
}
