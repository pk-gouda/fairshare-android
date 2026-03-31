package com.prathik.fairshare.domain.usecase.user

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        fullName           : String?  = null,
        phoneNumber        : String?  = null,
        preferredCurrency  : String?  = null,
        notificationEnabled: Boolean? = null,
    ): ApiResult<User> = userRepository.updateProfile(
        fullName            = fullName,
        phoneNumber         = phoneNumber,
        preferredCurrency   = preferredCurrency,
        language            = null,
        notificationEnabled = notificationEnabled,
    )
}