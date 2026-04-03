package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Orange400
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
fun FriendSettingsScreen(
    onBack: () -> Unit,
    onRemoved: () -> Unit,
    viewModel: FriendSettingsViewModel = hiltViewModel(),
) {
    val friend by viewModel.friend.collectAsState()
    val friendType by viewModel.friendType.collectAsState()
    val sharedGroups by viewModel.sharedGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showEditEmail by remember { mutableStateOf(false) }
    var editEmailValue by remember { mutableStateOf("") }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendSettingsActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }

            is FriendSettingsActionState.Success -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }

            else -> Unit
        }
    }

    // Remove dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = Surface2,
            title = { Text("Remove?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Remove ${friend?.fullName ?: "this person"} from your friends list?",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false; viewModel.removeFriend { onRemoved() }
                }) {
                    Text("Remove", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // Block dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = Surface2,
            title = {
                Text(
                    "Block ${friend?.fullName ?: "user"}?",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "They won't be able to contact you or see your profile.",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false; viewModel.blockUser { onRemoved() }
                }) {
                    Text("Block", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // Edit contact info dialog
    if (showEditEmail) {
        AlertDialog(
            onDismissRequest = { showEditEmail = false },
            containerColor = Surface2,
            title = {
                Text(
                    "Edit contact info",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                FsTextField(
                    value = editEmailValue,
                    onValueChange = { editEmailValue = it },
                    label = "Email or phone",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditEmail = false
                    viewModel.updateContactInfo(editEmailValue)
                }) {
                    Text("Save", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmail = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { FsTopBar(title = "Friend Settings", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Profile header ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FsAvatar(
                    name = friend?.fullName ?: "",
                    userId = friend?.id ?: "",
                    size = 48.dp,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = friend?.fullName ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Text(
                        text = when (val t = friendType) {
                            is FriendType.Invited -> t.email
                            FriendType.Placeholder -> "No email address"
                            FriendType.Pending -> "Request pending"
                            FriendType.Accepted -> friend?.email ?: ""
                        },
                        fontSize = 13.sp,
                        color = TextTertiary,
                    )
                }
            }

            HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Context-aware banner ──────────────────────────────────────────
            when (val t = friendType) {
                FriendType.Placeholder -> {
                    InviteBanner(
                        message = "${friend?.fullName ?: "This person"} is not on FairShare. Would you like to send an invite?",
                        actions = listOf(
                            BannerAction(
                                Icons.Outlined.Send,
                                "Invite ${friend?.fullName ?: "them"} to FairShare"
                            ) {
                                viewModel.sendInvite {}
                            },
                        ),
                    )
                }

                is FriendType.Invited -> {
                    LaunchedEffect(friend) { editEmailValue = t.email }
                    InviteBanner(
                        message = "${
                            friend?.fullName?.split(" ")?.firstOrNull() ?: "Their"
                        }'s invite has not been accepted. Is the email address \"${t.email}\" correct?",
                        actions = listOf(
                            BannerAction(Icons.Outlined.Edit, "No, edit contact info") {
                                editEmailValue = t.email
                                showEditEmail = true
                            },
                            BannerAction(Icons.Outlined.Send, "Yes, resend invite") {
                                viewModel.sendInvite {}
                            },
                        ),
                    )
                }

                else -> Unit
            }

            // ── Shared groups ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Shared groups",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            if (sharedGroups.isEmpty()) {
                Text(
                    text = "You and ${
                        friend?.fullName?.split(" ")?.firstOrNull() ?: "this person"
                    } do not share any groups.",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            } else {
                sharedGroups.forEach { group ->
                    Text(
                        text = group.name,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                }
            }

            // ── Manage relationship ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = "Manage relationship",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            ManageRow(
                icon = Icons.Outlined.PersonRemove,
                iconTint = Negative,
                label = "Remove from friends list",
                subtitle = "Remove this user from your friends list.",
                onClick = { showRemoveDialog = true },
            )

            // Block and Report — only for real users
            if (friendType == FriendType.Accepted || friendType == FriendType.Pending) {
                ManageRow(
                    icon = Icons.Outlined.Block,
                    iconTint = TextSecondary,
                    label = "Block user",
                    subtitle = "Remove this user from your friends list, hide any groups you share, and suppress future expenses/notifications from them.",
                    onClick = { showBlockDialog = true },
                )
                ManageRow(
                    icon = Icons.Outlined.Flag,
                    iconTint = TextSecondary,
                    label = "Report user",
                    subtitle = "Flag an abusive, suspicious, or spam account.",
                    onClick = { /* TODO */ },
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Banner ────────────────────────────────────────────────────────────────────

data class BannerAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun InviteBanner(message: String, actions: List<BannerAction>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .clip(RoundedCornerShape(Radius.xl))
            .background(Orange400),
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.md,
            ),
        )
        actions.forEach { action ->
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = action.onClick)
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(text = action.label, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

// ── Manage row ────────────────────────────────────────────────────────────────

@Composable
private fun ManageRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (iconTint == Negative) Negative else TextPrimary,
            )
            Text(text = subtitle, fontSize = 12.sp, color = TextTertiary)
        }
    }
}