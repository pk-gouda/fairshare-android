package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Locally stored invited friends — people invited by email/phone who don't have a FairShare account yet.
 * Also stores placeholders (no email).
 * These never come from the backend — they're created locally when a user sends an invite.
 */
@Entity(tableName = "invited_friends")
data class InvitedFriendEntity(
    @PrimaryKey val id         : String,  // locally generated UUID
    val displayName            : String,
    val emailOrPhone           : String,  // empty for placeholders
    val isPlaceholder          : Boolean,
    val invitedAt              : Long = System.currentTimeMillis(),
)