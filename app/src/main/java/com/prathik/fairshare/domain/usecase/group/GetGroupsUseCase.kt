package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches all groups the current user is a member of.
 */
class GetGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Group>> {
        return groupRepository.getMyGroups()
    }
}
