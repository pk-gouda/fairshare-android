package com.prathik.fairshare.domain.usecase.balance

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.repository.BalanceRepository
import javax.inject.Inject

/**
 * Fetches all balances for the current user across all groups and direct expenses.
 * Positive amount = other user owes you.
 * Negative amount = you owe other user.
 */
class GetAllBalancesUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Balance>> {
        return balanceRepository.getAllBalances()
    }
}
