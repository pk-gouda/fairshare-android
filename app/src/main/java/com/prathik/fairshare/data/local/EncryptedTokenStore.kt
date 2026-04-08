package com.prathik.fairshare.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences
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
 *
 * Why commit() instead of apply():
 * apply() writes to memory immediately but flushes to disk asynchronously.
 * If the app crashes between the memory write and disk flush, tokens are lost
 * and the user gets silently logged out on next launch.
 * commit() is synchronous — it only returns after the disk write succeeds.
 * The blocking time on EncryptedSharedPreferences is negligible (a few ms)
 * and these functions are always called from background threads.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = createPrefs()

    private fun createPrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                "fairshare_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // AEADBadTagException on fresh installs — corrupted or mismatched keystore key.
            // Delete the old prefs file and recreate from scratch.
            // User will need to log in again, but the app won't crash.
            context.deleteSharedPreferences("fairshare_secure_prefs")
            EncryptedSharedPreferences.create(
                context,
                "fairshare_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN       = "access_token"
        private const val KEY_REFRESH_TOKEN      = "refresh_token"
        private const val KEY_USER_ID            = "user_id"
        private const val KEY_PREFERRED_CURRENCY = "preferred_currency"
        private const val KEY_FULL_NAME          = "full_name"
    }

    fun saveTokens(
        accessToken      : String,
        refreshToken     : String,
        userId           : String,
        preferredCurrency: String = "USD",
        fullName         : String = "",
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PREFERRED_CURRENCY, preferredCurrency)
            .putString(KEY_FULL_NAME, fullName)
            .commit()
    }

    /**
     * Updates only the access token — called by TokenRefreshInterceptor
     * after a successful token refresh without disturbing other stored values.
     * Uses commit() so the new token is on disk before the retried request fires.
     */
    fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .commit()
    }

    /**
     * Returns the stored access token, or null if not logged in.
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getPreferredCurrency(): String = prefs.getString(KEY_PREFERRED_CURRENCY, "USD") ?: "USD"
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getFullName(): String = prefs.getString(KEY_FULL_NAME, "") ?: ""
    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_FULL_NAME)
            .commit()
    }
}