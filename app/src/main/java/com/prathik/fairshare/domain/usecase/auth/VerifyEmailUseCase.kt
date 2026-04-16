package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Verifies the user's email address using the userId and token
 * extracted from the fairshare://verify-email deep link.
 *
 * Calls POST /api/auth/verify-email with the token in the body (M2).
 * On success the account status changes from INVITED to ACTIVE on the backend.
 *
 * Returns:
 *   ApiResult.Success    — account activated
 *   ApiResult.Unauthorized — token invalid or expired
 *   ApiResult.Conflict   — email already verified (treat as success)
 *   ApiResult.NetworkError — no connectivity
 */
class VerifyEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(userId: String, token: String): ApiResult<Unit> =
        authRepository.verifyEmail(userId, token)
}