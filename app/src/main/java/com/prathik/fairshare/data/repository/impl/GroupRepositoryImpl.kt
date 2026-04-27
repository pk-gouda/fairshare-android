package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.GroupDao
import com.prathik.fairshare.data.local.GroupMemberDao
import com.prathik.fairshare.data.local.GroupMemberEntity
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
import com.prathik.fairshare.data.model.response.GroupPreviewResponse
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
    private val groupService   : GroupApiService,
    private val groupDao       : GroupDao,
    private val groupMemberDao : GroupMemberDao,
) : GroupRepository {

    override suspend fun getMyGroups(): ApiResult<List<Group>> {
        // Always fetch from network to ensure new/deleted groups appear immediately.
        val result = refreshGroupsFromNetwork()
        // Fall back to cache if network fails
        if (result is ApiResult.NetworkError) {
            val cached = groupDao.getAll()
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result
    }

    private suspend fun refreshGroupsFromNetwork(): ApiResult<List<Group>> {
        val result = safeApiCall { groupService.getMyGroups() }
        if (result is ApiResult.Success) {
            val entities = result.data.map { it.toGroupEntity() }
            groupDao.deleteAll()
            groupDao.insertAll(entities)
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getGroup(groupId: String): ApiResult<Group> {
        // Always fetch from network to reflect latest group state.
        val result = safeApiCall { groupService.getGroup(groupId) }
        if (result is ApiResult.Success) groupDao.insert(result.data.toGroupEntity())
        // Fall back to cache if network fails
        if (result is ApiResult.NetworkError) {
            val cached = groupDao.getById(groupId)
            if (cached != null) return ApiResult.Success(cached.toDomain())
        }
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
        defaultCurrency: String?,
    ): ApiResult<Group> {
        val raw = safeApiCall {
            groupService.updateGroup(groupId, UpdateGroupRequest(name, description, simplifyDebts, defaultCurrency))
        }
        if (raw is ApiResult.Success) {
            groupDao.insert(raw.data.toGroupEntity())
        }
        return raw.mapSuccess { it.toDomain() }
    }

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

    override suspend fun getMembers(groupId: String): ApiResult<List<GroupMember>> {
        val result = safeApiCall { groupService.getMembers(groupId) }
        if (result is ApiResult.Success) {
            // Cache so AddExpenseScreen can show Paid By / Split sections offline.
            groupMemberDao.deleteByGroupId(groupId)
            groupMemberDao.insertAll(result.data.map { it.toMemberEntity(groupId) })
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        val cached = groupMemberDao.getByGroupId(groupId)
        if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun archiveGroup(groupId: String): ApiResult<Unit> =
        safeApiCall { groupService.archiveGroup(groupId) }.mapSuccess { }

    override suspend fun unarchiveGroup(groupId: String): ApiResult<Unit> =
        safeApiCall { groupService.unarchiveGroup(groupId) }.mapSuccess { }

    override suspend fun restoreGroup(groupId: String): ApiResult<Group> =
        safeApiCall { groupService.restoreGroup(groupId) }
            .mapSuccess { it.toDomainDirect() }

    override suspend fun getDeletedGroups(): ApiResult<List<Group>> =
        safeApiCall { groupService.getDeletedGroups() }
            .mapSuccess { list -> list.map { it.toDomainDirect() } }

    override suspend fun getGroupBalances(groupId: String): ApiResult<List<Balance>> =
        safeApiCall { groupService.getGroupBalances(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getAllGroupBalances(groupId: String): ApiResult<List<Balance>> =
        safeApiCall { groupService.getAllGroupBalances(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getGroupSettlements(groupId: String): ApiResult<List<Settlement>> =
        safeApiCall { groupService.getGroupSettlements(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun previewGroup(inviteCode: String): ApiResult<GroupPreviewResponse> =
        safeApiCall { groupService.previewGroup(inviteCode) }

    override suspend fun regenerateInviteCode(groupId: String): ApiResult<String> =
        safeApiCall { groupService.regenerateInviteCode(groupId) }

    private fun com.prathik.fairshare.data.model.response.GroupResponse.toGroupEntity() = GroupEntity(
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
        lastRemainderIndex = lastRemainderIndex,
        defaultCurrency    = defaultCurrency,
    )

    private fun com.prathik.fairshare.data.model.response.GroupResponse.toDomainDirect() = Group(
        id = id,
        name = name,
        type = type.toGroupTypeSafe(),
        createdById = createdById,
        createdByName = createdByName,
        inviteCode = inviteCode ?: "",
        simplifyDebts = simplifyDebts ?: false,
        isArchived = isArchived ?: false,
        memberCount = memberCount ?: 0,
        groupNotes = groupNotes,
        groupImage = groupImage,
        lastActivityDate = lastActivityDate,
        tripStartDate = tripStartDate,
        tripEndDate = tripEndDate,
        createdAt = createdAt,
        lastRemainderIndex = lastRemainderIndex ?: 0,
        defaultCurrency = defaultCurrency,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
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
        lastRemainderIndex = lastRemainderIndex,
        defaultCurrency    = defaultCurrency,
        isDeleted          = isDeleted,
        deletedAt          = deletedAt,
    )


    // ── GroupMember cache helpers ─────────────────────────────────────────────

    private fun com.prathik.fairshare.data.model.response.GroupMemberResponse.toMemberEntity(
        groupId: String,
    ) = GroupMemberEntity(
        id               = id,
        groupId          = groupId,
        userId           = userId,
        fullName         = fullName,
        email            = email,
        profilePictureUrl = profilePictureUrl,
        joinedAt         = joinedAt,
    )

    private fun GroupMemberEntity.toDomain() = GroupMember(
        id                = id,
        userId            = userId,
        fullName          = fullName,
        email             = email,
        profilePictureUrl = profilePictureUrl,
        joinedAt          = joinedAt,
    )
}