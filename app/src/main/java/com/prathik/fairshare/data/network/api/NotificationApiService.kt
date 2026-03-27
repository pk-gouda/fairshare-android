package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.NotificationResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {

    @GET("api/notifications")
    suspend fun getAll(): ApiResponse<List<NotificationResponse>>

    @GET("api/notifications/unread")
    suspend fun getUnread(): ApiResponse<List<NotificationResponse>>

    @GET("api/notifications/unread/count")
    suspend fun getUnreadCount(): ApiResponse<Map<String, Int>>

    @POST("api/notifications/{notificationId}/read")
    suspend fun markRead(@Path("notificationId") notificationId: String): ApiResponse<Unit>

    @POST("api/notifications/read-all")
    suspend fun markAllRead(): ApiResponse<Unit>
}
