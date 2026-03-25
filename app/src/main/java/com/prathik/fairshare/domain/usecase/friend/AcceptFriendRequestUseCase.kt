package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Accepts a received friend request.
 */
class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(friendshipId: String): ApiResult<Friendship> {
        if (friendshipId.isBlank()) {
            return ApiResult.ValidationError("Friendship ID cannot be empty")
        }
        return friendRepository.acceptRequest(friendshipId)
    }
}
