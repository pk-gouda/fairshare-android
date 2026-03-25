package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles user login.
 * Validates inputs before hitting the network.
 * Returns the logged-in [User] or a descriptive failure.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Invalid email address"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        return authRepository.login(email.trim(), password)
    }
}