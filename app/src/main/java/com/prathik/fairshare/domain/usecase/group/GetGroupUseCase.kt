package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches a single group by ID.
 */
class GetGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): ApiResult<Group> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        return groupRepository.getGroup(groupId)
    }
}
