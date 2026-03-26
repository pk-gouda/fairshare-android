package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.FriendResponse
import com.prathik.fairshare.data.model.response.FriendshipResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FriendApiService {

    @POST("api/friends/request/{receiverId}")
    suspend fun sendRequest(@Path("receiverId") receiverId: String): ApiResponse<FriendshipResponse>

    @POST("api/friends/{friendshipId}/accept")
    suspend fun acceptRequest(@Path("friendshipId") friendshipId: String): ApiResponse<FriendshipResponse>

    @POST("api/friends/{friendshipId}/decline")
    suspend fun declineRequest(@Path("friendshipId") friendshipId: String): ApiResponse<Unit>

    @POST("api/friends/{friendshipId}/cancel")
    suspend fun cancelRequest(@Path("friendshipId") friendshipId: String): ApiResponse<Unit>

    @DELETE("api/friends/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: String): ApiResponse<Unit>

    @POST("api/friends/block/{blockedUserId}")
    suspend fun blockUser(@Path("blockedUserId") blockedUserId: String): ApiResponse<Unit>

    @DELETE("api/friends/block/{blockedUserId}")
    suspend fun unblockUser(@Path("blockedUserId") blockedUserId: String): ApiResponse<Unit>

    @GET("api/friends")
    suspend fun getFriends(): ApiResponse<List<FriendResponse>>

    @GET("api/friends/requests/received")
    suspend fun getReceivedRequests(): ApiResponse<List<FriendshipResponse>>

    @GET("api/friends/requests/sent")
    suspend fun getSentRequests(): ApiResponse<List<FriendshipResponse>>

    @GET("api/friends/status/{otherUserId}")
    suspend fun getFriendStatus(@Path("otherUserId") otherUserId: String): ApiResponse<String>

    @GET("api/friends/blocked")
    suspend fun getBlocked(): ApiResponse<List<FriendResponse>>
}
