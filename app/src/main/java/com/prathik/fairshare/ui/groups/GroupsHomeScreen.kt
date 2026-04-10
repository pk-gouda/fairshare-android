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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsSecondaryButton
import com.prathik.fairshare.ui.components.FsTopBar
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
import com.prathik.fairshare.ui.theme.TileCoupleEnd
import com.prathik.fairshare.ui.theme.TileCoupleStart
import com.prathik.fairshare.ui.theme.TileEventEnd
import com.prathik.fairshare.ui.theme.TileEventStart
import com.prathik.fairshare.ui.theme.TileFriendsEnd
import com.prathik.fairshare.ui.theme.TileFriendsStart
import com.prathik.fairshare.ui.theme.TileHomeEnd
import com.prathik.fairshare.ui.theme.TileHomeStart
import com.prathik.fairshare.ui.theme.TileOfficeEnd
import com.prathik.fairshare.ui.theme.TileOfficeStart
import com.prathik.fairshare.ui.theme.TileOtherEnd
import com.prathik.fairshare.ui.theme.TileOtherStart
import com.prathik.fairshare.ui.theme.TileTripEnd
import com.prathik.fairshare.ui.theme.TileTripStart
import com.prathik.fairshare.util.MoneyUtils

/**
 * Groups Home Screen — the main screen after login.
 *
 * Shows:
 * - Net balance hero section (how much you're owed overall)
 * - Vertical list of groups with balance status (settled up, owes you, you owe, no expenses)
 * - "Create a group" button at bottom of list (always visible)
 * - FAB → Add Expense (quick add)
 * - Top bar GroupAdd icon → Add Group sheet
 * - Empty state CTA → Add Group sheet
 *
 * Add Group bottom sheet offers 3 paths:
 *   1. Create a new group  → onNavigateToCreateGroup
 *   2. Join with invite code → onNavigateToJoinGroup
 *   3. Import from Splitwise → onNavigateToImport
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val groupsState     by viewModel.groupsState.collectAsState()
    val balanceSummary  by viewModel.balanceSummary.collectAsState()
    val groupBalanceMap by viewModel.groupBalanceMap.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val isLoading = groupsState is GroupsUiState.Loading

    var showAddGroupSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Refresh every time this screen becomes RESUMED (e.g. back from CreateGroup/GroupDetail)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    // ── Add Group bottom sheet ─────────────────────────────────────────────────
    if (showAddGroupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddGroupSheet = false },
            sheetState = sheetState,
            containerColor = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxxl),
            ) {
                Text(
                    text = "Add a group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how you want to connect",
                    fontSize = 14.sp,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                // Option 1 — Create a new group
                GroupSheetOption(
                    icon = Icons.Outlined.GroupWork,
                    iconBg = Green400,
                    title = "Create a new group",
                    subtitle = "Start fresh with a name and members",
                    onClick = {
                        showAddGroupSheet = false
                        onNavigateToCreateGroup()
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Option 2 — Join with invite code
                GroupSheetOption(
                    icon = Icons.Outlined.Link,
                    iconBg = Color(0xFF4A6FE8),
                    title = "Join with invite code",
                    subtitle = "Enter a code shared by a friend",
                    onClick = {
                        showAddGroupSheet = false
                        onNavigateToJoinGroup()
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Option 3 — Import from Splitwise
                GroupSheetOption(
                    icon = Icons.Outlined.FileUpload,
                    iconBg = Color(0xFFE8A84F),
                    title = "Import from Splitwise",
                    subtitle = "Upload a CSV to migrate a group",
                    onClick = {
                        showAddGroupSheet = false
                        onNavigateToImport()
                    },
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Cancel
                TextButton(
                    onClick = { showAddGroupSheet = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        color = TextSecondary,
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            Surface(color = Surface0, shadowElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Functional search bar — takes full remaining width
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .padding(horizontal = Spacing.md, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                            cursorBrush = SolidColor(Green400),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search groups...",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Add group icon button → opens sheet
                    FsIconButton(
                        icon = Icons.Outlined.GroupAdd,
                        contentDescription = "Add group",
                        onClick = { showAddGroupSheet = true },
                    )
                }
            }
        },
        floatingActionButton = {
            // FAB → Add Expense (quick action)
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = Green400,
                contentColor = Surface0,
                shape = RoundedCornerShape(Radius.full),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = groupsState) {
                is GroupsUiState.Loading -> FsLoadingScreen()

                is GroupsUiState.Error -> FsErrorScreen(
                    message = state.message,
                    isNetwork = state.isNetwork,
                    onRetry = { viewModel.loadData() },
                )

                is GroupsUiState.Success -> {
                    val displayGroups = if (searchQuery.isBlank()) state.groups
                    else state.groups.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (displayGroups.isEmpty() && searchQuery.isBlank()) {
                        FsEmptyState(
                            title = "No groups yet",
                            subtitle = "Create a group to start splitting expenses with friends",
                            ctaText = "Add a group",
                            onCta = { showAddGroupSheet = true },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {

                            // ── Balance hero ──────────────────────────────────
                            balanceSummary?.let { summary ->
                                BalanceHeroSection(
                                    summary = summary,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical = Spacing.lg,
                                    ),
                                )
                            }

                            // ── Group list ────────────────────────────────────
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = displayGroups,
                                    key = { it.id },
                                ) { group ->
                                    GroupRow(
                                        group   = group,
                                        balance = groupBalanceMap[group.id],
                                        onClick = { onNavigateToGroup(group.id) },
                                    )
                                    HorizontalDivider(
                                        color     = Surface3,
                                        thickness = 0.5.dp,
                                        modifier  = Modifier.padding(start = Spacing.lg + 56.dp),
                                    )
                                }

                                item {
                                    FsSecondaryButton(
                                        text    = "+ Add a group",
                                        onClick = { showAddGroupSheet = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                    )
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Add Group Sheet Option ────────────────────────────────────────────────────

@Composable
private fun GroupSheetOption(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface0)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg.copy(alpha = 0.15f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBg,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextTertiary,
            )
        }
        Text(text = "›", fontSize = 20.sp, color = TextTertiary)
    }
}

// ── Balance Hero Section ──────────────────────────────────────────────────────

@Composable
private fun BalanceHeroSection(
    summary: BalanceSummary,
    modifier: Modifier = Modifier,
) {
    val netColor = when {
        summary.netBalance > 0 -> Green400
        summary.netBalance < 0 -> Negative
        else -> TextSecondary
    }

    val netText = when {
        summary.netBalance > 0 -> MoneyUtils.format(summary.netBalance, summary.currency)
        summary.netBalance < 0 -> MoneyUtils.format(Math.abs(summary.netBalance), summary.currency)
        else -> MoneyUtils.format(0.0, summary.currency)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "NET BALANCE",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = netText,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            color = netColor,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Stats pills
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatPill(
                label = "Owed to you",
                amount = MoneyUtils.format(summary.owedToMe, summary.currency),
                color = Green400,
            )
            StatPill(
                label = "You owe",
                amount = MoneyUtils.format(summary.youOwe, summary.currency),
                color = Negative,
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    amount: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = amount, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Group Row ─────────────────────────────────────────────────────────────────

@Composable
private fun GroupRow(
    group  : Group,
    balance: Double?,   // null = no expenses, 0.0 = settled up, +/- = active balance
    onClick: () -> Unit,
) {
    val gradient = groupTypeGradient(group.type)

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Coloured group icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(Brush.verticalGradient(gradient)),
        ) {
            Text(groupTypeEmoji(group.type), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = group.name,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = "${group.memberCount} members",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        when {
            balance == null -> Text(
                text     = "no expenses",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
            balance > 0 -> Column(horizontalAlignment = Alignment.End) {
                Text(text = "you are owed", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Green400)
                Text(text = MoneyUtils.format(balance, "USD"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green400)
            }
            balance < 0 -> Column(horizontalAlignment = Alignment.End) {
                Text(text = "you owe", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Negative)
                Text(text = MoneyUtils.format(-balance, "USD"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Negative)
            }
            else -> Text(
                text     = "settled up",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun groupTypeGradient(type: GroupType): List<Color> = when (type) {
    GroupType.HOME      -> listOf(TileHomeStart, TileHomeEnd)
    GroupType.TRIP      -> listOf(TileTripStart, TileTripEnd)
    GroupType.COUPLE    -> listOf(TileCoupleStart, TileCoupleEnd)
    GroupType.OFFICE    -> listOf(TileOfficeStart, TileOfficeEnd)
    GroupType.FRIENDS   -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.EVENT     -> listOf(TileEventStart, TileEventEnd)
    GroupType.APARTMENT -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.OTHER     -> listOf(TileOtherStart, TileOtherEnd)
}

private fun groupTypeEmoji(type: GroupType): String = when (type) {
    GroupType.HOME      -> "🏠"
    GroupType.TRIP      -> "✈️"
    GroupType.COUPLE    -> "💑"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👫"
    GroupType.EVENT     -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER     -> "💰"
}