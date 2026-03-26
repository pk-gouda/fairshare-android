package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.ChangePasswordRequest
import com.prathik.fairshare.data.model.request.ForgotPasswordRequest
import com.prathik.fairshare.data.model.request.LoginRequest
import com.prathik.fairshare.data.model.request.OAuthLoginRequest
import com.prathik.fairshare.data.model.request.RegisterRequest
import com.prathik.fairshare.data.model.request.ResetPasswordRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for /api/auth/** endpoints.
 * 9 endpoints covering the full authentication lifecycle.
 */
interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(
        @Query("userId") userId: String,
        @Query("token") token: String,
    ): ApiResponse<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/oauth")
    suspend fun oauthLogin(@Body request: OAuthLoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<Unit>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<Unit>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): ApiResponse<AuthResponse>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
