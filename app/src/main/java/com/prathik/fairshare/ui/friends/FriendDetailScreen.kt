package com.prathik.fairshare.ui.friends

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsLoadingScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── Friend UI State ───────────────────────────────────────────────────────────

enum class FriendUiState {
    BRAND_NEW,            // $0, no history — gold ring, gray initials, all actions disabled
    SETTLED_WITH_HISTORY, // $0, has history — gold ring, gold checkmark, charts/history only
    THEY_OWE_YOU,         // netBalance > 0  — green ring, green initials, all actions enabled
    YOU_OWE_THEM,         // netBalance < 0  — red ring, red initials, no Remind pill
}


// ── Friend cover gradient palettes — deterministic from userId hash ───────────

private val FRIEND_GRADIENTS = listOf(
    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)), // deep purple
    listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF0B3D0B)), // teal-green
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)), // midnight blue
    listOf(Color(0xFF2C1654), Color(0xFF6B3FA0), Color(0xFF1A0D2E)), // violet
    listOf(Color(0xFF0D0D0D), Color(0xFF1A1A1A), Color(0xFF333333)), // dark mono
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), // ocean
    listOf(Color(0xFF360033), Color(0xFF0B8793), Color(0xFF1A0015)), // magenta-teal
    listOf(Color(0xFF1F1C2C), Color(0xFF928DAB), Color(0xFF1F1C2C)), // lavender
)

private fun friendGradient(userId: String): List<Color> {
    var hash = 0
    for (ch in userId) {
        hash = ((hash shl 5) - hash) + ch.code
        hash = hash and hash
    }
    return FRIEND_GRADIENTS[Math.abs(hash) % FRIEND_GRADIENTS.size]
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendDetailScreen(
    onBack                : () -> Unit,
    onNavigateToSettings  : () -> Unit,
    onNavigateToExpense   : (String) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSettle    : (friendId: String, groupId: String?, payerId: String?, payerName: String?) -> Unit,
    onNavigateToSearch    : () -> Unit,
    onNavigateToSettlement: (String) -> Unit = {},
    onNavigateToGroup     : (String) -> Unit = {},
    onNavigateToGroupsTab : () -> Unit = {},
    onNavigateToAnalytics : () -> Unit = {},
    onNavigateToRealFriend: (String) -> Unit = {},
    viewModel             : FriendDetailViewModel = hiltViewModel(),
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

    var showBalanceSheet by remember { mutableStateOf(false) }
    var showLinkSheet    by remember { mutableStateOf(false) }
    var showPayerSheet   by remember { mutableStateOf(false) }

    // ── Splitwise import flow ─────────────────────────────────────────────────
    var showImportInstructionsSheet by remember { mutableStateOf(false) }
    var showImportWhoAreYouSheet    by remember { mutableStateOf(false) }
    var showImportDisclaimerSheet   by remember { mutableStateOf(false) }
    var pendingImportCsv            by remember { mutableStateOf<String?>(null) }
    var pendingImporterName         by remember { mutableStateOf<String?>(null) }
    val csvMemberNames              by viewModel.csvMemberNames.collectAsState()
    val importState                 by viewModel.importState.collectAsState()
    val importContext               = androidx.compose.ui.platform.LocalContext.current

    val importFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val csv = try {
                importContext.contentResolver.openInputStream(it)
                    ?.bufferedReader()?.readText()
                    ?.replace("\r\n", "\n")
                    ?.replace("\r", "\n")
            } catch (e: Exception) { null }
            if (csv != null) {
                pendingImportCsv = csv
                viewModel.parseCsvNames(csv)
                showImportInstructionsSheet = false
                showImportWhoAreYouSheet = true
            }
        }
    }


    val groupBalanceSum = groupBalances.filter { it.groupId != null }.sumOf { it.amount }
    val nonGroupBalance = netBalance - groupBalanceSum

    val handleSettle: () -> Unit = {
        if (netBalance != 0.0 || groupBalances.isNotEmpty()) showBalanceSheet = true
        else showPayerSheet = true
    }

    val snackbarHost  = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    val showTopBarName by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > 350
        }
    }

    val friendName = friend?.fullName ?: ""
    val friendId   = friend?.id ?: viewModel.friendId
    val groupCount = groupBalances.count { it.groupId != null }



    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendDetailActionState.Error        -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is FriendDetailActionState.Success      -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is FriendDetailActionState.LinkedToFriend -> {
                viewModel.resetActionState()
                onNavigateToRealFriend(s.realFriendId)
            }
            else -> Unit
        }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is SplitwiseImportState.Success -> {
                snackbarHost.showSnackbar("Imported ${s.expensesCreated} expenses ✓")
                viewModel.resetImportState()
            }
            is SplitwiseImportState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetImportState()
            }
            else -> Unit
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshExpenses()
        }
    }

    val hasAnyActivity = netBalance != 0.0 ||
            groupBalances.isNotEmpty() ||
            settlements.isNotEmpty() ||
            (expensesState is FriendExpensesState.Success &&
                    (expensesState as FriendExpensesState.Success).expenses.isNotEmpty())

    // Derive the friend state — drives ring color, action bar, empty state
    val friendState: FriendUiState = when {
        netBalance > 0  -> FriendUiState.THEY_OWE_YOU
        netBalance < 0  -> FriendUiState.YOU_OWE_THEM
        hasAnyActivity  -> FriendUiState.SETTLED_WITH_HISTORY
        else            -> FriendUiState.BRAND_NEW
    }

    // Progress % for ring — depends on friendState
    val settledPct: Float = when (friendState) {
        FriendUiState.BRAND_NEW, FriendUiState.SETTLED_WITH_HISTORY -> 1f
        FriendUiState.THEY_OWE_YOU -> {
            val settled = settlements.sumOf { it.amount }
            val total   = settled + netBalance
            if (total > 0) (settled / total).toFloat().coerceIn(0f, 0.99f) else 0f
        }
        FriendUiState.YOU_OWE_THEM -> {
            val settled = settlements.sumOf { it.amount }
            val total   = settled + (-netBalance)
            if (total > 0) (settled / total).toFloat().coerceIn(0f, 0.99f) else 0f
        }
    }


    // ── Splitwise import: Step 1 — Instructions ───────────────────────────────
    if (showImportInstructionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportInstructionsSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = 40.dp),
            ) {
                Text(
                    text       = "Import from Splitwise",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(vertical = Spacing.md),
                )
                Text(
                    text     = "This imports your shared Splitwise history with $friendName into FairShare.",
                    fontSize = 14.sp,
                    color    = TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                listOf(
                    "1" to "Open Splitwise on web or mobile",
                    "2" to "Go to your friendship with $friendName",
                    "3" to "Tap Settings → Export to CSV",
                    "4" to "Save the file and come back here",
                ).forEach { (step, text) ->
                    Row(
                        modifier          = Modifier.padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Green400.copy(alpha = 0.15f)),
                        ) {
                            Text(step, fontSize = 12.sp, color = Green400, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(text, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
                FsPrimaryButton(
                    text     = "Choose CSV file",
                    onClick  = { importFilePicker.launch(arrayOf("text/*", "text/csv", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                FsSecondaryButton(
                    text     = "Cancel",
                    onClick  = { showImportInstructionsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // ── Splitwise import: Step 2 — Which one is you? ──────────────────────────
    if (showImportWhoAreYouSheet && csvMemberNames.isNotEmpty()) {
        val myName = viewModel.currentUserFullName
        ModalBottomSheet(
            onDismissRequest = {
                showImportWhoAreYouSheet = false
                viewModel.clearCsvNames()
                pendingImportCsv = null
            },
            sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 40.dp)) {
                Text(
                    text       = "Which one is you?",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                if (myName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .padding(bottom = Spacing.sm)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Green400.copy(alpha = 0.08f))
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("You are signed in as ", fontSize = 13.sp, color = TextSecondary)
                        Text(myName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green400)
                    }
                }
                Text(
                    text     = "Your Splitwise name may differ from your FairShare name.",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                csvMemberNames.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingImporterName = name
                                showImportWhoAreYouSheet = false
                                viewModel.clearCsvNames()
                                showImportDisclaimerSheet = true
                            }
                            .padding(horizontal = Spacing.lg, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Surface4),
                        ) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                fontSize   = 16.sp,
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("→", fontSize = 18.sp, color = TextTertiary)
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
            }
        }
    }

    // ── Splitwise import: Step 3 — Disclaimer ────────────────────────────────
    if (showImportDisclaimerSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showImportDisclaimerSheet = false
                pendingImportCsv = null
                pendingImporterName = null
            },
            sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = 40.dp),
            ) {
                Text(
                    text       = "Before you import",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(vertical = Spacing.md),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface3)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    listOf(
                        "All expenses from this CSV will be added to your history with $friendName.",
                        "Imported expenses cannot be deleted — they are permanent.",
                        "Importing the same CSV twice will create duplicate expenses.",
                        "Payments in the CSV will be recorded as settlements.",
                    ).forEach { text ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•  ", fontSize = 14.sp, color = Green400)
                            Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
                FsPrimaryButton(
                    text    = "I understand, import now",
                    onClick = {
                        val csv  = pendingImportCsv
                        val name = pendingImporterName
                        showImportDisclaimerSheet = false
                        pendingImportCsv    = null
                        pendingImporterName = null
                        if (csv != null && name != null) {
                            viewModel.importFromSplitwise(csv, name)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                FsSecondaryButton(
                    text     = "Cancel",
                    onClick  = {
                        showImportDisclaimerSheet = false
                        pendingImportCsv    = null
                        pendingImporterName = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, "Add expense", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh    = { viewModel.loadData() },
                modifier     = Modifier.fillMaxSize(),
            ) {
                if (isLoading && friend == null) {
                    FsLoadingScreen()
                } else {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {

                        // ── Cover header ──────────────────────────────────────
                        item {
                            FriendCoverHeader(
                                name        = friendName,
                                userId      = friendId,
                                settledPct  = settledPct,
                                groupCount  = groupCount,
                                friendState = friendState,
                            )
                        }

                        // ── Sticky balance bar ────────────────────────────────
                        stickyHeader {
                            FriendStickyBalanceBar(
                                netBalance  = netBalance,
                                currency    = currency,
                                groupCount  = groupCount,
                                friendState = friendState,
                            )
                        }

                        // ── Placeholder link banner ───────────────────────────
                        if (friendStatus == "placeholder") {
                            item {
                                PlaceholderLinkBanner(
                                    friendName = friendName,
                                    onLink     = { showLinkSheet = true },
                                    modifier   = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical   = Spacing.sm,
                                    ),
                                )
                            }
                        }

                        // ── Action bar ────────────────────────────────────────
                        item {
                            FriendActionBar(
                                onSettleUp            = handleSettle,
                                onNavigateToAnalytics = onNavigateToAnalytics,
                                onImportSplitwise     = { showImportInstructionsSheet = true },
                                friendState           = friendState,
                            )
                        }

                        // ── Unified Timeline ──────────────────────────────────
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
                                val allItems = buildList<FriendTimelineItem> {
                                    groupBalances.forEach { add(FriendTimelineItem.GroupBalanceItem(it)) }
                                    state.expenses.forEach { add(FriendTimelineItem.DirectExpenseItem(it)) }
                                    settlements.forEach { s ->
                                        if (s.isFullSettle && s.settleType == "ALL") {
                                            add(FriendTimelineItem.FullySettledItem(s))
                                        }
                                        add(FriendTimelineItem.SettlementItem(s))
                                    }
                                }.sortedByDescending { it.sortDate }

                                if (allItems.isEmpty()) {
                                    item {
                                        FriendEmptyState(
                                            friendName        = friendName,
                                            friendState       = friendState,
                                            onAddExpense      = onNavigateToAddExpense,
                                            onNavigateToGroup = onNavigateToGroupsTab,
                                        )
                                    }
                                } else {
                                    val grouped = allItems.groupBy { it.sortDate.toMonthHeader() }
                                    grouped.forEach { (month, monthItems) ->
                                        // Month sticky header
                                        stickyHeader(key = "month_$month") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0B0E13))
                                                    .padding(horizontal = Spacing.lg, vertical = 6.dp),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                            ) {
                                                Text(
                                                    text          = month.uppercase(),
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

                                        // Group by day for date rail logic
                                        val byDay = monthItems.groupBy { it.sortDate.toDayKey() }
                                        byDay.forEach { (_, dayItems) ->
                                            items(
                                                items = dayItems,
                                                key = {
                                                    when (it) {
                                                        is FriendTimelineItem.GroupBalanceItem  -> "gb_${it.balance.groupId}"
                                                        is FriendTimelineItem.DirectExpenseItem -> "ex_${it.expense.id}"
                                                        is FriendTimelineItem.SettlementItem    -> "st_${it.settlement.id}"
                                                        is FriendTimelineItem.FullySettledItem  -> "fs_${it.settlement.id}"
                                                    }
                                                },
                                            ) { item ->
                                                val showDateRail = item == dayItems.first()
                                                when (item) {
                                                    is FriendTimelineItem.GroupBalanceItem ->
                                                        GroupBalanceRow(
                                                            balance      = item.balance,
                                                            showDateRail = showDateRail,
                                                            onClick      = { gId -> if (gId != null) onNavigateToGroup(gId) },
                                                        )
                                                    is FriendTimelineItem.DirectExpenseItem ->
                                                        FriendExpenseRow(
                                                            expense      = item.expense,
                                                            showDateRail = showDateRail,
                                                            onClick      = { onNavigateToExpense(item.expense.id) },
                                                        )
                                                    is FriendTimelineItem.SettlementItem ->
                                                        FriendSettlementRow(
                                                            settlement   = item.settlement,
                                                            showDateRail = showDateRail,
                                                            onClick      = { onNavigateToSettlement(item.settlement.id) },
                                                            onDelete     = { viewModel.deleteSettlement(item.settlement.id) },
                                                        )
                                                    is FriendTimelineItem.FullySettledItem ->
                                                        FriendFullySettledRow(
                                                            settlement   = item.settlement,
                                                            showDateRail = showDateRail,
                                                            onClick      = { onNavigateToSettlement(item.settlement.id) },
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

            // ── Fixed overlay top bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (showTopBarName) Color(0xF0111112) else Color.Transparent)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .offset(y = (-20).dp)
                    .padding(horizontal = Spacing.lg, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .clickable { onBack() },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }

                if (showTopBarName) {
                    Text(
                        text       = friendName,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f).padding(horizontal = Spacing.md),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable { onNavigateToSearch() },
                    ) {
                        Icon(Icons.Outlined.Search, "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable { onNavigateToSettings() },
                    ) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────
    val friendsList by viewModel.friends.collectAsState()
    if (showLinkSheet && friendStatus == "placeholder") {
        FriendLinkSheet(
            friendName = friendName,
            friends    = friendsList,
            onLink     = { fId -> showLinkSheet = false; viewModel.assignFriendPlaceholder(viewModel.friendId, fId) },
            onDismiss  = { showLinkSheet = false },
        )
    }

    if (showBalanceSheet) {
        ModalBottomSheet(onDismissRequest = { showBalanceSheet = false }, containerColor = Surface2) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Select a balance to settle with ${friendName.ifBlank { "friend" }}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                val totalOwed = Math.abs(netBalance)
                if (totalOwed > 0) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { showBalanceSheet = false; onNavigateToSettle(viewModel.friendId, null, null, null) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = friendName, userId = viewModel.friendId, size = ComponentSize.avatarMd)
                        Spacer(Modifier.width(Spacing.md))
                        Text("Settle all balances", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = TextPrimary, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (netBalance > 0) "you are owed" else "you owe", fontSize = 11.sp, color = TextTertiary)
                            Text(MoneyUtils.format(totalOwed, currency), fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (netBalance > 0) Green400 else Negative)
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
                val groupsWithBalance = groupBalances.filter { it.groupId != null && it.amount != 0.0 }
                if (groupsWithBalance.isNotEmpty()) {
                    Text("Or settle a specific group balance", fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.sm))
                    groupsWithBalance.forEach { balance ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { showBalanceSheet = false; onNavigateToSettle(viewModel.friendId, balance.groupId, null, null) }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(contentAlignment = Alignment.Center,
                                modifier = Modifier.size(ComponentSize.avatarMd).clip(RoundedCornerShape(12.dp)).background(Surface4)) {
                                Text(balance.groupName?.firstOrNull()?.uppercase() ?: "G", fontSize = 18.sp,
                                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Text(balance.groupName ?: "Group", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = TextPrimary, modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (balance.amount > 0) "you are owed" else "you owe", fontSize = 11.sp, color = TextTertiary)
                                Text(MoneyUtils.format(Math.abs(balance.amount), balance.currency), fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (balance.amount > 0) Green400 else Negative)
                            }
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }
                if (Math.abs(nonGroupBalance) > 0.01) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { showBalanceSheet = false; onNavigateToSettle(viewModel.friendId, null, null, null) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ComponentSize.avatarMd).clip(RoundedCornerShape(12.dp)).background(Surface4)) {
                            Text("📋", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Text("Non-group expenses", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = TextPrimary, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (nonGroupBalance > 0) "you are owed" else "you owe", fontSize = 11.sp, color = TextTertiary)
                            Text(MoneyUtils.format(Math.abs(nonGroupBalance), currency), fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (nonGroupBalance > 0) Green400 else Negative)
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { showBalanceSheet = false; showPayerSheet = true }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("More options", fontSize = 15.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
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
                val meId = viewModel.currentUserId ?: ""
                listOf(meId to "You", viewModel.friendId to friendName).forEach { (userId, name) ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { showPayerSheet = false; onNavigateToSettle(viewModel.friendId, null, userId, name) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = name, userId = userId, size = ComponentSize.avatarMd)
                        Spacer(Modifier.width(Spacing.md))
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Friend Cover Header ───────────────────────────────────────────────────────

@Composable
private fun FriendCoverHeader(
    name       : String,
    userId     : String,
    settledPct : Float,
    groupCount : Int,
    friendState: FriendUiState = FriendUiState.BRAND_NEW,
) {
    val gradientColors = remember(userId) { friendGradient(userId) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Brush.linearGradient(gradientColors)),
    ) {
        // Bottom dark scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xEE0B0E13))
                )),
        )

        // Content — centered
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.md),
        ) {
            // Progress ring + avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(88.dp),
            ) {
                Canvas(modifier = Modifier.size(88.dp)) {
                    val stroke = 6f
                    val radius = size.minDimension / 2f - stroke / 2f

                    when (friendState) {
                        FriendUiState.BRAND_NEW, FriendUiState.SETTLED_WITH_HISTORY -> {
                            // Gold ring — full circle
                            drawCircle(
                                brush  = Brush.sweepGradient(listOf(Color(0xFFF59E0B), Color(0xFFFFD700), Color(0xFFFBBF24), Color(0xFFF59E0B))),
                                radius = radius,
                                style  = Stroke(width = stroke),
                            )
                        }
                        FriendUiState.THEY_OWE_YOU -> {
                            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = radius, style = Stroke(width = stroke))
                            if (settledPct > 0f) {
                                drawArc(color = Color(0xFF00C896), startAngle = -90f,
                                    sweepAngle = 360f * settledPct, useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round))
                            }
                        }
                        FriendUiState.YOU_OWE_THEM -> {
                            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = radius, style = Stroke(width = stroke))
                            if (settledPct > 0f) {
                                drawArc(color = Color(0xFFF87171), startAngle = -90f,
                                    sweepAngle = 360f * settledPct, useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round))
                            }
                        }
                        else -> {}
                    }
                }

                // Avatar inside ring — color and content vary by state
                when (friendState) {
                    FriendUiState.SETTLED_WITH_HISTORY -> {
                        // Gold checkmark — no avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1A1A1C)),
                        ) {
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
                        }
                    }
                    FriendUiState.BRAND_NEW -> {
                        // Gray avatar
                        FsAvatar(name = name, userId = userId, size = 56.dp, modifier = Modifier.clip(CircleShape))
                    }
                    FriendUiState.THEY_OWE_YOU -> {
                        // Green tinted avatar background
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF00C896))) {
                            Text(name.take(2).uppercase(), fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                    FriendUiState.YOU_OWE_THEM -> {
                        // Red tinted avatar background
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF87171))) {
                            Text(name.take(2).uppercase(), fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    else -> {
                        FsAvatar(name = name, userId = userId, size = 56.dp, modifier = Modifier.clip(CircleShape))
                    }
                }

                // % badge — only for active debt states
                val showBadge = (friendState == FriendUiState.THEY_OWE_YOU ||
                        friendState == FriendUiState.YOU_OWE_THEM) && settledPct > 0f
                if (showBadge) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color(0x99000000), RoundedCornerShape(Radius.full))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text("${(settledPct * 100).toInt()}%", fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(Spacing.sm))

            // Pills row
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                if (groupCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.full))
                            .background(Color(0x26FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text       = "${groupCount} ${if (groupCount == 1) "group" else "groups"}".uppercase(),
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
}

// ── Sticky Balance Bar ────────────────────────────────────────────────────────

@Composable
private fun FriendStickyBalanceBar(
    netBalance  : Double,
    currency    : String,
    groupCount  : Int,
    friendState : FriendUiState,
) {
    val isCentered = friendState == FriendUiState.BRAND_NEW || friendState == FriendUiState.SETTLED_WITH_HISTORY

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color(0xF2111112))
            .padding(horizontal = Spacing.lg, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start,
    ) {
        when (friendState) {
            FriendUiState.BRAND_NEW -> {
                Text("No expenses yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9AA3AF))
            }
            FriendUiState.SETTLED_WITH_HISTORY -> {
                Text("All settled 🎉", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00C896))
            }
            FriendUiState.THEY_OWE_YOU -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Owed to you", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                    Text(MoneyUtils.format(netBalance, currency), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF00C896))
                }
                if (groupCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("👥", fontSize = 12.sp)
                        Text("in $groupCount ${if (groupCount == 1) "group" else "groups"}",
                            fontSize = 12.sp, color = Color(0xFF9AA3AF))
                    }
                }
            }
            FriendUiState.YOU_OWE_THEM -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text("You owe", fontSize = 10.sp, color = Color(0xFF9AA3AF))
                    Text(MoneyUtils.format(-netBalance, currency), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                }
                if (groupCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("👥", fontSize = 12.sp)
                        Text("in $groupCount ${if (groupCount == 1) "group" else "groups"}",
                            fontSize = 12.sp, color = Color(0xFF9AA3AF))
                    }
                }
            }
            else -> {}
        }
    }
    HorizontalDivider(color = Surface4, thickness = 0.5.dp)
}

// ── Action Bar ────────────────────────────────────────────────────────────────

@Composable
private fun FriendActionBar(
    onSettleUp           : () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onImportSplitwise    : () -> Unit = {},
    friendState          : FriendUiState = FriendUiState.BRAND_NEW,
) {
    // Per-spec rules (see Summary of Rules):
    //   Brand new:          settle=off, remind=off, charts=off, history=off
    //   Settled w/ history: settle=off, remind=off, charts=on,  history=on
    //   They owe you:       settle=on,  remind=on,  charts=on,  history=on
    //   You owe them:       settle=on,  remind=HIDDEN, charts=on, history=on
    val settleEnabled    = friendState == FriendUiState.THEY_OWE_YOU || friendState == FriendUiState.YOU_OWE_THEM
    val showRemind       = friendState != FriendUiState.YOU_OWE_THEM  // hidden when you owe them
    val remindEnabled    = friendState == FriendUiState.THEY_OWE_YOU  // disabled in brand new + settled
    val secondaryEnabled = friendState != FriendUiState.BRAND_NEW

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

            // Remind — hidden on YOU_OWE_THEM, disabled on Brand New + Settled
            if (showRemind) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (remindEnabled) Color(0xFF151A21) else Color(0xFF232A34))
                        .then(if (remindEnabled) Modifier.clickable { } else Modifier)
                        .padding(horizontal = Spacing.md, vertical = 10.dp),
                ) {
                    Text("Remind", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (remindEnabled) Color(0xFFE8ECF2) else Color(0xFF6B7280))
                }
            }

            // Charts + History
            listOf("Charts" to onNavigateToAnalytics).forEach { (label, action) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (secondaryEnabled) Color(0xFF151A21) else Color(0xFF232A34))
                        .then(if (secondaryEnabled) Modifier.clickable { action() } else Modifier)
                        .padding(horizontal = Spacing.md, vertical = 10.dp),
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (secondaryEnabled) Color(0xFFE8ECF2) else Color(0xFF6B7280))
                }
            }

            // Import from Splitwise — always visible
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Color(0xFF151A21))
                    .clickable { onImportSplitwise() }
                    .padding(horizontal = Spacing.md, vertical = 10.dp),
            ) {
                Text("Import", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE8ECF2))
            }
        }

        // Fade edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(32.dp)
                .height(44.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF0B0E13))))
        )
    }
}

// ── Timeline rows — card style ────────────────────────────────────────────────

@Composable
private fun FriendExpenseRow(
    expense     : Expense,
    showDateRail: Boolean,
    onClick     : () -> Unit,
) {
    val youLent = expense.yourBalance > 0
    val youOwe  = expense.yourBalance < 0
    val balanceColor  = when { youLent -> Color(0xFF00C896); youOwe -> Color(0xFFF87171); else -> TextTertiary }
    val balanceLabel  = when { youLent -> "you lent"; youOwe -> "you owe"; else -> "settled" }
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
        // Date rail
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
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
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF232A34))) {
                Text(categoryEmoji(expense.category), fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE8ECF2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (expense.groupName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(expense.groupName, fontSize = 12.sp, color = Color(0xFF9AA3AF),
                            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(" · group", fontSize = 11.sp, color = Color(0xFF6B7280))
                    }
                } else {
                    Text(paidByText, fontSize = 12.sp, color = Color(0xFF9AA3AF))
                }
            }
            if (expense.yourBalance != 0.0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(balanceLabel, fontSize = 10.sp, color = Color(0xFF9AA3AF))
                    Text(balanceAmount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = balanceColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendSettlementRow(
    settlement  : Settlement,
    showDateRail: Boolean,
    onClick     : () -> Unit,
    onDelete    : () -> Unit,
) {
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
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = Negative)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { showDeleteDialog = true })
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
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
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00C896).copy(alpha = 0.12f))) {
                Text("🤝", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${settlement.payerName} paid ${settlement.receiverName}", fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = Color(0xFF00C896),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(settlement.notes?.takeIf { it.isNotBlank() } ?: "Payment",
                    fontSize = 12.sp, color = Color(0xFF6B7280))
            }
            Text(MoneyUtils.format(settlement.amount, settlement.currency), fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF00C896))
        }
    }
}

@Composable
private fun FriendFullySettledRow(
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
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
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00C896).copy(alpha = 0.12f))) {
                Text("⚖️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text("You fully settled up in all groups", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = Color(0xFF00C896), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GroupBalanceRow(
    balance     : Balance,
    showDateRail: Boolean,
    onClick     : (String?) -> Unit,
) {
    val isOwed    = balance.amount > 0
    val isSettled = balance.amount == 0.0
    val balanceColor = when { isSettled -> TextTertiary; isOwed -> Color(0xFF00C896); else -> Color(0xFFF87171) }
    val balanceLabel = when { isSettled -> "settled up"; isOwed -> "you lent"; else -> "you owe" }
    val displayAmount = if (isSettled) "" else MoneyUtils.format(Math.abs(balance.amount), balance.currency)

    val (monthAbbr, dayNum) = remember(balance.groupLastActivity) {
        if (balance.groupLastActivity.isNullOrBlank()) "—" to "—"
        else try {
            val dt = LocalDateTime.parse(balance.groupLastActivity, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) { "—" to "—" }
    }

    val boxColor = remember(balance.groupName) {
        val colors = listOf(
            android.graphics.Color.parseColor("#1A2A3A"),
            android.graphics.Color.parseColor("#1A3A1A"),
            android.graphics.Color.parseColor("#2A1A0A"),
            android.graphics.Color.parseColor("#1A1A3A"),
            android.graphics.Color.parseColor("#2A1A2A"),
        )
        val idx = (balance.groupName?.hashCode()?.and(0x7fffffff) ?: 0) % colors.size
        Color(colors[idx])
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick(balance.groupId) }
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
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
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(boxColor)) {
                Text(balance.groupName?.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(balance.groupName ?: "Group", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE8ECF2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("in group", fontSize = 12.sp, color = Color(0xFF9AA3AF))
            }
            if (isSettled) {
                Text("settled up", fontSize = 12.sp, color = TextTertiary)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(balanceLabel, fontSize = 10.sp, color = Color(0xFF9AA3AF))
                    Text(displayAmount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = balanceColor)
                }
            }
        }
    }
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
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text("Link $friendName to a FairShare friend", fontSize = 14.sp,
                fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("Tap to connect this placeholder to a real account",
                fontSize = 12.sp, color = TextSecondary)
        }
        Text("→", fontSize = 16.sp, color = Green400, fontWeight = FontWeight.SemiBold)
    }
}

// ── Friend Link Sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendLinkSheet(
    friendName: String,
    friends   : List<com.prathik.fairshare.domain.model.Friend>,
    onLink    : (friendId: String) -> Unit,
    onDismiss : () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface2,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Who is $friendName on FairShare?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
            Text("Link their expenses to a real account.", fontSize = 13.sp, color = TextSecondary,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md))
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            if (friends.isEmpty()) {
                Text("No FairShare friends found.", fontSize = 14.sp, color = TextTertiary,
                    modifier = Modifier.padding(Spacing.lg))
            } else {
                friends.forEach { friend ->
                    Row(
                        modifier          = Modifier.fillMaxWidth()
                            .clickable { onLink(friend.id) }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = friend.fullName, userId = friend.id, size = ComponentSize.avatarMd)
                        Spacer(Modifier.width(Spacing.md))
                        Text(friend.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss)
                    .padding(horizontal = Spacing.lg, vertical = 16.dp)
            ) {
                Text("Cancel", fontSize = 14.sp, color = TextTertiary)
            }
        }
    }
}

// ── Timeline model ────────────────────────────────────────────────────────────

sealed class FriendTimelineItem(val sortDate: String) {
    data class GroupBalanceItem(val balance: Balance)      : FriendTimelineItem(balance.groupLastActivity ?: "")
    data class DirectExpenseItem(val expense: Expense)     : FriendTimelineItem(expense.expenseDate)
    data class SettlementItem(val settlement: Settlement)  : FriendTimelineItem(settlement.settlementDate)
    data class FullySettledItem(val settlement: Settlement): FriendTimelineItem(settlement.settlementDate)
}

// ── Friend Empty State ────────────────────────────────────────────────────────

@Composable
private fun FriendEmptyState(
    friendName       : String,
    friendState      : FriendUiState,
    onAddExpense     : () -> Unit,
    onNavigateToGroup: () -> Unit,
) {
    val isSettled = friendState == FriendUiState.SETTLED_WITH_HISTORY

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
            Text(if (isSettled) "✅" else "🤝", fontSize = 36.sp)
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text       = if (isSettled) "All settled with ${friendName.ifBlank { "your friend" }} 🎉"
            else "You and ${friendName.ifBlank { "your friend" }} are all set",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFE8ECF2),
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text      = if (isSettled) "No outstanding balances"
            else "Split your first expense to start tracking\nwho owes who",
            fontSize  = 14.sp,
            color     = Color(0xFF9AA3AF),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xxl))

        if (isSettled) {
            // View past expenses — dark secondary button
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Color(0xFF151A21))
                    .clickable { }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("View past expenses", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFFE8ECF2))
            }
        } else {
            // Add first expense — green primary
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Color(0xFF00C896))
                    .clickable { onAddExpense() }
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text("Add your first expense", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text      = "Or add to a group together",
                fontSize  = 14.sp,
                color     = Color(0xFF9AA3AF),
                modifier  = Modifier.clickable { onNavigateToGroup() },
            )
        }

        Spacer(Modifier.height(80.dp))
    }
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