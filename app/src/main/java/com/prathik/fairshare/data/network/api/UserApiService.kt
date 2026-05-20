package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.UpdateProfileRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.FriendResponse
import com.prathik.fairshare.data.model.response.UserResponse
import com.prathik.fairshare.data.model.request.DeactivateAccountRequest
import com.prathik.fairshare.data.model.request.DeleteAccountRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.HTTP
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiService {

    @GET("api/users/me")
    suspend fun getMyProfile(): ApiResponse<UserResponse>

    @GET("api/users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): ApiResponse<UserResponse>

    @GET("api/users/search")
    suspend fun searchByEmail(@Query("email") email: String): ApiResponse<UserResponse>

    @GET("api/users/friend-code/{code}")
    suspend fun lookupByFriendCode(@Path("code") code: String): ApiResponse<FriendResponse>

    /**
     * Get the current user's friend code.
     * Generates and saves a new FAIR-XXXXXX code server-side if the user
     * doesn't have one yet (null or blank). Safe to call on every QR screen open.
     */
    @GET("api/users/me/friend-code")
    suspend fun getMyFriendCode(): ApiResponse<Map<String, String>>

    @POST("api/users/me/friend-code/regenerate")
    suspend fun regenerateFriendCode(): ApiResponse<Map<String, String>>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserResponse>

    @POST("api/users/me/change-email")
    suspend fun requestEmailChange(@Body body: Map<String, String>): ApiResponse<Unit>

    @POST("api/users/me/verify-email-change")
    suspend fun verifyEmailChange(@Query("token") token: String): ApiResponse<Unit>

    @POST("api/users/me/deactivate")
    suspend fun deactivateAccount(@Body request: DeactivateAccountRequest): ApiResponse<Unit>

    @POST("api/users/me/reactivate")
    suspend fun reactivateAccount(): ApiResponse<Unit>

    // @HTTP is required instead of @DELETE because Retrofit's @DELETE annotation
    // does not support a request body. @HTTP with hasBody=true is the correct pattern.
    @HTTP(method = "DELETE", path = "api/users/me", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): ApiResponse<Unit>
}