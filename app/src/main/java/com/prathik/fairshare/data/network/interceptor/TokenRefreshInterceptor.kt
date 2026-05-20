package com.prathik.fairshare.data.network.interceptor

import com.prathik.fairshare.data.local.EncryptedTokenStore
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Handles automatic JWT token refresh on 401 responses.
 *
 * Flow:
 * 1. Request goes out with access token (attached by AuthInterceptor)
 * 2. Backend returns 401 — access token expired
 * 3. This interceptor acquires a lock (prevents multiple simultaneous refreshes)
 * 4. Calls POST /api/auth/refresh with the stored refresh token
 * 5. On success: saves new access token, retries original request
 * 6. On failure: clears all tokens (user must log in again)
 *
 * Why ReentrantLock instead of Mutex:
 * OkHttp interceptors run on Java threads, not coroutines.
 * ReentrantLock is the correct synchronization primitive here.
 */
@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenStore: EncryptedTokenStore,
) : Interceptor {

    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalResponse = chain.proceed(originalRequest)

        // Only handle 401 responses
        if (originalResponse.code != 401) {
            return originalResponse
        }

        // Skip refresh for the refresh endpoint itself
        // to prevent infinite loop
        if (originalRequest.url.encodedPath.contains("/api/auth/refresh")) {
            // The refresh endpoint itself got a 401 — refresh token is expired/revoked.
            // Signal expiry so the UI can redirect to Login.
            tokenStore.clearTokensAndSignalExpiry()
            return originalResponse
        }

        // Skip refresh for login/register — they don't use tokens
        if (originalRequest.url.encodedPath.contains("/api/auth/login") ||
            originalRequest.url.encodedPath.contains("/api/auth/register")
        ) {
            return originalResponse
        }

        return lock.withLock {
            // Check if another thread already refreshed the token
            // while we were waiting for the lock
            val currentToken = tokenStore.getAccessToken()
            val requestToken = originalRequest.header("Authorization")
                ?.removePrefix("Bearer ")

            if (currentToken != null && currentToken != requestToken) {
                // Token was already refreshed by another thread — retry with new token
                originalResponse.close()
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // We need to refresh the token
                val refreshToken = tokenStore.getRefreshToken()

                if (refreshToken == null) {
                    // No refresh token — unrecoverable. Signal expiry so MainShell
                    // can navigate the user to Login instead of showing broken screens.
                    tokenStore.clearTokensAndSignalExpiry()
                    return@withLock originalResponse
                }

                val refreshed = tryRefreshToken(chain, refreshToken)

                if (refreshed) {
                    // Refresh succeeded — retry original request silently.
                    // User stays in the app; no session-expired event emitted.
                    originalResponse.close()
                    val newToken = tokenStore.getAccessToken()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    chain.proceed(retryRequest)
                } else {
                    // Refresh failed (401 from refresh endpoint, network error, etc.)
                    // Unrecoverable — signal expiry so MainShell navigates to Login.
                    tokenStore.clearTokensAndSignalExpiry()
                    originalResponse
                }
            }
        }
    }

    /**
     * Calls POST /api/auth/refresh with the stored refresh token.
     * Saves the new access token on success.
     * Returns true if refresh succeeded, false otherwise.
     */
    private fun tryRefreshToken(
        chain: Interceptor.Chain,
        refreshToken: String,
    ): Boolean {
        return try {
            val request = chain.request()
            // OkHttp returns -1 for default ports (443 for HTTPS, 80 for HTTP).
            // Using port > 0 guards against -1 and any other sentinel values.
            val port = request.url.port
            val portPart = if (port > 0 && port != 80 && port != 443) ":$port" else ""
            val baseUrl = "${request.url.scheme}://${request.url.host}$portPart".trimEnd('/')

            val refreshRequest = Request.Builder()
                .url("$baseUrl/api/auth/refresh")
                .post(
                    """{"refreshToken":"$refreshToken"}"""
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = chain.proceed(refreshRequest)

            if (response.isSuccessful) {
                val body = response.body?.string()
                response.close()
                if (body != null) {
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")
                    val newAccessToken  = data?.optString("accessToken")
                    val newRefreshToken = data?.optString("refreshToken")
                    if (!newAccessToken.isNullOrBlank()) {
                        tokenStore.updateAccessToken(newAccessToken)
                        // Backend rotates refresh tokens on every use — save the new one.
                        // If the field is missing (old backend), the old refresh token
                        // stays in place and the next refresh will fail, prompting re-login.
                        if (!newRefreshToken.isNullOrBlank()) {
                            tokenStore.updateRefreshToken(newRefreshToken)
                        }
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                response.close()
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}