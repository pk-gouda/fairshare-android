package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.FriendDao
import com.prathik.fairshare.data.local.FriendEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.FriendApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val friendService: FriendApiService,
    private val friendDao    : FriendDao,
) : FriendRepository {

    override suspend fun getCachedFriend(friendId: String): Friend? =
        friendDao.getAll().firstOrNull { it.id == friendId }?.toFriend()

    override suspend fun getCachedFriends(): List<Friend> =
        friendDao.getAll().map { it.toFriend() }

    override suspend fun getFriends(): ApiResult<List<Friend>> {
        // Always fetch from network to ensure newly added/removed friends appear immediately.
        val result = refreshFriendsFromNetwork()
        // Fall back to cache if network fails
        if (result is ApiResult.NetworkError) {
            val cached = friendDao.getAll()
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toFriend() })
        }
        return result
    }

    private suspend fun refreshFriendsFromNetwork(): ApiResult<List<Friend>> {
        val result = safeApiCall { friendService.getFriends() }
            .mapSuccess { list -> list.map { it.toDomain() } }
        if (result is ApiResult.Success) {
            friendDao.deleteAll()
            friendDao.insertAll(result.data.map { it.toEntity() })
        }
        return result
    }

    override suspend fun sendRequest(receiverId: String): ApiResult<Friendship> =
        safeApiCall { friendService.sendRequest(mapOf("receiverId" to receiverId)) }
            .mapSuccess { it.toDomain() }

    override suspend fun inviteFriend(email: String, name: String): ApiResult<Friendship> =
        safeApiCall { friendService.inviteFriend(mapOf("email" to email, "name" to name)) }
            .mapSuccess { it.toDomain() }

    override suspend fun createPlaceholder(name: String): ApiResult<Friendship> =
        safeApiCall { friendService.createPlaceholder(mapOf("name" to name)) }
            .mapSuccess { it.toDomain() }

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
                try { FriendStatus.valueOf(it) }
                catch (e: IllegalArgumentException) { FriendStatus.PENDING }
            }

    override suspend fun getBlocked(): ApiResult<List<Friend>> =
        safeApiCall { friendService.getBlocked() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun lookupByFriendCode(code: String): ApiResult<Friend> =
        safeApiCall { friendService.lookupByFriendCode(code) }
            .mapSuccess { it.toDomain() }



    override suspend fun getSharedGroups(friendId: String): ApiResult<List<Group>> =
    // Membership-based — NOT GroupBalance rows.
        // Uses GET /api/friends/{friendId}/shared-groups.
        safeApiCall { friendService.getSharedGroups(friendId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun addByFriendCode(code: String): ApiResult<Friendship> =
        safeApiCall { friendService.addByFriendCode(code) }
            .mapSuccess { it.toDomain() }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun FriendEntity.toFriend(): Friend = Friend(
    id                = id,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
    accountStatus     = try { AccountStatus.valueOf(accountStatus) }
    catch (e: IllegalArgumentException) { AccountStatus.ACTIVE },
)

private fun Friend.toEntity(): FriendEntity = FriendEntity(
    id                = id,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
    accountStatus     = accountStatus.name,
)