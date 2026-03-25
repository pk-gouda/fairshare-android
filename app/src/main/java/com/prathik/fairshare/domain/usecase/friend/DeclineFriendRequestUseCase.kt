package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Declines a received friend request.
 * The requester will not be notified.
 */
class DeclineFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(friendshipId: String): Result<Unit> {
        if (friendshipId.isBlank()) {
            return Result.failure(IllegalArgumentException("Friendship ID cannot be empty"))
        }
        return friendRepository.declineRequest(friendshipId)
    }
}
