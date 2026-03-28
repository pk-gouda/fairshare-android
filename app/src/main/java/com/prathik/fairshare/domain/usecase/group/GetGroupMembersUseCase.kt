package com.prathik.fairshare.domain.usecase.group

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Fetches all members of a group.
 * Used in AddExpenseScreen to populate payer + split selectors.
 */
class GetGroupMembersUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(groupId: String): ApiResult<List<GroupMember>> {
        return groupRepository.getMembers(groupId)
    }
}