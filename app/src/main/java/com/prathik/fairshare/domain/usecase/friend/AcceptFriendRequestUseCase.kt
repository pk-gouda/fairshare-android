package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Accepts a received friend request.
 * Changes friendship status from PENDING to ACCEPTED.
 */
class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(friendshipId: String): Result<Friendship> {
        if (friendshipId.isBlank()) {
            return Result.failure(IllegalArgumentException("Friendship ID cannot be empty"))
        }
        return friendRepository.acceptRequest(friendshipId)
    }
}
