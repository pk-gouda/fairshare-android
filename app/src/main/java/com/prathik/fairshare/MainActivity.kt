package com.prathik.fairshare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.prathik.fairshare.ui.navigation.NavGraph
import com.prathik.fairshare.ui.theme.FairShareTheme
import com.prathik.fairshare.ui.theme.Surface0
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

                    // ✅ Read deep links on cold start / recreate().
                    // verifyDeepLink — fairshare://verify-email?userId=xxx&token=yyy
                    // loginDeepLink  — fairshare://login (from reset-password.html)
                    val startVerifyDeepLink = remember { extractVerifyDeepLink(intent) }
                    val startLoginDeepLink  = remember { isLoginDeepLink(intent) }

                    NavGraph(
                        navController  = navController,
                        verifyDeepLink = startVerifyDeepLink,
                        loginDeepLink  = startLoginDeepLink,
                    )
                }
            }
        }
    }

    // ✅ Called when the app is already running (singleTask) and a deep link
    // arrives while the app is in the background. recreate() re-reads the
    // new intent and lets NavGraph route correctly.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
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
    }
}

/**
 * Carries the userId + token extracted from a fairshare://verify-email deep link.
 */
data class VerifyDeepLink(
    val userId: String,
    val token : String,
)