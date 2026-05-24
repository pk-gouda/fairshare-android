package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.Settings
import coil.compose.AsyncImage
import com.prathik.fairshare.R
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsDetailSkeleton
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSecondaryButton
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.domain.model.SettlementStatus
import androidx.compose.ui.text.style.TextDecoration
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextDisabled
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── Group UI State — drives ring, sticky bar, action bar, empty state ─────────

enum class GroupUiState {
    LOADING_DATA, // balances not yet known — show neutral loading state
    SOLO,         // 1 member — no expenses possible, add friends
    NEW_GROUP,    // 2+ members, 0 expenses — ready to split
    ACTIVE_DEBT,  // has expenses, netBalance != 0
    ALL_SETTLED,  // has expenses, netBalance == 0 — confirmed from cache or network
}


// ── Cover image pool — 24 bundled drawable resources ─────────────────────────
// Images are bucketed by group type so the cover feels intentional.
// Within each bucket, the group ID hash picks a specific image —
// so two HOME groups will always get different covers, but both look like home photos.
//
// Bucket layout (0-based indices into DEFAULT_COVERS):
//   HOME      : 0–4   (cover_01–05)
//   APARTMENT : 5–8   (cover_06–09)
//   TRIP      : 9–21  (cover_10–22)
//   OFFICE    : 22–25 (cover_23–26)
//   FRIENDS   : 26–31 (cover_27–32)
//   COUPLE    : 32–37 (cover_33–38)
//   EVENT     : 38–42 (cover_39–43)
//   OTHER     : 43–48 (cover_44–49)

private val DEFAULT_COVERS: List<Int> = listOf(
    // HOME (0–4)
    R.drawable.cover_01, R.drawable.cover_02, R.drawable.cover_03, R.drawable.cover_04, R.drawable.cover_05,
    // APARTMENT (5–8)
    R.drawable.cover_06, R.drawable.cover_07, R.drawable.cover_08, R.drawable.cover_09,
    // TRIP (9–21)
    R.drawable.cover_10, R.drawable.cover_11, R.drawable.cover_12, R.drawable.cover_13, R.drawable.cover_14,
    R.drawable.cover_15, R.drawable.cover_16, R.drawable.cover_17, R.drawable.cover_18, R.drawable.cover_19,
    R.drawable.cover_20, R.drawable.cover_21, R.drawable.cover_22,
    // OFFICE (22–25)
    R.drawable.cover_23, R.drawable.cover_24, R.drawable.cover_25, R.drawable.cover_26,
    // FRIENDS (26–31)
    R.drawable.cover_27, R.drawable.cover_28, R.drawable.cover_29, R.drawable.cover_30, R.drawable.cover_31, R.drawable.cover_32,
    // COUPLE (32–37)
    R.drawable.cover_33, R.drawable.cover_34, R.drawable.cover_35, R.drawable.cover_36, R.drawable.cover_37, R.drawable.cover_38,
    // EVENT (38–42)
    R.drawable.cover_39, R.drawable.cover_40, R.drawable.cover_41, R.drawable.cover_42, R.drawable.cover_43,
    // OTHER (43–48)
    R.drawable.cover_44, R.drawable.cover_45, R.drawable.cover_46, R.drawable.cover_47, R.drawable.cover_48, R.drawable.cover_49,
)

private fun groupTypeIndices(type: GroupType): IntRange = when (type) {
    GroupType.HOME      -> 0..4
    GroupType.APARTMENT -> 5..8
    GroupType.TRIP      -> 9..21
    GroupType.OFFICE    -> 22..25
    GroupType.FRIENDS   -> 26..31
    GroupType.COUPLE    -> 32..37
    GroupType.EVENT     -> 38..42
    GroupType.OTHER     -> 43..48
}

/** Returns a drawable resource ID — type-biased so it looks right, ID-hashed so each group is unique. */
private fun groupCoverRes(group: Group): Int {
    if (!group.groupImage.isNullOrBlank()) return -1 // handled separately
    var hash = 0
    for (ch in group.id) {
        hash = ((hash shl 5) - hash) + ch.code
        hash = hash and hash
    }
    val range = groupTypeIndices(group.type)
    val indexInRange = Math.abs(hash) % range.count()
    return DEFAULT_COVERS[range.first + indexInRange]
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreen(
    onBack                : () -> Unit,
    onNavigateToSettings  : (String) -> Unit,
    onNavigateToSearch    : (String) -> Unit = {},
    onNavigateToExpense   : (String) -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToAddMember : (String) -> Unit,
    onNavigateToSettle    : (otherUserId: String, payerId: String?, payerName: String?, currency: String?) -> Unit,
    onNavigateToSettlement: (String) -> Unit = {},
    onNavigateToBalances  : () -> Unit = {},
    onNavigateToAnalytics : () -> Unit = {},
    viewModel             : GroupDetailViewModel = hiltViewModel(),
) {
    val groupState           by viewModel.groupState.collectAsState()
    val expensesState        by viewModel.expensesState.collectAsState()
    val settlements          by viewModel.settlements.collectAsState()
    val balances             by viewModel.balances.collectAsState()
    val yourBalance          by viewModel.yourBalance.collectAsState()
    val settlementActionState by viewModel.settlementActionState.collectAsState()
    val members              by viewModel.members.collectAsState()
    val pendingExpenseIds      by viewModel.pendingExpenseIds.collectAsState()
    val pendingDeleteExpenseIds by viewModel.pendingDeleteExpenseIds.collectAsState()
    val balancesLoadFailed      by viewModel.balancesLoadFailed.collectAsState()
    val balancesLoaded          by viewModel.balancesLoaded.collectAsState()
    val optimisticYourBalance   by viewModel.optimisticYourBalance.collectAsState()
    val hasPendingBalanceSync      by viewModel.hasPendingBalanceSync.collectAsState()
    val optimisticBalanceCurrency  by viewModel.optimisticBalanceCurrency.collectAsState()
    // Optimistic balance: use pending-adjusted balance when available.
    val effectiveYourBalance = optimisticYourBalance ?: yourBalance
    val effectiveCurrency    = optimisticBalanceCurrency ?: (balances.firstOrNull()?.currency ?: "USD")
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

    // Settle up sheet state
    var showSettleSheet    by remember { mutableStateOf(false) }
    var showPayerSheet     by remember { mutableStateOf(false) }
    var showRecipientSheet by remember { mutableStateOf(false) }
    var selectedPayerId    by remember { mutableStateOf<String?>(null) }
    var selectedPayerName  by remember { mutableStateOf("") }

    val handleSettleUp: () -> Unit = {
        if (balances.isNotEmpty()) showSettleSheet = true
    }

    val groupName = (groupState as? GroupDetailUiState.Success)?.group?.name ?: ""
    val groupType = (groupState as? GroupDetailUiState.Success)?.group?.type ?: GroupType.OTHER
    val topBarColor = coverTopColor(groupType)
    val lazyListState = rememberLazyListState()

    // Show compact group name in top bar once cover scrolls away
    val showTopBarName by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > 300
        }
    }

    // Auto-refresh on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshExpenses()
        }
    }

    Scaffold(
        containerColor      = Surface0,
        contentWindowInsets = WindowInsets(0),
        snackbarHost        = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { onNavigateToAddExpense(viewModel.groupId) },
                containerColor = Green400,
                contentColor   = Surface0,
                shape          = RoundedCornerShape(16.dp), // rounded-2xl per spec
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = false,  // silent — no visible pull-refresh spinner
                onRefresh    = { viewModel.refreshExpenses() },
                modifier     = Modifier.fillMaxSize(),
                indicator    = {},     // hide drag indicator; refresh remains silent
            ) {
                when (val state = groupState) {
                    is GroupDetailUiState.Loading -> FsDetailSkeleton()
                    is GroupDetailUiState.Error   -> FsErrorScreen(
                        message   = state.message,
                        isNetwork = state.isNetwork,
                        onRetry   = { viewModel.loadData() },
                    )
                    is GroupDetailUiState.Success -> {
                        val group = state.group

                        // Derive group state
                        // ✅ Fix 2: guard against expensesState still Loading — without this,
                        // hasActivity is false while expenses are in-flight, causing the sticky
                        // bar to flash "No expenses yet" on every cold open before data arrives.
                        val hasActivity = expensesState is ExpensesUiState.Success &&
                                ((expensesState as ExpensesUiState.Success).expenses.isNotEmpty() ||
                                        settlements.isNotEmpty())

                        val expensesKnown = expensesState is ExpensesUiState.Success
                        val balanceKnown  = balancesLoaded || hasPendingBalanceSync
                        val dataStillLoading = !expensesKnown || (!balanceKnown && !balancesLoadFailed)

                        val groupUiState: GroupUiState = when {
                            group.memberCount <= 1  -> GroupUiState.SOLO
                            // Pending ops: user caused an offline change — balance bar must show.
                            hasPendingBalanceSync   -> GroupUiState.ACTIVE_DEBT
                            // Balances not yet known from cache or network — neutral state.
                            dataStillLoading        -> GroupUiState.LOADING_DATA
                            !hasActivity            -> GroupUiState.NEW_GROUP
                            // ALL_SETTLED only when balance is confirmed zero from cache or network.
                            kotlin.math.abs(effectiveYourBalance) < 0.005
                                    && balancesLoaded
                                    && !balancesLoadFailed -> GroupUiState.ALL_SETTLED
                            else                    -> GroupUiState.ACTIVE_DEBT
                        }

                        // Progress % for ring
                        val settledPct: Float = when (groupUiState) {
                            GroupUiState.LOADING_DATA,
                            GroupUiState.SOLO, GroupUiState.NEW_GROUP -> 0f
                            GroupUiState.ALL_SETTLED                  -> 1f
                            GroupUiState.ACTIVE_DEBT                  -> {
                                val settled = settlements.sumOf { it.amount }
                                val outstanding = Math.abs(effectiveYourBalance)
                                if (settled + outstanding > 0)
                                    (settled / (settled + outstanding)).toFloat().coerceIn(0f, 0.99f)
                                else 0f
                            }
                        }

                        LazyColumn(
                            state    = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // ── Cover header ──────────────────────────────────
                            item {
                                GroupCoverHeader(
                                    group           = group,
                                    settledPct      = settledPct,
                                    groupUiState    = groupUiState,
                                    yourBalance     = effectiveYourBalance,
                                    onSettingsClick = { onNavigateToSettings(viewModel.groupId) },
                                    onSearchClick   = { onNavigateToSearch(viewModel.groupId) },
                                )
                            }

                            // ── Sticky balance bar ────────────────────────────
                            stickyHeader {
                                StickyBalanceBar(
                                    yourBalance           = effectiveYourBalance,
                                    balances              = balances,
                                    currency              = effectiveCurrency,
                                    groupUiState          = groupUiState,
                                    hasPendingBalanceSync = hasPendingBalanceSync,
                                )
                            }

                            // ── Action bar ────────────────────────────────────
                            item {
                                ActionBar(
                                    onSettleUp            = handleSettleUp,
                                    onNavigateToBalances  = onNavigateToBalances,
                                    onNavigateToAnalytics = onNavigateToAnalytics,
                                    groupUiState          = groupUiState,
                                )
                            }

                            // ── Timeline ──────────────────────────────────────
                            when (val expenses = expensesState) {
                                is ExpensesUiState.Loading -> {
                                    items(4) { FsSkeletonTimelineRow() }
                                }
                                is ExpensesUiState.Error -> {
                                    item {
                                        FsErrorScreen(
                                            message   = expenses.message,
                                            isNetwork = expenses.isNetwork,
                                            onRetry   = { viewModel.refreshExpenses() },
                                        )
                                    }
                                }
                                is ExpensesUiState.Success -> {
                                    val timeline = buildList {
                                        expenses.expenses
                                            .filter { !it.isDeleted }
                                            .forEach { add(TimelineItem.ExpenseItem(it)) }
                                        settlements
                                            .filter { it.status == SettlementStatus.COMPLETED }
                                            .forEach { add(TimelineItem.SettlementItem(it)) }
                                    }.sortedByDescending { it.date }

                                    if (timeline.isEmpty()) {
                                        item {
                                            GroupEmptyState(
                                                groupUiState   = groupUiState,
                                                onAddMember    = { onNavigateToAddMember(viewModel.groupId) },
                                                onAddExpense   = { onNavigateToAddExpense(viewModel.groupId) },
                                            )
                                        }
                                    } else {
                                        // Group by month for section headers
                                        val grouped = timeline.groupBy { it.date.toMonthHeader() }

                                        grouped.forEach { (monthHeader, monthItems) ->
                                            // Month sticky header
                                            stickyHeader(key = "month_$monthHeader") {
                                                Row(
                                                    modifier          = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF0B0E13))
                                                        .padding(
                                                            horizontal = Spacing.lg,
                                                            vertical   = 6.dp,
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                                ) {
                                                    Text(
                                                        text          = monthHeader.uppercase(),
                                                        fontSize      = 12.sp,
                                                        fontWeight    = FontWeight.SemiBold,
                                                        color         = TextSecondary,
                                                        letterSpacing = 0.5.sp,
                                                    )
                                                    HorizontalDivider(
                                                        color     = Surface4,
                                                        thickness = 0.5.dp,
                                                        modifier  = Modifier.weight(1f),
                                                    )
                                                }
                                            }

                                            // Group month items by day for date rail logic
                                            val byDay = monthItems.groupBy { it.date.toDayKey() }

                                            byDay.forEach { (_, dayItems) ->
                                                items(
                                                    items = dayItems,
                                                    key = {
                                                        when (it) {
                                                            is TimelineItem.ExpenseItem    -> "e_${it.expense.id}"
                                                            is TimelineItem.SettlementItem -> "s_${it.settlement.id}"
                                                        }
                                                    },
                                                ) { item ->
                                                    // Show date rail only on first item of the day
                                                    val isFirstOfDay = item == dayItems.first()

                                                    when (item) {
                                                        is TimelineItem.ExpenseItem -> {
                                                            // Hide immediately when offline delete is queued
                                                            if (item.expense.id !in pendingDeleteExpenseIds) {
                                                                ExpenseRow(
                                                                    expense      = item.expense,
                                                                    showDateRail = isFirstOfDay,
                                                                    isPending    = item.expense.id in pendingExpenseIds,
                                                                    onClick      = { onNavigateToExpense(item.expense.id) },
                                                                )
                                                            }
                                                        }
                                                        is TimelineItem.SettlementItem ->
                                                            if (item.settlement.isFullSettle) {
                                                                GroupFullySettledRow(
                                                                    settlement   = item.settlement,
                                                                    showDateRail = isFirstOfDay,
                                                                    onClick      = { onNavigateToSettlement(item.settlement.id) },
                                                                )
                                                            } else {
                                                                SettlementRow(
                                                                    settlement   = item.settlement,
                                                                    showDateRail = isFirstOfDay,
                                                                    onClick      = { onNavigateToSettlement(item.settlement.id) },
                                                                    onDelete     = { viewModel.cancelSettlement(item.settlement.id) },
                                                                    onRestore    = { viewModel.restoreSettlement(item.settlement.id) },
                                                                )
                                                            }
                                                    }
                                                }
                                            }
                                        }

                                        item { Spacer(Modifier.height(80.dp)) }
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
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Back
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                        .clickable { onBack() },
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                // Group name — only visible once cover scrolls away
                if (showTopBarName) {
                    Text(
                        text       = groupName,
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
                            .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                            .clickable { onNavigateToSearch(viewModel.groupId) },
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
                            .background(if (showTopBarName) Color(0x66000000) else Color(0x55000000))
                            .clickable { onNavigateToSettings(viewModel.groupId) },
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

    // ── Settle Up sheets (unchanged logic) ───────────────────────────────────
    if (showSettleSheet) {
        ModalBottomSheet(onDismissRequest = { showSettleSheet = false }, containerColor = Surface2) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Settle up", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                if (balancesLoadFailed && balances.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFF9AA3AF),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Balances unavailable offline. Reconnect to refresh.",
                            fontSize = 12.sp,
                            color = Color(0xFF9AA3AF),
                        )
                    }
                }
                balances.filter { it.amount != 0.0 }.forEach { balance ->
                    val isOwed = balance.amount > 0
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { showSettleSheet = false; onNavigateToSettle(balance.otherUserId, null, null, balance.currency) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = balance.otherUserName, userId = balance.otherUserId, size = ComponentSize.avatarMd)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(balance.otherUserName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                text     = if (isOwed) "owes you ${MoneyUtils.format(balance.amount, balance.currency)}"
                                else "you owe ${MoneyUtils.format(-balance.amount, balance.currency)}",
                                fontSize = 12.sp, color = TextSecondary,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md))
                }
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { showSettleSheet = false; showPayerSheet = true }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(ComponentSize.avatarMd).clip(CircleShape).background(Surface3)) {
                        Text("⋯", fontSize = 18.sp, color = TextSecondary)
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text("More options", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                }
            }
        }
    }

    if (showPayerSheet) {
        ModalBottomSheet(onDismissRequest = { showPayerSheet = false }, containerColor = Surface2) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Who paid?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                members.forEach { member ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPayerId = member.userId; selectedPayerName = member.fullName; showPayerSheet = false; showRecipientSheet = true }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarMd)
                        Spacer(Modifier.width(Spacing.md))
                        Text(member.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md))
                }
            }
        }
    }

    if (showRecipientSheet) {
        ModalBottomSheet(onDismissRequest = { showRecipientSheet = false }, containerColor = Surface2) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Who received?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                members.forEach { member ->
                    val isPayer = member.userId == selectedPayerId
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .then(if (!isPayer) Modifier.clickable {
                                showRecipientSheet = false
                                onNavigateToSettle(member.userId, selectedPayerId, selectedPayerName, null)
                            } else Modifier)
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarMd,
                            modifier = Modifier.alpha(if (isPayer) 0.35f else 1f))
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(member.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium,
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

// ── Cover Header — photo + ring + icon + title ────────────────────────────────

@Composable
private fun GroupCoverHeader(
    group          : Group,
    settledPct     : Float,
    groupUiState   : GroupUiState = GroupUiState.NEW_GROUP,
    yourBalance    : Double = 0.0,
    onSettingsClick: () -> Unit,
    onSearchClick  : () -> Unit,
) {
    val coverRes = remember(group.id, group.groupImage) {
        if (!group.groupImage.isNullOrBlank()) null else groupCoverRes(group)
    }
    val emoji = coverEmoji(group.type)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        // Cover photo — bundled drawable or custom upload
        if (coverRes != null) {
            androidx.compose.foundation.Image(
                painter            = painterResource(id = coverRes),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model              = group.groupImage,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }

        // Dark gradient overlay — bottom-heavy
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f   to Color(0x4D0B0E13),
                        0.6f to Color(0xF20B0E13),
                        1f   to Color(0xFF0B0E13),
                    )
                )
        )

        // Content: centered icon + title + pills
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.md),
        ) {
            // Progress ring + icon
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(88.dp),
            ) {
                // Ring drawn with Canvas
                Canvas(modifier = Modifier.size(88.dp)) {
                    val stroke = 6f
                    val radius = size.minDimension / 2f - stroke / 2f

                    // Track — always drawn
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.1f),
                        radius = radius,
                        style  = Stroke(width = stroke),
                    )
                    when (groupUiState) {
                        GroupUiState.ALL_SETTLED -> {
                            // Gold gradient full ring
                            drawCircle(
                                brush  = Brush.sweepGradient(listOf(Color(0xFFF59E0B), Color(0xFFFFD700), Color(0xFFFBBF24), Color(0xFFF59E0B))),
                                radius = radius,
                                style  = Stroke(width = stroke),
                            )
                        }
                        GroupUiState.ACTIVE_DEBT -> {
                            if (settledPct > 0f) {
                                // Red arc when you owe, green arc when owed to you
                                val arcColor = if (yourBalance < 0) Color(0xFFF87171) else Color(0xFF00C896)
                                drawArc(
                                    color      = arcColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * settledPct,
                                    useCenter  = false,
                                    style      = Stroke(width = stroke, cap = StrokeCap.Round),
                                )
                            }
                        }
                        else -> { /* SOLO/NEW_GROUP — gray track only, no arc */ }
                    }
                }

                // Group icon — gold checkmark at ALL_SETTLED, emoji otherwise
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF232A34))
                        .clickable(onClick = onSettingsClick),
                ) {
                    if (groupUiState == GroupUiState.ALL_SETTLED) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            val w = size.width; val h = size.height
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(w * 0.15f, h * 0.52f)
                                    lineTo(w * 0.40f, h * 0.75f)
                                    lineTo(w * 0.85f, h * 0.25f)
                                },
                                brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))),
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round),
                            )
                        }
                    } else {
                        Text(text = emoji, fontSize = 26.sp)
                    }
                }

                // Badge — only on active debt
                if (groupUiState == GroupUiState.ACTIVE_DEBT && settledPct > 0f) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color(0x99000000), RoundedCornerShape(Radius.full))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text       = "${(settledPct * 100).toInt()}%",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Group name
            Text(
                text       = group.name,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Members pill + type pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Color(0x26FFFFFF))
                        .clickable(onClick = onSettingsClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("👥", fontSize = 12.sp)
                        Text("${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Color(0x26FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text       = group.type.name,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

// ── Sticky Balance Bar ────────────────────────────────────────────────────────

@Composable
private fun StickyBalanceBar(
    yourBalance           : Double,
    balances              : List<Balance>,
    currency              : String,
    groupUiState          : GroupUiState = GroupUiState.ACTIVE_DEBT,
    hasPendingBalanceSync : Boolean = false,
) {
    val isCentered = groupUiState == GroupUiState.LOADING_DATA ||
            groupUiState == GroupUiState.SOLO ||
            groupUiState == GroupUiState.NEW_GROUP ||
            groupUiState == GroupUiState.ALL_SETTLED

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color(0xF2111112))
            .padding(horizontal = Spacing.lg, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start,
    ) {
        when (groupUiState) {
            GroupUiState.LOADING_DATA -> {
                Text("Loading group data…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF))
            }
            GroupUiState.SOLO, GroupUiState.NEW_GROUP -> {
                Text("No expenses yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF))
            }
            GroupUiState.ALL_SETTLED -> {
                Text("All settled 🎉", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00C896))
            }
            GroupUiState.ACTIVE_DEBT -> {
                Column(modifier = Modifier.weight(1f)) {
                    if (hasPendingBalanceSync) {
                        // Show the optimistic scalar (yourBalance = effectiveYourBalance from parent)
                        // as one number while pending ops are in flight, not the stale per-currency list.
                        val label  = if (yourBalance >= 0) "Owed to you" else "You owe"
                        val color  = if (yourBalance >= 0) Color(0xFF00C896) else Color(0xFFF87171)
                        Text(label, fontSize = 10.sp, color = Color(0xFF9AA3AF))
                        Text(
                            text = MoneyUtils.format(kotlin.math.abs(yourBalance), currency),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color
                        )
                        Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
                    } else {
                        // Normal confirmed multi-currency display
                        val netByCurrency = balances.groupBy { it.currency }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                            .filter { it.value != 0.0 }
                        val owedToYou = netByCurrency.filter { it.value > 0 }
                        val youOwe    = netByCurrency.filter { it.value < 0 }
                        val posTotal  = owedToYou.values.sumOf { it }
                        val negTotal  = youOwe.values.sumOf { -it }
                        when {
                            youOwe.isEmpty() -> {
                                Text("Owed to you", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                                Text(
                                    text = owedToYou.entries.sortedByDescending { it.value }
                                        .joinToString(" + ") { (c,a) -> MoneyUtils.format(a,c) },
                                    fontSize = if (owedToYou.size > 1) 14.sp else 18.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF00C896)
                                )
                            }
                            owedToYou.isEmpty() -> {
                                Text("You owe", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                                Text(
                                    text = youOwe.entries.sortedBy { it.value }
                                        .joinToString(" + ") { (c,a) -> MoneyUtils.format(-a,c) },
                                    fontSize = if (youOwe.size > 1) 14.sp else 18.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFFF87171)
                                )
                            }
                            negTotal >= posTotal -> {
                                Text("You owe", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                                Text(
                                    text = youOwe.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(-a,c) },
                                    fontSize = if (youOwe.size > 1) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFFF87171)
                                )
                                Text(
                                    text = "You lent " + owedToYou.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(a,c) },
                                    fontSize = 11.sp, color = Color(0xFF00C896)
                                )
                            }
                            else -> {
                                Text("Owed to you", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                                Text(
                                    text = owedToYou.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(a,c) },
                                    fontSize = if (owedToYou.size > 1) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF00C896)
                                )
                                Text(
                                    text = "You owe " + youOwe.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(-a,c) },
                                    fontSize = 11.sp, color = Color(0xFFF87171)
                                )
                            }   // closes else ->
                        }       // closes when {
                    }           // closes } else { (hasPendingBalanceSync)
                }               // closes Column
                // Avatar stack — colored by balance direction
                val relevant = balances.filter { it.amount != 0.0 }.take(3)
                if (relevant.isNotEmpty()) {
                    Box(modifier = Modifier.width((relevant.size * 18 + 6).dp).height(28.dp)) {
                        relevant.forEachIndexed { i, balance ->
                            val avatarColor = if (balance.amount < 0) Color(0xFFF87171) else Color(0xFF00C896)
                            val textColor   = if (balance.amount < 0) Color.White else Color.Black
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .offset(x = (i * 18).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                            ) {
                                Text(balance.otherUserName.take(1).uppercase(), fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }
            }
            else -> { /* no-op */ }
        }
    }
    HorizontalDivider(color = Surface4, thickness = 0.5.dp)
}

// ── Action Bar ────────────────────────────────────────────────────────────────

@Composable
private fun ActionBar(
    onSettleUp           : () -> Unit,
    onNavigateToBalances : () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    groupUiState         : GroupUiState = GroupUiState.ACTIVE_DEBT,
) {
    // Settle up: only when there's active debt
    val settleEnabled    = groupUiState == GroupUiState.ACTIVE_DEBT
    // Balances/Charts: enabled once there's any history
    val secondaryEnabled = groupUiState == GroupUiState.ACTIVE_DEBT ||
            groupUiState == GroupUiState.ALL_SETTLED

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Settle up
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(if (settleEnabled) Color(0xFF00C896) else Color(0xFF232A34))
                    .then(if (settleEnabled) Modifier.clickable { onSettleUp() } else Modifier)
                    .padding(horizontal = Spacing.md, vertical = 10.dp),
            ) {
                Text("Settle up", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (settleEnabled) Color.Black else Color(0xFF6B7280))
            }

            // Secondary pills
            listOf(
                "Balances" to onNavigateToBalances,
                "Charts"   to onNavigateToAnalytics,
                "Export"   to {},
            ).forEach { (label, action) ->
                val enabled = secondaryEnabled && label != "Export"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (secondaryEnabled && label != "Export") Color(0xFF151A21) else Color(0xFF232A34))
                        .then(if (enabled) Modifier.clickable { action() } else Modifier)
                        .padding(horizontal = Spacing.md, vertical = 10.dp),
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color(0xFFE8ECF2) else Color(0xFF6B7280))
                }
            }
        }

        // Right fade edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(32.dp)
                .height(44.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF0B0E13))))
        )
    }
}

// ── Expense Row — card style with date rail ───────────────────────────────────

@Composable
private fun ExpenseRow(
    expense     : Expense,
    showDateRail: Boolean,
    isPending   : Boolean = false,
    onClick     : () -> Unit,
) {
    val youLent = expense.yourBalance > 0
    val youOwe  = expense.yourBalance < 0

    val balanceLabel  = when { youLent -> "you lent"; youOwe -> "you owe"; else -> "settled" }
    val balanceColor  = when { youLent -> Color(0xFF00C896); youOwe -> Color(0xFFF87171); else -> TextTertiary }
    val balanceAmount = when {
        youLent -> MoneyUtils.format(expense.yourBalance, expense.currency)
        youOwe  -> MoneyUtils.format(-expense.yourBalance, expense.currency)
        else    -> ""
    }

    val paidByText = expense.payers.firstOrNull()?.let {
        "${it.fullName} paid ${MoneyUtils.format(it.amountPaid, expense.currency)}"
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
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date rail — only on first item of day
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(24.dp),
        ) {
            if (showDateRail) {
                Text(monthAbbr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF), textAlign = TextAlign.Center)
                Text(dayNum, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFE8ECF2), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.width(10.dp))

        // Card
        Row(
            modifier          = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151A21))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji icon
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF232A34)),
            ) {
                Text(categoryEmoji(expense.category), fontSize = 20.sp)
            }

            Spacer(Modifier.width(10.dp))

            // Description + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = expense.description,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFE8ECF2),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text     = paidByText,
                    fontSize = 12.sp,
                    color    = Color(0xFF9AA3AF),
                )
            }

            // Sync-pending dot (Wave 2D-4)
            if (isPending) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFFFFA726), androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(6.dp))
            }

            // Balance
            if (expense.yourBalance != 0.0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(balanceLabel, fontSize = 10.sp, color = Color(0xFF9AA3AF))
                    Text(balanceAmount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = balanceColor)
                }
            }
        }
    }
}

// ── Group Fully Settled Row ───────────────────────────────────────────────────

@Composable
private fun GroupFullySettledRow(
    settlement  : Settlement,
    showDateRail: Boolean,
    onClick     : () -> Unit,
) {
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
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(24.dp),
        ) {
            if (showDateRail) {
                Text(monthAbbr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF), textAlign = TextAlign.Center)
                Text(dayNum, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFE8ECF2), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.width(10.dp))

        Row(
            modifier          = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151A21))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00C896).copy(alpha = 0.12f)),
            ) {
                Text("⚖️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "${settlement.payerName} fully settled up with ${settlement.receiverName}",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF00C896),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Settlement Row ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettlementRow(
    settlement  : Settlement,
    showDateRail: Boolean,
    onClick     : () -> Unit,
    onDelete    : () -> Unit,
    onRestore   : (() -> Unit)? = null,
) {
    val isCancelled = settlement.status == SettlementStatus.CANCELLED
    val (monthAbbr, dayNum) = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) { "" to "" }
    }

    var showCancelDialog  by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title   = { Text("Cancel settlement?") },
            text    = { Text("This will reverse the balance changes.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showCancelDialog = false; onDelete() }) {
                    Text("Cancel settlement", color = Negative)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCancelDialog = false }) { Text("Keep") }
            },
        )
    }

    if (showRestoreDialog && onRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title   = { Text("Restore settlement?") },
            text    = { Text("This will apply the settlement again and update balances.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showRestoreDialog = false; onRestore() }) {
                    Text("Restore", color = Green400)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
        )
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = {
                    if (isCancelled) { if (onRestore != null) showRestoreDialog = true }
                    else showCancelDialog = true
                },
            )
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(24.dp),
        ) {
            if (showDateRail) {
                Text(monthAbbr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF), textAlign = TextAlign.Center)
                Text(dayNum, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFE8ECF2), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.width(10.dp))

        val rowAlpha = if (isCancelled) 0.5f else 1f
        Row(
            modifier          = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151A21))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background((if (isCancelled) Color(0xFF6B7280) else Color(0xFF00C896)).copy(alpha = 0.12f)),
            ) {
                Text(if (isCancelled) "↩️" else "🤝", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = "${settlement.payerName} paid ${settlement.receiverName}",
                    fontSize       = 14.sp,
                    fontWeight     = FontWeight.Medium,
                    color          = if (isCancelled) Color(0xFF6B7280) else Color(0xFF00C896),
                    maxLines       = 1,
                    overflow       = TextOverflow.Ellipsis,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    text  = if (isCancelled) "Payment cancelled" else "Payment",
                    fontSize = 12.sp,
                    color    = Color(0xFF6B7280),
                )
            }
            Text(
                text           = MoneyUtils.format(settlement.amount, settlement.currency),
                fontSize       = 14.sp,
                fontWeight     = FontWeight.SemiBold,
                color          = if (isCancelled) Color(0xFF6B7280) else Color(0xFF00C896),
                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}

// ── Group Empty State ─────────────────────────────────────────────────────────

@Composable
private fun GroupEmptyState(
    groupUiState: GroupUiState,
    onAddMember : () -> Unit,
    onAddExpense: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl)
            .padding(top = 40.dp),
    ) {
        // Icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF151A21)),
        ) {
            Text(
                text     = if (groupUiState == GroupUiState.SOLO) "👥" else "🧾",
                fontSize = 36.sp,
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text       = if (groupUiState == GroupUiState.SOLO) "Add friends to get started"
            else "Ready to split",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFE8ECF2),
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text      = if (groupUiState == GroupUiState.SOLO)
                "You need at least 2 people to\nsplit expenses in a group"
            else
                "Add your first expense with\nthe group to get started",
            fontSize  = 14.sp,
            color     = Color(0xFF9AA3AF),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xxl))

        // Primary CTA
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Color(0xFF00C896))
                .clickable { if (groupUiState == GroupUiState.SOLO) onAddMember() else onAddExpense() }
                .padding(horizontal = 32.dp, vertical = 14.dp),
        ) {
            Text(
                text       = if (groupUiState == GroupUiState.SOLO) "Add group members"
                else "Add your first expense",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.Black,
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Secondary CTA
        Text(
            text     = if (groupUiState == GroupUiState.SOLO) "Or share group link"
            else "Or add more members",
            fontSize = 14.sp,
            color    = Color(0xFF9AA3AF),
            modifier = Modifier.clickable {
                if (groupUiState != GroupUiState.SOLO) onAddMember()
            },
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ── Cover top color (for top bar background when scrolled) ───────────────────

private fun coverTopColor(type: GroupType): Color = when (type) {
    GroupType.TRIP      -> Color(0xFF0D2137)
    GroupType.HOME      -> Color(0xFF1A0D2E)
    GroupType.OFFICE    -> Color(0xFF1A1000)
    GroupType.FRIENDS   -> Color(0xFF0D1F0D)
    GroupType.COUPLE    -> Color(0xFF1F0D1A)
    GroupType.EVENT     -> Color(0xFF1A1000)
    GroupType.APARTMENT -> Color(0xFF0D1A1F)
    GroupType.OTHER     -> Color(0xFF111112)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toMonthHeader(): String = try {
    LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
} catch (e: Exception) { "Earlier" }

private fun String.toDayKey(): String = try {
    LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
} catch (e: Exception) { this }

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

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    ExpenseCategory.DINING_OUT        -> "🍽️"
    ExpenseCategory.GROCERIES         -> "🛒"
    ExpenseCategory.CAR               -> "🚗"
    ExpenseCategory.TAXI              -> "🚕"
    ExpenseCategory.PLANE             -> "✈️"
    ExpenseCategory.HOTEL             -> "🏨"
    ExpenseCategory.RENT              -> "🏠"
    ExpenseCategory.ELECTRICITY       -> "⚡"
    ExpenseCategory.WATER             -> "💧"
    ExpenseCategory.GAS_FUEL          -> "⛽"
    ExpenseCategory.TV_PHONE_INTERNET -> "📱"
    ExpenseCategory.MOVIES            -> "🎬"
    ExpenseCategory.GAMES             -> "🎮"
    ExpenseCategory.MUSIC             -> "🎵"
    ExpenseCategory.SPORTS            -> "⚽"
    ExpenseCategory.MEDICAL           -> "💊"
    ExpenseCategory.EDUCATION         -> "📚"
    ExpenseCategory.GIFTS             -> "🎁"
    ExpenseCategory.LIQUOR            -> "🍺"
    ExpenseCategory.PETS              -> "🐾"
    ExpenseCategory.CLOTHING          -> "👕"
    ExpenseCategory.BUS_TRAIN         -> "🚌"
    ExpenseCategory.PARKING           -> "🅿️"
    ExpenseCategory.CLEANING          -> "🧹"
    ExpenseCategory.HEAT_GAS          -> "🔥"
    ExpenseCategory.TRASH             -> "🗑️"
    else                              -> "💰"
}