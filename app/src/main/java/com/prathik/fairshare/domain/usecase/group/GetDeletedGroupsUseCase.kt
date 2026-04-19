package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/** Fetches soft-deleted groups for the current user — shown in Recently Deleted. */
class GetDeletedGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Group>> =
        groupRepository.getDeletedGroups()
}