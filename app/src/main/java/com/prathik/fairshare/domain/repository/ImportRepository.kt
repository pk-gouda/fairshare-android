package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember

/**
 * Contract for Splitwise CSV import operations.
 * Implementation lives in data/repository/impl/ImportRepositoryImpl.kt
 *
 * Import flow:
 * 1. User exports CSV from Splitwise
 * 2. App reads CSV file content as String
 * 3. POST to backend — creates group/expenses + PLACEHOLDER users
 * 4. Show UnclaimedMembersScreen — let users claim or assign placeholders
 */
interface ImportRepository {

    /**
     * Imports a Splitwise group CSV.
     * Creates a new group, all expenses, and PLACEHOLDER users
     * for every person in the CSV who isn't a registered FairShare user.
     *
     * [csvContent] — raw CSV file content read from the file picker
     * [groupName]  — name for the newly created group
     *
     * Returns import stats: expenses imported, members created, total amount.
     * Returns [ApiResult.ValidationError] if CSV format is invalid.
     */
    suspend fun importGroup(
        csvContent: String,
        groupName: String,
        groupType: String,
        importerCsvName: String? = null,
    ): ApiResult<Map<String, Any>>

    /**
     * Imports a Splitwise friend CSV.
     * Creates non-group direct expenses + PLACEHOLDER users.
     * No group is created.
     *
     * Returns import stats.
     */
    suspend fun importFriend(csvContent: String, importerCsvName: String? = null): ApiResult<Map<String, Any>>

    /**
     * Fetches all PLACEHOLDER members in a group that haven't been
     * claimed or assigned yet.
     * Used to populate the UnclaimedMembersScreen after import.
     */
    suspend fun getUnclaimedMembers(groupId: String): ApiResult<List<GroupMember>>

    /**
     * Assigns a PLACEHOLDER user to a real existing friend.
     * Used when the group creator knows which friend the placeholder represents.
     *
     * [groupId]           — the group containing the placeholder
     * [placeholderUserId] — the PLACEHOLDER user's ID
     * [friendUserId]      — the real user's ID to assign to
     */
    suspend fun assignPlaceholder(
        groupId: String,
        placeholderUserId: String,
        friendUserId: String,
    ): ApiResult<Map<String, Any>>

    /**
     * Claims a PLACEHOLDER as the current user.
     * Used when the current user recognises themselves in the import.
     *
     * [groupId]           — the group containing the placeholder
     * [placeholderUserId] — the PLACEHOLDER user's ID to claim
     */
    suspend fun claimIdentity(
        groupId: String,
        placeholderUserId: String,
    ): ApiResult<Map<String, Any>>

    /**
     * Reverses a wrong claim (creator only).
     * Used when the wrong person claimed a placeholder during Splitwise import.
     *
     * Requires a real, active claimer user ID and the original CSV name —
     * NOT a placeholderUserId. Do not call with a placeholder ID or a blank
     * CSV name; the backend rejects both. This method is currently DORMANT:
     * no UI/ViewModel calls it yet, because no backend endpoint exposes the
     * original CSV name for an already-claimed member. Wire it up only once
     * that claimed-member metadata is available.
     *
     * [groupId]            — the group containing the wrongly-claimed identity
     * [wrongClaimerUserId] — the real user who claimed the identity by mistake
     * [originalCsvName]    — the original CSV name to restore as a placeholder
     */
    suspend fun unclaimIdentity(
        groupId: String,
        wrongClaimerUserId: String,
        originalCsvName: String,
    ): ApiResult<Map<String, Any>>

    suspend fun assignFriendPlaceholder(placeholderUserId: String, friendUserId: String): ApiResult<Map<String, Any>>
}