package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupPreviewResponse(
    @SerialName("name")        val name: String,
    @SerialName("type")        val type: String,
    @SerialName("memberCount") val memberCount: Int,
)