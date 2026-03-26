package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.CreateReminderRequest
import com.prathik.fairshare.data.model.request.UpdateReminderRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.ReminderResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit interface for reminder endpoints.
 * 4 endpoints for group reminder management.
 */
interface ReminderApiService {

    @POST("api/groups/{groupId}/reminders")
    suspend fun createReminder(
        @Path("groupId") groupId: String,
        @Body request: CreateReminderRequest,
    ): ApiResponse<ReminderResponse>

    @GET("api/groups/{groupId}/reminders")
    suspend fun getGroupReminders(
        @Path("groupId") groupId: String,
    ): ApiResponse<List<ReminderResponse>>

    @PUT("api/reminders/{reminderId}")
    suspend fun updateReminder(
        @Path("reminderId") reminderId: String,
        @Body request: UpdateReminderRequest,
    ): ApiResponse<ReminderResponse>

    @DELETE("api/reminders/{reminderId}")
    suspend fun deleteReminder(@Path("reminderId") reminderId: String): ApiResponse<Unit>
}
