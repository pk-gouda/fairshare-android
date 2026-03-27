package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.FieldError
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles user login.
 * Validates inputs before hitting the network.
 * Uses regex instead of android.util.Patterns so this use case
 * can be unit tested on a plain JVM without Robolectric.
 *
 * Each validation failure includes a FieldError so the UI can show
 * inline errors on the correct field via getFieldError(fieldName).
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<User> {
        if (email.isBlank()) {
            return ApiResult.ValidationError(
                message = "Email cannot be empty",
                errors  = listOf(FieldError(field = "email", message = "Email cannot be empty")),
            )
        }
        if (!EMAIL_REGEX.matches(email.trim())) {
            return ApiResult.ValidationError(
                message = "Invalid email address",
                errors  = listOf(FieldError(field = "email", message = "Invalid email address")),
            )
        }
        if (password.isBlank()) {
            return ApiResult.ValidationError(
                message = "Password cannot be empty",
                errors  = listOf(FieldError(field = "password", message = "Password cannot be empty")),
            )
        }
        if (password.length < 6) {
            return ApiResult.ValidationError(
                message = "Password must be at least 6 characters",
                errors  = listOf(FieldError(field = "password", message = "Password must be at least 6 characters")),
            )
        }
        return authRepository.login(email.trim(), password)
    }

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
        )
    }
}