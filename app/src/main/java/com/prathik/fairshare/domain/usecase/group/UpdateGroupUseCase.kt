package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Updates group name, description, or simplifyDebts setting.
 * Only non-null values are updated.
 */
class UpdateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        name: String?,
        description: String?,
        simplifyDebts: Boolean?,
        defaultCurrency: String? = null,
    ): ApiResult<Group> {
        if (groupId.isBlank()) {
            return ApiResult.ValidationError("Group ID cannot be empty")
        }
        if (name != null && name.isBlank()) {
            return ApiResult.ValidationError("Group name cannot be empty")
        }
        if (name != null && name.trim().length < 2) {
            return ApiResult.ValidationError("Group name must be at least 2 characters")
        }
        return groupRepository.updateGroup(
            groupId         = groupId,
            name            = name?.trim(),
            description     = description?.trim(),
            simplifyDebts   = simplifyDebts,
            defaultCurrency = defaultCurrency,
        )
    }
}