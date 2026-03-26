package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching the current user's profile locally.
 * Used to display profile info without a network call on every launch.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val preferredCurrency: String,
    val language: String,
    val notificationEnabled: Boolean,
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
