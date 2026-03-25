package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Checks if the user is currently logged in.
 * Used by the Splash screen to decide navigation destination —
 * if logged in → Groups Home, if not → Login.
 */
class IsLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}