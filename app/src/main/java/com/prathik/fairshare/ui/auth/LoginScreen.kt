package com.prathik.fairshare.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPasswordField
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Login screen.
 *
 * Collects email + password, delegates to AuthViewModel.
 * On LoginSuccess → navigates to Groups (clearing auth back stack).
 * On Error → shows Snackbar.
 * On ValidationError → shows inline field errors.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister     : () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToGroups       : () -> Unit,
    viewModel                : AuthViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsState()
    val email       by viewModel.email.collectAsState()
    val password    by viewModel.password.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Handle state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.LoginSuccess -> {
                onNavigateToGroups()
                viewModel.onEvent(AuthUiEvent.OnResetState)
            }
            is AuthUiState.Error -> {
                snackbarHost.showSnackbar(state.message)
                viewModel.onEvent(AuthUiEvent.OnResetState)
            }
            else -> Unit
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
            Spacer(modifier = Modifier.height(80.dp))

            // Headline
            Text(
                text       = "Welcome\nback",
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 40.sp,
                color      = TextPrimary,
                lineHeight = 48.sp,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text     = "Sign in to continue",
                fontSize = 15.sp,
                color    = TextSecondary,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email
            FsTextField(
                value         = email,
                onValueChange = { viewModel.onEvent(AuthUiEvent.OnEmailChanged(it)) },
                label         = "Email",
                keyboardType  = KeyboardType.Email,
                imeAction     = ImeAction.Next,
                error         = viewModel.getFieldError("email"),
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Password
            FsPasswordField(
                value         = password,
                onValueChange = { viewModel.onEvent(AuthUiEvent.OnPasswordChanged(it)) },
                label         = "Password",
                imeAction     = ImeAction.Done,
                error         = viewModel.getFieldError("password"),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Forgot password
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                FsTextButton(
                    text    = "Forgot password?",
                    onClick = onNavigateToForgotPassword,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Sign in button
            FsPrimaryButton(
                text      = "Sign in",
                onClick   = { viewModel.onEvent(AuthUiEvent.OnLoginClicked) },
                modifier  = Modifier.fillMaxWidth(),
                isLoading = isLoading,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Register link
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "No account? ",
                    color    = TextSecondary,
                    fontSize = 14.sp,
                )
                Text(
                    text       = "Sign up",
                    color      = Green400,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { onNavigateToRegister() },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}