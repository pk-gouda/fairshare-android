package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Settlement

/**
 * Contract for all group-related operations.
 * Implementation lives in data/repository/impl/GroupRepositoryImpl.kt
 */
interface GroupRepository {

    /**
     * Fetches all groups the current user is a member of.
     * Returns empty list if the user has no groups yet.
     */
    suspend fun getMyGroups(): ApiResult<List<Group>>

    /**
     * Fetches a single group by ID.
     * Returns [ApiResult.NotFound] if group doesn't exist or user is not a member.
     */
    suspend fun getGroup(groupId: String): ApiResult<Group>

    /**
     * Creates a new group with the current user as admin.
     * Returns [ApiResult.ValidationError] if name is blank or too long.
     */
    suspend fun createGroup(
        name: String,
        type: String,
        description: String?,
    ): ApiResult<Group>

    /**
     * Updates group name, description, or simplifyDebts setting.
     * Only non-null fields are updated.
     */
    suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
        simplifyDebts: Boolean?,
        defaultCurrency: String? = null,
    ): ApiResult<Group>

    /**
     * Permanently deletes a group.
     * Returns [ApiResult.Forbidden] if user is not the group creator.
     * Returns [ApiResult.Conflict] if group has unsettled balances.
     */
    suspend fun deleteGroup(groupId: String): ApiResult<Unit>
    suspend fun leaveGroup(groupId: String): ApiResult<Unit>

    /**
     * Joins a group using an invite code.
     * Returns [ApiResult.NotFound] if invite code is invalid.
     * Returns [ApiResult.Conflict] if user is already a member.
     */
    suspend fun joinGroup(inviteCode: String): ApiResult<GroupMember>

    /**
     * Adds a member to a group by their userId.
     * Returns [ApiResult.Conflict] if user is already a member.
     */
    suspend fun addMember(groupId: String, userId: String): ApiResult<GroupMember>

    /**
     * Removes a member from a group.
     * Returns [ApiResult.Conflict] if member has unsettled balances.
     */
    suspend fun removeMember(groupId: String, memberId: String): ApiResult<Unit>

    /**
     * Fetches all members of a group.
     */
    suspend fun getMembers(groupId: String): ApiResult<List<GroupMember>>

    /**
     * Archives a group — hides it from the main list but keeps all data.
     */
    suspend fun archiveGroup(groupId: String): ApiResult<Unit>

    /**
     * Unarchives a previously archived group.
     */
    suspend fun unarchiveGroup(groupId: String): ApiResult<Unit>

    /**
     * Fetches all balances within a group.
     * Positive amount = other user owes you.
     * Negative amount = you owe other user.
     */
    suspend fun getGroupBalances(groupId: String): ApiResult<List<Balance>>

    /**
     * Fetches all member-to-member balances across the whole group.
     * Used by the Balances screen to show every member's net position.
     */
    suspend fun getAllGroupBalances(groupId: String): ApiResult<List<Balance>>

    /**
     * Fetches all settlements within a group, ordered by date descending.
     */
    suspend fun getGroupSettlements(groupId: String): ApiResult<List<Settlement>>
}