package com.prathik.fairshare.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsPasswordField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack      : () -> Unit,
    onLoggedOut : () -> Unit = {},
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val currentPassword    by viewModel.currentPassword.collectAsState()
    val newPassword        by viewModel.newPassword.collectAsState()
    val confirmPassword    by viewModel.confirmPassword.collectAsState()
    val isLoading          by viewModel.isLoading.collectAsState()
    val actionState        by viewModel.actionState.collectAsState()
    val currentPasswordErr by viewModel.currentPasswordError.collectAsState()
    val newPasswordErr     by viewModel.newPasswordError.collectAsState()
    val confirmPasswordErr by viewModel.confirmPasswordError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is ChangePasswordActionState.Success -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
                onLoggedOut()
            }
            is ChangePasswordActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Change Password", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            FsPasswordField(
                value         = currentPassword,
                onValueChange = { viewModel.onCurrentPasswordChanged(it) },
                label         = "Current password",
                error         = currentPasswordErr,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            FsPasswordField(
                value         = newPassword,
                onValueChange = { viewModel.onNewPasswordChanged(it) },
                label         = "New password",
                error         = newPasswordErr,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            FsPasswordField(
                value         = confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                label         = "Confirm new password",
                error         = confirmPasswordErr,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            FsPrimaryButton(
                text      = "Update password",
                onClick   = { viewModel.changePassword() },
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}