package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Fetches all accepted friends of the current user.
 */
class GetFriendsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Friend>> {
        return friendRepository.getFriends()
    }
}
