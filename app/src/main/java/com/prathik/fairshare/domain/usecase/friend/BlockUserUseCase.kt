package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Blocks a user.
 * Blocked users cannot send friend requests or be added to shared groups.
 * Any existing friendship is automatically removed on block.
 */
class BlockUserUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(blockedUserId: String): Result<Unit> {
        if (blockedUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return friendRepository.blockUser(blockedUserId)
    }
}
