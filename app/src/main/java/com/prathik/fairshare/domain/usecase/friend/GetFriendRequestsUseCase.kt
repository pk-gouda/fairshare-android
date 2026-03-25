package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Fetches all pending friend requests received by the current user.
 */
class GetFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Friendship>> {
        return friendRepository.getReceivedRequests()
    }
}
