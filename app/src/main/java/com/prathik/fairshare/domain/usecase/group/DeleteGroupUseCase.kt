package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Permanently deletes a group.
 * Only the group creator can do this.
 * Server rejects if there are unsettled balances.
 */
class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): ApiResult<Unit> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        return groupRepository.deleteGroup(groupId)
    }
}
