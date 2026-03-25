package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.ReminderFrequency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateReminderRequest(
    @SerialName("groupId")       val groupId: String,
    @SerialName("frequency")     val frequency: ReminderFrequency,
    @SerialName("dayOfWeek")     val dayOfWeek: Int? = null,
    @SerialName("dayOfMonth")    val dayOfMonth: Int? = null,
    @SerialName("notifyViaApp")  val notifyViaApp: Boolean? = null,
    @SerialName("notifyViaEmail") val notifyViaEmail: Boolean? = null,
)

@Serializable
data class UpdateReminderRequest(
    @SerialName("frequency")     val frequency: ReminderFrequency? = null,
    @SerialName("dayOfWeek")     val dayOfWeek: Int? = null,
    @SerialName("dayOfMonth")    val dayOfMonth: Int? = null,
    @SerialName("notifyViaApp")  val notifyViaApp: Boolean? = null,
    @SerialName("notifyViaEmail") val notifyViaEmail: Boolean? = null,
    @SerialName("isActive")      val isActive: Boolean? = null,
)
