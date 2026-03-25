package com.prathik.fairshare.domain.usecase.settlement

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject

/**
 * Fetches the full settlement history between the current user and another user.
 */
class GetSettlementHistoryUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(otherUserId: String): ApiResult<List<Settlement>> {
        if (otherUserId.isBlank()) {
            return ApiResult.ValidationError("User ID cannot be empty")
        }
        return settlementRepository.getHistory(otherUserId)
    }
}
