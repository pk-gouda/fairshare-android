package com.prathik.fairshare.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Forgot password screen.
 *
 * User enters their email to receive a reset link.
 * On ForgotPasswordSuccess → shows success state with resend option (60s cooldown).
 * On Error → shows Snackbar.
 */
@Composable
fun ForgotPasswordScreen(
    onBack    : () -> Unit,
    viewModel : AuthViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsState()
    val email        by viewModel.email.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()

    // Resend cooldown state
    var resendCooldown    by rememberSaveable { mutableStateOf(0) }
    var showSuccessState  by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.ForgotPasswordSuccess -> {
                showSuccessState = true
                resendCooldown   = 60
                viewModel.onEvent(AuthUiEvent.OnResetState)
            }
            is AuthUiState.Error -> {
                snackbarHost.showSnackbar(state.message)
                viewModel.onEvent(AuthUiEvent.OnResetState)
            }
            else -> Unit
        }
    }

    // Count down resend cooldown
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
        }
    }

    val isLoading = uiState is AuthUiState.Loading

    Box(modifier = Modifier.fillMaxSize().background(Surface0)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xxl)
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Back button
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            if (!showSuccessState) {

                // ── Input state ────────────────────────────────────────────────
                Text(
                    text       = "Reset\npassword",
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 40.sp,
                    color      = TextPrimary,
                    lineHeight = 48.sp,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text     = "Enter your email and we'll send you a reset link.",
                    fontSize = 15.sp,
                    color    = TextSecondary,
                )

                Spacer(modifier = Modifier.height(40.dp))

                FsTextField(
                    value         = email,
                    onValueChange = { viewModel.onEvent(AuthUiEvent.OnEmailChanged(it)) },
                    label         = "Email",
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Done,
                    error         = viewModel.getFieldError("email"),
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                FsPrimaryButton(
                    text      = "Send reset link",
                    onClick   = { viewModel.onEvent(AuthUiEvent.OnForgotPasswordClicked) },
                    modifier  = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                )

            } else {

                // ── Success state ──────────────────────────────────────────────
                Text(
                    text       = "Check your\nemail",
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 40.sp,
                    color      = TextPrimary,
                    lineHeight = 48.sp,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text     = "We sent a reset link to $email",
                    fontSize = 15.sp,
                    color    = TextSecondary,
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text     = "Didn't receive it? Check your spam folder or resend.",
                    fontSize = 14.sp,
                    color    = TextSecondary,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Resend button with cooldown
                FsPrimaryButton(
                    text    = if (resendCooldown > 0) "Resend in ${resendCooldown}s"
                    else "Resend email",
                    onClick = {
                        scope.launch {
                            viewModel.onEvent(AuthUiEvent.OnForgotPasswordClicked)
                        }
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    enabled   = resendCooldown == 0,
                    isLoading = isLoading,
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Back to login
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FsTextButton(
                        text    = "Back to sign in",
                        onClick = onBack,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}