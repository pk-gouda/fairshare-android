package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching friend data locally.
 * Friends are fetched from the network and cached here for instant display on cold start.
 */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String?,
    val accountStatus: String,   // AccountStatus enum name
    val cachedAt: Long = System.currentTimeMillis(),
)