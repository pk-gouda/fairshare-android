package com.prathik.fairshare.ui.groups

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Group Settings Screen.
 *
 * Sections:
 * - Group avatar + name + type + created date
 * - Members list with remove option, + Add member
 * - Invite link with Copy / Share / Reset
 * - Preferences: simplify debts toggle, mute notifications
 * - Tools: Spending analytics, Recurring expenses, Reminders (placeholders)
 * - Danger zone: Archive, Leave group, Delete group
 * - Group ID footer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    onBack              : () -> Unit,
    onNavigateToAddMember: (String) -> Unit,
    onGroupDeleted      : () -> Unit,
    viewModel           : GroupSettingsViewModel = hiltViewModel(),
) {
    val group             by viewModel.group.collectAsState()
    val members           by viewModel.members.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val actionState       by viewModel.actionState.collectAsState()
    val simplifyDebts     by viewModel.simplifyDebts.collectAsState()
    val muteNotifications by viewModel.muteNotifications.collectAsState()
    val friends           by viewModel.friends.collectAsState()
    val claimState        by viewModel.claimState.collectAsState()

    val snackbarHost  = remember { SnackbarHostState() }
    val clipboard     = LocalClipboardManager.current

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showRemoveDialog  by remember { mutableStateOf<GroupMember?>(null) }
    var showNameDialog    by remember { mutableStateOf(false) }
    var showLeaveDialog   by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showAssignSheet   by remember { mutableStateOf<GroupMember?>(null) }

    // Auto-refresh when screen resumes (e.g. returning from AddMember)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    // Handle action states
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is GroupSettingsActionState.Success     -> {
                snackbarHost.showSnackbar(state.message)
                viewModel.resetActionState()
            }
            is GroupSettingsActionState.Error       -> {
                snackbarHost.showSnackbar(state.message)
                viewModel.resetActionState()
            }
            is GroupSettingsActionState.GroupDeleted -> onGroupDeleted()
            else -> Unit
        }
    }

    // Handle claim states
    LaunchedEffect(claimState) {
        when (val s = claimState) {
            is ClaimActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetClaimState() }
            is ClaimActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetClaimState() }
            else -> Unit
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Surface2,
            title = {
                Text("Delete group?", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "This will permanently delete the group and all its expenses. This cannot be undone.",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteGroup() }) {
                    Text("Delete", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    showRemoveDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            containerColor   = Surface2,
            title = {
                Text("Remove ${member.fullName}?", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "They will be removed from the group. This cannot be undone if they have unsettled balances.",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.removeMember(member.userId); showRemoveDialog = null }) {
                    Text("Remove", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = {
            FsTopBar(
                title  = "Settings",
                onBack = onBack,
                actions = {
                    FsIconButton(
                        icon               = Icons.Outlined.Edit,
                        contentDescription = "Edit group name",
                        onClick            = { showNameDialog = true },
                        tint               = TextSecondary,
                    )
                }
            )
        },
    ) { innerPadding ->

        if (isLoading && group == null) {
            FsLoadingScreen()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {

            // ── Group avatar + name ───────────────────────────────────────────
            group?.let { g ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                ) {
                    // Cover thumbnail with + badge
                    Box(modifier = Modifier.size(72.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(coverGradientColors(g.type))),
                        ) {
                            Text(text = coverEmoji(g.type), fontSize = 32.sp)
                        }
                        // + badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Green400)
                                .align(Alignment.BottomEnd),
                        ) {
                            Text(text = "+", fontSize = 14.sp, color = Surface0, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Text(
                        text       = g.name,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.sm))
                                .background(Color(0xFF1A3A1A))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text       = g.type.name,
                                fontSize   = 10.sp,
                                color      = Green400,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text     = "${g.memberCount} members · Created ${g.createdAt.toShortDate()}",
                            fontSize = 11.sp,
                            color    = TextTertiary,
                        )
                    }
                }
            }

            // ── Members ───────────────────────────────────────────────────────
            SectionLabel("MEMBERS")
            SettingsCard {
                members.forEachIndexed { index, member ->
                    val isMe          = member.userId == viewModel.currentUserId
                    val isLast        = index == members.lastIndex
                    val isPlaceholder = member.email.startsWith("placeholder+") &&
                            (member.email.contains("@fairshare.import") ||
                                    member.email.contains("@fairshare.local"))
                    MemberRow(
                        member    = member,
                        isMe      = isMe,
                        isCreator = group?.createdById == member.userId,
                        onAssign  = if (isPlaceholder && !isMe) { { showAssignSheet = member } } else null,
                        onRemove  = if (!isPlaceholder && !isMe) { { showRemoveDialog = member } } else null,
                    )
                    if (!isLast) HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }

                // Add member row
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAddMember(viewModel.groupId) }
                        .padding(vertical = Spacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(text = "+ ", fontSize = 14.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                    Text(text = "Add member", fontSize = 14.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Invite link ───────────────────────────────────────────────────
            SectionLabel("INVITE LINK")
            SettingsCard {
                val inviteCode = group?.inviteCode ?: ""
                val inviteLink = "fairshare.app/join/$inviteCode"

                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = inviteLink,
                        fontSize = 12.sp,
                        color    = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Green400)
                            .clickable { clipboard.setText(AnnotatedString(inviteLink)) }
                            .padding(horizontal = Spacing.sm, vertical = 4.dp),
                    ) {
                        Text(text = "Copy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Surface0)
                    }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Surface4)
                            .clickable { /* TODO: share */ }
                            .padding(horizontal = Spacing.sm, vertical = 4.dp),
                    ) {
                        Text(text = "Share", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                HorizontalDivider(color = Surface3, thickness = 0.5.dp)

                // Reset link
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: reset invite link API */ }
                        .padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "↺  Reset invite link", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "Invalidates old link", fontSize = 11.sp, color = TextTertiary)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Preferences ───────────────────────────────────────────────────
            SectionLabel("PREFERENCES")
            SettingsCard {
                ToggleRow(
                    label    = "Simplify debts",
                    subtitle = "Reduces total payments between members",
                    checked  = simplifyDebts,
                    onToggle = {
                        viewModel.onSimplifyDebtsToggled()
                        viewModel.saveSimplifyDebts(!simplifyDebts)
                    },
                )
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                ToggleRow(
                    label    = "Mute notifications",
                    subtitle = null,
                    checked  = muteNotifications,
                    onToggle = { viewModel.onMuteNotificationsToggled() },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Tools ─────────────────────────────────────────────────────────
            SectionLabel("TOOLS")
            SettingsCard {
                NavRow(label = "Spending analytics", onClick = { /* TODO Day 17 */ })
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                NavRow(label = "Recurring expenses", badge = null, onClick = { /* TODO Day 18 */ })
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                NavRow(label = "Reminders", onClick = { /* TODO Day 18 */ })
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Danger zone ───────────────────────────────────────────────────
            SectionLabel("DANGER ZONE")
            SettingsCard {
                NavRow(label = "Archive group", onClick = { showArchiveDialog = true })
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                NavRow(
                    label     = "Leave group",
                    textColor = Negative,
                    onClick   = { showLeaveDialog = true },
                )
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                NavRow(
                    label     = "Delete group",
                    textColor = Color(0xFFFF4444),
                    onClick   = { showDeleteDialog = true },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Footer ────────────────────────────────────────────────────────
            Text(
                text     = "Group ID: ${group?.inviteCode?.take(8) ?: ""} · v1.0.0",
                fontSize = 10.sp,
                color    = TextTertiary.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }

    // ── Leave group dialog ────────────────────────────────────────────────────
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor   = Surface2,
            title = { Text("Leave group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    "You will be removed from this group. You must settle all balances before leaving.",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; viewModel.leaveGroup() }) {
                    Text("Leave", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Archive group dialog ──────────────────────────────────────────────────
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            containerColor   = Surface2,
            title = { Text("Archive group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    "The group will be hidden from your active groups list. Expenses and balances are preserved.",
                    color = TextSecondary, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showArchiveDialog = false; viewModel.archiveGroup() }) {
                    Text("Archive", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Assign / Claim sheet for PLACEHOLDER members ──────────────────────────
    showAssignSheet?.let { member ->
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAssignSheet = null },
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Who is ${member.fullName}?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                Text(
                    text     = "Link this placeholder to a real FairShare account.",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)

                // "That's me"
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { showAssignSheet = null; viewModel.claimMember(member.userId) }
                        .padding(horizontal = Spacing.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(ComponentSize.avatarMd)
                            .clip(CircleShape)
                            .background(Green400.copy(alpha = 0.15f)),
                    ) { Text("👤", fontSize = 18.sp) }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("That's me", fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = Green400)
                        Text("Claim this as your identity", fontSize = 12.sp, color = TextTertiary)
                    }
                }
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)

                // Friends list
                if (friends.isNotEmpty()) {
                    Text("Or assign to a friend:",
                        fontSize = 12.sp, color = TextTertiary,
                        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg,
                            top = Spacing.md, bottom = Spacing.sm))
                    friends.forEach { friend ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAssignSheet = null
                                    viewModel.assignMember(member.userId, friend.id)
                                }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FsAvatar(name = friend.fullName, userId = friend.id,
                                size = ComponentSize.avatarMd)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(friend.fullName, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary,
                                modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }

                // Skip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAssignSheet = null }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) { Text("Skip for now", fontSize = 14.sp, color = TextTertiary) }
            }
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextTertiary,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(start = 2.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(horizontal = Spacing.md),
    ) {
        content()
    }
}

@Composable
private fun MemberRow(
    member   : GroupMember,
    isMe     : Boolean,
    isCreator: Boolean,
    onAssign : (() -> Unit)?,
    onRemove : (() -> Unit)?,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsAvatar(
            name   = member.fullName,
            userId = member.userId,
            size   = ComponentSize.avatarMd,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = member.fullName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A3A1A))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(text = "you", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                text     = member.email,
                fontSize = 11.sp,
                color    = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCreator) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(Surface4)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(text = "admin", fontSize = 10.sp, color = TextSecondary)
            }
        } else if (onAssign != null) {
            // PLACEHOLDER — show link icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Green400.copy(alpha = 0.12f))
                    .clickable { onAssign() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🔗", fontSize = 14.sp)
            }
        } else if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⋮", fontSize = 18.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label   : String,
    subtitle: String?,
    checked : Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = TextTertiary)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor       = Color.White,
                checkedTrackColor       = Green400,
                uncheckedThumbColor     = TextTertiary,
                uncheckedTrackColor     = Surface4,
                uncheckedBorderColor    = Surface4,
            ),
        )
    }
}

@Composable
private fun NavRow(
    label    : String,
    badge    : String? = null,
    textColor: Color   = TextSecondary,
    onClick  : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text      = label,
            fontSize  = 14.sp,
            color     = textColor,
            modifier  = Modifier.weight(1f),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Surface4)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(text = badge, fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toShortDate(): String {
    return try {
        val dt = java.time.LocalDateTime.parse(this, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        try {
            val dt = java.time.LocalDate.parse(this.take(10))
            dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e2: Exception) { this }
    }
}

private fun coverGradientColors(type: GroupType): List<Color> = when (type) {
    GroupType.TRIP      -> listOf(Color(0xFF0D2137), Color(0xFF1A3A5C), Color(0xFF0D4A6B))
    GroupType.HOME      -> listOf(Color(0xFF1A0D2E), Color(0xFF2E1A4A), Color(0xFF3D1A5C))
    GroupType.OFFICE    -> listOf(Color(0xFF1A1000), Color(0xFF2E1E00), Color(0xFF3D2800))
    GroupType.FRIENDS   -> listOf(Color(0xFF0D1F0D), Color(0xFF1A3020), Color(0xFF1A3D1A))
    GroupType.COUPLE    -> listOf(Color(0xFF1F0D1A), Color(0xFF2E1022), Color(0xFF3D1A2E))
    GroupType.EVENT     -> listOf(Color(0xFF1A1000), Color(0xFF2A1800), Color(0xFF3A2200))
    GroupType.APARTMENT -> listOf(Color(0xFF0D1A1F), Color(0xFF0D2A30), Color(0xFF0A2D35))
    GroupType.OTHER     -> listOf(Color(0xFF111112), Color(0xFF1A1A1C), Color(0xFF222222))
}

private fun coverEmoji(type: GroupType): String = when (type) {
    GroupType.TRIP      -> "✈️"
    GroupType.HOME      -> "🏠"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👫"
    GroupType.COUPLE    -> "💑"
    GroupType.EVENT     -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER     -> "💰"
}