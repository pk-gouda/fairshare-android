package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Sends a friend request to another user.
 * The server will reject this if a request already exists
 * or if the users are already friends.
 */
class SendFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(receiverId: String): Result<Friendship> {
        if (receiverId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return friendRepository.sendRequest(receiverId)
    }
}
