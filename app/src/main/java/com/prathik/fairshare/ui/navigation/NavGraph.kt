package com.prathik.fairshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.prathik.fairshare.MainActivity.Companion.extractVerifyDeepLink
import com.prathik.fairshare.MainActivity.Companion.extractEmailChangeToken
import com.prathik.fairshare.VerifyDeepLink
import com.prathik.fairshare.ui.account.ConfirmEmailChangeScreen
import com.prathik.fairshare.ui.auth.SplashScreen
import com.prathik.fairshare.ui.auth.LoginScreen
import com.prathik.fairshare.ui.auth.RegisterScreen
import com.prathik.fairshare.ui.auth.ForgotPasswordScreen
import com.prathik.fairshare.ui.auth.VerifyEmailScreen
import com.prathik.fairshare.ui.shell.MainShell

/**
 * Top-level navigation graph.
 *
 * Only auth screens, the main shell, and routes with deep links live here.
 * All in-app navigation (groups, expenses, friends, settings, etc.) is
 * handled by MainShell's own nested NavHost.
 */
@Composable
fun NavGraph(
    navController    : NavHostController,
    modifier         : Modifier = Modifier,
    verifyDeepLink   : VerifyDeepLink? = null,
    loginDeepLink    : Boolean = false,
    emailChangeToken : String? = null,
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
        modifier         = modifier,
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToGroups = {
                    navController.navigate(Screen.Groups.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route(null)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToConfirmEmailChange = {
                    navController.navigate(Screen.ConfirmEmailChange.route(emailChangeToken ?: "")) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                verifyDeepLink   = verifyDeepLink,
                loginDeepLink    = loginDeepLink,
                emailChangeToken = emailChangeToken,
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister       = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToGroups         = {
                    navController.navigate(Screen.Groups.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin       = { navController.popBackStack() },
                onNavigateToVerifyEmail = { email ->
                    navController.navigate(Screen.VerifyEmail.route(email)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route     = Screen.VerifyEmail.route,
            arguments = listOf(
                navArgument("email") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "fairshare://verify-email" }
            ),
        ) { backStackEntry ->
            VerifyEmailScreen(
                email             = backStackEntry.arguments?.getString("email"),
                verifyUserId      = verifyDeepLink?.userId,
                verifyToken       = verifyDeepLink?.token,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                },
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        // ── Email change deep link ────────────────────────────────────────────
        composable(
            route     = Screen.ConfirmEmailChange.route,
            arguments = listOf(
                navArgument("token") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "fairshare://confirm-email-change?token={token}" }
            ),
        ) { backStackEntry ->
            val token = emailChangeToken ?: backStackEntry.arguments?.getString("token")
            ConfirmEmailChangeScreen(
                token  = token,
                onDone = {
                    navController.navigate(Screen.Groups.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Main shell (handles all in-app navigation internally) ─────────────
        composable(Screen.Groups.route) {
            MainShell(rootNavController = navController, emailChangeToken = emailChangeToken)
        }
    }
}