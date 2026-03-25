package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Joins a group using an invite code.
 * Validates the invite code is not empty before hitting the network.
 */
class JoinGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(inviteCode: String): Result<GroupMember> {
        if (inviteCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Invite code cannot be empty"))
        }
        return groupRepository.joinGroup(inviteCode.trim())
    }
}
