package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Deletes a group permanently.
 * Only the group creator can do this.
 * The server will reject this if there are unsettled balances.
 */
class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        return groupRepository.deleteGroup(groupId)
    }
}
