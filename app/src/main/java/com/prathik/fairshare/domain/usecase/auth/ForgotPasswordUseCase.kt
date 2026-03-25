package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles forgot password requests.
 * Validates email format before hitting the network.
 * Note: Always returns success even if email doesn't exist —
 * this is intentional for security (prevents email enumeration attacks).
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Invalid email address"))
        }
        return authRepository.forgotPassword(email.trim())
    }
}