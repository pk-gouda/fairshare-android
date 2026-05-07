package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Permanently deletes a group.
 * Any member can delete. Name confirmation required. Balance guard removed.
 * Server rejects if there are unsettled balances.
 */
class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String, confirmName: String): ApiResult<Unit> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        if (confirmName.isBlank()) {
            return ApiResult.ValidationError("Please type the group name to confirm deletion")
        }
        return groupRepository.deleteGroup(groupId, confirmName)
    }
}