package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Sends a friend request to another user.
 */
class SendFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(receiverId: String): ApiResult<Friendship> {
        if (receiverId.isBlank()) {
            return ApiResult.ValidationError("User ID cannot be empty")
        }
        return friendRepository.sendRequest(receiverId)
    }
}
