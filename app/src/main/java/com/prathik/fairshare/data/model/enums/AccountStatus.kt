package com.prathik.fairshare.data.model.enums

/**
 * Represents the lifecycle state of a user account.
 *
 * PLACEHOLDER   — name-only account created during Splitwise import, no real user yet
 * INVITED       — account created, verification email sent, not yet verified
 * ACTIVE        — fully registered and verified
 * INACTIVE      — soft deleted by user
 * SUSPENDED     — suspended by admin
 * DEACTIVATED   — placeholder account that was claimed by a real user
 */
enum class AccountStatus {
    PLACEHOLDER,
    INVITED,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DEACTIVATED
}