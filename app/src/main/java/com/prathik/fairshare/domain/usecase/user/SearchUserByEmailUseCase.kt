package com.prathik.fairshare.domain.usecase.user

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.UserRepository
import javax.inject.Inject

class SearchUserByEmailUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(email: String): ApiResult<User> =
        userRepository.searchByEmail(email)
}