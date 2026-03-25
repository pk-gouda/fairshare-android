package com.prathik.fairshare.domain.model

/**
 * Represents the lifecycle state of a user account.
 */
enum class AccountStatus {
    PLACEHOLDER,
    INVITED,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DEACTIVATED
}