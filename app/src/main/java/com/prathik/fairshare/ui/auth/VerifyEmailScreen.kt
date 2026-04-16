package com.prathik.fairshare.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Verify email screen — shown after registration and after tapping the
 * verification link in the email.
 *
 * Three modes:
 *
 * 1. WAITING  — no deep link yet. Shows the animated envelope + "Check your
 *               email" UI with a resend button and cooldown timer.
 *
 * 2. VERIFYING (VerifyEmailLoading) — deep link received, API call in-flight.
 *               Shows a spinner.
 *
 * 3. SUCCESS  (VerifyEmailSuccess) — account activated. Shows a green checkmark
 *               and auto-navigates to Login after 2 seconds.
 *
 * 4. ERROR    (VerifyEmailError) — token invalid/expired. Shows error message
 *               with a "Back to sign in" option.
 *
 * @param email            The email address registered (shown in waiting state)
 * @param verifyUserId     userId from deep link (null if no deep link)
 * @param verifyToken      token from deep link (null if no deep link)
 * @param onNavigateToLogin Navigate to Login screen
 * @param viewModel        Shared auth ViewModel
 */
@Composable
fun VerifyEmailScreen(
    email            : String?,
    verifyUserId     : String? = null,
    verifyToken      : String? = null,
    onNavigateToLogin: () -> Unit,
    viewModel        : AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── If deep link params arrived, fire verification immediately ────────────
    LaunchedEffect(verifyUserId, verifyToken) {
        if (!verifyUserId.isNullOrBlank() && !verifyToken.isNullOrBlank()) {
            viewModel.onEvent(AuthUiEvent.OnVerifyEmail(verifyUserId, verifyToken))
        }
    }

    // ── Auto-navigate to Login 2 seconds after success ────────────────────────
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.VerifyEmailSuccess) {
            delay(2000L)
            viewModel.onEvent(AuthUiEvent.OnResetState)
            onNavigateToLogin()
        }
    }

    // ── Resend cooldown (waiting state only) ──────────────────────────────────
    var resendCooldown by rememberSaveable { mutableStateOf(60) }
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
        }
    }

    // ── Pulsing envelope animation (waiting state only) ───────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "envelope")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Box(
        modifier         = Modifier.fillMaxSize().background(Surface0),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState   = uiState,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label         = "verify_state",
        ) { state ->
            when (state) {

                // ── Verifying — spinner ───────────────────────────────────────
                is AuthUiState.VerifyEmailLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    ) {
                        CircularProgressIndicator(
                            color    = Green400,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        Text(
                            text       = "Verifying your account…",
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text      = "This will only take a moment.",
                            fontSize  = 15.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // ── Success — green checkmark ─────────────────────────────────
                is AuthUiState.VerifyEmailSuccess -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint               = Green400,
                            modifier           = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        Text(
                            text       = "Email verified!",
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 36.sp,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text      = "Your account is active. Taking you to sign in…",
                            fontSize  = 15.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // ── Already verified — shown on second tap of the email link ──
                is AuthUiState.VerifyEmailAlreadyVerified -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint               = Green400,
                            modifier           = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        Text(
                            text       = "Already verified",
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 36.sp,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text      = "Your email is already verified.\nYou can sign in to FairShare.",
                            fontSize  = 15.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        FsPrimaryButton(
                            text     = "Sign in",
                            onClick  = {
                                viewModel.onEvent(AuthUiEvent.OnResetState)
                                onNavigateToLogin()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Error — token invalid/expired ─────────────────────────────
                is AuthUiState.VerifyEmailError -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint               = Negative,
                            modifier           = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        Text(
                            text       = "Verification failed",
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 32.sp,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text      = state.message,
                            fontSize  = 15.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                        FsPrimaryButton(
                            text     = "Back to sign in",
                            onClick  = {
                                viewModel.onEvent(AuthUiEvent.OnResetState)
                                onNavigateToLogin()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Waiting — animated envelope (default / no deep link) ──────
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    ) {
                        // Animated envelope icon
                        Icon(
                            imageVector        = Icons.Outlined.Email,
                            contentDescription = null,
                            tint               = Green400,
                            modifier           = Modifier
                                .size(72.dp)
                                .scale(scale),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xxl))

                        // Headline — centered
                        Text(
                            text       = "Check your\nemail",
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 40.sp,
                            color      = TextPrimary,
                            lineHeight = 48.sp,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Subtitle — centered
                        Text(
                            text      = "We sent a verification link to",
                            fontSize  = 15.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        // Email address — centered + bold
                        Text(
                            text       = email ?: "your email",
                            fontSize   = 15.sp,
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Instructions — centered
                        Text(
                            text       = "Click the link in the email to verify your account. Check your spam folder if you don't see it.",
                            fontSize   = 14.sp,
                            color      = TextSecondary,
                            lineHeight = 20.sp,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xxl))

                        // Resend button with cooldown
                        FsPrimaryButton(
                            text     = if (resendCooldown > 0) "Resend in ${resendCooldown}s"
                            else "Resend verification email",
                            onClick  = { resendCooldown = 60 },
                            modifier = Modifier.fillMaxWidth(),
                            enabled  = resendCooldown == 0,
                        )

                        Spacer(modifier = Modifier.height(Spacing.lg))

                        // Back to login
                        FsTextButton(
                            text    = "Back to sign in",
                            onClick = onNavigateToLogin,
                        )
                    }
                }
            }
        }
    }
}