package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches all balances within a group.
 * Each balance represents what one member owes another within that group.
 * Positive amount = other user owes you. Negative = you owe them.
 */
class GetGroupBalancesUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): Result<List<Balance>> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        return groupRepository.getGroupBalances(groupId)
    }
}
