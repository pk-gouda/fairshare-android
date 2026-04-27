package com.prathik.fairshare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.prathik.fairshare.ui.navigation.NavGraph
import com.prathik.fairshare.ui.theme.FairShareTheme
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.data.sync.CacheWarmupCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cacheWarmupCoordinator: CacheWarmupCoordinator

    // Holds the current intent so deep links arriving via onNewIntent
    // trigger recomposition without a full recreate().
    private var currentIntent by mutableStateOf(intent)

    override fun onStart() {
        super.onStart()
        // Foreground warmup — fires immediately in-process when the app
        // is opened or returns to foreground. Throttled to once per 5 min.
        cacheWarmupCoordinator.warmupIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FairShareTheme {
                Surface(
                    color    = Surface0,
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                ) {
                    val navController = rememberNavController()

                    // Read deep links from currentIntent — updates reactively when
                    // onNewIntent fires (e.g. email change confirmation deep link).
                    val startVerifyDeepLink   = remember(currentIntent) { extractVerifyDeepLink(currentIntent) }
                    val startLoginDeepLink    = remember(currentIntent) { isLoginDeepLink(currentIntent) }
                    val startEmailChangeToken = remember(currentIntent) { extractEmailChangeToken(currentIntent) }

                    NavGraph(
                        navController      = navController,
                        verifyDeepLink     = startVerifyDeepLink,
                        loginDeepLink      = startLoginDeepLink,
                        emailChangeToken   = startEmailChangeToken,
                    )
                }
            }
        }
    }

    // Called when the app is already running (singleTask) and a deep link arrives.
    // Updates currentIntent which triggers recomposition and routes to the right screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }

    companion object {

        /**
         * Extracts userId + token from a fairshare://verify-email deep link.
         * Returns null if the intent is not a verification link.
         */
        fun extractVerifyDeepLink(intent: Intent?): VerifyDeepLink? {
            val data = intent?.data ?: return null
            if (data.scheme != "fairshare" || data.host != "verify-email") return null
            val userId = data.getQueryParameter("userId") ?: return null
            val token  = data.getQueryParameter("token")  ?: return null
            return VerifyDeepLink(userId, token)
        }

        /**
         * Returns true if the intent is a fairshare://login deep link.
         * Fired by reset-password.html after a successful password reset
         * so the user lands on LoginScreen instead of Splash.
         */
        fun isLoginDeepLink(intent: Intent?): Boolean {
            val data = intent?.data ?: return false
            return data.scheme == "fairshare" && data.host == "login"
        }

        /**
         * Extracts the token from a fairshare://confirm-email-change?token=xxx deep link.
         * Returns null if the intent is not an email change confirmation link.
         */
        fun extractEmailChangeToken(intent: Intent?): String? {
            val data = intent?.data ?: return null
            if (data.scheme != "fairshare" || data.host != "confirm-email-change") return null
            return data.getQueryParameter("token")
        }
    }
}

/**
 * Carries the userId + token extracted from a fairshare://verify-email deep link.
 */
data class VerifyDeepLink(
    val userId: String,
    val token : String,
)