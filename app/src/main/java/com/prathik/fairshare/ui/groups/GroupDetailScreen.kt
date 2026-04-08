package com.prathik.fairshare.ui.groups

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSecondaryButton
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextDisabled
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Group Detail Screen — redesigned.
 *
 * Layout:
 * - Top bar: back + group name + search + settings (round icon)
 * - Balance card: "You are owed $X overall" + per-person breakdown with avatars
 * - Action pills row: Settle up (green) · Balances · Charts · Totals · Export
 * - Expense list grouped by month with date column + emoji icon + description + amount
 * - FAB: add expense
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
    onNavigateToExpense: (String) -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToAddMember: (String) -> Unit,
    onNavigateToSettle: (otherUserId: String, payerId: String?) -> Unit,
    onNavigateToSettlement: (String) -> Unit = {},
    onNavigateToBalances: () -> Unit = {},
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val groupState by viewModel.groupState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val yourBalance by viewModel.yourBalance.collectAsState()
    val settlementActionState by viewModel.settlementActionState.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading = groupState is GroupDetailUiState.Loading

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(settlementActionState) {
        when (val s = settlementActionState) {
            is SettlementActionState.Deleted -> {
                snackbarHostState.showSnackbar("Settlement deleted")
                viewModel.resetSettlementActionState()
            }
            is SettlementActionState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetSettlementActionState()
            }
            else -> Unit
        }
    }

    val groupName = (groupState as? GroupDetailUiState.Success)?.group?.name ?: "Group"
    val groupType = (groupState as? GroupDetailUiState.Success)?.group?.type ?: GroupType.OTHER

    // ── Settle up sheet state ─────────────────────────────────────────────────
    // Step 1: always show person list (even 1 person) + "More options"
    var showSettleSheet    by remember { mutableStateOf(false) }
    // Step 2 (More options): select payer from all group members
    var showPayerSheet     by remember { mutableStateOf(false) }
    // Step 3: select recipient (payer faded), then navigate
    var showRecipientSheet by remember { mutableStateOf(false) }
    var selectedPayerId    by remember { mutableStateOf<String?>(null) }
    var selectedPayerName  by remember { mutableStateOf("") }

    val handleSettleUp: () -> Unit = {
        if (balances.isNotEmpty()) showSettleSheet = true
        // if all settled up: nothing to do (button should be disabled/hidden upstream)
    }

    // Cover height in px — used to detect when cover scrolls away
    val coverHeightDp = 220
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Show top bar name only once the cover (220dp) has scrolled past
    val showTopBarName by remember {
        derivedStateOf {
            val firstVisible = lazyListState.firstVisibleItemIndex
            val offset = lazyListState.firstVisibleItemScrollOffset
            // Cover is item 0 — once it's scrolled past OR offset is large enough
            firstVisible > 0 || offset > 400
        }
    }

    // Top gradient color per group type — used for top bar background
    val topBarColor = coverTopColor(groupType)

    // Auto-refresh expenses every time screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshExpenses()
        }
    }

    Scaffold(
        containerColor = Surface0,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddExpense(viewModel.groupId) },
                containerColor = Green400,
                contentColor = Surface0,
                shape = RoundedCornerShape(Radius.lg),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    modifier = Modifier.size(24.dp),
                )
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
                onRefresh = { viewModel.loadData() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val state = groupState) {
                    is GroupDetailUiState.Loading -> FsLoadingScreen()

                    is GroupDetailUiState.Error -> FsErrorScreen(
                        message = state.message,
                        isNetwork = state.isNetwork,
                        onRetry = { viewModel.loadData() },
                    )

                    is GroupDetailUiState.Success -> {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                        ) {

                            // ── Cover photo ───────────────────────────────────────
                            item {
                                GroupCoverPhoto(
                                    group = state.group,
                                )
                            }

                            // ── Balance card ──────────────────────────────────────
                            item {
                                BalanceCard(
                                    yourBalance = yourBalance,
                                    balances = balances,
                                    currency = "USD",
                                    onSettleUser = { otherUserId -> onNavigateToSettle(otherUserId, null) },
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical = Spacing.md,
                                    ),
                                )
                            }

                            // ── Action pills row ──────────────────────────────────
                            item {
                                ActionPillsRow(
                                    onSettleUp       = handleSettleUp,
                                    onNavigateToBalances = onNavigateToBalances,
                                    modifier = Modifier.padding(
                                        start = Spacing.lg,
                                        end = Spacing.lg,
                                        bottom = Spacing.md,
                                    ),
                                )
                            }

                            // ── Expenses ──────────────────────────────────────────
                            when (val expenses = expensesState) {
                                is ExpensesUiState.Loading -> {
                                    item { FsLoadingScreen(modifier = Modifier.height(200.dp)) }
                                }

                                is ExpensesUiState.Error -> {
                                    item {
                                        FsErrorScreen(
                                            message = expenses.message,
                                            isNetwork = expenses.isNetwork,
                                            onRetry = { viewModel.refreshExpenses() },
                                        )
                                    }
                                }

                                is ExpensesUiState.Success -> {
                                    // Build unified timeline regardless of expense count
                                    val timeline = buildList {
                                        expenses.expenses.forEach { add(TimelineItem.ExpenseItem(it)) }
                                        // Settlements intentionally excluded from group timeline.
                                        // Friend-level payments that touch a group should not
                                        // appear here — they are visible in the friend's timeline.
                                    }.sortedByDescending { it.date }

                                    if (timeline.isEmpty()) {
                                        item {
                                            if (state.group.memberCount <= 1) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = Spacing.lg)
                                                        .clip(RoundedCornerShape(Radius.xl))
                                                        .background(Surface2)
                                                        .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
                                                ) {
                                                    Text(
                                                        text = "You're the only one here!",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                    Spacer(modifier = Modifier.height(Spacing.xl))
                                                    FsPrimaryButton(
                                                        text = "Add group members",
                                                        onClick = { onNavigateToAddMember(viewModel.groupId) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                    Spacer(modifier = Modifier.height(Spacing.md))
                                                    FsSecondaryButton(
                                                        text = "Share group link",
                                                        onClick = { /* TODO: share invite code */ },
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                }
                                            } else {
                                                FsEmptyState(
                                                    title = "No expenses yet",
                                                    subtitle = "Add the first expense to get started",
                                                    modifier = Modifier.height(300.dp),
                                                )
                                            }
                                        }
                                    } else {
                                        val grouped = timeline.groupBy { it.date.toMonthHeader() }

                                        grouped.forEach { (monthHeader, items) ->
                                            item {
                                                Text(
                                                    text = monthHeader.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextTertiary,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.padding(
                                                        horizontal = Spacing.lg,
                                                        vertical = Spacing.sm,
                                                    ),
                                                )
                                            }
                                            items(
                                                items = items,
                                                key = { when (it) {
                                                    is TimelineItem.ExpenseItem -> "e_${it.expense.id}"
                                                    is TimelineItem.SettlementItem -> "s_${it.settlement.id}"
                                                }},
                                            ) { item ->
                                                when (item) {
                                                    is TimelineItem.ExpenseItem -> ExpenseRow(
                                                        expense = item.expense,
                                                        onClick = { onNavigateToExpense(item.expense.id) },
                                                    )
                                                    is TimelineItem.SettlementItem -> SettlementRow(
                                                        settlement = item.settlement,
                                                        onClick    = { onNavigateToSettlement(item.settlement.id) },
                                                        onDelete   = { viewModel.deleteSettlement(item.settlement.id) },
                                                    )
                                                }
                                            }
                                        }

                                        item { Spacer(modifier = Modifier.height(80.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Fixed overlay top bar — always visible, stays on scroll ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (showTopBarName) topBarColor else Color.Transparent)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .offset(y = (-20).dp)
                    .padding(horizontal = Spacing.lg, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                        .clickable { onBack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Group name — only visible once cover name scrolls away
                if (showTopBarName) {
                    Text(
                        text = groupName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
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
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                            .clickable { onNavigateToSearch(viewModel.groupId) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                            .clickable { onNavigateToSettings(viewModel.groupId) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } // end outer Box
    }

    // ── Settle Up — Balance Picker Sheet ──────────────────────────────────────
    // ── Sheet 1: Who are you settling with? ──────────────────────────────────
    if (showSettleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettleSheet = false },
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Settle up",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)

                // One row per person you have a balance with
                balances.filter { it.amount != 0.0 }.forEach { balance ->
                    val isOwed = balance.amount > 0
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettleSheet = false
                                onNavigateToSettle(balance.otherUserId, null)
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = balance.otherUserName, userId = balance.otherUserId,
                            size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(balance.otherUserName, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                text     = if (isOwed) "owes you ${MoneyUtils.format(balance.amount, balance.currency)}"
                                else "you owe ${MoneyUtils.format(-balance.amount, balance.currency)}",
                                fontSize = 12.sp, color = TextSecondary,
                            )
                        }
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint               = TextTertiary,
                            modifier           = Modifier.size(14.dp),
                        )
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md))
                }

                // More options — custom payer/recipient
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettleSheet = false
                            showPayerSheet  = true
                        }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(ComponentSize.avatarMd)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Surface3),
                    ) {
                        Text("⋯", fontSize = 18.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text("More options", fontSize = 15.sp,
                        fontWeight = FontWeight.Medium, color = TextSecondary)
                }
            }
        }
    }

    // ── Sheet 2: Select payer ─────────────────────────────────────────────────
    if (showPayerSheet) {
        ModalBottomSheet(
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
                members.forEach { member ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPayerId   = member.userId
                                selectedPayerName = member.fullName
                                showPayerSheet    = false
                                showRecipientSheet = true
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = member.fullName, userId = member.userId,
                            size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(member.fullName, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary,
                            modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md))
                }
            }
        }
    }

    // ── Sheet 3: Select recipient (payer faded) ───────────────────────────────
    if (showRecipientSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecipientSheet = false },
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Who received?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                members.forEach { member ->
                    val isPayer = member.userId == selectedPayerId
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isPayer) Modifier.clickable {
                                    showRecipientSheet = false
                                    onNavigateToSettle(member.userId, selectedPayerId)
                                } else Modifier
                            )
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = member.fullName, userId = member.userId,
                            size = ComponentSize.avatarMd,
                            modifier = Modifier.alpha(if (isPayer) 0.35f else 1f))
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.fullName, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isPayer) TextTertiary else TextPrimary)
                            if (isPayer) Text("paying", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md))
                }
            }
        }
    }
}

// ── Group Cover Photo ─────────────────────────────────────────────────────────

@Composable
private fun GroupCoverPhoto(
    group: Group,
) {
    val colors = coverGradientColors(group.type)
    val emoji = coverEmoji(group.type)
    val style = coverEmojiStyle(group.type)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.linearGradient(colors)),
    ) {
        // Large emoji — unique position per group type
        Text(
            text = emoji,
            fontSize = style.fontSize.sp,
            modifier = Modifier
                .align(style.alignment)
                .offset(x = style.offset.first.dp, y = style.offset.second.dp)
                .graphicsLayer { },
        )

        // Scrim at bottom for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xDD000000))
                    )
                ),
        )

        // Bottom: group name + member avatars + type pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            Text(
                text = group.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(modifier = Modifier.width((group.memberCount.coerceAtMost(4) * 18 + 10).dp)) {
                    repeat(group.memberCount.coerceAtMost(4)) { i ->
                        Box(
                            modifier = Modifier
                                .offset(x = (i * 18).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x55FFFFFF))
                        )
                    }
                }
                Text(
                    text = "${group.memberCount} members",
                    fontSize = 12.sp,
                    color = Color(0xCCFFFFFF),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Color(0x44FFFFFF))
                        .padding(horizontal = Spacing.sm, vertical = 3.dp),
                ) {
                    Text(
                        text = group.type.name,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ── Cover helpers ─────────────────────────────────────────────────────────────

/** Returns the top/darkest color of the cover gradient — used for the fixed top bar background */
private fun coverTopColor(type: GroupType): Color = when (type) {
    GroupType.TRIP -> Color(0xFF0D2137)
    GroupType.HOME -> Color(0xFF1A0D2E)
    GroupType.OFFICE -> Color(0xFF1A1000)
    GroupType.FRIENDS -> Color(0xFF0D1F0D)
    GroupType.COUPLE -> Color(0xFF1F0D1A)
    GroupType.EVENT -> Color(0xFF1A1000)
    GroupType.APARTMENT -> Color(0xFF0D1A1F)
    GroupType.OTHER -> Color(0xFF111112)
}

private fun coverGradientColors(type: GroupType): List<Color> = when (type) {
    GroupType.TRIP -> listOf(Color(0xFF0D2137), Color(0xFF1A3A5C), Color(0xFF0D4A6B))
    GroupType.HOME -> listOf(Color(0xFF1A0D2E), Color(0xFF2E1A4A), Color(0xFF3D1A5C))
    GroupType.OFFICE -> listOf(Color(0xFF1A1000), Color(0xFF2E1E00), Color(0xFF3D2800))
    GroupType.FRIENDS -> listOf(Color(0xFF0D1F0D), Color(0xFF1A3020), Color(0xFF1A3D1A))
    GroupType.COUPLE -> listOf(Color(0xFF1F0D1A), Color(0xFF2E1022), Color(0xFF3D1A2E))
    GroupType.EVENT -> listOf(Color(0xFF1A1000), Color(0xFF2A1800), Color(0xFF3A2200))
    GroupType.APARTMENT -> listOf(Color(0xFF0D1A1F), Color(0xFF0D2A30), Color(0xFF0A2D35))
    GroupType.OTHER -> listOf(Color(0xFF111112), Color(0xFF1A1A1C), Color(0xFF222222))
}

private fun coverEmoji(type: GroupType): String = when (type) {
    GroupType.TRIP -> "✈️"
    GroupType.HOME -> "🏠"
    GroupType.OFFICE -> "💼"
    GroupType.FRIENDS -> "👫"
    GroupType.COUPLE -> "💑"
    GroupType.EVENT -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER -> "💰"
}

/**
 * Returns (alignment, rotation, fontSize, offsetXY) per group type.
 * Emoji is large and positioned in the upper-center area of the cover.
 */
private fun coverEmojiStyle(type: GroupType): CoverEmojiStyle = when (type) {
    // Airplane: center-top area, tilted flying in from right
    GroupType.TRIP -> CoverEmojiStyle(Alignment.TopCenter, -15f, 80, Pair(20, 50))
    // House: center, slightly above mid
    GroupType.HOME -> CoverEmojiStyle(Alignment.Center, 0f, 82, Pair(0, -20))
    // Briefcase: center, slightly right
    GroupType.OFFICE -> CoverEmojiStyle(Alignment.Center, 0f, 78, Pair(20, -10))
    // Friends: center-top
    GroupType.FRIENDS -> CoverEmojiStyle(Alignment.TopCenter, 8f, 76, Pair(-10, 52))
    // Couple: center
    GroupType.COUPLE -> CoverEmojiStyle(Alignment.Center, 0f, 80, Pair(0, -16))
    // Event: top-center, rotated, oversized
    GroupType.EVENT -> CoverEmojiStyle(Alignment.TopCenter, 15f, 86, Pair(16, 44))
    // Apartment: center
    GroupType.APARTMENT -> CoverEmojiStyle(Alignment.Center, 0f, 76, Pair(0, -20))
    // Other: center
    GroupType.OTHER -> CoverEmojiStyle(Alignment.Center, 0f, 74, Pair(0, -16))
}

private data class CoverEmojiStyle(
    val alignment: Alignment,
    val rotation: Float,
    val fontSize: Int,
    val offset: Pair<Int, Int>, // x, y in dp
)

// ── Balance Card ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    yourBalance: Double,
    balances: List<Balance>,
    currency: String,
    onSettleUser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overallColor = when {
        yourBalance > 0 -> Green400
        yourBalance < 0 -> Negative
        else -> TextSecondary
    }

    val overallText = when {
        yourBalance > 0 -> "You are owed ${MoneyUtils.format(yourBalance, currency)} overall"
        yourBalance < 0 -> "You owe ${MoneyUtils.format(-yourBalance, currency)} overall"
        balances.all { it.amount == 0.0 } -> "All settled up"
        else -> "Expenses are balanced"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text     = overallText,
            fontSize = 13.sp,
            color    = overallColor,
        )
    }
}

// ── Action Pills Row ──────────────────────────────────────────────────────────

@Composable
private fun ActionPillsRow(
    onSettleUp          : () -> Unit,
    onNavigateToBalances: () -> Unit = {},
    modifier            : Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Settle up — green primary pill
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Green400)
                .clickable { onSettleUp() }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(
                text = "Settle up",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Surface0,
            )
        }

        // Balances — navigates to full member balance breakdown
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Surface2)
                .clickable { onNavigateToBalances() }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(
                text = "Balances",
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }

        // Info pills — placeholder for now
        listOf("Charts", "Totals").forEach { label ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Surface2)
                    .clickable { /* TODO */ }
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }

        // Export — muted placeholder
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Surface2)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(
                text = "Export",
                fontSize = 12.sp,
                color = TextDisabled,
            )
        }
    }
}

// ── Expense Row ───────────────────────────────────────────────────────────────

@Composable
private fun ExpenseRow(
    expense: Expense,
    onClick: () -> Unit,
) {
    val youLent = expense.yourBalance > 0
    val youOwe = expense.yourBalance < 0

    val balanceLabel = when {
        youLent -> "you lent"
        youOwe -> "you owe"
        else -> "settled"
    }
    val balanceColor = when {
        youLent -> Green400
        youOwe -> Negative
        else -> TextTertiary
    }
    val balanceAmount = when {
        youLent -> MoneyUtils.format(expense.yourBalance, expense.currency)
        youOwe -> MoneyUtils.format(-expense.yourBalance, expense.currency)
        else -> ""
    }

    // "Who paid" subtitle
    val paidByText = expense.payers.firstOrNull()?.let { payer ->
        "${payer.fullName} paid ${MoneyUtils.format(payer.amountPaid, expense.currency)}"
    } ?: "${expense.addedByName} paid ${MoneyUtils.format(expense.totalAmount, expense.currency)}"

    // Date components
    val (monthAbbr, dayNum) = remember(expense.expenseDate) {
        try {
            val dt = LocalDateTime.parse(expense.expenseDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val month = dt.format(DateTimeFormatter.ofPattern("MMM"))
            val day = dt.dayOfMonth.toString()
            month to day
        } catch (e: Exception) {
            "" to ""
        }
    }

    // Emoji background tint based on category
    val emojiBg = categoryBgColor(expense.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(30.dp),
        ) {
            Text(
                text = monthAbbr,
                fontSize = 10.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = dayNum,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Category emoji box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(emojiBg),
        ) {
            Text(
                text = categoryEmoji(expense.category),
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Description + who paid
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.description,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = paidByText,
                fontSize = 12.sp,
                color = TextTertiary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Balance column
        if (expense.yourBalance != 0.0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = balanceLabel,
                    fontSize = 10.sp,
                    color = TextTertiary,
                )
                Text(
                    text = balanceAmount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor,
                )
            }
        }
    }

    HorizontalDivider(
        color = Surface3,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = Spacing.lg + 28.dp + Spacing.sm),
    )
}

// ── Settlement Row ────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SettlementRow(settlement: Settlement, onClick: () -> Unit, onDelete: () -> Unit) {
    val (monthAbbr, dayNum) = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val month = dt.format(DateTimeFormatter.ofPattern("MMM"))
            val day = dt.dayOfMonth.toString()
            month to day
        } catch (e: Exception) {
            "" to ""
        }
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
        modifier = Modifier
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
            modifier = Modifier.width(30.dp),
        ) {
            Text(monthAbbr, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Text(dayNum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Payment icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Green400.copy(alpha = 0.12f)),
        ) {
            Text("🤝", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${settlement.payerName} paid ${settlement.receiverName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Green400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Payment",
                fontSize = 12.sp,
                color = TextTertiary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Amount
        Text(
            text = MoneyUtils.format(settlement.amount, settlement.currency),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Green400,
        )
    }

    HorizontalDivider(
        color = Surface3,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = Spacing.lg + 28.dp + Spacing.sm),
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toMonthHeader(): String {
    return try {
        val dateTime = LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (e: Exception) {
        "Earlier"
    }
}

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    ExpenseCategory.DINING_OUT -> "🍽️"
    ExpenseCategory.GROCERIES -> "🛒"
    ExpenseCategory.CAR -> "🚗"
    ExpenseCategory.TAXI -> "🚕"
    ExpenseCategory.PLANE -> "✈️"
    ExpenseCategory.HOTEL -> "🏨"
    ExpenseCategory.RENT -> "🏠"
    ExpenseCategory.ELECTRICITY -> "⚡"
    ExpenseCategory.WATER -> "💧"
    ExpenseCategory.GAS_FUEL -> "⛽"
    ExpenseCategory.TV_PHONE_INTERNET -> "📱"
    ExpenseCategory.MOVIES -> "🎬"
    ExpenseCategory.GAMES -> "🎮"
    ExpenseCategory.MUSIC -> "🎵"
    ExpenseCategory.SPORTS -> "⚽"
    ExpenseCategory.MEDICAL -> "💊"
    ExpenseCategory.EDUCATION -> "📚"
    ExpenseCategory.GIFTS -> "🎁"
    ExpenseCategory.LIQUOR -> "🍺"
    ExpenseCategory.PETS -> "🐾"
    ExpenseCategory.CLOTHING -> "👕"
    ExpenseCategory.BUS_TRAIN -> "🚌"
    ExpenseCategory.PARKING -> "🅿️"
    else -> "💰"
}

private fun categoryBgColor(category: ExpenseCategory?): androidx.compose.ui.graphics.Color {
    return when (category) {
        ExpenseCategory.DINING_OUT, ExpenseCategory.GROCERIES,
        ExpenseCategory.LIQUOR -> androidx.compose.ui.graphics.Color(0xFF1A2A1A)

        ExpenseCategory.RENT, ExpenseCategory.MORTGAGE -> androidx.compose.ui.graphics.Color(
            0xFF1A2A3A
        )

        ExpenseCategory.TAXI, ExpenseCategory.CAR,
        ExpenseCategory.BUS_TRAIN, ExpenseCategory.PLANE -> androidx.compose.ui.graphics.Color(
            0xFF2A1A0A
        )

        ExpenseCategory.ELECTRICITY, ExpenseCategory.WATER,
        ExpenseCategory.GAS_FUEL,
        ExpenseCategory.TV_PHONE_INTERNET -> androidx.compose.ui.graphics.Color(0xFF1A1A3A)

        ExpenseCategory.MOVIES, ExpenseCategory.GAMES,
        ExpenseCategory.MUSIC, ExpenseCategory.SPORTS -> androidx.compose.ui.graphics.Color(
            0xFF2A1A2A
        )

        ExpenseCategory.MEDICAL -> androidx.compose.ui.graphics.Color(0xFF2A1A1A)
        else -> androidx.compose.ui.graphics.Color(0xFF1E1E20)
    }
}