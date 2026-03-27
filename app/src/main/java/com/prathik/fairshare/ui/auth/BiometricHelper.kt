package com.prathik.fairshare.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps AndroidX BiometricPrompt as a suspend function.
 *
 * Used in SettleUpScreen for settlements above the threshold ($50).
 * Supports fingerprint, face unlock, and device PIN/pattern as fallback.
 *
 * Usage:
 *   val result = biometricHelper.authenticate(activity, "Confirm settlement of $50.00")
 *   when (result) {
 *       is BiometricResult.Success  → proceed with settlement
 *       is BiometricResult.Error    → show error message
 *       is BiometricResult.Cancelled → do nothing
 *   }
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns true if biometric or device credential auth is available.
     * If false, settlements proceed without biometric check.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt and suspends until the user responds.
     * Must be called from a composable context with a FragmentActivity.
     *
     * [activity]    — the current FragmentActivity (MainActivity)
     * [title]       — prompt title e.g. "Confirm settlement of $50.00"
     * [subtitle]    — optional subtitle shown below the title
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "Use biometric or device PIN to confirm",
    ): BiometricResult = suspendCancellableCoroutine { continuation ->

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                if (continuation.isActive) continuation.resume(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        continuation.resume(BiometricResult.Cancelled)
                    } else {
                        continuation.resume(BiometricResult.Error(errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                // Called when biometric doesn't match — prompt stays open,
                // user can try again. Don't resume here.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)

        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    object Success                      : BiometricResult()
    object Cancelled                    : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}