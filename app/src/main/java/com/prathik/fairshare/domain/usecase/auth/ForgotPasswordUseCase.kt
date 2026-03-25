package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles forgot password requests.
 * Validates email format before hitting the network.
 * Note: Always returns Success even if email doesn't exist —
 * intentional for security (prevents email enumeration attacks).
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): ApiResult<Unit> {
        if (email.isBlank()) {
            return ApiResult.ValidationError("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ApiResult.ValidationError("Invalid email address")
        }
        return authRepository.forgotPassword(email.trim())
    }
}
