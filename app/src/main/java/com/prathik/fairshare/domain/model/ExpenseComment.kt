package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseComment(
    val id: String,
    val userId: String,
    val userFullName: String,
    val comment: String,
    val createdAt: String,
) : Parcelable