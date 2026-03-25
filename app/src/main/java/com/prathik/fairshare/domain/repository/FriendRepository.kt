package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship

/**
 * Contract for friendship-related operations.
 * Implementation lives in data/repository/impl/FriendRepositoryImpl.kt
 */
interface FriendRepository {

    /**
     * Fetches all accepted friends of the current user.
     * Returns empty list if the user has no friends yet.
     */
    suspend fun getFriends(): ApiResult<List<Friend>>

    /**
     * Sends a friend request to another user.
     * Returns [ApiResult.Conflict] if a request already exists
     * or if the users are already friends.
     */
    suspend fun sendRequest(receiverId: String): ApiResult<Friendship>

    /**
     * Accepts a received friend request.
     * Changes friendship status from PENDING to ACCEPTED.
     * Returns [ApiResult.Forbidden] if current user is not the receiver.
     */
    suspend fun acceptRequest(friendshipId: String): ApiResult<Friendship>

    /**
     * Declines a received friend request.
     * The requester is not notified.
     */
    suspend fun declineRequest(friendshipId: String): ApiResult<Unit>

    /**
     * Cancels a sent friend request that hasn't been accepted yet.
     * Returns [ApiResult.Forbidden] if current user is not the requester.
     */
    suspend fun cancelRequest(friendshipId: String): ApiResult<Unit>

    /**
     * Removes an existing friend.
     * Both users lose each other from their friends list.
     */
    suspend fun removeFriend(friendId: String): ApiResult<Unit>

    /**
     * Blocks a user.
     * Blocked users cannot send requests or be added to shared groups.
     * Any existing friendship is automatically removed on block.
     */
    suspend fun blockUser(blockedUserId: String): ApiResult<Unit>

    /**
     * Unblocks a previously blocked user.
     */
    suspend fun unblockUser(blockedUserId: String): ApiResult<Unit>

    /**
     * Fetches all received friend requests that are still pending.
     * Used to show the pending section in the Friends tab.
     */
    suspend fun getReceivedRequests(): ApiResult<List<Friendship>>

    /**
     * Fetches all friend requests sent by the current user.
     */
    suspend fun getSentRequests(): ApiResult<List<Friendship>>

    /**
     * Gets the friendship status between current user and another user.
     */
    suspend fun getFriendStatus(otherUserId: String): ApiResult<FriendStatus>

    /**
     * Fetches all users blocked by the current user.
     */
    suspend fun getBlocked(): ApiResult<List<Friend>>
}