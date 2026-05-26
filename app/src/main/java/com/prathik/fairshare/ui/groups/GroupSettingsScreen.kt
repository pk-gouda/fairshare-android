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
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.prathik.fairshare.ui.components.FsPrimaryButton
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
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
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
    onNavigateToInvite   : (groupId: String) -> Unit = {},
    defaultCurrency      : String = "USD",
    onNavigateToCurrency : (currentCurrency: String) -> Unit = {},
    viewModel            : GroupSettingsViewModel = hiltViewModel(),
) {
    val group            by viewModel.group.collectAsState()
    val members          by viewModel.members.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val groupLoadFailed  by viewModel.groupLoadFailed.collectAsState()
    val actionState      by viewModel.actionState.collectAsState()
    val friends          by viewModel.friends.collectAsState()
    val friendsLoaded    by viewModel.friendsLoaded.collectAsState()
    val claimState       by viewModel.claimState.collectAsState()
    val yourGroupBalances by viewModel.yourGroupBalances.collectAsState()
    val editName         by viewModel.editName.collectAsState()
    val editDescription  by viewModel.editDescription.collectAsState()
    val simplifyDebts    by viewModel.simplifyDebts.collectAsState()
    val muteNotifications by viewModel.muteNotifications.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val clipboard    = LocalClipboardManager.current

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var deleteConfirmText  by remember { mutableStateOf("") }
    var showRemoveDialog  by remember { mutableStateOf<GroupMember?>(null) }
    var showNameDialog    by remember { mutableStateOf(false) }
    var showLeaveDialog   by remember { mutableStateOf(false) }
    var showArchiveDialog   by remember { mutableStateOf(false) }
    var showUnarchiveDialog by remember { mutableStateOf(false) }
    var showAssignSheet      by remember { mutableStateOf<GroupMember?>(null) }
    var pendingAssignMember  by remember { mutableStateOf<GroupMember?>(null) }
    var pendingAssignFriendId by remember { mutableStateOf<String?>(null) }
    var showAssignConfirmDialog by remember { mutableStateOf(false) }

    val isCreator = group?.createdById == viewModel.currentUserId
    val isMember = group != null  // any member can perform all group actions
    val isAdmin = isCreator  // kept for backward compat — only used for restore

    val lifecycleOwner = LocalLifecycleOwner.current
    // GroupSettings loads once on init — no live refresh needed.
    // Refreshing on every resume caused race conditions with saveSimplifyDebts:
    // loadData() would overwrite the optimistic toggle state mid-flight.

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

    // ── Linking overlay — shown while placeholder assignment is in progress ──
    if (claimState is ClaimActionState.Loading) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                .then(Modifier), // absorb all touches
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color    = Green400,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text      = "Linking member…",
                    fontSize  = 15.sp,
                    color     = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text      = "Updating balances across all expenses",
                    fontSize  = 12.sp,
                    color     = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────


    if (showAssignConfirmDialog && pendingAssignMember != null) {
        val ownerName = group?.createdByName ?: "the group owner"
        val memberName = pendingAssignMember!!.fullName
        AlertDialog(
            onDismissRequest = {
                showAssignConfirmDialog = false
                pendingAssignMember = null
                pendingAssignFriendId = null
            },
            containerColor = Surface2,
            title = {
                Text(
                    "This cannot be undone",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    "Linking $memberName to a FairShare account is permanent and can only be reversed by $ownerName.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                FsPrimaryButton(
                    text = "Link account",
                    onClick = {
                        pendingAssignMember?.let { m ->
                            pendingAssignFriendId?.let { fid ->
                                viewModel.assignMember(m.userId, fid)
                            }
                        }
                        showAssignConfirmDialog = false
                        pendingAssignMember = null
                        pendingAssignFriendId = null
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showAssignConfirmDialog = false
                    pendingAssignMember = null
                    pendingAssignFriendId = null
                }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showDeleteDialog) {
        val groupName = group?.name ?: ""
        val nameMatches = deleteConfirmText == groupName

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmText = ""
            },
            containerColor = Surface2,
            title = {
                Text(
                    "Delete group?",
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column {
                    Text(
                        "This will delete the group for ALL members. " +
                                "Any member can restore it within 60 days from the Activity tab. " +
                                "Outstanding balances will be cleared.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Type the group name to confirm:",
                        color    = TextSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        placeholder   = { Text(groupName, color = TextTertiary) },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError       = deleteConfirmText.isNotEmpty() && !nameMatches,
                        supportingText = if (deleteConfirmText.isNotEmpty() && !nameMatches) {
                            { Text("Name doesn't match", color = Negative, fontSize = 12.sp) }
                        } else null,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nameMatches,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGroup(deleteConfirmText)
                        deleteConfirmText = ""
                    },
                ) {
                    Text(
                        "Delete",
                        color      = if (nameMatches) Negative else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteConfirmText = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
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
        // Refresh friends every time the sheet opens to avoid stale empty list
        androidx.compose.runtime.LaunchedEffect(member) {
            viewModel.refreshFriends()
        }
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
                if (!friendsLoaded && friends.isEmpty()) {
                    // Friends still loading — show skeleton rows
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { FsSkeletonTimelineRow() }
                    }
                } else if (assignable.isNotEmpty()) {
                    assignable.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingAssignMember = member
                                    pendingAssignFriendId = friend.id
                                    showAssignSheet = null
                                    showAssignConfirmDialog = true
                                }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FsAvatar(name = friend.fullName, userId = friend.id, imageUrl = friend.profilePictureUrl, size = ComponentSize.avatarMd)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(friend.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                } else {
                    // Friends loaded but all are already in the group
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No friends to assign — add them to FairShare first",
                            fontSize = 13.sp, color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        )
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
                actions = if (isCreator) {
                    {
                        androidx.compose.material3.IconButton(onClick = { showNameDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextSecondary)
                        }
                    }
                } else null,
            )
        },
    ) { innerPadding ->

        if (groupLoadFailed && group == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Couldn't load group settings", fontSize = 15.sp,
                        color = com.prathik.fairshare.ui.theme.TextPrimary)
                    Text("Check your connection and try again", fontSize = 13.sp,
                        color = com.prathik.fairshare.ui.theme.TextTertiary)
                    androidx.compose.material3.TextButton(onClick = { viewModel.loadData() }) {
                        Text("Retry", color = com.prathik.fairshare.ui.theme.Green400, fontSize = 14.sp)
                    }
                }
            }
            return@Scaffold
        }
        if (isLoading && group == null) {
            GroupSettingsSkeleton()
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
                        // Emoji tile — shows groupImage if set, otherwise group type emoji
                        Box(modifier = Modifier.size(56.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Surface2)
                                    .border(1.dp, Surface4, RoundedCornerShape(16.dp))
                                    .then(if (isMember) Modifier.clickable { showNameDialog = true } else Modifier),
                            ) {
                                if (!g.groupImage.isNullOrBlank()) {
                                    var imageLoadFailed by remember(g.groupImage) { mutableStateOf(false) }
                                    if (!imageLoadFailed) {
                                        coil.compose.AsyncImage(
                                            model              = g.groupImage,
                                            contentDescription = g.name,
                                            contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                            onError            = { imageLoadFailed = true },
                                            modifier           = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(coverEmoji(g.type), fontSize = 26.sp)
                                    }
                                } else {
                                    Text(coverEmoji(g.type), fontSize = 26.sp)
                                }
                            }
                            if (isMember) {
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
                        val netByCurrency = yourGroupBalances.groupBy { it.currency }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                            .filter { it.value != 0.0 }
                        val netTotal = netByCurrency.values.sumOf { it }
                        Column {
                            Text("Your balance", fontSize = 11.sp, color = TextTertiary)
                            when {
                                yourGroupBalances.isEmpty() -> Text(
                                    "No expenses yet",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
                                )
                                netTotal < 0 && netByCurrency.any { it.value > 0 } -> {
                                    // Mixed: you owe in one currency, owed in another
                                    val oweText  = netByCurrency.filter { it.value < 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(-a,c) }
                                    val owedText = netByCurrency.filter { it.value > 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(a,c) }
                                    Text("You owe $oweText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Negative)
                                    Text("You are owed $owedText", fontSize = 12.sp, color = Green400)
                                }
                                netTotal < 0 -> {
                                    val oweText = netByCurrency.filter { it.value < 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(-a,c) }
                                    Text("You owe $oweText",
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Negative)
                                }
                                netTotal > 0 && netByCurrency.any { it.value < 0 } -> {
                                    val owedText = netByCurrency.filter { it.value > 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(a,c) }
                                    val oweText  = netByCurrency.filter { it.value < 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(-a,c) }
                                    Text("You are owed $owedText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Green400)
                                    Text("You owe $oweText", fontSize = 12.sp, color = Negative)
                                }
                                netTotal > 0 -> {
                                    val owedText = netByCurrency.filter { it.value > 0 }.entries
                                        .joinToString(" + ") { (c,a) -> com.prathik.fairshare.util.MoneyUtils.format(a,c) }
                                    Text("You are owed $owedText",
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Green400)
                                }
                                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✓ ", fontSize = 14.sp, color = Green400)
                                    Text("Settled up", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                }
                            }
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
                    // ── Group notes ───────────────────────────────────────────
                    if (isMember) {
                        OutlinedTextField(
                            value         = editDescription,
                            onValueChange = { if (it.length <= 100) viewModel.onDescriptionChanged(it) },
                            placeholder   = { Text("Group notes (optional)…", fontSize = 14.sp, color = TextTertiary) },
                            label         = { Text("Group notes", fontSize = 12.sp) },
                            maxLines      = 4,
                            modifier      = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor    = Green400,
                                unfocusedBorderColor  = Surface4,
                                focusedLabelColor     = Green400,
                                unfocusedLabelColor   = TextTertiary,
                                focusedTextColor      = TextPrimary,
                                unfocusedTextColor    = TextPrimary,
                                focusedContainerColor = Surface2,
                                unfocusedContainerColor = Surface2,
                            ),
                        )
                        val descChanged = editDescription.trim() != (group?.groupNotes ?: "").trim()
                        if (descChanged) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, bottom = Spacing.sm),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text     = "Save notes",
                                    fontSize = 13.sp,
                                    color    = Green400,
                                    modifier = Modifier.clickable { viewModel.saveDescription() },
                                )
                            }
                        }
                        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                    }

                    // ── Simplify debts toggle ─────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isMember) {
                                viewModel.saveSimplifyDebts(!simplifyDebts)
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
                            onCheckedChange = if (isMember) { value ->
                                viewModel.saveSimplifyDebts(value)
                            } else null,
                            enabled = isMember,
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
                            .clickable { onNavigateToCurrency(defaultCurrency) }
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
                                text       = defaultCurrency,
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

            // ── Recurring & Reminders ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .border(1.dp, Surface3, RoundedCornerShape(Radius.xl)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRecurring(viewModel.groupId) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text("Recurring payments", fontSize = 14.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Scheduled repeating expenses", fontSize = 12.sp,
                                color = TextTertiary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReminders(viewModel.groupId) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text("Reminders", fontSize = 14.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Settle-up reminders for this group", fontSize = 12.sp,
                                color = TextTertiary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = TextTertiary, modifier = Modifier.size(16.dp))
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
                    // Add button — any member
                    if (isMember) {
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
                            isCreator    = isCreator,
                            isPlaceholder = isPlaceholder,
                            showControls = !isMe,
                            onAssign     = if (isPlaceholder) { { showAssignSheet = member } } else null,
                            onRemove     = if (!isPlaceholder && !isMe) { { showRemoveDialog = member } } else null,
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

            // ── Invite ────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text(
                    text          = "Invite",
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
                    group?.id?.let { gId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToInvite(gId) }
                                .padding(horizontal = Spacing.md, vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Link,
                                contentDescription = null,
                                tint               = Green400,
                                modifier           = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Invite via link", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("Share a QR code or link to invite members", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text("›", fontSize = 18.sp, color = TextTertiary)
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
                    if (isMember) {
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
                    if (isMember) {
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
    isCreator    : Boolean,
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

        // Right side badge/action — CREATOR badge replaces ADMIN
        when {
            isCreator && !isMe -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Green400.copy(alpha = 0.1f))
                        .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.sm))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("CREATOR", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.Bold)
                }
            }
            isCreator && isMe -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Green400.copy(alpha = 0.1f))
                        .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.sm))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("CREATOR", fontSize = 9.sp, color = Green400, fontWeight = FontWeight.Bold)
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
// ── GroupSettings skeleton placeholder ───────────────────────────────────────

@Composable
private fun GroupSettingsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Group avatar + name placeholder
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FsSkeletonBlock(height = 56.dp, widthFraction = 0.16f, cornerRadius = 28.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FsSkeletonBlock(height = 16.dp, widthFraction = 0.5f)
                FsSkeletonBlock(height = 12.dp, widthFraction = 0.3f)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Settings card placeholders
        repeat(3) {
            FsSkeletonBlock(height = 52.dp, widthFraction = 1f, cornerRadius = 10.dp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Members section placeholder
        FsSkeletonBlock(height = 14.dp, widthFraction = 0.25f, cornerRadius = 4.dp)
        repeat(3) { FsSkeletonTimelineRow() }
    }
}