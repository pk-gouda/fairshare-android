package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Leaves a group as a member.
 * Any member can do this (except the sole owner if there are other members).
 */
class LeaveGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): ApiResult<Unit> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        return groupRepository.leaveGroup(groupId)
    }
}