package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExpense: (String) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSettle: (String) -> Unit,
    viewModel: FriendDetailViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val friend by viewModel.friend.collectAsState()
    val friendStatus by viewModel.friendStatus.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendDetailActionState.Error -> {
                snackbarHost.showSnackbar(s.message); viewModel.resetActionState()
            }

            is FriendDetailActionState.Success -> {
                snackbarHost.showSnackbar(s.message); viewModel.resetActionState()
            }

            else -> Unit
        }
    }

    val balanceColor = when {
        netBalance > 0 -> Green400
        netBalance < 0 -> Negative
        else -> TextSecondary
    }
    val balanceLabel = when {
        netBalance > 0 -> "owes you"
        netBalance < 0 -> "you owe"
        else -> "settled up"
    }

    Scaffold(
        containerColor = Surface0,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            if (friendStatus == null) {
                FloatingActionButton(
                    onClick = onNavigateToAddExpense,
                    containerColor = Green400,
                    contentColor = Surface0,
                    shape = RoundedCornerShape(Radius.lg),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add expense",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        topBar = {
            FsTopBar(
                title = friend?.fullName ?: "",
                onBack = onBack,
                actions = {
                    FsIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Friend settings",
                        onClick = onNavigateToSettings,
                    )
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isLoading && friend == null) {
                FsLoadingScreen(); return@PullToRefreshBox
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Profile + balance hero ────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface2)
                            .padding(vertical = Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FsAvatar(
                            name = friend?.fullName ?: "",
                            userId = friend?.id ?: "",
                            size = 72.dp,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = friend?.fullName ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )

                        // Status or email
                        if (friendStatus != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = friendStatus!!, fontSize = 12.sp, color = TextTertiary)
                        } else if (friend?.email?.isNotBlank() == true) {
                            Text(text = friend?.email ?: "", fontSize = 13.sp, color = TextTertiary)
                        }

                        // Balance — only for accepted friends
                        if (friendStatus == null) {
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Text(
                                text = MoneyUtils.format(kotlin.math.abs(netBalance), currency),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor,
                            )
                            Text(text = balanceLabel, fontSize = 13.sp, color = TextSecondary)

                            if (netBalance != 0.0) {
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.xl))
                                        .background(
                                            if (netBalance < 0) Green400 else Surface0.copy(
                                                alpha = 0.2f
                                            )
                                        )
                                        .clickable { onNavigateToSettle(viewModel.friendId) }
                                        .padding(horizontal = Spacing.xl, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = if (netBalance < 0) "Settle up" else "Remind",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (netBalance < 0) Surface0 else TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Expenses ──────────────────────────────────────────────────
                when (val state = expensesState) {
                    is FriendExpensesState.Loading -> {
                        item { FsLoadingScreen() }
                    }

                    is FriendExpensesState.Error -> {
                        item {
                            FsEmptyState(
                                title = "Couldn't load expenses",
                                subtitle = state.message,
                                modifier = Modifier.height(200.dp),
                            )
                        }
                    }

                    is FriendExpensesState.Success -> {
                        if (state.expenses.isEmpty()) {
                            item {
                                FsEmptyState(
                                    title = "No shared expenses",
                                    subtitle = "Add an expense to get started",
                                    modifier = Modifier.height(300.dp),
                                )
                            }
                        } else {
                            val grouped = state.expenses
                                .groupBy { it.expenseDate.toMonthHeader() }

                            grouped.forEach { (month, expenses) ->
                                item {
                                    Text(
                                        text = month.uppercase(),
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
                                items(expenses, key = { it.id }) { expense ->
                                    FriendExpenseRow(
                                        expense = expense,
                                        onClick = { onNavigateToExpense(expense.id) },
                                    )
                                    HorizontalDivider(
                                        color = Surface3,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = Spacing.lg + 56.dp),
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

// ── Expense Row — identical to GroupDetailScreen ──────────────────────────────

@Composable
private fun FriendExpenseRow(expense: Expense, onClick: () -> Unit) {
    val youLent = expense.yourBalance > 0
    val youOwe = expense.yourBalance < 0

    val balanceColor = when {
        youLent -> Green400
        youOwe -> Negative
        else -> TextTertiary
    }
    val balanceLabel = when {
        youLent -> "you lent"
        youOwe -> "you owe"
        else -> "settled"
    }
    val balanceAmount = when {
        youLent -> MoneyUtils.format(expense.yourBalance, expense.currency)
        youOwe -> MoneyUtils.format(-expense.yourBalance, expense.currency)
        else -> ""
    }

    val paidByText = expense.payers.firstOrNull()?.let { payer ->
        "${payer.fullName} paid ${MoneyUtils.format(payer.amountPaid, expense.currency)}"
    } ?: "${expense.addedByName} paid ${MoneyUtils.format(expense.totalAmount, expense.currency)}"

    val (monthAbbr, dayNum) = remember(expense.expenseDate) {
        try {
            val dt = LocalDateTime.parse(expense.expenseDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM")) to dt.dayOfMonth.toString()
        } catch (e: Exception) {
            "" to ""
        }
    }

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
            Text(text = monthAbbr, fontSize = 10.sp, color = TextTertiary)
            Text(
                text = dayNum,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Emoji
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
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
                text = expense.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
            )
            Text(text = paidByText, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
        }

        // Balance
        if (balanceAmount.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = balanceLabel, fontSize = 10.sp, color = balanceColor)
                Text(
                    text = balanceAmount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor
                )
            }
        } else {
            Text(text = "settled", fontSize = 12.sp, color = TextTertiary)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toMonthHeader(): String {
    return try {
        val dt = LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (e: Exception) {
        "Earlier"
    }
}

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    // Entertainment
    ExpenseCategory.GAMES -> "🎮"
    ExpenseCategory.MOVIES -> "🎬"
    ExpenseCategory.MUSIC -> "🎵"
    ExpenseCategory.SPORTS -> "⚽"
    // Food & drink
    ExpenseCategory.DINING_OUT -> "🍽️"
    ExpenseCategory.GROCERIES -> "🛒"
    ExpenseCategory.LIQUOR -> "🍺"
    // Home
    ExpenseCategory.ELECTRONICS -> "📱"
    ExpenseCategory.FURNITURE -> "🛋️"
    ExpenseCategory.HOUSEHOLD_SUPPLIES -> "🧹"
    ExpenseCategory.MAINTENANCE -> "🔧"
    ExpenseCategory.MORTGAGE -> "🏦"
    ExpenseCategory.PETS -> "🐾"
    ExpenseCategory.RENT -> "🏠"
    ExpenseCategory.SERVICES -> "🛠️"
    // Life
    ExpenseCategory.CHILDCARE -> "👶"
    ExpenseCategory.CLOTHING -> "👕"
    ExpenseCategory.EDUCATION -> "📚"
    ExpenseCategory.GIFTS -> "🎁"
    ExpenseCategory.INSURANCE -> "🛡️"
    ExpenseCategory.MEDICAL -> "💊"
    ExpenseCategory.TAXES -> "🧾"
    // Transportation
    ExpenseCategory.BICYCLE -> "🚲"
    ExpenseCategory.BUS_TRAIN -> "🚌"
    ExpenseCategory.CAR -> "🚗"
    ExpenseCategory.GAS_FUEL -> "⛽"
    ExpenseCategory.HOTEL -> "🏨"
    ExpenseCategory.PARKING -> "🅿️"
    ExpenseCategory.PLANE -> "✈️"
    ExpenseCategory.TAXI -> "🚕"
    // Utilities
    ExpenseCategory.CLEANING -> "🧽"
    ExpenseCategory.ELECTRICITY -> "⚡"
    ExpenseCategory.HEAT_GAS -> "🔥"
    ExpenseCategory.TRASH -> "🗑️"
    ExpenseCategory.TV_PHONE_INTERNET -> "📺"
    ExpenseCategory.WATER -> "💧"
    // Misc
    ExpenseCategory.GENERAL, ExpenseCategory.OTHER, null -> "📋"
}

private fun categoryBgColor(category: ExpenseCategory?): androidx.compose.ui.graphics.Color =
    when (category) {
        ExpenseCategory.DINING_OUT, ExpenseCategory.GROCERIES, ExpenseCategory.LIQUOR ->
            androidx.compose.ui.graphics.Color(0xFF1A2A0D)

        ExpenseCategory.CAR, ExpenseCategory.TAXI, ExpenseCategory.PLANE,
        ExpenseCategory.GAS_FUEL, ExpenseCategory.BUS_TRAIN, ExpenseCategory.BICYCLE,
        ExpenseCategory.PARKING ->
            androidx.compose.ui.graphics.Color(0xFF0D1A2A)

        ExpenseCategory.HOTEL, ExpenseCategory.RENT, ExpenseCategory.FURNITURE,
        ExpenseCategory.MAINTENANCE, ExpenseCategory.MORTGAGE ->
            androidx.compose.ui.graphics.Color(0xFF2A1A0D)

        ExpenseCategory.ELECTRICITY, ExpenseCategory.WATER, ExpenseCategory.TV_PHONE_INTERNET,
        ExpenseCategory.HEAT_GAS, ExpenseCategory.CLEANING, ExpenseCategory.TRASH ->
            androidx.compose.ui.graphics.Color(0xFF1A1A2A)

        ExpenseCategory.GAMES, ExpenseCategory.MOVIES, ExpenseCategory.MUSIC, ExpenseCategory.SPORTS ->
            androidx.compose.ui.graphics.Color(0xFF2A0D1A)

        else -> androidx.compose.ui.graphics.Color(0xFF1A1A1A)
    }