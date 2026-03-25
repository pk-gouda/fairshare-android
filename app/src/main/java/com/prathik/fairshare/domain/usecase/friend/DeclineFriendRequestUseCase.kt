package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Declines a received friend request.
 */
class DeclineFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(friendshipId: String): ApiResult<Unit> {
        if (friendshipId.isBlank()) {
            return ApiResult.ValidationError("Friendship ID cannot be empty")
        }
        return friendRepository.declineRequest(friendshipId)
    }
}
