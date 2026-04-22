package com.prathik.fairshare.ui.expense

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notes
import com.prathik.fairshare.ui.theme.TextTertiary
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSectionLabel
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.domain.model.ExpenseChangeLog
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.util.MoneyUtils
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Expense Detail Screen.
 *
 * Shows full expense info:
 * - Large amount + description header
 * - Meta: group, date, category, notes
 * - Who paid section
 * - Split breakdown (each member's share + settled status)
 * - Your balance summary
 * - Settle Up button (if you owe)
 * - Edit / Delete (if you created the expense)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettle: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val expenseState  by viewModel.expenseState.collectAsState()
    val actionState   by viewModel.actionState.collectAsState()
    val items            by viewModel.items.collectAsState()
    val itemsLoading     by viewModel.itemsLoading.collectAsState()
    val changeLog        by viewModel.changeLog.collectAsState()
    val changeLogLoading by viewModel.changeLogLoading.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isLoading = expenseState is ExpenseDetailUiState.Loading

    // Auto-refresh when screen resumes (e.g. returning from EditExpense)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadExpense()
        }
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ExpenseActionState.Deleted -> {
                onDeleted(); viewModel.resetActionState()
            }

            is ExpenseActionState.Restored -> {
                viewModel.loadExpense(); viewModel.resetActionState()
            }

            is ExpenseActionState.Error -> {
                snackbarHost.showSnackbar(state.message); viewModel.resetActionState()
            }

            else -> Unit
        }
    }

    // Expense truly does not exist on server (different from soft-deleted)
    LaunchedEffect(expenseState) {
        if (expenseState is ExpenseDetailUiState.Deleted) {
            snackbarHost.showSnackbar("This expense no longer exists")
            onBack()
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title = "Expense detail",
                onBack = onBack,
                actions = {
                    val expense = (expenseState as? ExpenseDetailUiState.Success)?.expense
                    if (expense != null) {
                        if (expense.isDeleted) {
                            // Deleted expense — show Restore only
                            FsTextButton(
                                text = "Restore",
                                onClick = { viewModel.restoreExpense() },
                            )
                        } else if (expense.canEdit) {
                            // Active expense — show Edit + Delete
                            FsIconButton(
                                icon = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                onClick = { onNavigateToEdit(viewModel.expenseId) },
                            )
                            FsIconButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                onClick = { showDeleteDialog = true },
                                tint = Negative,
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = expenseState) {
                is ExpenseDetailUiState.Loading -> FsLoadingScreen()
                is ExpenseDetailUiState.Deleted -> FsLoadingScreen() // briefly shown before LaunchedEffect pops back
                is ExpenseDetailUiState.Error -> FsErrorScreen(
                    message = state.message, isNetwork = state.isNetwork,
                    onRetry = { viewModel.loadExpense() })

                is ExpenseDetailUiState.Success -> ExpenseDetailContent(
                    expense = state.expense,
                    currentUserId = viewModel.currentUserId,
                    items = items,
                    onSettle = { onNavigateToSettle(it) },
                    isDeleting = actionState is ExpenseActionState.Loading,
                    changeLog = changeLog,
                    changeLogLoading = changeLogLoading,
                    onRestore = { viewModel.restoreExpense() },
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete expense?") },
            text = { Text("This will permanently delete the expense and update all balances. This cannot be undone.") },
            confirmButton = {
                FsPrimaryButton(
                    text = "Delete",
                    onClick = { showDeleteDialog = false; viewModel.deleteExpense() },
                    isLoading = actionState is ExpenseActionState.Loading,
                )
            },
            dismissButton = {
                FsTextButton(text = "Cancel", onClick = { showDeleteDialog = false })
            },
            containerColor = Surface2,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun ExpenseDetailContent(
    expense          : Expense,
    currentUserId    : String?,
    items            : List<com.prathik.fairshare.domain.model.ExpenseItem>,
    onSettle         : (String) -> Unit,
    isDeleting       : Boolean,
    changeLog        : List<ExpenseChangeLog>,
    changeLogLoading : Boolean,
    onRestore        : () -> Unit = {},
) {
    var showItemBreakdown by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Amount hero ───────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl),
        ) {
            // Category emoji
            Text(
                text = categoryEmoji(expense.category?.name),
                fontSize = 36.sp,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = MoneyUtils.format(expense.totalAmount, expense.currency),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 42.sp,
                color = if (expense.isDeleted) TextSecondary else TextPrimary,
                textDecoration = if (expense.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = expense.description,
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                textDecoration = if (expense.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
            )
            // Deleted-by line
            if (expense.isDeleted) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                val deletedLabel = buildString {
                    append("Deleted")
                    if (expense.deletedByName != null) append(" by ${expense.deletedByName}")
                    if (expense.deletedAt != null) {
                        val date = try {
                            java.time.LocalDateTime.parse(expense.deletedAt,
                                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .toLocalDate()
                                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        } catch (e: Exception) { expense.deletedAt.take(10) }
                        append(" on $date")
                    }
                }
                Text(
                    text = deletedLabel,
                    fontSize = 13.sp,
                    color = Negative,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Your balance pill — hidden for deleted expenses
            if (!expense.isDeleted) {
                Spacer(modifier = Modifier.height(Spacing.md))
                val balanceColor = when {
                    expense.yourBalance > 0 -> Green400
                    expense.yourBalance < 0 -> Negative
                    else -> TextSecondary
                }
                val balanceText = when {
                    expense.yourBalance > 0 -> "you get back ${
                        MoneyUtils.format(expense.yourBalance, expense.currency)
                    }"
                    expense.yourBalance < 0 -> "you owe ${
                        MoneyUtils.format(-expense.yourBalance, expense.currency)
                    }"
                    else -> "you're settled up"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(balanceColor.copy(alpha = 0.12f))
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Text(
                        text = balanceText,
                        fontSize = 13.sp,
                        color = balanceColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(color = Surface4, thickness = 0.5.dp)

        // ── Meta card ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2),
        ) {
            if (expense.groupName != null) {
                MetaRow(icon = "👥", label = "Group", value = expense.groupName)
                HorizontalDivider(
                    color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            MetaRow(
                icon = "📅",
                label = "Date",
                value = formatDate(expense.expenseDate),
            )
            if (expense.category != null) {
                HorizontalDivider(
                    color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
                MetaRow(
                    icon = categoryEmoji(expense.category.name),
                    label = "Category",
                    value = expense.category.name.lowercase().replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                )
            }
            if (!expense.notes.isNullOrBlank()) {
                HorizontalDivider(
                    color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
                MetaRow(icon = "📝", label = "Notes", value = expense.notes)
            }

            // ── Receipt image ──────────────────────────────────────────────────
            expense.receipt?.imageUrl?.takeIf { it.isNotBlank() && !it.startsWith("pending") && !it.startsWith("s3-upload-failed") }?.let { imageUrl ->
                HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧾", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Receipt", fontSize = 14.sp, color = TextSecondary,
                            modifier = Modifier.weight(1f))
                        expense.receipt.merchantName?.let {
                            Text(it, fontSize = 13.sp,
                                color = com.prathik.fairshare.ui.theme.TextTertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    var showFullImage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Receipt",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.lg))
                            .clickable { showFullImage = true },
                    )
                    if (showFullImage) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showFullImage = false }) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Receipt full size",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.lg))
                                    .clickable { showFullImage = false },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = Surface4, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )
            MetaRow(
                icon = "👤",
                label = "Added by",
                value = if (expense.addedById == currentUserId) "You" else expense.addedByName,
            )
        }

        // ── Who paid ──────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(Spacing.sm))
        FsSectionLabel(
            text = "Who paid",
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2),
        ) {
            expense.payers.forEachIndexed { index, payer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val name = if (payer.userId == currentUserId) "You" else payer.fullName
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = MoneyUtils.format(payer.amountPaid, expense.currency),
                        fontSize = 15.sp,
                        color = Green400,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (index < expense.payers.lastIndex) {
                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                }
            }
        }

        // ── Split breakdown ───────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(Spacing.lg))
        FsSectionLabel(
            text = "Split breakdown",
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2),
        ) {
            expense.splits.forEachIndexed { index, split ->
                val name = if (split.userId == currentUserId) "You" else split.fullName
                val rowColor = when {
                    split.isSettled -> TextSecondary
                    split.userId == currentUserId && expense.yourBalance < 0 -> Negative
                    else -> TextPrimary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            color = rowColor,
                            fontWeight = FontWeight.Medium
                        )
                        if (split.isSettled) {
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Radius.xs))
                                    .background(Green400.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "settled",
                                    fontSize = 10.sp,
                                    color = Green400,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    // Show amount + percentage/shares if applicable
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = MoneyUtils.format(split.amountOwed, expense.currency),
                            fontSize = 15.sp,
                            color = if (split.isSettled) TextSecondary else rowColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (expense.splitType == SplitType.PERCENTAGE && split.percentage != null) {
                            Text(
                                text = "${split.percentage.toInt()}%",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                        if (expense.splitType == SplitType.SHARES && split.shares != null) {
                            Text(
                                text = "${split.shares} shares",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                if (index < expense.splits.lastIndex) {
                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                }
            }
        }


        // ── Item breakdown (expandable) ───────────────────────────────────────
        if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Tap to expand row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .clickable { showItemBreakdown = !showItemBreakdown }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint               = Green400,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text       = "View item breakdown",
                        fontSize   = 14.sp,
                        color      = Green400,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    imageVector        = if (showItemBreakdown) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint               = Green400,
                    modifier           = Modifier.size(18.dp),
                )
            }

            // Expanded item list
            if (showItemBreakdown) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    items.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = item.name,
                                        fontSize   = 14.sp,
                                        color      = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    item.quantity?.let { qty ->
                                        if (qty > 1) Text("×$qty", fontSize = 12.sp, color = TextTertiary)
                                    }
                                }
                                Text(
                                    text       = MoneyUtils.format(item.totalPrice, expense.currency),
                                    fontSize   = 14.sp,
                                    color      = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            // Assigned members
                            if (item.assignedTo.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val assignedNames = item.assignedTo.map { assigned ->
                                    if (assigned.userId == currentUserId) "You" else assigned.fullName.split(" ").first()
                                }
                                Text(
                                    text     = assignedNames.joinToString(", "),
                                    fontSize = 12.sp,
                                    color    = Green400,
                                )
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Split equally among all", fontSize = 12.sp, color = TextTertiary)
                            }
                        }
                        if (index < items.lastIndex) {
                            HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = Spacing.lg))
                        }
                    }
                }
            }
        }

        // ── Settle Up button (only if you owe, and not deleted) ─────────────
        if (!expense.isDeleted && expense.yourBalance < 0) {
            val payer = expense.payers.firstOrNull()
            if (payer != null) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                FsPrimaryButton(
                    text = "Settle up · ${
                        MoneyUtils.format(
                            -expense.yourBalance,
                            expense.currency
                        )
                    }",
                    onClick = { onSettle(payer.userId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }
        }

        // ── Change Log (hidden for deleted expenses) ─────────────────────────
        if (!expense.isDeleted && (changeLogLoading || changeLog.isNotEmpty())) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            FsSectionLabel(
                text     = "CHANGE HISTORY",
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (changeLogLoading) {
                Box(
                    modifier        = Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Green400,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    changeLog.forEach { entry: ExpenseChangeLog ->
                        ChangeLogEntry(entry = entry)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxxl))
    }
}

// ── Change Log Entry ──────────────────────────────────────────────────────────

@Composable
private fun ChangeLogEntry(entry: ExpenseChangeLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            text       = "${entry.changedByName} updated this expense:",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = TextPrimary,
        )
        entry.changes.forEach { change ->
            Text(
                text     = buildChangeText(change),
                fontSize = 13.sp,
                color    = TextSecondary,
            )
        }
    }
}

private fun buildChangeText(change: ExpenseChangeLog.FieldChange): String {
    return when (change.fieldName) {
        "cost"          -> "- Cost changed from ${change.oldValue} to ${change.newValue}"
        "currency"      -> "- Currency changed from ${change.oldValue} to ${change.newValue}"
        "splitType"     -> "- Split type changed from ${change.oldValue} to ${change.newValue}"
        "payer"         -> "- Payer changed from ${change.oldValue} to ${change.newValue}"
        "memberAdded"   -> "- ${change.oldValue} added to split"
        "memberRemoved" -> "- ${change.oldValue} removed from split"
        else            -> "- ${change.fieldName} changed"
    }
}

// ── Meta Row ──────────────────────────────────────────────────────────────────

@Composable
private fun MetaRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(text = label, fontSize = 15.sp, color = TextSecondary)
        }
        Text(
            text = value,
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDate(isoDate: String): String {
    return try {
        val date = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun categoryEmoji(category: String?): String = when (category) {
    "DINING_OUT" -> "🍽️"
    "GROCERIES" -> "🛒"
    "TAXI" -> "🚕"
    "BUS_TRAIN" -> "🚌"
    "PLANE" -> "✈️"
    "HOTEL" -> "🏨"
    "RENT" -> "🏠"
    "ELECTRICITY" -> "⚡"
    "WATER" -> "💧"
    "GAS_FUEL" -> "⛽"
    "TV_PHONE_INTERNET" -> "📱"
    "MOVIES" -> "🎬"
    "GAMES" -> "🎮"
    "MUSIC" -> "🎵"
    "SPORTS" -> "⚽"
    "MEDICAL" -> "💊"
    "EDUCATION" -> "📚"
    "GIFTS" -> "🎁"
    "LIQUOR" -> "🍺"
    "PETS" -> "🐾"
    "CLOTHING" -> "👕"
    "PARKING" -> "🅿️"
    else -> "💰"
}