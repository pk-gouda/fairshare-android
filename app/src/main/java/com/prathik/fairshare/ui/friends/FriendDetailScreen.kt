package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsLoadingScreen
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
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    onBack                 : () -> Unit,
    onNavigateToSettings   : () -> Unit,
    onNavigateToExpense    : (String) -> Unit,
    onNavigateToAddExpense  : () -> Unit,
    onNavigateToSettle     : (friendId: String, groupId: String?, payerId: String?) -> Unit,
    onNavigateToSearch     : () -> Unit,
    onNavigateToSettlement : (String) -> Unit = {},
    onNavigateToGroup      : (String) -> Unit = {},
    onNavigateToAnalytics  : () -> Unit = {},
    viewModel              : FriendDetailViewModel = hiltViewModel(),
) {
    val isLoading     by viewModel.isLoading.collectAsState()
    val friend        by viewModel.friend.collectAsState()
    val netBalance    by viewModel.netBalance.collectAsState()
    val currency      by viewModel.currency.collectAsState()
    val groupBalances by viewModel.groupBalances.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val settlements   by viewModel.settlements.collectAsState()
    val actionState   by viewModel.actionState.collectAsState()
    val friendStatus  by viewModel.friendStatus.collectAsState()

    // ── Settle up sheet state ─────────────────────────────────────────────────
    var showBalanceSheet by remember { mutableStateOf(false) }
    var showLinkSheet    by remember { mutableStateOf(false) }
    var showPayerSheet   by remember { mutableStateOf(false) }

    // Non-group balance = net - sum of group balances
    val groupBalanceSum   = groupBalances.filter { it.groupId != null }.sumOf { it.amount }
    val nonGroupBalance   = netBalance - groupBalanceSum

    val handleSettle: () -> Unit = {
        if (netBalance != 0.0 || groupBalances.isNotEmpty()) showBalanceSheet = true
        else showPayerSheet = true  // fully settled up: ask who paid
    }
    val snackbarHost  = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    // Show top bar name once cover scrolls away (cover is 220dp)
    val showTopBarName by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > 400
        }
    }

    val friendName = friend?.fullName ?: ""

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendDetailActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is FriendDetailActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    // Auto-refresh expenses + balance every time screen resumes
    // (e.g. returning from AddExpense, SettleUp, ExpenseDetail)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshExpenses()
        }
    }

    Scaffold(
        containerColor      = Surface0,
        contentWindowInsets = WindowInsets(0),
        snackbarHost        = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNavigateToAddExpense,
                containerColor = Green400,
                contentColor   = Surface0,
                shape          = RoundedCornerShape(Radius.lg),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh    = { viewModel.loadData() },
                modifier     = Modifier.fillMaxSize(),
            ) {
                if (isLoading && friend == null) {
                    FsLoadingScreen()
                } else {
                    LazyColumn(
                        state    = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                    ) {

                        // ── Friend Cover ──────────────────────────────────────
                        item {
                            FriendCoverPhoto(
                                name      = friendName,
                                userId    = friend?.id ?: "",
                                email     = friend?.email ?: "",
                            )
                        }

                        // ── Balance Card ──────────────────────────────────────
                        item {
                            FriendBalanceCard(
                                netBalance      = netBalance,
                                currency        = currency,
                                groupBalances   = groupBalances,
                                nonGroupBalance = nonGroupBalance,
                                friendName      = friendName,
                                onSettle        = handleSettle,
                                modifier        = Modifier.padding(
                                    horizontal = Spacing.lg,
                                    vertical   = Spacing.md,
                                ),
                            )
                        }

                        // ── Placeholder link banner ───────────────────────────
                        if (friendStatus == "placeholder") {
                            item {
                                PlaceholderLinkBanner(
                                    friendName = friendName,
                                    onLink     = { showLinkSheet = true },
                                    modifier   = Modifier.padding(
                                        start  = Spacing.lg,
                                        end    = Spacing.lg,
                                        bottom = Spacing.sm,
                                    ),
                                )
                            }
                        }

                        // ── Action Pills ──────────────────────────────────────
                        item {
                            FriendActionPills(
                                onSettleUp = handleSettle,
                                modifier   = Modifier.padding(
                                    start  = Spacing.lg,
                                    end    = Spacing.lg,
                                    bottom = Spacing.md,
                                ),
                            )
                        }

                        // ── Unified Timeline: group balances + direct expenses + settlements ──
                        when (val state = expensesState) {
                            is FriendExpensesState.Loading -> {
                                item { FsLoadingScreen(modifier = Modifier.height(200.dp)) }
                            }
                            is FriendExpensesState.Error -> {
                                item {
                                    FsEmptyState(
                                        title    = "Couldn't load expenses",
                                        subtitle = state.message,
                                        modifier = Modifier.height(200.dp),
                                    )
                                }
                            }
                            is FriendExpensesState.Success -> {
                                // Build unified timeline
                                val allItems = buildList<FriendTimelineItem> {
                                    // Show ALL group balances including settled (amount == 0)
                                    // so the group row stays visible with "settled up" label
                                    groupBalances.forEach { balance ->
                                        add(FriendTimelineItem.GroupBalanceItem(balance))
                                    }
                                    state.expenses.forEach { expense ->
                                        add(FriendTimelineItem.DirectExpenseItem(expense))
                                    }
                                    settlements.forEach { settlement ->
                                        // For ALL+isFullSettle settlements, add a separate
                                        // scales icon row in addition to the payment row
                                        if (settlement.isFullSettle && settlement.settleType == "ALL") {
                                            add(FriendTimelineItem.FullySettledItem(settlement))
                                        }
                                        add(FriendTimelineItem.SettlementItem(settlement))
                                    }
                                }.sortedByDescending { it.sortDate }

                                // Apply search filter
                                val timelineItems = allItems

                                if (timelineItems.isEmpty()) {
                                    item {
                                        FsEmptyState(
                                            title    = "No shared expenses",
                                            subtitle = "Add an expense to get started",
                                            modifier = Modifier.height(300.dp),
                                        )
                                    }
                                } else {
                                    val grouped = timelineItems.groupBy { it.sortDate.toMonthHeader() }
                                    grouped.forEach { (month, items) ->
                                        item {
                                            Text(
                                                text          = month.uppercase(),
                                                fontSize      = 10.sp,
                                                fontWeight    = FontWeight.SemiBold,
                                                color         = TextTertiary,
                                                letterSpacing = 1.sp,
                                                modifier      = Modifier.padding(
                                                    horizontal = Spacing.lg,
                                                    vertical   = Spacing.sm,
                                                ),
                                            )
                                        }
                                        items(
                                            items = items,
                                            key   = { when (it) {
                                                is FriendTimelineItem.GroupBalanceItem  -> "gb_${it.balance.groupId}"
                                                is FriendTimelineItem.DirectExpenseItem -> "ex_${it.expense.id}"
                                                is FriendTimelineItem.SettlementItem    -> "st_${it.settlement.id}"
                                                is FriendTimelineItem.FullySettledItem  -> "fs_${it.settlement.id}"
                                            }},
                                        ) { item ->
                                            when (item) {
                                                is FriendTimelineItem.GroupBalanceItem ->
                                                    GroupBalanceRow(
                                                        balance = item.balance,
                                                        onClick = { gId ->
                                                            if (gId != null) onNavigateToGroup(gId)
                                                        },
                                                    )
                                                is FriendTimelineItem.DirectExpenseItem ->
                                                    FriendExpenseRow(
                                                        expense = item.expense,
                                                        onClick = { onNavigateToExpense(item.expense.id) },
                                                    )
                                                is FriendTimelineItem.SettlementItem ->
                                                    FriendSettlementRow(
                                                        settlement = item.settlement,
                                                        onClick    = { onNavigateToSettlement(item.settlement.id) },
                                                        onDelete   = { viewModel.deleteSettlement(item.settlement.id) },
                                                    )
                                                is FriendTimelineItem.FullySettledItem ->
                                                    FriendFullySettledRow(
                                                        settlement = item.settlement,
                                                        onClick    = { onNavigateToSettlement(item.settlement.id) },
                                                    )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }

            // ── Fixed overlay top bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (showTopBarName) Color(0xFF1A1A2E) else Color.Transparent)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .offset(y = (-20).dp)
                    .padding(horizontal = Spacing.lg, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Back
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .clickable { onBack() },
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                // Friend name — only visible once cover scrolls away
                if (showTopBarName) {
                    Text(
                        text       = friendName,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.md),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Search + Settings
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable { onNavigateToSearch() },
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint               = Color.White,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable { onNavigateToSettings() },
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint               = Color.White,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    // ── Sheet 1: Balance selection (Splitwise-style) ──────────────────────────
    // Link sheet for placeholder friends
    val friendsList by viewModel.friends.collectAsState()
    if (showLinkSheet && friendStatus == "placeholder") {
        FriendLinkSheet(
            friendName = friendName,
            friends    = friendsList,
            onLink     = { friendId ->
                showLinkSheet = false
                viewModel.assignFriendPlaceholder(viewModel.friendId, friendId)
            },
            onDismiss  = { showLinkSheet = false },
        )
    }

    if (showBalanceSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showBalanceSheet = false },
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                // Title
                Text(
                    text       = "Select a balance to settle with ${friendName.ifBlank { "friend" }}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)

                // Settle all balances
                val totalOwed = Math.abs(netBalance)
                if (totalOwed > 0) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBalanceSheet = false
                                onNavigateToSettle(viewModel.friendId, null, null)
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = friendName, userId = viewModel.friendId,
                            size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text("Settle all balances", fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary,
                            modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text     = if (netBalance > 0) "you are owed" else "you owe",
                                fontSize = 11.sp, color = TextTertiary,
                            )
                            Text(
                                text       = MoneyUtils.format(totalOwed, currency),
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (netBalance > 0) Green400 else Negative,
                            )
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }

                // Per-group balances
                val groupsWithBalance = groupBalances.filter { it.groupId != null && it.amount != 0.0 }
                if (groupsWithBalance.isNotEmpty()) {
                    Text(
                        text     = "Or, settle a specific group balance",
                        fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(
                            start  = Spacing.lg,
                            end    = Spacing.lg,
                            top    = Spacing.md,
                            bottom = Spacing.sm,
                        ),
                    )
                    groupsWithBalance.forEach { balance ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBalanceSheet = false
                                    onNavigateToSettle(viewModel.friendId, balance.groupId, null)
                                }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Group icon placeholder
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(ComponentSize.avatarMd)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .background(Surface4),
                            ) {
                                Text(
                                    text     = balance.groupName?.firstOrNull()?.uppercase() ?: "G",
                                    fontSize = 18.sp, color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text       = balance.groupName ?: "Group",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color      = TextPrimary,
                                modifier   = Modifier.weight(1f),
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text     = if (balance.amount > 0) "you are owed" else "you owe",
                                    fontSize = 11.sp, color = TextTertiary,
                                )
                                Text(
                                    text       = MoneyUtils.format(Math.abs(balance.amount), balance.currency),
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (balance.amount > 0) Green400 else Negative,
                                )
                            }
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }

                // Non-group balance row
                if (Math.abs(nonGroupBalance) > 0.01) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBalanceSheet = false
                                onNavigateToSettle(viewModel.friendId, null, null)
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(ComponentSize.avatarMd)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(Surface4),
                        ) {
                            Text("📋", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text("Non-group expenses", fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary,
                            modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text     = if (nonGroupBalance > 0) "you are owed" else "you owe",
                                fontSize = 11.sp, color = TextTertiary,
                            )
                            Text(
                                text       = MoneyUtils.format(Math.abs(nonGroupBalance), currency),
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (nonGroupBalance > 0) Green400 else Negative,
                            )
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }

                // More options
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showBalanceSheet = false
                            showPayerSheet   = true
                        }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("More options", fontSize = 15.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // ── Sheet 2: Payer selection (for "More options" or fully settled up) ─────
    if (showPayerSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showPayerSheet = false },
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Who paid?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                val meId = viewModel.currentUserId ?: ""
                listOf(meId to "You", viewModel.friendId to friendName).forEach { (userId, name) ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPayerSheet = false
                                onNavigateToSettle(viewModel.friendId, null, userId)
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = name, userId = userId, size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Timeline data model ───────────────────────────────────────────────────────

// ── Link Friend Sheet (shown when tapping PlaceholderLinkBanner) ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendLinkSheet(
    friendName  : String,
    friends     : List<com.prathik.fairshare.domain.model.Friend>,
    onLink      : (friendId: String) -> Unit,
    onDismiss   : () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface2,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text       = "Who is $friendName on FairShare?",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
            Text(
                text     = "Link their expenses to a real account.",
                fontSize = 13.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
            )
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            if (friends.isEmpty()) {
                Text("No FairShare friends found.", fontSize = 14.sp, color = TextTertiary,
                    modifier = Modifier.padding(Spacing.lg))
            } else {
                friends.forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLink(friend.id) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = friend.fullName, userId = friend.id,
                            size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(friend.fullName, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = Spacing.lg, vertical = 16.dp),
            ) {
                Text("Cancel", fontSize = 14.sp, color = TextTertiary)
            }
        }
    }
}


sealed class FriendTimelineItem(val sortDate: String) {
    data class GroupBalanceItem(val balance: Balance) :
        FriendTimelineItem(balance.groupLastActivity ?: "")
    data class DirectExpenseItem(val expense: Expense) :
        FriendTimelineItem(expense.expenseDate)
    data class SettlementItem(val settlement: Settlement) :
        FriendTimelineItem(settlement.settlementDate)
    /** Scales icon row shown for ALL+isFullSettle settlements, separate from the payment row. */
    data class FullySettledItem(val settlement: Settlement) :
        FriendTimelineItem(settlement.settlementDate)
}

// ── Friend Settlement Row ─────────────────────────────────────────────────────

/**
 * Shows a settlement as an expense-like row in the friend detail timeline.
 * Same layout as GroupDetailScreen's SettlementRow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendSettlementRow(settlement: Settlement, onClick: () -> Unit, onDelete: () -> Unit) {
    val (monthAbbr, dayNum) = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) { "—" to "—" }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete settlement?") },
            text    = { Text("This will reverse the balance changes. This cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete", color = Negative) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = { showDeleteDialog = true },
            )
            .padding(horizontal = Spacing.lg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(30.dp),
        ) {
            Text(monthAbbr, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Text(dayNum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Handshake icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Green400.copy(alpha = 0.12f)),
        ) {
            Text("🤝", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Payer → Receiver + notes
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "${settlement.payerName} paid ${settlement.receiverName}",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = Green400,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = settlement.notes?.takeIf { it.isNotBlank() } ?: "Payment",
                fontSize = 12.sp,
                color    = TextTertiary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Amount
        Text(
            text       = MoneyUtils.format(settlement.amount, settlement.currency),
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Green400,
        )
    }

    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = Spacing.lg + 30.dp + Spacing.sm),
    )
}

// ── Placeholder Link Banner ───────────────────────────────────────────────────

@Composable
private fun PlaceholderLinkBanner(
    friendName: String,
    onLink    : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .clickable(onClick = onLink)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔗", fontSize = 20.sp)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Link $friendName to a FairShare friend",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
            )
            Text(
                text     = "Tap to connect this placeholder to a real account",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
        }
        Text("→", fontSize = 16.sp, color = Green400, fontWeight = FontWeight.SemiBold)
    }
}


// ── Friend Fully Settled Row ──────────────────────────────────────────────────

/**
 * Scales icon row shown in the friend timeline for ALL+isFullSettle settlements.
 * Displayed alongside (above) the regular payment row.
 * Tapping navigates to the "fully settled up" detail screen.
 */
@Composable
private fun FriendFullySettledRow(settlement: Settlement, onClick: () -> Unit) {
    val (monthAbbr, dayNum) = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) { "—" to "—" }
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(30.dp),
        ) {
            Text(monthAbbr, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Text(dayNum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Scales icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Green400.copy(alpha = 0.12f)),
        ) {
            Text("⚖️", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Text(
            text       = "You fully settled up in all groups",
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = Green400,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f),
        )
    }

    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = Spacing.lg + 30.dp + Spacing.sm),
    )
}


// ── Group Balance Row ─────────────────────────────────────────────────────────

/**
 * Shows a shared group balance as an expense-like row in the friend detail timeline.
 *
 * Layout:
 *   [Mon]  [Group initial box]  [Group name]         you lent / you owe
 *   [Day]                       in group              $XX.XX
 */
@Composable
private fun GroupBalanceRow(balance: Balance, onClick: (String?) -> Unit = {}) {
    val isOwed    = balance.amount > 0
    val isSettled = balance.amount == 0.0
    val balanceColor = when {
        isSettled -> TextTertiary
        isOwed    -> Green400
        else      -> Negative
    }
    val balanceLabel = when {
        isSettled -> "settled up"
        isOwed    -> "you lent"
        else      -> "you owe"
    }
    val displayAmount = if (isSettled) "" else MoneyUtils.format(Math.abs(balance.amount), balance.currency)

    val (monthAbbr, dayNum) = remember(balance.groupLastActivity) {
        if (balance.groupLastActivity.isNullOrBlank()) {
            "—" to "—"
        } else {
            try {
                val dt = LocalDateTime.parse(balance.groupLastActivity, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
            } catch (e: Exception) {
                "—" to "—"
            }
        }
    }

    // Color the group initial box — deterministic from group name
    val boxColor = remember(balance.groupName) {
        val colors = listOf(
            android.graphics.Color.parseColor("#1A2A3A"),
            android.graphics.Color.parseColor("#1A3A1A"),
            android.graphics.Color.parseColor("#2A1A0A"),
            android.graphics.Color.parseColor("#1A1A3A"),
            android.graphics.Color.parseColor("#2A1A2A"),
        )
        val idx = (balance.groupName?.hashCode()?.and(0x7fffffff) ?: 0) % colors.size
        androidx.compose.ui.graphics.Color(colors[idx])
    }

    val initial = balance.groupName?.firstOrNull()?.uppercaseChar()?.toString() ?: "G"

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick(balance.groupId) }
            .padding(horizontal = Spacing.lg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(30.dp),
        ) {
            Text(text = monthAbbr, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Text(text = dayNum,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Group icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(boxColor),
        ) {
            Text(text = initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Group name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = balance.groupName ?: "Group",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = "in group",
                fontSize = 12.sp,
                color    = TextTertiary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Balance direction + amount (or "settled up")
        Column(horizontalAlignment = Alignment.End) {
            if (isSettled) {
                Text(text = "settled up", fontSize = 12.sp, color = TextTertiary)
            } else {
                Text(text = balanceLabel, fontSize = 10.sp, color = TextTertiary)
                Text(text = displayAmount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = balanceColor)
            }
        }
    }

    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = Spacing.lg + 30.dp + Spacing.sm),
    )
}

// ── Friend Cover Photo ────────────────────────────────────────────────────────

@Composable
private fun FriendCoverPhoto(
    name  : String,
    userId: String,
    email : String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            ),
    ) {
        // Scrim at bottom for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))
                ),
        )

        // Bottom: avatar + name + email
        Column(
            modifier            = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            FsAvatar(
                name   = name,
                userId = userId,
                size   = 56.dp,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text       = name,
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
            )
            if (email.isNotBlank()) {
                Text(
                    text     = email,
                    fontSize = 13.sp,
                    color    = Color(0xCCFFFFFF),
                )
            }
        }
    }
}

// ── Friend Balance Card ───────────────────────────────────────────────────────

@Composable
private fun FriendBalanceCard(
    netBalance      : Double,
    currency        : String,
    groupBalances   : List<Balance>,
    nonGroupBalance : Double,
    friendName      : String,
    onSettle        : () -> Unit,
    modifier        : Modifier = Modifier,
) {
    val balanceColor = when {
        netBalance > 0 -> Green400
        netBalance < 0 -> Negative
        else           -> TextSecondary
    }
    val balanceText = when {
        netBalance > 0 -> "owes you ${MoneyUtils.format(netBalance, currency)} overall"
        netBalance < 0 -> "you owe ${MoneyUtils.format(-netBalance, currency)} overall"
        else           -> "All settled up"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(Spacing.md),
    ) {
        // Overall balance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            Text(
                text     = balanceText,
                fontSize = 13.sp,
                color    = balanceColor,
                modifier = Modifier.weight(1f),
            )
        }

        // Per-group and non-group breakdown
        val nonZeroBalances  = groupBalances.filter { it.amount != 0.0 }
        val showNonGroup     = Math.abs(nonGroupBalance) > 0.01
        val showBreakdown    = nonZeroBalances.isNotEmpty() || showNonGroup

        if (showBreakdown) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Per-group rows
            nonZeroBalances.forEach { balance ->
                val label = balance.groupName ?: "Group"
                val color = if (balance.amount > 0) Green400 else Negative
                val text  = if (balance.amount > 0)
                    "owes you ${MoneyUtils.format(balance.amount, balance.currency)}"
                else
                    "you owe ${MoneyUtils.format(-balance.amount, balance.currency)}"

                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text     = "in \"$label\"",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text       = text,
                        fontSize   = 12.sp,
                        color      = color,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Non-group row — appears after a partial settle-all that doesn't
            // clear group balances. E.g. "you owe $1.00 in non-group expenses"
            if (showNonGroup) {
                val color = if (nonGroupBalance > 0) Green400 else Negative
                val text  = if (nonGroupBalance > 0)
                    "owes you ${MoneyUtils.format(nonGroupBalance, currency)}"
                else
                    "you owe ${MoneyUtils.format(-nonGroupBalance, currency)}"

                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text     = "in non-group expenses",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text       = text,
                        fontSize   = 12.sp,
                        color      = color,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ── Action Pills ──────────────────────────────────────────────────────────────

@Composable
private fun FriendActionPills(
    onSettleUp           : () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    modifier             : Modifier = Modifier,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Settle up
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Green400)
                .clickable { onSettleUp() }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(text = "Settle up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Surface0)
        }

        // Charts pill
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Surface2)
                .clickable { onNavigateToAnalytics() }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(text = "Charts", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ── Expense Row ───────────────────────────────────────────────────────────────

@Composable
private fun FriendExpenseRow(expense: Expense, onClick: () -> Unit) {
    val youLent = expense.yourBalance > 0
    val youOwe  = expense.yourBalance < 0

    val balanceColor = when {
        youLent -> Green400
        youOwe  -> Negative
        else    -> TextTertiary
    }
    val balanceLabel = when {
        youLent -> "you lent"
        youOwe  -> "you owe"
        else    -> "settled"
    }
    val balanceAmount = when {
        youLent -> MoneyUtils.format(expense.yourBalance, expense.currency)
        youOwe  -> MoneyUtils.format(-expense.yourBalance, expense.currency)
        else    -> ""
    }

    val paidByText = expense.payers.firstOrNull()?.let { payer ->
        "${payer.fullName} paid ${MoneyUtils.format(payer.amountPaid, expense.currency)}"
    } ?: "${expense.addedByName} paid ${MoneyUtils.format(expense.totalAmount, expense.currency)}"

    val (monthAbbr, dayNum) = remember(expense.expenseDate) {
        try {
            val dt = LocalDateTime.parse(expense.expenseDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) { "" to "" }
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(30.dp),
        ) {
            Text(text = monthAbbr, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Text(text = dayNum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Category emoji
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(categoryBgColor(expense.category)),
        ) {
            Text(text = categoryEmoji(expense.category), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Description + context subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = expense.description,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (expense.groupName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = expense.groupName,
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text     = " · group",
                        fontSize = 11.sp,
                        color    = TextTertiary,
                    )
                }
            } else {
                Text(text = paidByText, fontSize = 12.sp, color = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Balance
        if (expense.yourBalance != 0.0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = balanceLabel,  fontSize = 10.sp, color = TextTertiary)
                Text(text = balanceAmount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = balanceColor)
            }
        }
    }

    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = Spacing.lg + 28.dp + Spacing.sm),
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toMonthHeader(): String {
    return try {
        val dt = LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (e: Exception) { "Earlier" }
}

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    ExpenseCategory.GAMES        -> "🎮"
    ExpenseCategory.MOVIES       -> "🎬"
    ExpenseCategory.MUSIC        -> "🎵"
    ExpenseCategory.SPORTS       -> "⚽"
    ExpenseCategory.DINING_OUT   -> "🍽️"
    ExpenseCategory.GROCERIES    -> "🛒"
    ExpenseCategory.LIQUOR       -> "🍺"
    ExpenseCategory.ELECTRONICS  -> "📱"
    ExpenseCategory.FURNITURE    -> "🛋️"
    ExpenseCategory.HOUSEHOLD_SUPPLIES -> "🧹"
    ExpenseCategory.MAINTENANCE  -> "🔧"
    ExpenseCategory.MORTGAGE     -> "🏦"
    ExpenseCategory.PETS         -> "🐾"
    ExpenseCategory.RENT         -> "🏠"
    ExpenseCategory.SERVICES     -> "🛠️"
    ExpenseCategory.CHILDCARE    -> "👶"
    ExpenseCategory.CLOTHING     -> "👕"
    ExpenseCategory.EDUCATION    -> "📚"
    ExpenseCategory.GIFTS        -> "🎁"
    ExpenseCategory.INSURANCE    -> "🛡️"
    ExpenseCategory.MEDICAL      -> "💊"
    ExpenseCategory.TAXES        -> "🧾"
    ExpenseCategory.BICYCLE      -> "🚲"
    ExpenseCategory.BUS_TRAIN    -> "🚌"
    ExpenseCategory.CAR          -> "🚗"
    ExpenseCategory.GAS_FUEL     -> "⛽"
    ExpenseCategory.HOTEL        -> "🏨"
    ExpenseCategory.PARKING      -> "🅿️"
    ExpenseCategory.PLANE        -> "✈️"
    ExpenseCategory.TAXI         -> "🚕"
    ExpenseCategory.CLEANING     -> "🧽"
    ExpenseCategory.ELECTRICITY  -> "⚡"
    ExpenseCategory.HEAT_GAS     -> "🔥"
    ExpenseCategory.TRASH        -> "🗑️"
    ExpenseCategory.TV_PHONE_INTERNET -> "📺"
    ExpenseCategory.WATER        -> "💧"
    else                         -> "💰"
}

private fun categoryBgColor(category: ExpenseCategory?): Color = when (category) {
    ExpenseCategory.DINING_OUT, ExpenseCategory.GROCERIES, ExpenseCategory.LIQUOR ->
        Color(0xFF1A2A1A)
    ExpenseCategory.CAR, ExpenseCategory.TAXI, ExpenseCategory.PLANE,
    ExpenseCategory.GAS_FUEL, ExpenseCategory.BUS_TRAIN, ExpenseCategory.BICYCLE,
    ExpenseCategory.PARKING ->
        Color(0xFF2A1A0A)
    ExpenseCategory.RENT, ExpenseCategory.MORTGAGE ->
        Color(0xFF1A2A3A)
    ExpenseCategory.ELECTRICITY, ExpenseCategory.WATER, ExpenseCategory.TV_PHONE_INTERNET,
    ExpenseCategory.HEAT_GAS, ExpenseCategory.CLEANING, ExpenseCategory.TRASH ->
        Color(0xFF1A1A3A)
    ExpenseCategory.GAMES, ExpenseCategory.MOVIES, ExpenseCategory.MUSIC, ExpenseCategory.SPORTS ->
        Color(0xFF2A1A2A)
    ExpenseCategory.MEDICAL ->
        Color(0xFF2A1A1A)
    else -> Color(0xFF1E1E20)
}