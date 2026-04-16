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
import com.prathik.fairshare.ui.navigation.Screen
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

                    // ✅ Read deep link on cold start.
                    // If the activity was launched via fairshare://verify-email,
                    // extract userId + token and pass to NavGraph so it can
                    // navigate to the VerifyEmailScreen in "verifying" mode.
                    val startDeepLink = remember { extractVerifyDeepLink(intent) }

                    NavGraph(
                        navController  = navController,
                        verifyDeepLink = startDeepLink,
                    )
                }
            }
        }
    }

    // ✅ Called when the app is already running (singleTask) and the user
    // taps the verification link while the app is in the background.
    // android:launchMode="singleTask" in the manifest ensures this fires
    // instead of creating a new activity instance.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The NavGraph will re-read the intent next composition via
        // a side-effect in the VerifyEmailScreen deep link handler.
        // For simplicity we recreate — singleTask guarantees no stack loss.
        recreate()
    }

    companion object {
        /**
         * Extracts userId and token from a fairshare://verify-email deep link intent.
         * Returns null if the intent is not a verification link.
         */
        fun extractVerifyDeepLink(intent: Intent?): VerifyDeepLink? {
            val data = intent?.data ?: return null
            if (data.scheme != "fairshare" || data.host != "verify-email") return null
            val userId = data.getQueryParameter("userId") ?: return null
            val token  = data.getQueryParameter("token")  ?: return null
            return VerifyDeepLink(userId, token)
        }
    }
}

/**
 * Carries the userId + token extracted from a fairshare://verify-email deep link.
 * Passed from MainActivity down to NavGraph so the VerifyEmailScreen can
 * immediately call the verify API instead of waiting for user action.
 */
data class VerifyDeepLink(
    val userId: String,
    val token : String,
)