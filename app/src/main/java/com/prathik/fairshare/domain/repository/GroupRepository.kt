package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember

/**
 * Contract for all group-related operations.
 * Implementation lives in data/repository/impl/GroupRepositoryImpl.kt
 */
interface GroupRepository {

    /**
     * Fetches all groups the current user is a member of.
     */
    suspend fun getMyGroups(): Result<List<Group>>

    /**
     * Fetches a single group by ID.
     */
    suspend fun getGroup(groupId: String): Result<Group>

    /**
     * Creates a new group with the current user as admin.
     */
    suspend fun createGroup(name: String, type: String, description: String?): Result<Group>

    /**
     * Updates group name, description, or simplifyDebts setting.
     */
    suspend fun updateGroup(groupId: String, name: String?, description: String?, simplifyDebts: Boolean?): Result<Group>

    /**
     * Deletes a group. Only the creator can do this.
     */
    suspend fun deleteGroup(groupId: String): Result<Unit>

    /**
     * Joins a group using an invite code.
     */
    suspend fun joinGroup(inviteCode: String): Result<GroupMember>

    /**
     * Adds a member to a group by userId.
     */
    suspend fun addMember(groupId: String, userId: String): Result<GroupMember>

    /**
     * Removes a member from a group.
     */
    suspend fun removeMember(groupId: String, memberId: String): Result<Unit>

    /**
     * Fetches all members of a group.
     */
    suspend fun getMembers(groupId: String): Result<List<GroupMember>>

    /**
     * Archives a group — hides it from the main list but keeps all data.
     */
    suspend fun archiveGroup(groupId: String): Result<Unit>

    /**
     * Unarchives a previously archived group.
     */
    suspend fun unarchiveGroup(groupId: String): Result<Unit>

    /**
     * Fetches all balances within a group.
     * Each balance represents what one member owes another.
     */
    suspend fun getGroupBalances(groupId: String): Result<List<Balance>>
}