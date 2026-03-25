package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Fetches all pending friend requests received by the current user.
 * Used to show the pending requests section in the Friends tab.
 */
class GetFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(): Result<List<Friendship>> {
        return friendRepository.getReceivedRequests()
    }
}
