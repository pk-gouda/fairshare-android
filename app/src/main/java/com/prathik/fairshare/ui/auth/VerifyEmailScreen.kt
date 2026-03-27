package com.prathik.fairshare.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Verify email screen.
 *
 * Shown after registration — user needs to verify their email before logging in.
 * Shows animated envelope icon + email address.
 * Provides resend option with 60s cooldown.
 *
 * [email]             — the email address that was registered
 * [onNavigateToLogin] — navigates back to Login
 */
@Composable
fun VerifyEmailScreen(
    email            : String?,
    onNavigateToLogin: () -> Unit,
) {
    // Resend cooldown
    var resendCooldown by rememberSaveable { mutableStateOf(60) }

    // Count down cooldown 1 second at a time
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
        }
    }

    // Pulsing envelope animation
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