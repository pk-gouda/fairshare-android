package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.FriendshipType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendshipResponse(
    @SerialName("id")             val id: String,
    @SerialName("requesterId")    val requesterId: String,
    @SerialName("requesterName")  val requesterName: String,
    @SerialName("receiverId")     val receiverId: String,
    @SerialName("receiverName")   val receiverName: String,
    @SerialName("status")         val status: FriendStatus,
    @SerialName("friendshipType") val friendshipType: FriendshipType,
    @SerialName("createdAt")      val createdAt: String,
)
