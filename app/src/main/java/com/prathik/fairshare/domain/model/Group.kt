package com.prathik.fairshare.domain.model


data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val createdById: String,
    val createdByName: String,
    val inviteCode: String,
    val simplifyDebts: Boolean,
    val isArchived: Boolean,
    val memberCount: Int,
    val groupNotes: String?,
    val groupImage: String?,
    val lastActivityDate: String?,
    val createdAt: String,
)