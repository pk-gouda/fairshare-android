package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
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
    onBack               : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToExpense  : (String) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSettle   : (String) -> Unit,
    viewModel            : FriendDetailViewModel = hiltViewModel(),
) {
    val isLoading     by viewModel.isLoading.collectAsState()
    val friend        by viewModel.friend.collectAsState()
    val netBalance    by viewModel.netBalance.collectAsState()
    val currency      by viewModel.currency.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val actionState   by viewModel.actionState.collectAsState()
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
                                netBalance = netBalance,
                                currency   = currency,
                                onSettle   = { onNavigateToSettle(viewModel.friendId) },
                                modifier   = Modifier.padding(
                                    horizontal = Spacing.lg,
                                    vertical   = Spacing.md,
                                ),
                            )
                        }

                        // ── Action Pills ──────────────────────────────────────
                        item {
                            FriendActionPills(
                                onSettleUp = { onNavigateToSettle(viewModel.friendId) },
                                modifier   = Modifier.padding(
                                    start  = Spacing.lg,
                                    end    = Spacing.lg,
                                    bottom = Spacing.md,
                                ),
                            )
                        }

                        // ── Expenses ──────────────────────────────────────────
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
                                if (state.expenses.isEmpty()) {
                                    item {
                                        FsEmptyState(
                                            title    = "No shared expenses",
                                            subtitle = "Add an expense to get started",
                                            modifier = Modifier.height(300.dp),
                                        )
                                    }
                                } else {
                                    val grouped = state.expenses
                                        .sortedByDescending { it.expenseDate }
                                        .groupBy { it.expenseDate.toMonthHeader() }

                                    grouped.forEach { (month, expenses) ->
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
                                        items(expenses, key = { it.id }) { expense ->
                                            FriendExpenseRow(
                                                expense = expense,
                                                onClick = { onNavigateToExpense(expense.id) },
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
                        text     = friendName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color    = Color.White,
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
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable { /* TODO: search within friend expenses */ },
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
    netBalance: Double,
    currency  : String,
    onSettle  : () -> Unit,
    modifier  : Modifier = Modifier,
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
    }
}

// ── Action Pills ──────────────────────────────────────────────────────────────

@Composable
private fun FriendActionPills(
    onSettleUp: () -> Unit,
    modifier  : Modifier = Modifier,
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

        // Info pills
        listOf("Balances", "Charts").forEach { label ->
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Surface2)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Text(text = label, fontSize = 12.sp, color = TextSecondary)
            }
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

        // Description + paid by
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = expense.description,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(text = paidByText, fontSize = 12.sp, color = TextTertiary)
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