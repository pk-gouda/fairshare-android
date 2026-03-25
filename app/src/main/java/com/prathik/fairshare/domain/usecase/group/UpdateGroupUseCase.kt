package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Updates group name, description, or simplifyDebts setting.
 * Only passes non-null values to the repository — null means no change.
 */
class UpdateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        name: String?,
        description: String?,
        simplifyDebts: Boolean?,
    ): Result<Group> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        if (name != null && name.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        if (name != null && name.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Group name must be at least 2 characters"))
        }
        return groupRepository.updateGroup(
            groupId       = groupId,
            name          = name?.trim(),
            description   = description?.trim(),
            simplifyDebts = simplifyDebts,
        )
    }
}
