package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching group data locally.
 * Matches backend Group entity fields exactly.
 * cachedAt tracks when this was last fetched from network.
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val groupImage: String?,
    val createdById: String,
    val createdByName: String,
    val tripStartDate: String?,
    val tripEndDate: String?,
    val simplifyDebts: Boolean,
    val inviteCode: String,
    val groupNotes: String?,
    val lastActivityDate: String?,
    val isArchived: Boolean,
    val memberCount: Int,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
