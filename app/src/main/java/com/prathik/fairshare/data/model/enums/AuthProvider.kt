package com.prathik.fairshare.data.model.enums

/**
 * The authentication provider used to create the account.
 *
 * LOCAL  — registered with email + password
 * GOOGLE — signed in via Google OAuth
 * APPLE  — signed in via Apple OAuth
 */
enum class AuthProvider {
    LOCAL,
    GOOGLE,
    APPLE
}