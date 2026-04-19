package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/** Restores a soft-deleted group within the 30-day window. Only the creator can restore. */
class RestoreGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): ApiResult<Group> {
        if (groupId.isBlank()) return ApiResult.ValidationError("Group ID cannot be empty")
        return groupRepository.restoreGroup(groupId)
    }
}