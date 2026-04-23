package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship

interface FriendRepository {
    suspend fun getFriends(): ApiResult<List<Friend>>
    suspend fun sendRequest(receiverId: String): ApiResult<Friendship>
    suspend fun inviteFriend(email: String, name: String): ApiResult<Friendship>
    suspend fun createPlaceholder(name: String): ApiResult<Friendship>
    suspend fun acceptRequest(friendshipId: String): ApiResult<Friendship>
    suspend fun declineRequest(friendshipId: String): ApiResult<Unit>
    suspend fun cancelRequest(friendshipId: String): ApiResult<Unit>
    suspend fun removeFriend(friendId: String): ApiResult<Unit>
    suspend fun blockUser(blockedUserId: String): ApiResult<Unit>
    suspend fun unblockUser(blockedUserId: String): ApiResult<Unit>
    suspend fun getReceivedRequests(): ApiResult<List<Friendship>>
    suspend fun getSentRequests(): ApiResult<List<Friendship>>
    suspend fun getFriendStatus(otherUserId: String): ApiResult<FriendStatus>
    suspend fun getBlocked(): ApiResult<List<Friend>>
    suspend fun addByFriendCode(code: String): ApiResult<Friendship>
    suspend fun lookupByFriendCode(code: String): ApiResult<Friend>
}