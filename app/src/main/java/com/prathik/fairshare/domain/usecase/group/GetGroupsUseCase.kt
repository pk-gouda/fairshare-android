package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches all groups the current user is a member of.
 * Returns an empty list if the user has no groups.
 */
class GetGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(): Result<List<Group>> {
        return groupRepository.getMyGroups()
    }
}
