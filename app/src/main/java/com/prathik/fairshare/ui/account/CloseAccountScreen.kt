package com.prathik.fairshare.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

/**
 * Dedicated screen for account closure (deactivation and permanent deletion).
 *
 * Navigation: Account screen → "Close your account" → this screen.
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │  ← Close your account              │  ← FsTopBar
 * ├─────────────────────────────────────┤
 * │  Deactivate your account            │  ← Section header
 * │  Reversible. Your data is kept.     │
 * │  ┌───────────────────────────────┐  │
 * │  │  [Deactivate account]  button │  │  ← outlined red button
 * │  └───────────────────────────────┘  │
 * ├─────────────────────────────────────┤
 * │  Delete your account                │  ← Section header
 * │  Permanent. Data is anonymised.     │
 * │  ┌───────────────────────────────┐  │
 * │  │  [Delete account]  button     │  │  ← filled red button, de-emphasised
 * │  └───────────────────────────────┘  │
 * └─────────────────────────────────────┘
 *
 * Tapping either button shows:
 *   1. Confirmation AlertDialog (describes consequences)
 *   2. Password AlertDialog — LOCAL accounts only
 *      (GOOGLE/APPLE: confirmation tap goes straight to API call)
 */
@Composable
fun CloseAccountScreen(
    onBack      : () -> Unit,
    onLoggedOut : () -> Unit,
    viewModel   : AccountViewModel = hiltViewModel(),
) {
    val profile     by viewModel.profile.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Dialog state
    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm     by remember { mutableStateOf(false) }
    var showPasswordDialog    by remember { mutableStateOf(false) }

    // Shared password state across both flows
    var pendingPassword    by remember { mutableStateOf("") }
    var passwordVisible    by remember { mutableStateOf(false) }
    var passwordError      by remember { mutableStateOf<String?>(null) }
    var pendingIsDelete    by remember { mutableStateOf(false) }

    val isLoading = actionState is AccountActionState.Loading

    // Observe action results
    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AccountActionState.Error -> {
                if (showPasswordDialog) {
                    // Surface error inline in the password dialog
                    passwordError = s.message
                } else {
                    snackbarHost.showSnackbar(s.message)
                }
                viewModel.resetActionState()
            }
            is AccountActionState.Deactivated -> {
                showPasswordDialog = false
                pendingPassword = ""
                onLoggedOut()
            }
            is AccountActionState.Deleted -> {
                showPasswordDialog = false
                pendingPassword = ""
                onLoggedOut()
            }
            else -> Unit
        }
    }

    // ── Confirmation dialogs ──────────────────────────────────────────────────

    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirm = false },
            containerColor   = Surface2,
            title = {
                Text(
                    "Deactivate account?",
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    "Your account will be hidden and you'll be logged out. " +
                            "You can reactivate anytime by logging back in. " +
                            "You must have no outstanding balances to proceed.",
                    color    = TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeactivateConfirm = false
                        pendingIsDelete = false
                        if (profile?.authProvider == "LOCAL") {
                            // LOCAL: require password re-entry before proceeding
                            pendingPassword = ""
                            passwordError   = null
                            showPasswordDialog = true
                        } else {
                            // GOOGLE/APPLE: valid JWT is sufficient — no password needed
                            viewModel.deactivateAccount(password = null, onDone = {})
                        }
                    },
                ) {
                    Text(
                        "Deactivate",
                        color      = Negative,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = Surface2,
            title = {
                Text(
                    "Delete account permanently?",
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    "This cannot be undone. Your personal data will be permanently removed. " +
                            "Expense history will be anonymised for other users. " +
                            "You must have no outstanding balances to proceed.",
                    color    = TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        pendingIsDelete = true
                        if (profile?.authProvider == "LOCAL") {
                            pendingPassword = ""
                            passwordError   = null
                            showPasswordDialog = true
                        } else {
                            viewModel.deleteAccount(password = null, onDone = {})
                        }
                    },
                ) {
                    Text(
                        "Delete permanently",
                        color      = Negative,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // ── Password verification dialog (LOCAL accounts only) ────────────────────

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                pendingPassword   = ""
                passwordError     = null
                viewModel.resetActionState()
            },
            containerColor = Surface2,
            title = {
                Text(
                    if (pendingIsDelete) "Confirm deletion" else "Confirm deactivation",
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your current password to continue.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = pendingPassword,
                        onValueChange = { pendingPassword = it; passwordError = null },
                        label         = { Text("Password") },
                        singleLine    = true,
                        isError       = passwordError != null,
                        supportingText = passwordError?.let { err ->
                            { Text(err) }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Filled.VisibilityOff
                                    else
                                        Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible)
                                        "Hide password" else "Show password",
                                    tint = TextTertiary,
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            errorBorderColor       = Negative,
                            errorLabelColor        = Negative,
                            errorSupportingTextColor = Negative,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingPassword.isNotBlank() && !isLoading,
                    onClick = {
                        if (pendingIsDelete)
                            viewModel.deleteAccount(password = pendingPassword, onDone = {})
                        else
                            viewModel.deactivateAccount(password = pendingPassword, onDone = {})
                    },
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = Negative,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (pendingIsDelete) "Delete" else "Deactivate",
                            color      = Negative,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        showPasswordDialog = false
                        pendingPassword   = ""
                        passwordError     = null
                        viewModel.resetActionState()
                    },
                ) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Main screen ───────────────────────────────────────────────────────────

    Scaffold(
        topBar        = { FsTopBar(title = "Close your account", onBack = onBack) },
        snackbarHost  = { SnackbarHost(snackbarHost) },
        containerColor = Surface0,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Deactivate section ────────────────────────────────────────────
            CloseAccountSection(
                title       = "Deactivate your account",
                description = "Your account will be temporarily hidden and you'll be logged " +
                        "out. You can reactivate it anytime by logging back in with your " +
                        "existing credentials. Your data is kept in full.",
                buttonLabel = "Deactivate account",
                isDestructive = false,
                onClick     = { showDeactivateConfirm = true },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(color = Surface3)
            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Delete section ────────────────────────────────────────────────
            CloseAccountSection(
                title       = "Delete your account",
                description = "Permanently removes your personal data. This cannot be undone. " +
                        "Shared expense history will be anonymised but remain visible to " +
                        "other members of your groups.",
                buttonLabel = "Delete account permanently",
                isDestructive = true,
                onClick     = { showDeleteConfirm = true },
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

// ── Private components ────────────────────────────────────────────────────────

@Composable
private fun CloseAccountSection(
    title         : String,
    description   : String,
    buttonLabel   : String,
    isDestructive : Boolean,
    onClick       : () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text       = title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isDestructive) Negative else TextPrimary,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text     = description,
            fontSize = 14.sp,
            color    = TextSecondary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        TextButton(
            onClick  = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(
                    if (isDestructive) Negative.copy(alpha = 0.1f)
                    else Surface2
                ),
        ) {
            Text(
                text       = buttonLabel,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (isDestructive) Negative else TextSecondary,
            )
        }
    }
}
