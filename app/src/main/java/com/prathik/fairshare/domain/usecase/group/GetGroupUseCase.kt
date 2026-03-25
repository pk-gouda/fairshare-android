package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches a single group by ID.
 * Returns failure if the group doesn't exist or user is not a member.
 */
class GetGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): Result<Group> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        return groupRepository.getGroup(groupId)
    }
}
