package com.prathik.fairshare.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences (AES256).
 *
 * Replaces plain DataStore for token storage because tokens are
 * security-sensitive credentials — they should never be stored in
 * plain text on disk. EncryptedSharedPreferences encrypts both
 * keys and values at rest using AES256-GCM.
 *
 * Stores:
 * - accessToken  — JWT access token (expires in 15 minutes)
 * - refreshToken — JWT refresh token (expires in 7 days)
 * - userId       — current user's UUID
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "fairshare_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID       = "user_id"
    }

    /**
     * Saves all three tokens atomically after successful login or register.
     */
    fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Updates only the access token — called by TokenRefreshInterceptor
     * after a successful token refresh without disturbing other stored values.
     */
    fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    /**
     * Returns the stored access token, or null if not logged in.
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Returns the stored refresh token, or null if not logged in.
     */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Returns the stored user ID, or null if not logged in.
     */
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    /**
     * Returns true if an access token exists — used by IsLoggedInUseCase.
     * Does not validate the token — that happens on the first API call.
     */
    fun isLoggedIn(): Boolean = getAccessToken() != null

    /**
     * Clears all stored tokens — called on logout.
     * After this, isLoggedIn() returns false and all API calls will 401.
     */
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }
}