package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The authentication provider used to create the account.
 */
@Serializable
enum class AuthProvider {
    LOCAL,
    GOOGLE,
    APPLE
}