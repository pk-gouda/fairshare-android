package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Blocks a user.
 * Blocked users cannot send requests or be added to shared groups.
 */
class BlockUserUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(blockedUserId: String): ApiResult<Unit> {
        if (blockedUserId.isBlank()) {
            return ApiResult.ValidationError("User ID cannot be empty")
        }
        return friendRepository.blockUser(blockedUserId)
    }
}
