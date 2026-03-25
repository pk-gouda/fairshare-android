package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Joins a group using an invite code.
 */
class JoinGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(inviteCode: String): ApiResult<GroupMember> {
        if (inviteCode.isBlank()) {
            return ApiResult.ValidationError("Invite code cannot be empty")
        }
        return groupRepository.joinGroup(inviteCode.trim())
    }
}
