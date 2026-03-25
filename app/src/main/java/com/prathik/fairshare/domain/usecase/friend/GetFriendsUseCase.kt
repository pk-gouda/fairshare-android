package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

/**
 * Fetches all accepted friends of the current user.
 * Returns an empty list if the user has no friends yet.
 */
class GetFriendsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(): Result<List<Friend>> {
        return friendRepository.getFriends()
    }
}
