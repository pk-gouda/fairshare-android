package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.ReminderFrequency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReminderResponse(
    @SerialName("id")             val id: String,
    @SerialName("groupId")        val groupId: String,
    @SerialName("groupName")      val groupName: String,
    @SerialName("createdById")    val createdById: String,
    @SerialName("createdByName")  val createdByName: String,
    @SerialName("frequency")      val frequency: ReminderFrequency,
    @SerialName("dayOfWeek")      val dayOfWeek: Int? = null,
    @SerialName("dayOfMonth")     val dayOfMonth: Int? = null,
    @SerialName("notifyViaApp")   val notifyViaApp: Boolean,
    @SerialName("notifyViaEmail") val notifyViaEmail: Boolean,
    @SerialName("isActive")       val isActive: Boolean,
    @SerialName("lastSentAt")     val lastSentAt: String? = null,
    @SerialName("createdAt")      val createdAt: String,
)
