package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Handles user logout.
 * Invalidates the refresh token on the server and clears local tokens.
 * Always succeeds locally even if the server call fails —
 * we never want a user stuck in a logged-in state they can't exit.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}