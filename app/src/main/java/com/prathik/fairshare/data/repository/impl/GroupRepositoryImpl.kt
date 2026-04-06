package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.GroupDao
import com.prathik.fairshare.data.local.GroupEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.AddMemberRequest
import com.prathik.fairshare.data.model.request.CreateGroupRequest
import com.prathik.fairshare.data.model.request.JoinGroupRequest
import com.prathik.fairshare.data.model.request.UpdateGroupRequest
import com.prathik.fairshare.data.network.api.GroupApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.prathik.fairshare.domain.model.GroupType

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupService: GroupApiService,
    private val groupDao: GroupDao,
) : GroupRepository {

    override suspend fun getMyGroups(): ApiResult<List<Group>> {
        // Return cached data immediately if available
        val cached = groupDao.getAll()
        if (cached.isNotEmpty()) {
            // Refresh in background — caller gets cache instantly
            refreshGroupsFromNetwork()
            return ApiResult.Success(cached.map { it.toDomain() })
        }
        // No cache — must wait for network
        return refreshGroupsFromNetwork()
    }

    private suspend fun refreshGroupsFromNetwork(): ApiResult<List<Group>> {
        val result = safeApiCall { groupService.getMyGroups() }
        if (result is ApiResult.Success) {
            val entities = result.data.map { it.toEntity() }
            groupDao.deleteAll()
            groupDao.insertAll(entities)
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getGroup(groupId: String): ApiResult<Group> {
        // Return cached group immediately if available
        val cached = groupDao.getById(groupId)
        if (cached != null) {
            // Refresh in background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val result = safeApiCall { groupService.getGroup(groupId) }
                if (result is ApiResult.Success) groupDao.insert(result.data.toEntity())
            }
            return ApiResult.Success(cached.toDomain())
        }
        val result = safeApiCall { groupService.getGroup(groupId) }
        if (result is ApiResult.Success) groupDao.insert(result.data.toEntity())
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun createGroup(
        name: String,
        type: String,
        description: String?,
    ): ApiResult<Group> =
        safeApiCall {
            groupService.createGroup(
                CreateGroupRequest(
                    name = name,
                    type = type.toGroupTypeSafe(),
                    description = description,
                )
            )
        }.mapSuccess { it.toDomain() }

    private fun String.toGroupTypeSafe(): GroupType =
        try {
            GroupType.valueOf(this)
        } catch (e: IllegalArgumentException) {
            GroupType.OTHER
        }

    override suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
        simplifyDebts: Boolean?,
    ): ApiResult<Group> =
        safeApiCall {
            groupService.updateGroup(groupId, UpdateGroupRequest(name, description, simplifyDebts))
        }.mapSuccess { it.toDomain() }

    override suspend fun deleteGroup(groupId: String): ApiResult<Unit> {
        val result = safeApiCall { groupService.deleteGroup(groupId) }.mapSuccess { }
        if (result is ApiResult.Success) {
            groupDao.deleteById(groupId)
        }
        return result
    }

    override suspend fun leaveGroup(groupId: String): ApiResult<Unit> {
        val result = safeApiCall { groupService.leaveGroup(groupId) }.mapSuccess { }
        if (result is ApiResult.Success) {
            groupDao.deleteById(groupId)
        }
        return result
    }

    override suspend fun joinGroup(inviteCode: String): ApiResult<GroupMember> =
        safeApiCall {
            groupService.joinGroup(JoinGroupRequest(inviteCode))
        }.mapSuccess { it.toDomain() }

    override suspend fun addMember(groupId: String, userId: String): ApiResult<GroupMember> =
        safeApiCall {
            groupService.addMember(groupId, AddMemberRequest(userId))
        }.mapSuccess { it.toDomain() }

    override suspend fun removeMember(groupId: String, memberId: String): ApiResult<Unit> =
        safeApiCall { groupService.removeMember(groupId, memberId) }.mapSuccess { }

    override suspend fun getMembers(groupId: String): ApiResult<List<GroupMember>> =
        safeApiCall { groupService.getMembers(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun archiveGroup(groupId: String): ApiResult<Unit> =
        safeApiCall { groupService.archiveGroup(groupId) }.mapSuccess { }

    override suspend fun unarchiveGroup(groupId: String): ApiResult<Unit> =
        safeApiCall { groupService.unarchiveGroup(groupId) }.mapSuccess { }

    override suspend fun getGroupBalances(groupId: String): ApiResult<List<Balance>> =
        safeApiCall { groupService.getGroupBalances(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getGroupSettlements(groupId: String): ApiResult<List<Settlement>> =
        safeApiCall { groupService.getGroupSettlements(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    private fun com.prathik.fairshare.data.model.response.GroupResponse.toEntity() = GroupEntity(
        id = id,
        name = name,
        type = type,
        groupImage = groupImage,
        createdById = createdById,
        createdByName = createdByName,
        tripStartDate = tripStartDate,
        tripEndDate = tripEndDate,
        simplifyDebts = simplifyDebts,
        inviteCode = inviteCode,
        groupNotes = groupNotes,
        lastActivityDate = lastActivityDate,
        isArchived = isArchived,
        memberCount = memberCount,
        createdAt = createdAt,
    )

    private fun GroupEntity.toDomain() = Group(
        id = id,
        name = name,
        type = type.toGroupTypeSafe(),
        createdById = createdById,
        createdByName = createdByName,
        inviteCode = inviteCode,
        simplifyDebts = simplifyDebts,
        isArchived = isArchived,
        memberCount = memberCount,
        groupNotes = groupNotes,
        groupImage = groupImage,
        lastActivityDate = lastActivityDate,
        tripStartDate = tripStartDate,
        tripEndDate = tripEndDate,
        createdAt = createdAt,
    )
}