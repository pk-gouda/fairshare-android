package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Removes a member from a group.
 * The server will reject this if the member has unsettled balances in the group.
 */
class RemoveMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String, memberId: String): Result<Unit> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        if (memberId.isBlank()) {
            return Result.failure(IllegalArgumentException("Member ID cannot be empty"))
        }
        return groupRepository.removeMember(groupId, memberId)
    }
}
