package com.prathik.fairshare.domain.usecase.friend

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.repository.FriendRepository
import javax.inject.Inject

class AddByFriendCodeUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    suspend operator fun invoke(code: String): ApiResult<Friendship> =
        friendRepository.addByFriendCode(code)
}