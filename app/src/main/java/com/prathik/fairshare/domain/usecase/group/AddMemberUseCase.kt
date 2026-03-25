package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Adds a member to a group by their userId.
 * The server will reject this if the user is already a member.
 */
class AddMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String, userId: String): Result<GroupMember> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return groupRepository.addMember(groupId, userId)
    }
}
