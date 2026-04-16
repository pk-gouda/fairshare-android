package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.theme.AvatarColors
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

/**
 * Group Settings Screen — two states:
 *  • Admin  — edit pencil in top bar, Add member button, Archive + Leave + Delete in danger zone
 *  • Member — no edit pencil, no Add button, only Leave in danger zone
 *
 * Both states show: group header, balance card, members list, danger zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    onBack               : () -> Unit,
    onNavigateToAddMember: (String) -> Unit,
    onGroupDeleted       : () -> Unit,
    onNavigateToSettleUp : (groupId: String) -> Unit = {},
    onNavigateToMembers  : (String) -> Unit = {},
    onNavigateToAnalytics: (String) -> Unit = {},
    onNavigateToRecurring: (String) -> Unit = {},
    onNavigateToReminders: (String) -> Unit = {},
    viewModel            : GroupSettingsViewModel = hiltViewModel(),
) {
    val group            by viewModel.group.collectAsState()
    val members          by viewModel.members.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val actionState      by viewModel.actionState.collectAsState()
    val friends          by viewModel.friends.collectAsState()
    val claimState       by viewModel.claimState.collectAsState()
    val yourGroupBalance: Double? by viewModel.yourGroupBalance.collectAsState()
    val editName         by viewModel.editName.collectAsState()
    val simplifyDebts    by viewModel.simplifyDebts.collectAsState()
    val muteNotifications by viewModel.muteNotifications.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val clipboard    = LocalClipboardManager.current

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showRemoveDialog  by remember { mutableStateOf<GroupMember?>(null) }
    var showNameDialog    by remember { mutableStateOf(false) }
    var showLeaveDialog   by remember { mutableStateOf(false) }
    var showArchiveDialog   by remember { mutableStateOf(false) }
    var showUnarchiveDialog by remember { mutableStateOf(false) }
    var showAssignSheet   by remember { mutableStateOf<GroupMember?>(null) }

    val isAdmin = group?.createdById == viewModel.currentUserId

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is GroupSettingsActionState.Success     -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is GroupSettingsActionState.Error       -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is GroupSettingsActionState.GroupDeleted -> onGroupDeleted()
            else -> Unit
        }
    }

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
            title = { Text("Delete group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("This will permanently delete the group and all its expenses. This cannot be undone.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteGroup() }) {
                    Text("Delete", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    showRemoveDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            containerColor   = Surface2,
            title = { Text("Remove ${member.fullName}?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("They will be removed from this group. You must settle balances first.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.removeMember(member.userId); showRemoveDialog = null }) {
                    Text("Remove", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor   = Surface2,
            title = { Text("Leave group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("You will be removed from this group. Settle all balances before leaving.", color = TextSecondary, fontSize = 14.sp) },
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

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            containerColor   = Surface2,
            title = { Text("Archive group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("The group will be hidden from your active list. Expenses and balances are preserved.", color = TextSecondary, fontSize = 14.sp) },
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

    if (showUnarchiveDialog) {
        AlertDialog(
            onDismissRequest = { showUnarchiveDialog = false },
            containerColor   = Surface2,
            title = { Text("Restore group?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("This group will reappear in your active groups list.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showUnarchiveDialog = false; viewModel.unarchiveGroup() }) {
                    Text("Restore", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Edit group name dialog ────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor   = Surface2,
            title = { Text("Edit group name", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                com.prathik.fairshare.ui.components.FsTextField(
                    value         = editName,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label         = "Group name",
                    modifier      = androidx.compose.ui.Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        showNameDialog = false
                        viewModel.saveGroupName()
                    }
                }) {
                    Text("Save", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Assign sheet ──────────────────────────────────────────────────────────
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
                val realMemberIds = members.filter { !it.email.startsWith("placeholder+") }.map { it.userId }.toSet()
                val assignable    = friends.filter { it.id !in realMemberIds }
                if (assignable.isNotEmpty()) {
                    assignable.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAssignSheet = null; viewModel.assignMember(member.userId, friend.id) }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FsAvatar(name = friend.fullName, userId = friend.id, size = ComponentSize.avatarMd)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(friend.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showAssignSheet = null }.padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) { Text("Skip for now", fontSize = 14.sp, color = TextTertiary) }
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            com.prathik.fairshare.ui.components.FsTopBar(
                title  = "Group Settings",
                onBack = onBack,
                actions = if (isAdmin) {
                    {
                        androidx.compose.material3.IconButton(onClick = { showNameDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextSecondary)
                        }
                    }
                } else null,
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
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Group header ──────────────────────────────────────────────────
            group?.let { g ->
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        // Emoji tile — tappable for admin (change photo)
                        Box(modifier = Modifier.size(56.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Surface2)
                                    .border(1.dp, Surface4, RoundedCornerShape(16.dp))
                                    .then(if (isAdmin) Modifier.clickable { showNameDialog = true } else Modifier),
                            ) {
                                Text(coverEmoji(g.type), fontSize = 26.sp)
                            }
                            if (isAdmin) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Green400)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    Text("+", fontSize = 11.sp, color = Surface0, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                            Text(
                                text       = g.name,
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Type badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.sm))
                                        .background(Green400.copy(alpha = 0.1f))
                                        .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.sm))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(g.type.name, fontSize = 10.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                                }
                                Text("${g.memberCount} members", fontSize = 12.sp, color = TextTertiary)
                                Text("·", fontSize = 12.sp, color = TextTertiary)
                                Text(g.createdAt.toShortDate(), fontSize = 12.sp, color = TextTertiary)
                            }
                        }
                    }

                    // Balance card — state-aware
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Surface2)
                            .border(1.dp, Surface4, RoundedCornerShape(Radius.xl))
                            .padding(horizontal = Spacing.md, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val balance = yourGroupBalance   // local snapshot → enables smart cast
                        Column {
                            Text("Your balance", fontSize = 11.sp, color = TextTertiary)
                            when {
                                balance == null -> Text(
                                    "No expenses yet",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
                                )
                                balance < 0 -> Text(
                                    "You owe ${com.prathik.fairshare.util.MoneyUtils.format(-balance, "USD")}",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Negative,
                                )
                                balance > 0 -> Text(
                                    "You are owed ${com.prathik.fairshare.util.MoneyUtils.format(balance, "USD")}",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Green400,
                                )
                                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✓ ", fontSize = 14.sp, color = Green400)
                                    Text("Settled up", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                }
                            }
                        }
                        // CTA — only when balance exists
                        when {
                            balance != null && balance < 0 -> Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .background(Green400)
                                    .clickable { onNavigateToSettleUp(viewModel.groupId) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("Settle up", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Surface0)
                            }
                            balance != null && balance > 0 -> Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .background(Surface2)
                                    .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                                    .clickable { /* remind */ }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("Remind", fontSize = 13.sp, color = TextSecondary)
                            }
                            else -> {}
                        }
                    }
                }
            }

            SectionDivider()

            // ── Preferences ───────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text(
                    text          = "Preferences",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(bottom = Spacing.sm),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2)
                        .border(1.dp, Surface4, RoundedCornerShape(Radius.xl)),
                ) {
                    // ── Simplify debts toggle ─────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAdmin) {
                                val newValue = !simplifyDebts
                                viewModel.onSimplifyDebtsToggled()
                                viewModel.saveSimplifyDebts(newValue)
                            }
                            .padding(horizontal = Spacing.md, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
                            Text("Simplify debts", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                "Reduce the number of payments needed to settle all debts",
                                fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp,
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked         = simplifyDebts,
                            onCheckedChange = if (isAdmin) { value ->
                                viewModel.onSimplifyDebtsToggled()
                                viewModel.saveSimplifyDebts(value)
                            } else null,
                            enabled = isAdmin,
                            colors  = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor  = Surface0,
                                checkedTrackColor  = Green400,
                            ),
                        )
                    }

                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)

                    // ── Mute notifications toggle ─────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onMuteNotificationsToggled() }
                            .padding(horizontal = Spacing.md, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
                            Text("Mute notifications", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                "Stop receiving notifications for this group",
                                fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp,
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked         = muteNotifications,
                            onCheckedChange = { viewModel.onMuteNotificationsToggled() },
                            colors          = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor  = Surface0,
                                checkedTrackColor  = Green400,
                            ),
                        )
                    }

                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)

                    // ── Default currency ──────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: open currency picker */ }
                            .padding(horizontal = Spacing.md, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default currency", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                "Currency used for new expenses in this group",
                                fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text       = "USD", // TODO: add defaultCurrency to Group model
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextSecondary,
                            )
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint               = TextTertiary,
                                modifier           = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            SectionDivider()

            // ── Members ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Members", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    // Add button — admin only
                    if (isAdmin) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .clip(RoundedCornerShape(Radius.md))
                                .background(Green400.copy(alpha = 0.1f))
                                .border(1.dp, Green400, RoundedCornerShape(Radius.md))
                                .clickable { onNavigateToAddMember(viewModel.groupId) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("+", fontSize = 14.sp, color = Green400, fontWeight = FontWeight.Bold)
                                Text("Add", fontSize = 12.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Fix 4: Banner when all other members are pending
                val pendingCount  = members.count { it.email.startsWith("placeholder+") || it.userId == viewModel.currentUserId }
                val showBanner    = members.size > 1 && pendingCount >= members.size - 1
                if (showBanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text("ℹ️", fontSize = 13.sp)
                        Text(
                            "Pending members will see expenses when they join FairShare",
                            fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2)
                        .border(1.dp, Surface4, RoundedCornerShape(Radius.xl)),
                ) {
                    members.forEachIndexed { index, member ->
                        val isMe          = member.userId == viewModel.currentUserId
                        val isCreator     = group?.createdById == member.userId
                        val isPlaceholder = member.email.startsWith("placeholder+")
                        val isPending     = member.email.contains("@invited") ||
                                (!isPlaceholder && member.email.isNotBlank() && isCreator.not() && !member.email.contains("@fairshare"))

                        MemberRow(
                            member       = member,
                            isMe         = isMe,
                            isAdmin      = isCreator,
                            isPlaceholder = isPlaceholder,
                            showControls = isAdmin && !isMe,
                            onAssign     = if (isPlaceholder && isAdmin) { { showAssignSheet = member } } else null,
                            onRemove     = if (!isPlaceholder && !isMe && isAdmin) { { showRemoveDialog = member } } else null,
                        )
                        if (index < members.lastIndex) {
                            HorizontalDivider(
                                color    = Surface3,
                                thickness = 0.5.dp,
                                modifier  = Modifier.padding(start = 56.dp),
                            )
                        }
                    }
                }
            }

            SectionDivider()

            // ── Danger zone ───────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text(
                    text          = "Danger Zone",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = Negative,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(bottom = Spacing.sm),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2)
                        .border(1.dp, Negative.copy(alpha = 0.3f), RoundedCornerShape(Radius.xl)),
                ) {
                    if (isAdmin) {
                        val isArchived = group?.isArchived == true
                        if (isArchived) {
                            DangerRow(
                                icon     = Icons.Outlined.Archive,
                                iconTint = Green400,
                                label    = "Restore group",
                                subtitle = "Move back to your active list",
                                labelColor = Green400,
                                onClick  = { showUnarchiveDialog = true },
                            )
                        } else {
                            DangerRow(
                                icon     = Icons.Outlined.Archive,
                                iconTint = TextSecondary,
                                label    = "Archive group",
                                subtitle = "Hide from main list",
                                onClick  = { showArchiveDialog = true },
                            )
                        }
                        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                    }
                    DangerRow(
                        icon     = Icons.Outlined.ExitToApp,
                        iconTint = Negative,
                        label    = "Leave group",
                        subtitle = "You won't see expenses",
                        labelColor = Negative,
                        onClick  = { showLeaveDialog = true },
                    )
                    if (isAdmin) {
                        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                        DangerRow(
                            icon     = Icons.Outlined.Delete,
                            iconTint = Negative,
                            label    = "Delete group",
                            subtitle = "Permanent. Cannot be undone",
                            labelColor = Negative,
                            onClick  = { showDeleteDialog = true },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Section divider ───────────────────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Surface0)
            .border(0.5.dp, Surface4, RoundedCornerShape(0.dp)),
    )
}

// ── Member row ────────────────────────────────────────────────────────────────

@Composable
private fun MemberRow(
    member       : GroupMember,
    isMe         : Boolean,
    isAdmin      : Boolean,
    isPlaceholder: Boolean,
    showControls : Boolean,
    onAssign     : (() -> Unit)?,
    onRemove     : (() -> Unit)?,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        val avatarColor = AvatarColors[Math.abs(member.userId.hashCode()) % AvatarColors.size]
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor),
        ) {
            Text(
                text       = member.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = Surface0,
            )
        }

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
                            .background(Green400.copy(alpha = 0.1f))
                            .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text("YOU", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.Bold)
                    }
                }
                if (isPlaceholder) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Negative.copy(alpha = 0.1f))
                            .border(1.dp, Negative.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text("PENDING", fontSize = 9.sp, color = Negative, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text     = if (isPlaceholder) "Invited ${member.joinedAt.toRelativeDate()}" else member.email,
                fontSize = 11.sp,
                color    = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Right side badge/action
        when {
            isAdmin && !isMe -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Green400.copy(alpha = 0.1f))
                        .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.sm))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("ADMIN", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.Bold)
                }
            }
            isAdmin && isMe -> {
                // Show both YOU badge (already inline) and ADMIN badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Green400.copy(alpha = 0.1f))
                        .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.sm))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("ADMIN", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.Bold)
                }
            }
            onAssign != null -> {
                // Placeholder — 44dp tap target
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Surface4)
                        .clickable { onAssign() },
                ) {
                    Text("🔗", fontSize = 14.sp)
                }
            }
            onRemove != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Surface4)
                        .clickable { onRemove() },
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Danger row ────────────────────────────────────────────────────────────────

@Composable
private fun DangerRow(
    icon      : ImageVector,
    iconTint  : Color,
    label     : String,
    subtitle  : String,
    labelColor: Color = TextPrimary,
    onClick   : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(iconTint.copy(alpha = 0.1f)),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = labelColor)
            Text(subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text("›", fontSize = 20.sp, color = TextTertiary)
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

private fun String.toRelativeDate(): String {
    return try {
        val dt   = java.time.LocalDateTime.parse(this, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val days = java.time.temporal.ChronoUnit.DAYS.between(dt.toLocalDate(), java.time.LocalDate.now())
        when {
            days == 0L -> "today"
            days == 1L -> "1d ago"
            days < 7   -> "${days}d ago"
            days < 30  -> "${days / 7}w ago"
            else       -> dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) { "" }
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