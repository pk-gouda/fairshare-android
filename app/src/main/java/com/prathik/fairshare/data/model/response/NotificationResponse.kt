package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    @SerialName("id")            val id: String,
    @SerialName("title")         val title: String,
    @SerialName("message")       val message: String,
    @SerialName("type")          val type: NotificationType,
    @SerialName("referenceId")   val referenceId: String? = null,
    @SerialName("referenceType") val referenceType: String? = null,
    @SerialName("isRead")        val isRead: Boolean,
    @SerialName("createdAt")     val createdAt: String,
)
