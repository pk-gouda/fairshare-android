package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Creates a new group with the current user as admin.
 * Validates group name before hitting the network.
 */
class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(
        name: String,
        type: String,
        description: String?,
    ): ApiResult<Group> {
        if (name.isBlank()) {
            return ApiResult.ValidationError("Group name cannot be empty")
        }
        if (name.trim().length < 2) {
            return ApiResult.ValidationError("Group name must be at least 2 characters")
        }
        if (name.trim().length > 50) {
            return ApiResult.ValidationError("Group name cannot exceed 50 characters")
        }
        return groupRepository.createGroup(
            name        = name.trim(),
            type        = type,
            description = description?.trim(),
        )
    }
}