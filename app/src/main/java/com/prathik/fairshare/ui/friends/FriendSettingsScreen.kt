package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.AvatarColors
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
import com.prathik.fairshare.util.MoneyUtils

/**
 * Friend Settings Screen — three states:
 *  • Accepted    — balance in header, Settle up CTA, real shared groups, remove/block/report
 *  • Invited     — "Pending" status, orange invite-pending card, enabled groups section, cancel/remove
 *  • Placeholder — "Offline" status, green invite card, enabled groups section, remove only
 *
 *  Removing a friend with an outstanding balance is blocked — must settle first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSettingsScreen(
    onBack                 : () -> Unit,
    onRemoved              : () -> Unit,
    onNavigateToSettleUp   : () -> Unit      = {},
    onNavigateToGroup      : (String) -> Unit = {},
    onNavigateToCreateGroup: () -> Unit       = {},
    viewModel              : FriendSettingsViewModel = hiltViewModel(),
) {
    val friend        by viewModel.friend.collectAsState()
    val friendType    by viewModel.friendType.collectAsState()
    val sharedGroups  by viewModel.sharedGroups.collectAsState()
    val allGroups     by viewModel.allGroups.collectAsState()
    val directBalance    by viewModel.directBalance.collectAsState()
    val balanceEntries   by viewModel.balanceEntries.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val actionState        by viewModel.actionState.collectAsState()
    val recurringExpenses  by viewModel.recurringExpenses.collectAsState()
    val snackbarHost        = remember { SnackbarHostState() }
    val groupPickerSheet    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRemoveDialog          by remember { mutableStateOf(false) }
    var stopRecurringTarget       by remember { mutableStateOf<Expense?>(null) }
    var editRecurringTarget       by remember { mutableStateOf<Expense?>(null) }
    var showCancelInviteDialog    by remember { mutableStateOf(false) }
    var showSharedGroupsBlocker   by remember { mutableStateOf(false) }
    var showBlockDialog           by remember { mutableStateOf(false) }
    var showEmailDialog           by remember { mutableStateOf(false) }
    var showGroupPicker           by remember { mutableStateOf(false) }
    var editEmailValue            by remember { mutableStateOf("") }

    val firstName = friend?.fullName?.split(" ")?.firstOrNull() ?: "them"

    // Remove guard:
    //  • Has shared groups → show blocker (must leave groups first)
    //  • No shared groups  → show Splitwise-style disclaimer (backend will delete direct expenses)
    //  • Invited/Pending   → no balance possible, skip both checks
    val tryRemove: () -> Unit = {
        when {
            isLoading -> { /* still loading — ignore tap */ }
            sharedGroups.isNotEmpty() -> showSharedGroupsBlocker = true
            else -> showRemoveDialog = true
        }
    }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendSettingsActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is FriendSettingsActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    // Pre-fill email dialog for invited friends
    LaunchedEffect(friendType) {
        if (friendType is FriendType.Invited) {
            editEmailValue = (friendType as FriendType.Invited).email
        }
    }

    // ── Remove dialog — Splitwise-style disclaimer (Image 1) ──────────────────
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor   = Surface2,
            title = { Text("Remove $firstName?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Column {
                    Text(
                        "This will remove ${friend?.fullName ?: "this person"} from your friends list, and delete any non-group expenses you two have shared.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        "They will be able to see you've removed them (from Activity) but they won't receive an email or push notification.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; viewModel.removeFriend { onRemoved() } }) {
                    Text("Remove", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Shared groups blocker dialog (Image 2) ────────────────────────────────
    if (showSharedGroupsBlocker) {
        AlertDialog(
            onDismissRequest = { showSharedGroupsBlocker = false },
            containerColor   = Surface2,
            title = { Text("You have shared groups", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Column {
                    Text(
                        "If you wish to delete this person from your friends list, you will need to delete them (or yourself) from your groups, or remove the groups entirely.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        "You can access these settings by tapping on a group on this screen.",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSharedGroupsBlocker = false }) {
                    Text("OK", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    // ── Cancel invite dialog ──────────────────────────────────────────────────
    if (showCancelInviteDialog) {
        AlertDialog(
            onDismissRequest = { showCancelInviteDialog = false },
            containerColor   = Surface2,
            title = { Text("Cancel invite?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("This will remove $firstName from your friends list and cancel their invite.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showCancelInviteDialog = false; viewModel.removeFriend { onRemoved() } }) {
                    Text("Cancel invite", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelInviteDialog = false }) { Text("Keep", color = TextSecondary) }
            },
        )
    }


    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor   = Surface2,
            title = { Text("Block ${friend?.fullName ?: "user"}?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("They won't be able to contact you or see your profile.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showBlockDialog = false; viewModel.blockUser { onRemoved() } }) {
                    Text("Block", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Add email / invite dialog ─────────────────────────────────────────────
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            containerColor   = Surface2,
            title = { Text("Add email & invite", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Column {
                    Text(
                        "Enter $firstName's email to send them a FairShare invite.",
                        color    = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                    FsTextField(
                        value         = editEmailValue,
                        onValueChange = { editEmailValue = it },
                        label         = "Email address",
                        keyboardType  = KeyboardType.Email,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmailDialog = false
                    viewModel.updateContactInfo(editEmailValue)
                }) {
                    Text("Send invite", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    // ── Add to existing group picker sheet ────────────────────────────────────
    if (showGroupPicker) {
        ModalBottomSheet(
            onDismissRequest = { showGroupPicker = false },
            sheetState       = groupPickerSheet,
            containerColor   = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxxl),
            ) {
                Text(
                    text       = "Add $firstName to a group",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "Choose a group to add them to",
                    fontSize = 14.sp,
                    color    = TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                if (allGroups.isEmpty()) {
                    Text(
                        text     = "You don't have any groups yet.",
                        fontSize = 14.sp,
                        color    = TextTertiary,
                        modifier = Modifier.padding(bottom = Spacing.lg),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = Spacing.md),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    ) {
                        items(allGroups, key = { it.id }) { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.xl))
                                    .background(Surface0)
                                    .clickable {
                                        showGroupPicker = false
                                        onNavigateToGroup(group.id)
                                    }
                                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(Radius.md))
                                        .background(Surface2)
                                        .border(1.dp, Surface4, RoundedCornerShape(Radius.md)),
                                ) {
                                    Text(groupEmoji(group.type), fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("${group.memberCount} members", fontSize = 12.sp, color = TextTertiary)
                                }
                                Text("›", fontSize = 20.sp, color = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }


    // ── Stop recurring dialog ─────────────────────────────────────────────────
    stopRecurringTarget?.let { expense ->
        AlertDialog(
            onDismissRequest = { stopRecurringTarget = null },
            title = { Text("Stop recurring?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("\"${expense.description}\" will no longer repeat automatically.",
                color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopDirectRecurring(expense.id)
                    stopRecurringTarget = null
                }) { Text("Stop", color = Negative, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { stopRecurringTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // ── Edit Schedule dialog ──────────────────────────────────────────────────
    editRecurringTarget?.let { expense ->
        val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY")
        val labels = mapOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly")
        var selected by remember(expense.id) { mutableStateOf(expense.repeatInterval ?: "MONTHLY") }
        AlertDialog(
            onDismissRequest = { editRecurringTarget = null },
            title = { Text("Edit schedule — ${expense.description}",
                color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("How often should this repeat?", fontSize = 14.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        frequencies.forEach { freq ->
                            val isSelected = selected == freq
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Green400.copy(alpha = 0.15f) else Surface2)
                                    .border(1.dp, if (isSelected) Green400 else Surface3, RoundedCornerShape(20.dp))
                                    .clickable { selected = freq }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(labels[freq] ?: freq, fontSize = 13.sp,
                                    color = if (isSelected) Green400 else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDirectSchedule(expense.id, selected)
                    editRecurringTarget = null
                }) { Text("Save", color = Green400, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { editRecurringTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
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
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FsAvatar(name = friend?.fullName ?: "", userId = friend?.id ?: "", size = 56.dp)
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = friend?.fullName ?: "",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary,
                    )
                    Text(
                        text     = when (val t = friendType) {
                            is FriendType.Invited  -> t.email
                            FriendType.Placeholder -> "No email address"
                            FriendType.Pending     -> "Request pending"
                            FriendType.Accepted    -> friend?.email ?: ""
                        },
                        fontSize = 13.sp,
                        color    = TextTertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))

                // Right-side: balance (accepted) or status pill (others)
                when (friendType) {
                    FriendType.Accepted -> {
                        val bal = directBalance
                        if (bal != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text     = if (bal > 0) "Owed to you" else "You owe",
                                    fontSize = 11.sp,
                                    color    = TextTertiary,
                                )
                                val amtText = if (balanceEntries.isEmpty())
                                    MoneyUtils.format(Math.abs(bal), "USD")
                                else balanceEntries
                                    .filter { if (bal > 0) it.first > 0 else it.first < 0 }
                                    .joinToString(" + ") { (amt, cur) -> MoneyUtils.format(Math.abs(amt), cur) }
                                Text(
                                    text       = amtText,
                                    fontSize   = if (balanceEntries.size > 1) 13.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (bal > 0) Green400 else Negative,
                                )
                            }
                        }
                    }
                    is FriendType.Invited, FriendType.Pending -> {
                        StatusPill(label = "Pending", color = Negative)
                    }
                    FriendType.Placeholder -> {
                        StatusPill(label = "Offline", color = TextTertiary)
                    }
                }
            }

            SectionDivider()

            // ── Context card — invited or placeholder ─────────────────────────
            when (val t = friendType) {
                is FriendType.Invited -> {
                    InvitePendingCard(
                        firstName   = firstName,
                        email       = t.email,
                        onResend    = { viewModel.sendInvite {} },
                        onCancel    = { showCancelInviteDialog = true },
                        modifier    = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    )
                    SectionDivider()
                }
                FriendType.Placeholder -> {
                    PlaceholderInviteCard(
                        firstName       = firstName,
                        onAddEmailInvite = {
                            editEmailValue = ""
                            showEmailDialog = true
                        },
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    )
                    SectionDivider()
                }
                else -> Unit
            }

            // ── Shared groups ─────────────────────────────────────────────────
            val isNonActive = friendType is FriendType.Invited ||
                    friendType == FriendType.Placeholder

            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("Shared groups", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(Spacing.sm))

                if (isNonActive) {
                    // Empty groups card — enabled so user can still create/add
                    if (sharedGroups.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .border(1.dp, Surface4, RoundedCornerShape(Radius.xl))
                                .padding(Spacing.md),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(Radius.lg))
                                        .background(Surface3)
                                        .border(1.dp, Surface4, RoundedCornerShape(Radius.lg)),
                                ) {
                                    Text("👥", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text     = "You and $firstName don't share any groups yet",
                                    fontSize = 13.sp,
                                    color    = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                            // Create group — green border, enabled
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .background(Surface2)
                                    .border(1.dp, Green400, RoundedCornerShape(Radius.lg))
                                    .clickable { onNavigateToCreateGroup() }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text       = "Create group with $firstName",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Green400,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text     = if (friendType is FriendType.Invited)
                                    "$firstName will see it when they join"
                                else
                                    "$firstName will see it when they join FairShare",
                                fontSize = 11.sp,
                                color    = TextTertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        sharedGroups.forEachIndexed { index, group ->
                            SharedGroupRow(group = group, onClick = { onNavigateToGroup(group.id) })
                            if (index < sharedGroups.lastIndex) {
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Surface4))
                            }
                        }
                    }
                    // Add to existing group — always shown for non-active
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                            .clickable { showGroupPicker = true }
                            .padding(vertical = 11.dp),
                    ) {
                        Row(
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to existing group", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        }
                    }
                } else if (sharedGroups.isEmpty()) {
                    Text(
                        text     = "You and $firstName don't share any groups.",
                        fontSize = 13.sp,
                        color    = TextTertiary,
                    )
                } else {
                    sharedGroups.forEachIndexed { index, group ->
                        SharedGroupRow(group = group, onClick = { onNavigateToGroup(group.id) })
                        if (index < sharedGroups.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(Surface4)
                                    .padding(start = 52.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // ── Recurring expenses ────────────────────────────────────────────
            if (friendType is FriendType.Accepted && recurringExpenses.isNotEmpty()) {
                SectionDivider()
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "RECURRING EXPENSES",
                        fontSize = 11.sp, color = TextTertiary,
                        fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    recurringExpenses.forEach { expense ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Repeat, contentDescription = null,
                                tint = Green400, modifier = androidx.compose.ui.Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    "${MoneyUtils.format(expense.totalAmount, expense.currency)} · ${(expense.repeatInterval ?: "").lowercase().replaceFirstChar { it.uppercase() }}",
                                    fontSize = 12.sp, color = TextTertiary,
                                )
                            }
                            TextButton(onClick = { editRecurringTarget = expense }) {
                                Text("Edit", fontSize = 13.sp, color = TextSecondary)
                            }
                            TextButton(onClick = { stopRecurringTarget = expense }) {
                                Text("Stop", fontSize = 13.sp, color = Negative)
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            SectionDivider()

            // ── Quick actions — accepted friends only ─────────────────────────
            if (friendType is FriendType.Accepted) {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text          = "QUICK ACTIONS",
                        fontSize      = 11.sp,
                        color         = TextTertiary,
                        fontWeight    = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    SettingsRow(
                        icon     = Icons.Outlined.History,
                        iconTint = TextSecondary,
                        label    = "View expense history",
                        onClick  = { onBack() },   // history lives on FriendDetail timeline
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                SectionDivider()
            }

            // ── Manage relationship ───────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text          = "MANAGE RELATIONSHIP",
                    fontSize      = 11.sp,
                    color         = TextTertiary,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                when (friendType) {
                    is FriendType.Invited, FriendType.Pending -> {
                        SettingsRow(
                            icon     = Icons.Outlined.Delete,
                            iconTint = TextSecondary,
                            label    = "Cancel invite",
                            subtitle = "Remove pending invitation",
                            onClick  = { showCancelInviteDialog = true },   // no balance check needed
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Surface4))
                        SettingsRow(
                            icon       = Icons.Outlined.PersonRemove,
                            iconTint   = Negative,
                            label      = "Remove from friends list",
                            subtitle   = "Delete contact",
                            labelColor = Negative,
                            onClick    = { tryRemove() },
                        )
                    }
                    FriendType.Placeholder -> {
                        SettingsRow(
                            icon       = Icons.Outlined.PersonRemove,
                            iconTint   = Negative,
                            label      = "Remove from friends list",
                            subtitle   = "Delete this contact",
                            labelColor = Negative,
                            onClick    = { tryRemove() },
                        )
                    }
                    FriendType.Accepted -> {
                        SettingsRow(
                            icon       = Icons.Outlined.PersonRemove,
                            iconTint   = Negative,
                            label      = "Remove from friends list",
                            subtitle   = "You won't see each other's expenses",
                            labelColor = Negative,
                            onClick    = { tryRemove() },
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Surface4))
                        SettingsRow(
                            icon     = Icons.Outlined.Block,
                            iconTint = TextSecondary,
                            label    = "Block user",
                            subtitle = "Hide shared groups and suppress future notifications",
                            onClick  = { showBlockDialog = true },
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Surface4))
                        SettingsRow(
                            icon     = Icons.Outlined.Flag,
                            iconTint = TextSecondary,
                            label    = "Report user",
                            subtitle = "Flag an abusive, suspicious, or spam account",
                            onClick  = { viewModel.showError("Report feature coming soon") },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
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
            .border(
                width = 0.5.dp,
                color = Surface4,
                shape = RoundedCornerShape(0.dp),
            ),
    )
}

// ── Status pill ───────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(label: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.full))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("Status", fontSize = 10.sp, color = TextTertiary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

// ── Invite Pending card (invited state) ───────────────────────────────────────

@Composable
private fun InvitePendingCard(
    firstName: String,
    email    : String,
    onResend : () -> Unit,
    onCancel : () -> Unit,
    modifier : Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .border(1.dp, Negative.copy(alpha = 0.3f), RoundedCornerShape(Radius.xl))
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Negative.copy(alpha = 0.15f)),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Negative, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text("Invite pending", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$firstName hasn't accepted your invite yet. Is the address \"$email\" correct?",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
                    // Resend
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Negative)
                            .clickable(onClick = onResend)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Resend invite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    // Cancel
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Surface3)
                            .border(1.dp, Surface4, RoundedCornerShape(Radius.md))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Cancel invite", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ── Placeholder Invite card ───────────────────────────────────────────────────

@Composable
private fun PlaceholderInviteCard(
    firstName       : String,
    onAddEmailInvite: () -> Unit,
    modifier        : Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .border(1.dp, Green400.copy(alpha = 0.3f), RoundedCornerShape(Radius.xl))
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Green400.copy(alpha = 0.15f)),
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Green400, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text("Invite to FairShare", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$firstName isn't on FairShare yet. Add an email to send an invite and let them track expenses too.",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Green400)
                        .clickable(onClick = onAddEmailInvite)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Add email & invite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Surface0)
                }
            }
        }
    }
}

// ── Shared Group Row ──────────────────────────────────────────────────────────

@Composable
private fun SharedGroupRow(group: Group, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(Surface2)
                .border(1.dp, Surface4, RoundedCornerShape(Radius.lg)),
        ) {
            Text(groupEmoji(group.type), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("${group.memberCount} members", fontSize = 12.sp, color = TextTertiary)
        }
        Text("›", fontSize = 20.sp, color = TextTertiary)
    }
}

// ── Settings Row ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon      : ImageVector,
    iconTint  : Color,
    label     : String,
    subtitle  : String  = "",
    labelColor: Color   = TextPrimary,
    onClick   : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.Top,
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
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = labelColor)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text("›", fontSize = 20.sp, color = TextTertiary, modifier = Modifier.padding(top = 2.dp))
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun groupEmoji(type: com.prathik.fairshare.domain.model.GroupType): String = when (type) {
    GroupType.HOME      -> "🏠"
    GroupType.TRIP      -> "✈️"
    GroupType.COUPLE    -> "💑"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👫"
    GroupType.EVENT     -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER     -> "💰"
}