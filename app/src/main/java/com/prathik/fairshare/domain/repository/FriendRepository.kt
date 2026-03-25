package com.prathik.fairshare.domain.repository

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
     */
    suspend fun getFriends(): Result<List<Friend>>

    /**
     * Sends a friend request to another user.
     */
    suspend fun sendRequest(receiverId: String): Result<Friendship>

    /**
     * Accepts a received friend request.
     */
    suspend fun acceptRequest(friendshipId: String): Result<Friendship>

    /**
     * Declines a received friend request.
     */
    suspend fun declineRequest(friendshipId: String): Result<Unit>

    /**
     * Cancels a sent friend request.
     */
    suspend fun cancelRequest(friendshipId: String): Result<Unit>

    /**
     * Removes an existing friend.
     */
    suspend fun removeFriend(friendId: String): Result<Unit>

    /**
     * Blocks a user — they can no longer send requests or be added to shared groups.
     */
    suspend fun blockUser(blockedUserId: String): Result<Unit>

    /**
     * Unblocks a previously blocked user.
     */
    suspend fun unblockUser(blockedUserId: String): Result<Unit>

    /**
     * Fetches all received friend requests that are still pending.
     */
    suspend fun getReceivedRequests(): Result<List<Friendship>>

    /**
     * Fetches all friend requests sent by the current user.
     */
    suspend fun getSentRequests(): Result<List<Friendship>>

    /**
     * Gets the friendship status between current user and another user.
     */
    suspend fun getFriendStatus(otherUserId: String): Result<FriendStatus>

    /**
     * Fetches all blocked users.
     */
    suspend fun getBlocked(): Result<List<Friend>>
}