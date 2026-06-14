package com.prathik.fairshare.data.network.interceptor

import com.prathik.fairshare.data.local.EncryptedTokenStore
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
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
 * 6. On DEFINITIVE failure (refresh token rejected 400/401/403, malformed
 *    response): clears all tokens (user must log in again)
 * 7. On TRANSIENT failure (network IOException, refresh endpoint 5xx/429):
 *    keeps tokens intact and returns the original 401 — the session survives
 *    and the next request retries the refresh from scratch.
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

    /**
     * Outcome of a refresh attempt. Distinguishes failures that mean the
     * refresh token is genuinely dead (DEFINITIVE_FAILURE → clear session)
     * from failures where the token is still valid but the attempt couldn't
     * complete (TRANSIENT_FAILURE → keep session, surface the original 401).
     */
    private enum class RefreshAttemptResult {
        SUCCESS,
        DEFINITIVE_FAILURE,
        TRANSIENT_FAILURE,
    }

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

                val result = tryRefreshToken(chain, refreshToken)

                when (result) {
                    RefreshAttemptResult.SUCCESS -> {
                        // Refresh succeeded — retry original request silently.
                        // User stays in the app; no session-expired event emitted.
                        originalResponse.close()
                        val newToken = tokenStore.getAccessToken()
                        val retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        chain.proceed(retryRequest)
                    }
                    RefreshAttemptResult.TRANSIENT_FAILURE -> {
                        // Network error or backend 5xx/429 during refresh —
                        // the refresh token is still valid. Do NOT clear the
                        // session. Return the original 401; the next request
                        // will retry the refresh from scratch.
                        originalResponse
                    }
                    RefreshAttemptResult.DEFINITIVE_FAILURE -> {
                        // Refresh token rejected (400/401/403) or response was
                        // unusable. Unrecoverable — signal expiry so MainShell
                        // navigates to Login.
                        tokenStore.clearTokensAndSignalExpiry()
                        originalResponse
                    }
                }
            }
        }
    }

    /**
     * Calls POST /api/auth/refresh with the stored refresh token.
     * Saves the new access (and rotated refresh) token on success.
     *
     * Classification:
     *  - 2xx with usable tokens           → SUCCESS
     *  - IOException (network failure)    → TRANSIENT_FAILURE (token still valid)
     *  - HTTP 408/429/5xx from refresh    → TRANSIENT_FAILURE (timeout/backend transient)
     *  - HTTP 400/401/403 from refresh    → DEFINITIVE_FAILURE (token rejected)
     *  - malformed JSON / missing fields /
     *    URL bug / any other exception    → DEFINITIVE_FAILURE (unrecoverable)
     */
    private fun tryRefreshToken(
        chain: Interceptor.Chain,
        refreshToken: String,
    ): RefreshAttemptResult {
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

            if (!response.isSuccessful) {
                val code = response.code
                // Always close non-success responses before returning —
                // otherwise the response body leaks.
                response.close()
                return when {
                    code == 400 || code == 401 || code == 403 -> RefreshAttemptResult.DEFINITIVE_FAILURE
                    code == 408 || code == 429 || code >= 500 -> RefreshAttemptResult.TRANSIENT_FAILURE
                    else -> RefreshAttemptResult.DEFINITIVE_FAILURE
                }
            }

            val body = response.body?.string()
            response.close()
            if (body == null) {
                return RefreshAttemptResult.DEFINITIVE_FAILURE
            }

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
                RefreshAttemptResult.SUCCESS
            } else {
                RefreshAttemptResult.DEFINITIVE_FAILURE
            }
        } catch (e: IOException) {
            // Network failure mid-refresh — the refresh token is still valid.
            RefreshAttemptResult.TRANSIENT_FAILURE
        } catch (e: Exception) {
            // JSON parse error, URL construction bug, or anything unexpected —
            // treat as unrecoverable.
            RefreshAttemptResult.DEFINITIVE_FAILURE
        }
    }
}