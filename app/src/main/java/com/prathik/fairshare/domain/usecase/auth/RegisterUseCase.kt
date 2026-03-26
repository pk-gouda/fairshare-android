package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles new user registration.
 * Validates all inputs before hitting the network.
 * Uses regex instead of android.util.Patterns so this use case
 * can be unit tested on a plain JVM without Robolectric.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        fullName: String,
        password: String,
        confirmPassword: String,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
    ): ApiResult<User> {
        if (fullName.isBlank()) {
            return ApiResult.ValidationError("Full name cannot be empty")
        }
        if (fullName.trim().length < 2) {
            return ApiResult.ValidationError("Full name must be at least 2 characters")
        }
        if (email.isBlank()) {
            return ApiResult.ValidationError("Email cannot be empty")
        }
        if (!EMAIL_REGEX.matches(email.trim())) {
            return ApiResult.ValidationError("Invalid email address")
        }
        if (password.isBlank()) {
            return ApiResult.ValidationError("Password cannot be empty")
        }
        if (password.length < 6) {
            return ApiResult.ValidationError("Password must be at least 6 characters")
        }
        if (password != confirmPassword) {
            return ApiResult.ValidationError("Passwords do not match")
        }
        return authRepository.register(
            email             = email.trim(),
            fullName          = fullName.trim(),
            password          = password,
            phoneNumber       = phoneNumber?.trim(),
            preferredCurrency = preferredCurrency,
            language          = language,
        )
    }

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
        )
    }
}