package com.prathik.fairshare.domain.usecase.balance

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.BalanceRepository
import javax.inject.Inject

/**
 * Fetches overall balance summary for the current user.
 * Returns total owed to you and total you owe.
 * Used by the Groups Home screen header to show net balance.
 */
class GetBalanceSummaryUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    suspend operator fun invoke(): ApiResult<Map<String, Any>> {
        return balanceRepository.getBalanceSummary()
    }
}
