package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * Represents the lifecycle state of a user account.
 */
@Serializable
enum class AccountStatus {
    PLACEHOLDER,
    INVITED,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DEACTIVATED
}