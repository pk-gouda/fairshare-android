package com.prathik.fairshare.domain.usecase.user

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.UserRepository
import javax.inject.Inject

class RegenerateFriendCodeUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): ApiResult<String> =
        userRepository.regenerateFriendCode()
}