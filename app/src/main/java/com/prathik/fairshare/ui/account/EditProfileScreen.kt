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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack              : () -> Unit,
    onNavigateToPassword: () -> Unit,
    viewModel           : EditProfileViewModel = hiltViewModel(),
) {
    val fullName    by viewModel.fullName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val email       by viewModel.email.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is EditProfileActionState.Success -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
                onBack()
            }
            is EditProfileActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Edit Profile", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Full name
            FsTextField(
                value         = fullName,
                onValueChange = { viewModel.onFullNameChanged(it) },
                label         = "Full name",
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Email — read only
            FsTextField(
                value         = email,
                onValueChange = {},
                label         = "Email",
                enabled       = false,
                modifier      = Modifier.fillMaxWidth(),
            )
            Text(
                text     = "Email cannot be changed",
                fontSize = 11.sp,
                color    = TextTertiary,
                modifier = Modifier.padding(start = Spacing.xs, top = 4.dp),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Phone number
            FsTextField(
                value         = phoneNumber,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label         = "Phone number",
                keyboardType  = KeyboardType.Phone,
                placeholder   = "Optional",
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Save button
            FsPrimaryButton(
                text      = "Save changes",
                onClick   = { viewModel.saveProfile() },
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Change password link
            androidx.compose.material3.TextButton(
                onClick  = onNavigateToPassword,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = "Change password", color = com.prathik.fairshare.ui.theme.Green400)
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}