package com.prathik.fairshare.domain.model

data class InvitedFriend(
    val id           : String,
    val displayName  : String,
    val emailOrPhone : String,
    val isPlaceholder: Boolean,
    val invitedAt    : Long,
)