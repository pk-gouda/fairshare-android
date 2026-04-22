package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    @SerialName("id")             val id: String,
    @SerialName("title")          val title: String,
    @SerialName("message")        val message: String,
    @SerialName("type")           val type: String,
    @SerialName("referenceId")    val referenceId: String? = null,
    @SerialName("referenceType")  val referenceType: String? = null,
    @SerialName("isRead")         val isRead: Boolean,
    @SerialName("createdAt")      val createdAt: String,
    @SerialName("groupId")        val groupId: String? = null,
    @SerialName("groupName")      val groupName: String? = null,
    @SerialName("isGroupDeleted") val isGroupDeleted: Boolean = false,
)