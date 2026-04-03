package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.FriendApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val friendService: FriendApiService,
) : FriendRepository {

    override suspend fun getFriends(): ApiResult<List<Friend>> =
        safeApiCall { friendService.getFriends() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun sendRequest(receiverId: String): ApiResult<Friendship> =
        safeApiCall { friendService.sendRequest(mapOf("receiverId" to receiverId)) }
            .mapSuccess { it.toDomain() }

    override suspend fun inviteFriend(email: String, name: String): ApiResult<Unit> =
        safeApiCall { friendService.inviteFriend(mapOf("email" to email, "name" to name)) }
            .mapSuccess { }

    override suspend fun acceptRequest(friendshipId: String): ApiResult<Friendship> =
        safeApiCall { friendService.acceptRequest(friendshipId) }
            .mapSuccess { it.toDomain() }

    override suspend fun declineRequest(friendshipId: String): ApiResult<Unit> =
        safeApiCall { friendService.declineRequest(friendshipId) }.mapSuccess { }

    override suspend fun cancelRequest(friendshipId: String): ApiResult<Unit> =
        safeApiCall { friendService.cancelRequest(friendshipId) }.mapSuccess { }

    override suspend fun removeFriend(friendId: String): ApiResult<Unit> =
        safeApiCall { friendService.removeFriend(friendId) }.mapSuccess { }

    override suspend fun blockUser(blockedUserId: String): ApiResult<Unit> =
        safeApiCall { friendService.blockUser(blockedUserId) }.mapSuccess { }

    override suspend fun unblockUser(blockedUserId: String): ApiResult<Unit> =
        safeApiCall { friendService.unblockUser(blockedUserId) }.mapSuccess { }

    override suspend fun getReceivedRequests(): ApiResult<List<Friendship>> =
        safeApiCall { friendService.getReceivedRequests() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getSentRequests(): ApiResult<List<Friendship>> =
        safeApiCall { friendService.getSentRequests() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getFriendStatus(otherUserId: String): ApiResult<FriendStatus> =
        safeApiCall { friendService.getFriendStatus(otherUserId) }
            .mapSuccess {
                try {
                    FriendStatus.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    FriendStatus.PENDING
                }
            }

    override suspend fun getBlocked(): ApiResult<List<Friend>> =
        safeApiCall { friendService.getBlocked() }
            .mapSuccess { list -> list.map { it.toDomain() } }
}