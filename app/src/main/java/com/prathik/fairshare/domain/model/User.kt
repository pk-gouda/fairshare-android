package com.prathik.fairshare.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val preferredCurrency: String,
    val language: String,
    val notificationEnabled: Boolean,
    val isActive: Boolean,
)