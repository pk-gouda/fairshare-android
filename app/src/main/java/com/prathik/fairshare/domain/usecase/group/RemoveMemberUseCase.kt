package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Removes a member from a group.
 * Server rejects if member has unsettled balances.
 */
class RemoveMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String, memberId: String): ApiResult<Unit> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        if (memberId.isBlank()) {
            return ApiResult.ValidationError("Member ID cannot be empty")
        }
        return groupRepository.removeMember(groupId, memberId)
    }
}
