package com.prathik.fairshare.domain.usecase.settlement

import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject

/**
 * Fetches the full settlement history between the current user and another user.
 * Ordered by date descending — most recent first.
 */
class GetSettlementHistoryUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(otherUserId: String): Result<List<Settlement>> {
        if (otherUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return settlementRepository.getHistory(otherUserId)
    }
}
