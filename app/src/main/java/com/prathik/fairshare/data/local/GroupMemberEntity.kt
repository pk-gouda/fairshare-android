package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching group member rows locally.
 *
 * Populated when [GroupRepositoryImpl.getMembers] succeeds. On network
 * failure the cached rows are returned, so AddExpenseScreen can show the
 * Paid By / Split sections and allow offline expense creation when the user
 * has previously opened the group while online.
 *
 * Primary key is the server-assigned membership [id], which is unique per
 * group+user pair.
 */
@Entity(
    tableName = "group_members",
    indices   = [Index(value = ["groupId"])],
)
data class GroupMemberEntity(
    @PrimaryKey val id              : String,
    val groupId          : String,
    val userId           : String,
    val fullName         : String,
    val email            : String,
    val profilePictureUrl: String?,
    val joinedAt         : String,
)