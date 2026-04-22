package com.prathik.fairshare.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.model.NotificationType
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onNavigateToExpense: (String) -> Unit,
    onNavigateToFriend: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSettlement: (String) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreGroupId by remember { mutableStateOf<String?>(null) }
    var pendingRestoreGroupName by remember { mutableStateOf("") }
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val grouped by remember { derivedStateOf { viewModel.groupedNotifications(selectedFilter) } }
    val hasUnread by remember { derivedStateOf { viewModel.hasUnread } }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActivityActionState.GroupRestored -> {
                snackbarHost.showSnackbar("Group restored"); viewModel.resetActionState()
            }

            is ActivityActionState.Success -> {
                snackbarHost.showSnackbar(state.message); viewModel.resetActionState()
            }

            is ActivityActionState.Error -> {
                snackbarHost.showSnackbar(state.message); viewModel.resetActionState()
            }

            else -> Unit
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore group?") },
            text = { Text("Restore '$pendingRestoreGroupName'? Groups can be restored within 30 days of deletion.") },
            confirmButton = {
                FsPrimaryButton(
                    text = "Restore",
                    onClick = {
                        showRestoreDialog = false
                        pendingRestoreGroupId?.let { viewModel.restoreGroup(it) }
                    },
                )
            },
            dismissButton = {
                FsTextButton(
                    text = "Cancel",
                    onClick = { showRestoreDialog = false },
                )
            },
            containerColor = Surface2,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            FsTopBar(
                title = "Activity",
                actions = {
                    if (hasUnread) {
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.markAllRead() },
                        ) {
                            Text(text = "Mark all read", fontSize = 13.sp, color = Green400)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface0)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    Spacing.sm
                ),
            ) {
                listOf(
                    ActivityFilter.ALL to "All",
                    ActivityFilter.EXPENSES to "Expenses",
                    ActivityFilter.SETTLEMENTS to "Settlements",
                    ActivityFilter.GROUPS to "Groups",
                ).forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.full))
                            .background(if (isSelected) Green400 else Surface2)
                            .border(
                                1.dp,
                                if (isSelected) Green400 else Surface4,
                                RoundedCornerShape(Radius.full)
                            )
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = Spacing.md, vertical = 6.dp),
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Surface0 else TextSecondary,
                        )
                    }
                }
            }
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadData() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isLoading) {
                    FsLoadingScreen(); return@PullToRefreshBox
                }

                if (grouped.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔔", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(text = "No activity yet", fontSize = 15.sp, color = TextSecondary)
                            Text(
                                text = "Expense and settlement updates\nwill appear here",
                                fontSize = 12.sp, color = TextTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    return@PullToRefreshBox
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    listOf("Today", "Yesterday", "Earlier").forEach { section ->
                        val sectionItems = grouped[section] ?: return@forEach
                        item {
                            Text(
                                text = section.uppercase(),
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                color = TextTertiary, letterSpacing = 1.sp,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.lg,
                                    vertical = Spacing.sm
                                ),
                            )
                        }
                        items(items = sectionItems, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onNavigateToExpense = onNavigateToExpense,
                                onNavigateToFriend = onNavigateToFriend,
                                onNavigateToGroup = onNavigateToGroup,
                                onNavigateToSettlement = onNavigateToSettlement,
                                onGroupDeleted = { groupId, groupName ->
                                    pendingRestoreGroupId = groupId
                                    pendingRestoreGroupName = groupName
                                    showRestoreDialog = true
                                },
                            )
                            HorizontalDivider(
                                color = Surface3, thickness = 0.5.dp,
                                modifier = Modifier.padding(start = Spacing.lg + 50.dp),
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onNavigateToExpense: (String) -> Unit,
    onNavigateToFriend: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSettlement: (String) -> Unit,
    onGroupDeleted: (groupId: String, groupName: String) -> Unit,
) {
    val isUnread = !notification.isRead
    val isTappable = when (notification.type) {
        NotificationType.SETTLEMENT_CANCELLED,
        NotificationType.GROUP_MEMBER_REMOVED -> false

        else -> notification.referenceId != null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isUnread) Color(0xFF0D1A0D) else Surface0)
            .then(
                if (isTappable) Modifier.clickable {
                    // Helper: show restore dialog instead of navigating when group is deleted
                    fun handleGroupDeleted() {
                        val name = notification.groupName?.ifBlank { null }
                            ?: notification.message.substringAfter("'").substringBefore("'")
                                .ifBlank { "this group" }
                        val id = notification.groupId ?: notification.referenceId
                        id?.let { onGroupDeleted(it, name) }
                    }

                    when (notification.type) {
                        NotificationType.EXPENSE_ADDED,
                        NotificationType.EXPENSE_UPDATED,
                        NotificationType.EXPENSE_DELETED,
                        NotificationType.EXPENSE_RECURRING_SET,
                        NotificationType.EXPENSE_RECURRING_STOPPED,
                        NotificationType.EXPENSE_AUTO_CREATED,
                        NotificationType.EXPENSE_RESTORED -> {
                            if (notification.isGroupDeleted) {
                                handleGroupDeleted()
                            } else {
                                notification.referenceId?.let { onNavigateToExpense(it) }
                            }
                        }

                        NotificationType.SETTLEMENT_RECEIVED,
                        NotificationType.SETTLEMENT_CONFIRMED -> notification.referenceId?.let {
                            onNavigateToSettlement(it)
                        }

                        NotificationType.FRIEND_REQUEST_RECEIVED,
                        NotificationType.FRIEND_REQUEST_ACCEPTED -> onNavigateToFriend()

                        NotificationType.GROUP_DELETED -> handleGroupDeleted()

                        NotificationType.GROUP_INVITE_RECEIVED,
                        NotificationType.GROUP_MEMBER_JOINED,
                        NotificationType.GROUP_MEMBER_ADDED,
                        NotificationType.GROUP_CREATED,
                        NotificationType.GROUP_IMPORTED,
                        NotificationType.GROUP_RESTORED,
                        NotificationType.GROUP_ARCHIVED,
                        NotificationType.GROUP_UNARCHIVED,
                        NotificationType.SIMPLIFY_DEBTS_CHANGED,
                        NotificationType.PLACEHOLDER_ASSIGNED -> {
                            if (notification.isGroupDeleted) {
                                handleGroupDeleted()
                            } else {
                                notification.referenceId?.let { onNavigateToGroup(it) }
                            }
                        }

                        NotificationType.SETTLE_UP_REMINDER -> onNavigateToFriend()
                        else -> Unit
                    }
                } else Modifier
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(Radius.full))
                .background(if (isUnread) Green400 else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(notificationBgColor(notification.type)),
        ) {
            Text(text = notificationEmoji(notification.type), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title, fontSize = 13.sp,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Medium,
                color = TextPrimary,
            )
            if (notification.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = notification.message, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = notification.createdAt.toRelativeTime(),
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
        if (isUnread) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Green400)
                    .align(Alignment.CenterVertically),
            )
        }
    }
}

private fun notificationEmoji(type: NotificationType): String = when (type) {
    NotificationType.EXPENSE_ADDED -> "🧾"
    NotificationType.EXPENSE_UPDATED -> "✏️"
    NotificationType.EXPENSE_DELETED -> "🗑️"
    NotificationType.EXPENSE_RESTORED -> "♻️"
    NotificationType.SETTLEMENT_RECEIVED -> "💸"
    NotificationType.SETTLEMENT_CONFIRMED -> "✅"
    NotificationType.SETTLEMENT_CANCELLED -> "❌"
    NotificationType.FRIEND_REQUEST_RECEIVED -> "👤"
    NotificationType.FRIEND_REQUEST_ACCEPTED -> "🤝"
    NotificationType.GROUP_INVITE_RECEIVED -> "👥"
    NotificationType.GROUP_MEMBER_JOINED -> "👥"
    NotificationType.GROUP_MEMBER_REMOVED -> "👋"
    NotificationType.GROUP_MEMBER_ADDED -> "➕"
    NotificationType.GROUP_CREATED -> "✨"
    NotificationType.GROUP_IMPORTED -> "📥"
    NotificationType.GROUP_DELETED -> "🗑️"
    NotificationType.GROUP_RESTORED -> "♻️"
    NotificationType.GROUP_ARCHIVED -> "📦"
    NotificationType.GROUP_UNARCHIVED -> "📤"
    NotificationType.SIMPLIFY_DEBTS_CHANGED -> "⚙️"
    NotificationType.PLACEHOLDER_ASSIGNED -> "🔗"
    NotificationType.EXPENSE_RECURRING_SET -> "🔄"
    NotificationType.EXPENSE_RECURRING_STOPPED -> "⏹️"
    NotificationType.EXPENSE_AUTO_CREATED -> "🤖"
    NotificationType.SETTLE_UP_REMINDER -> "⏰"
}

private fun notificationBgColor(type: NotificationType): Color = when (type) {
    NotificationType.EXPENSE_ADDED,
    NotificationType.EXPENSE_UPDATED,
    NotificationType.EXPENSE_DELETED -> Color(0xFF1A2A1A)

    NotificationType.EXPENSE_RESTORED -> Color(0xFF1A2A2A)
    NotificationType.SETTLEMENT_RECEIVED,
    NotificationType.SETTLEMENT_CONFIRMED -> Color(0xFF1A3A1A)

    NotificationType.SETTLEMENT_CANCELLED -> Color(0xFF2A1A1A)
    NotificationType.FRIEND_REQUEST_RECEIVED,
    NotificationType.FRIEND_REQUEST_ACCEPTED -> Color(0xFF1A1A3A)

    NotificationType.GROUP_INVITE_RECEIVED,
    NotificationType.GROUP_MEMBER_JOINED -> Color(0xFF1A2A3A)

    NotificationType.GROUP_MEMBER_REMOVED -> Color(0xFF2A2A1A)
    NotificationType.GROUP_MEMBER_ADDED -> Color(0xFF1A2A3A)
    NotificationType.GROUP_CREATED,
    NotificationType.GROUP_IMPORTED,
    NotificationType.GROUP_RESTORED,
    NotificationType.GROUP_ARCHIVED,
    NotificationType.GROUP_UNARCHIVED -> Color(0xFF1A2A3A)

    NotificationType.GROUP_DELETED -> Color(0xFF2A1A1A)
    NotificationType.SIMPLIFY_DEBTS_CHANGED,
    NotificationType.PLACEHOLDER_ASSIGNED -> Color(0xFF1A1A2A)

    NotificationType.EXPENSE_RECURRING_SET,
    NotificationType.EXPENSE_RECURRING_STOPPED,
    NotificationType.EXPENSE_AUTO_CREATED -> Color(0xFF1A2A1A)

    NotificationType.SETTLE_UP_REMINDER -> Color(0xFF2A1800)
}

private fun String.toRelativeTime(): String {
    return try {
        val instant = try {
            java.time.Instant.parse(this)
        } catch (e: Exception) {
            java.time.LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(java.time.ZoneOffset.UTC)
        }
        val zoneId = java.time.ZoneId.systemDefault()
        val ldt = java.time.LocalDateTime.ofInstant(instant, zoneId)
        val today = java.time.LocalDate.now(zoneId)
        val localDate = ldt.toLocalDate()
        val timeStr = ldt.format(DateTimeFormatter.ofPattern("h:mm a"))
        val daysDiff = today.toEpochDay() - localDate.toEpochDay()
        when {
            daysDiff == 0L -> "Today, $timeStr"
            daysDiff == 1L -> "Yesterday, $timeStr"
            daysDiff < 7L -> "$daysDiff days ago, $timeStr"
            else -> ldt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
        }
    } catch (e: Exception) {
        this
    }
}