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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsSectionLabel
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.util.DateFormatter
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Group Detail Screen.
 *
 * Shows:
 * - Group name in top bar with settings overflow
 * - Balance card — how much you lent/owe in this group
 * - Expense list grouped by month section headers
 * - FAB to add expense in this group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack              : () -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onNavigateToExpense : (String) -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToSettle  : (String) -> Unit,
    viewModel           : GroupDetailViewModel = hiltViewModel(),
) {
    val groupState    by viewModel.groupState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val yourBalance   by viewModel.yourBalance.collectAsState()
    val isLoading = groupState is GroupDetailUiState.Loading

    val groupName = (groupState as? GroupDetailUiState.Success)?.group?.name ?: "Group"

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title   = groupName,
                onBack  = onBack,
                actions = {
                    FsIconButton(
                        icon               = Icons.Filled.MoreVert,
                        contentDescription = "Settings",
                        onClick            = { onNavigateToSettings(viewModel.groupId) },
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { onNavigateToAddExpense(viewModel.groupId) },
                containerColor = Green400,
                contentColor   = Surface0,
                shape          = RoundedCornerShape(Radius.full),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    modifier           = Modifier.size(24.dp),
                )
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh    = { viewModel.loadData() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = groupState) {
                is GroupDetailUiState.Loading -> FsLoadingScreen()

                is GroupDetailUiState.Error -> FsErrorScreen(
                    message   = state.message,
                    isNetwork = state.isNetwork,
                    onRetry   = { viewModel.loadData() },
                )

                is GroupDetailUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // ── Balance card ──────────────────────────────────────
                        item {
                            BalanceCard(
                                yourBalance = yourBalance,
                                currency    = state.group.memberCount.toString(),
                                onSettle    = { onNavigateToSettle(viewModel.groupId) },
                                modifier    = Modifier.padding(Spacing.lg),
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
                                if (expenses.expenses.isEmpty()) {
                                    item {
                                        FsEmptyState(
                                            title    = "No expenses yet",
                                            subtitle = "Add the first expense to get started",
                                            modifier = Modifier.height(300.dp),
                                        )
                                    }
                                } else {
                                    // Group expenses by month
                                    val grouped = expenses.expenses
                                        .groupBy { it.expenseDate.toMonthHeader() }

                                    grouped.forEach { (monthHeader, monthExpenses) ->
                                        item {
                                            FsSectionLabel(
                                                text     = monthHeader,
                                                modifier = Modifier.padding(
                                                    horizontal = Spacing.lg,
                                                    vertical   = Spacing.sm,
                                                )
                                            )
                                        }
                                        items(
                                            items = monthExpenses,
                                            key   = { it.id },
                                        ) { expense ->
                                            ExpenseRow(
                                                expense = expense,
                                                onClick = { onNavigateToExpense(expense.id) },
                                            )
                                            HorizontalDivider(
                                                color     = Surface4,
                                                thickness = 0.5.dp,
                                                modifier  = Modifier.padding(horizontal = Spacing.lg),
                                            )
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
    }
}

// ── Balance Card ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    yourBalance: Double,
    currency   : String,
    onSettle   : () -> Unit,
    modifier   : Modifier = Modifier,
) {
    val balanceColor = when {
        yourBalance > 0 -> Green400
        yourBalance < 0 -> Negative
        else            -> TextSecondary
    }

    val balanceLabel = when {
        yourBalance > 0 -> "You are owed"
        yourBalance < 0 -> "You owe"
        else            -> "All settled up"
    }

    val balanceText = when {
        yourBalance > 0 -> "+${MoneyUtils.format(yourBalance, "USD")}"
        yourBalance < 0 -> "-${MoneyUtils.format(Math.abs(yourBalance), "USD")}"
        else            -> MoneyUtils.format(0.0, "USD")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(Spacing.lg),
    ) {
        Column {
            Text(
                text     = balanceLabel.uppercase(),
                fontSize = 11.sp,
                color    = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text       = balanceText,
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 36.sp,
                color      = balanceColor,
            )

            if (yourBalance != 0.0) {
                Spacer(modifier = Modifier.height(Spacing.md))

                LinearProgressIndicator(
                    progress        = { if (yourBalance > 0) 0.6f else 0.4f },
                    modifier        = Modifier.fillMaxWidth(),
                    color           = balanceColor,
                    trackColor      = Surface4,
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Settle up button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Green400)
                        .clickable(onClick = onSettle)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Text(
                        text       = "Settle up →",
                        color      = Surface0,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                    )
                }
            }
        }
    }
}

// ── Expense Row ───────────────────────────────────────────────────────────────

@Composable
private fun ExpenseRow(
    expense: Expense,
    onClick: () -> Unit,
) {
    val balanceColor = when {
        expense.yourBalance > 0 -> Green400
        expense.yourBalance < 0 -> Negative
        else                    -> TextSecondary
    }

    val balanceText = when {
        expense.yourBalance > 0 -> "+${MoneyUtils.format(expense.yourBalance, expense.currency)}"
        expense.yourBalance < 0 -> "-${MoneyUtils.format(Math.abs(expense.yourBalance), expense.currency)}"
        else                    -> "settled"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category emoji
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Surface2),
        ) {
            Text(
                text     = categoryEmoji(expense.category),
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Description + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = expense.description,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = "${expense.addedByName} · ${expense.expenseDate.toRelativeDate()}",
                fontSize = 12.sp,
                color    = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Amount + your share
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = MoneyUtils.format(expense.totalAmount, expense.currency),
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = balanceText,
                fontSize = 12.sp,
                color    = balanceColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
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

private fun String.toRelativeDate(): String {
    return try {
        val dateTime = LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        DateFormatter.toRelativeTime(dateTime.toString())
    } catch (e: Exception) {
        this
    }
}

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    ExpenseCategory.DINING_OUT   -> "🍽️"
    ExpenseCategory.GROCERIES    -> "🛒"
    ExpenseCategory.CAR          -> "🚗"
    ExpenseCategory.TAXI         -> "🚕"
    ExpenseCategory.PLANE        -> "✈️"
    ExpenseCategory.HOTEL        -> "🏨"
    ExpenseCategory.RENT         -> "🏠"
    ExpenseCategory.ELECTRICITY  -> "⚡"
    ExpenseCategory.MOVIES       -> "🎬"
    ExpenseCategory.GAMES        -> "🎮"
    ExpenseCategory.MUSIC        -> "🎵"
    ExpenseCategory.SPORTS       -> "⚽"
    ExpenseCategory.MEDICAL      -> "💊"
    ExpenseCategory.EDUCATION    -> "📚"
    ExpenseCategory.GIFTS        -> "🎁"
    ExpenseCategory.LIQUOR       -> "🍺"
    ExpenseCategory.PETS         -> "🐾"
    ExpenseCategory.CLOTHING     -> "👕"
    ExpenseCategory.BUS_TRAIN    -> "🚌"
    else                         -> "💰"
}