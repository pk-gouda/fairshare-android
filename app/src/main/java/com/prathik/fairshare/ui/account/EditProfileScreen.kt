package com.prathik.fairshare.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsPasswordField
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack              : () -> Unit,
    onNavigateToPassword: () -> Unit,
    viewModel           : EditProfileViewModel = hiltViewModel(),
) {
    val fullName         by viewModel.fullName.collectAsState()
    val phoneNumber      by viewModel.phoneNumber.collectAsState()
    val email            by viewModel.email.collectAsState()
    val editingField     by viewModel.editingField.collectAsState()
    val emailChangeState by viewModel.emailChangeState.collectAsState()
    val pendingNewEmail  by viewModel.pendingNewEmail.collectAsState()
    val currentPassword  by viewModel.currentPassword.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val actionState      by viewModel.actionState.collectAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is EditProfileActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is EditProfileActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Avatar ────────────────────────────────────────────────────────
            FsAvatar(
                name     = fullName,
                userId   = email,
                imageUrl = profilePictureUrl,
                size     = 72.dp,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text     = "Change photo · coming soon",
                fontSize = 12.sp,
                color    = com.prathik.fairshare.ui.theme.TextTertiary,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Profile fields card ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2),
            ) {
                // Full name
                ProfileField(
                    label    = "Full name",
                    value    = fullName,
                    isEditing = editingField == EditingField.NAME,
                    onEditClick = { viewModel.startEditing(EditingField.NAME) },
                    editContent = {
                        FsTextField(
                            value         = fullName,
                            onValueChange = { viewModel.onFullNameChanged(it) },
                            label         = "Full name",
                            imeAction     = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { viewModel.saveName() }),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TextButton(onClick = { viewModel.stopEditing() }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            FsPrimaryButton(
                                text      = "Save",
                                onClick   = { viewModel.saveName() },
                                isLoading = isLoading,
                                modifier  = Modifier.weight(1f),
                            )
                        }
                    },
                )

                FieldDivider()

                // Email
                ProfileField(
                    label    = "Email address",
                    value    = email,
                    editLabel = "Change",
                    isEditing = editingField == EditingField.EMAIL,
                    onEditClick = { viewModel.startEditing(EditingField.EMAIL) },
                    editContent = {
                        Text(
                            text     = "Confirm your password to change email",
                            fontSize = 12.sp,
                            color    = TextTertiary,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                        FsPasswordField(
                            value         = currentPassword,
                            onValueChange = { viewModel.onCurrentPasswordChanged(it) },
                            label         = "Current password",
                            imeAction     = ImeAction.Next,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        FsTextField(
                            value         = pendingNewEmail,
                            onValueChange = { viewModel.onPendingEmailChanged(it) },
                            label         = "New email address",
                            keyboardType  = KeyboardType.Email,
                            imeAction     = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { viewModel.requestEmailChange() }),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TextButton(onClick = { viewModel.cancelEmailChange() }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            FsPrimaryButton(
                                text      = "Send verification",
                                onClick   = { viewModel.requestEmailChange() },
                                isLoading = isLoading,
                                modifier  = Modifier.weight(1f),
                            )
                        }
                    },
                )

                FieldDivider()

                // Phone
                ProfileField(
                    label    = "Phone number",
                    value    = phoneNumber.ifBlank { "Not set" },
                    isEditing = editingField == EditingField.PHONE,
                    onEditClick = { viewModel.startEditing(EditingField.PHONE) },
                    editContent = {
                        FsTextField(
                            value         = phoneNumber,
                            onValueChange = { viewModel.onPhoneChanged(it) },
                            label         = "Phone number",
                            keyboardType  = KeyboardType.Phone,
                            placeholder   = "Optional",
                            imeAction     = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { viewModel.savePhone() }),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TextButton(onClick = { viewModel.stopEditing() }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            FsPrimaryButton(
                                text      = "Save",
                                onClick   = { viewModel.savePhone() },
                                isLoading = isLoading,
                                modifier  = Modifier.weight(1f),
                            )
                        }
                    },
                )

                FieldDivider()

                // Password — navigates to ChangePassword
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPassword() }
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Password", fontSize = 11.sp, color = TextTertiary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "••••••••", fontSize = 18.sp, color = TextPrimary, letterSpacing = 3.sp)
                    }
                    Text(text = "Change", fontSize = 13.sp, color = Green400, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun ProfileField(
    label      : String,
    value      : String,
    editLabel  : String = "Edit",
    isEditing  : Boolean,
    onEditClick: () -> Unit,
    editContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    ) {
        // Always-visible row
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, color = TextTertiary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            if (!isEditing) {
                TextButton(onClick = onEditClick) {
                    Text(text = editLabel, fontSize = 13.sp, color = Green400, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Expandable edit area
        AnimatedVisibility(
            visible = isEditing,
            enter   = expandVertically(),
            exit    = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(bottom = Spacing.md)) {
                editContent()
            }
        }
    }
}

@Composable
private fun FieldDivider() {
    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = Spacing.md),
    )
}