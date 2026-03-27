package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.ChangePasswordRequest
import com.prathik.fairshare.data.model.request.ForgotPasswordRequest
import com.prathik.fairshare.data.model.request.LoginRequest
import com.prathik.fairshare.data.model.request.OAuthLoginRequest
import com.prathik.fairshare.data.model.request.RegisterRequest
import com.prathik.fairshare.data.model.request.ResetPasswordRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.AuthResponse
import com.prathik.fairshare.data.model.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query


interface AuthApiService {

    // Returns UserResponse — no tokens until email is verified + user logs in
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(
        @Query("userId") userId: String,
        @Query("token") token: String,
    ): ApiResponse<Unit>

    // Returns AuthResponse with accessToken + refreshToken
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/oauth")
    suspend fun oauthLogin(@Body request: OAuthLoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<Unit>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<Unit>

    // refreshToken is a @Query param on the backend (@RequestParam in Spring)
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Query("refreshToken") refreshToken: String): ApiResponse<AuthResponse>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    // refreshToken is a @Query param on the backend (@RequestParam in Spring)
    @POST("api/auth/logout")
    suspend fun logout(@Query("refreshToken") refreshToken: String): ApiResponse<Unit>
}

/**
 * Retrofit interface for /api/auth/** endpoints.
 * 9 endpoints covering the full authentication lifecycle.
 *
 * Key backend differences vs typical auth APIs:
 * - register  → returns UserResponse (no tokens — user must verify email first)
 * - refresh   → takes refreshToken as @Query param, not @Body
 * - logout    → takes refreshToken as @Query param, not @Body
 */