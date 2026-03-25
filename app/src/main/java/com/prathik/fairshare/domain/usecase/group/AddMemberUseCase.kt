package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Adds a member to a group by their userId.
 */
class AddMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String, userId: String): ApiResult<GroupMember> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        if (userId.isBlank()) {
            return ApiResult.ValidationError("User ID cannot be empty")
        }
        return groupRepository.addMember(groupId, userId)
    }
}
