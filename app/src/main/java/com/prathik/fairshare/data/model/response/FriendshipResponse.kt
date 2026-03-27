package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendshipResponse(
    @SerialName("id")             val id: String,
    @SerialName("requesterId")    val requesterId: String,
    @SerialName("requesterName")  val requesterName: String,
    @SerialName("receiverId")     val receiverId: String,
    @SerialName("receiverName")   val receiverName: String,
    @SerialName("status")         val status: String,
    @SerialName("friendshipType") val friendshipType: String,
    @SerialName("createdAt")      val createdAt: String,
)
