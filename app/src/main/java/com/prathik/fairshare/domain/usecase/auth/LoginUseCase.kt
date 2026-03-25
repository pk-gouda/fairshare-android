package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles user login.
 * Validates inputs before hitting the network.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<User> {
        if (email.isBlank()) {
            return ApiResult.ValidationError("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ApiResult.ValidationError("Invalid email address")
        }
        if (password.isBlank()) {
            return ApiResult.ValidationError("Password cannot be empty")
        }
        if (password.length < 6) {
            return ApiResult.ValidationError("Password must be at least 6 characters")
        }
        return authRepository.login(email.trim(), password)
    }
}
