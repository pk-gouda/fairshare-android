package com.prathik.fairshare.data.network.interceptor

import com.prathik.fairshare.data.local.EncryptedTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the JWT access token to every outgoing request.
 *
 * Reads the token synchronously from EncryptedTokenStore —
 * this is safe because OkHttp interceptors run on a background thread.
 *
 * If no token exists (user not logged in), the request proceeds without
 * an auth header. The backend will return 401 for protected endpoints.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: EncryptedTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip if request already has Authorization header
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenStore.getAccessToken()
            ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}