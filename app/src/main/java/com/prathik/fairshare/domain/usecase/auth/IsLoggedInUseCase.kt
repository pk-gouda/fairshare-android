package com.prathik.fairshare.domain.usecase.auth

import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Checks if the user is currently logged in.
 * Used by SplashScreen to decide navigation destination:
 * true  → GroupsHome
 * false → Login
 */
class IsLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}
