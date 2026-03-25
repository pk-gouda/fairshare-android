package com.prathik.fairshare.domain.usecase.settlement

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject

/**
 * Settles balances with another user.
 * Validates required fields based on settlement type.
 */
class SettleUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(
        otherUserId: String,
        type: SettleType,
        groupId: String?,
        amount: Double?,
        currency: String?,
        paymentMethod: String?,
        notes: String?,
    ): ApiResult<List<Settlement>> {
        if (otherUserId.isBlank()) {
            return ApiResult.ValidationError("User ID cannot be empty")
        }
        if (type == SettleType.GROUP && groupId.isNullOrBlank()) {
            return ApiResult.ValidationError("Group ID is required for GROUP settlement")
        }
        if (type == SettleType.PARTIAL) {
            if (amount == null || amount <= 0) {
                return ApiResult.ValidationError("Amount must be greater than 0 for partial settlement")
            }
            if (currency.isNullOrBlank()) {
                return ApiResult.ValidationError("Currency is required for partial settlement")
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
