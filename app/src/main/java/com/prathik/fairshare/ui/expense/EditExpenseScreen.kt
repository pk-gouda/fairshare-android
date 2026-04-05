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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Edit Expense Screen.
 *
 * Reuses the same Splitwise-style layout as AddExpenseScreen:
 * - Description, amount, currency
 * - "Paid by [pill] and split [pill]"
 * - Date / Category / Note card
 * - Member avatars row
 *
 * Key differences from AddExpenseScreen:
 * - Title: "Edit expense"
 * - No tab toggle (no Transfer tab)
 * - No receipt scan
 * - No group selector (group is fixed from the original expense)
 * - Pre-fills all fields from the existing expense
 * - Button: "Save changes"
 * - Calls UpdateExpenseUseCase instead of CreateExpenseUseCase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    onBack              : () -> Unit,
    onSuccess           : () -> Unit,
    onNavigateToCurrency: () -> Unit,
    viewModel           : EditExpenseViewModel = hiltViewModel(),
) {
    val loadState    by viewModel.loadState.collectAsState()
    val saveState    by viewModel.saveState.collectAsState()
    val description  by viewModel.description.collectAsState()
    val amount       by viewModel.amount.collectAsState()
    val currency     by viewModel.currency.collectAsState()
    val groupName    by viewModel.groupName.collectAsState()
    val splitType    by viewModel.splitType.collectAsState()
    val category     by viewModel.category.collectAsState()
    val notes        by viewModel.notes.collectAsState()
    val expenseDate  by viewModel.expenseDate.collectAsState()
    val members      by viewModel.members.collectAsState()
    val payerData    by viewModel.payerData.collectAsState()
    val splitData    by viewModel.splitData.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    var showPayerSheet     by remember { mutableStateOf(false) }
    var showSplitSheet     by remember { mutableStateOf(false) }
    var showSplitTypeSheet by remember { mutableStateOf(false) }
    var showCategorySheet  by remember { mutableStateOf(false) }
    var showDatePicker     by remember { mutableStateOf(false) }
    var showNoteField      by remember { mutableStateOf(notes.isNotBlank()) }

    // Update showNoteField when notes load from expense
    LaunchedEffect(notes) {
        if (notes.isNotBlank()) showNoteField = true
    }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is EditSaveState.Success -> { onSuccess(); viewModel.resetSaveState() }
            is EditSaveState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetSaveState() }
            else -> Unit
        }
    }

    val isSaving         = saveState is EditSaveState.Loading
    val amountDouble     = amount.toDoubleOrNull() ?: 0.0
    val displayDate      = remember(expenseDate) { editFormatDisplayDate(expenseDate) }
    val categoryResolved : ExpenseCategory? = category

    val paidByText = when {
        payerData.isEmpty() -> "you"
        payerData.size == 1 -> if (payerData.keys.first() == viewModel.currentUserId) "you"
        else members.find { it.userId == payerData.keys.first() }?.fullName ?: "1 person"
        else -> "${payerData.size} people"
    }

    val splitLabel = when (splitType) {
        SplitType.EQUAL      -> "equally"
        SplitType.UNEQUAL    -> "by exact amount"
        SplitType.PERCENTAGE -> "by percentage"
        SplitType.SHARES     -> "by shares"
    }

    val categoryText = if (categoryResolved == null) "Auto-detect"
    else categoryResolved.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Edit expense", onBack = onBack) },
    ) { innerPadding ->

        when (loadState) {
            is EditLoadState.Loading -> {
                FsLoadingScreen()
                return@Scaffold
            }
            is EditLoadState.Error -> {
                val err = loadState as EditLoadState.Error
                FsErrorScreen(
                    message   = err.message,
                    isNetwork = err.isNetwork,
                    onRetry   = { /* re-init would need exposing loadExpense; for now back out */ onBack() },
                )
                return@Scaffold
            }
            is EditLoadState.Success -> { /* continue to form */ }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {

            // ── Group badge (read-only) ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Group, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Group", fontSize = 14.sp, color = TextSecondary)
                }
                Text(
                    text       = groupName ?: "No group (direct)",
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            // ── Description ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Edit, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Spacing.md))
                BasicTextField(
                    value         = description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    textStyle     = TextStyle(fontSize = 16.sp, color = TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (description.isBlank()) {
                                Text("Enter a description", fontSize = 16.sp, color = TextTertiary)
                            }
                            inner()
                        }
                    }
                )
            }
            HorizontalDivider(color = Green400, thickness = 1.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg))

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Amount ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Surface2)
                        .clickable { onNavigateToCurrency() }
                        .padding(horizontal = Spacing.sm, vertical = 6.dp),
                ) {
                    Text(editCurrencySymbol(currency), fontSize = 22.sp,
                        color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                val amountFontSize = when {
                    amount.length <= 4 -> 48.sp
                    amount.length <= 6 -> 36.sp
                    amount.length <= 8 -> 28.sp
                    else               -> 22.sp
                }
                BasicTextField(
                    value         = amount,
                    onValueChange = { new ->
                        val regex = Regex("^\\d*(\\.\\d{0,2})?$")
                        if (new.isEmpty() || regex.matches(new)) viewModel.onAmountChanged(new)
                    },
                    textStyle     = TextStyle(fontSize = amountFontSize, fontWeight = FontWeight.Bold, color = TextPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isBlank()) {
                                Text("0.00", fontSize = amountFontSize,
                                    color = TextTertiary, fontWeight = FontWeight.Bold)
                            }
                            inner()
                        }
                    }
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Surface2)
                        .clickable { onNavigateToCurrency() }
                        .padding(horizontal = Spacing.sm, vertical = 6.dp),
                ) {
                    Text(currency, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))

            // ── "Paid by [pill] and split [pill]" ────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Paid by", fontSize = 14.sp, color = TextSecondary)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Surface2)
                        .clickable { showPayerSheet = true }
                        .padding(horizontal = Spacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(paidByText, fontSize = 14.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Green400, modifier = Modifier.size(14.dp))
                }

                Text("and split", fontSize = 14.sp, color = TextSecondary)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Surface2)
                        .clickable { showSplitTypeSheet = true }
                        .padding(horizontal = Spacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(splitLabel, fontSize = 14.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Green400, modifier = Modifier.size(14.dp))
                }
            }

            HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))

            // ── Date / Category / Note card ───────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2),
            ) {
                EditCompactDetailRow(icon = Icons.Outlined.CalendarMonth, label = "Date",
                    value = displayDate, onClick = { showDatePicker = true })
                HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                EditCompactDetailRow(icon = Icons.Outlined.Category, label = "Category",
                    value = categoryText, onClick = { showCategorySheet = true })
                HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNoteField = true }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Message, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text("Note", fontSize = 14.sp, color = TextSecondary)
                    }
                    if (notes.isBlank()) Text("Add", fontSize = 14.sp, color = TextTertiary)
                    else Text(notes, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                if (showNoteField) {
                    HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg))
                    FsTextField(
                        value         = notes,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label         = "Add a note",
                        imeAction     = ImeAction.Done,
                        singleLine    = false,
                        maxLines      = 3,
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    )
                }
            }

            // ── Member avatars row ────────────────────────────────────────────
            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    members.forEach { member ->
                        val name = if (member.userId == viewModel.currentUserId) "You" else member.fullName
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = Spacing.md),
                        ) {
                            FsAvatar(name = member.fullName, userId = member.userId, size = 36.dp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(name, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        Icon(Icons.Outlined.CalendarMonth, "Date", tint = TextSecondary,
                            modifier = Modifier.size(22.dp).clickable { showDatePicker = true })
                        Icon(Icons.Outlined.Category, "Category", tint = TextSecondary,
                            modifier = Modifier.size(22.dp).clickable { showCategorySheet = true })
                        Icon(Icons.Outlined.Edit, "Split", tint = TextSecondary,
                            modifier = Modifier.size(22.dp).clickable {
                                if (splitType != SplitType.EQUAL) showSplitSheet = true
                            })
                    }
                }
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            FsPrimaryButton(
                text      = "Save changes",
                onClick   = { viewModel.save() },
                isLoading = isSaving,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }

    // ── Dialogs + Sheets ──────────────────────────────────────────────────────

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                FsTextButton(text = "OK", onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.onDateChanged(date.atStartOfDay().toString())
                    }
                    showDatePicker = false
                })
            },
            dismissButton = { FsTextButton(text = "Cancel", onClick = { showDatePicker = false }) },
        ) { DatePicker(state = datePickerState) }
    }

    if (showPayerSheet) {
        EditPayerBottomSheet(members = members, payerData = payerData, total = amountDouble,
            currentUserId = viewModel.currentUserId,
            onChanged = { userId, amt -> viewModel.onPayerChanged(userId, amt) },
            onDismiss = { showPayerSheet = false })
    }

    if (showSplitTypeSheet) {
        EditSplitTypeSheet(
            selected  = splitType,
            onSelect  = { type ->
                viewModel.onSplitTypeChanged(type)
                showSplitTypeSheet = false
                if (type != SplitType.EQUAL) showSplitSheet = true
            },
            onDismiss = { showSplitTypeSheet = false },
        )
    }

    if (showSplitSheet && splitType != SplitType.EQUAL) {
        EditSplitBottomSheet(splitType = splitType, members = members, splitData = splitData,
            total = amountDouble, currentUserId = viewModel.currentUserId,
            onConfirm = { newData -> viewModel.onSplitDataConfirmed(newData); showSplitSheet = false },
            onDismiss = { showSplitSheet = false })
    }

    if (showCategorySheet) {
        EditCategoryBottomSheet(selected = categoryResolved,
            onSelect = { viewModel.onCategoryChanged(it); showCategorySheet = false },
            onDismiss = { showCategorySheet = false })
    }
}

// ── Compact Detail Row ────────────────────────────────────────────────────────

@Composable
private fun EditCompactDetailRow(
    icon   : ImageVector,
    label  : String,
    value  : String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(label, fontSize = 14.sp, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null,
                tint = TextSecondary, modifier = Modifier.size(12.dp))
        }
    }
}

// ── Payer Bottom Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPayerBottomSheet(
    members      : List<GroupMember>,
    payerData    : Map<String, Double>,
    total        : Double,
    currentUserId: String?,
    onChanged    : (String, Double) -> Unit,
    onDismiss    : () -> Unit,
) {
    var multipleMode by remember { mutableStateOf(payerData.size > 1) }
    val initialSingle = payerData.keys.firstOrNull() ?: currentUserId
    var singleSelected by remember { mutableStateOf(initialSingle) }

    val multiAmounts = remember(members) {
        members.associate { member ->
            member.userId to mutableStateOf(
                payerData[member.userId]?.let {
                    if (it > 0) it.toBigDecimal().stripTrailingZeros().toPlainString() else ""
                } ?: ""
            )
        }
    }

    val multiSum = multiAmounts.values.sumOf { it.value.toDoubleOrNull() ?: 0.0 }
    val multiValid = total <= 0 || Math.abs(multiSum - total) < 0.01

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface2,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Who paid?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (multipleMode) Green400 else Surface3)
                        .clickable { multipleMode = !multipleMode }
                        .padding(horizontal = Spacing.md, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Multiple",
                        fontSize = 12.sp,
                        color = if (multipleMode) Surface0 else TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            HorizontalDivider(color = Surface4)

            if (!multipleMode) {
                members.forEach { member ->
                    val name     = if (member.userId == currentUserId) "You" else member.fullName
                    val selected = member.userId == singleSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                singleSelected = member.userId
                                members.forEach { m ->
                                    if (m.userId == member.userId)
                                        onChanged(m.userId, if (total > 0) total else 1.0)
                                    else
                                        onChanged(m.userId, 0.0)
                                }
                                onDismiss()
                            }
                            .background(if (selected) Surface4 else Surface2)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarSm)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(name,
                                color      = if (selected) Green400 else TextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) Green400 else Surface3),
                        ) {
                            if (selected) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50))
                                    .background(Surface0))
                            }
                        }
                    }
                    HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                }
            } else {
                if (total > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total: ${MoneyUtils.format(total, "USD")}", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text  = "Sum: ${MoneyUtils.format(multiSum, "USD")}",
                            fontSize = 12.sp,
                            color = if (multiValid) Green400 else Negative,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                members.forEach { member ->
                    val name       = if (member.userId == currentUserId) "You" else member.fullName
                    val amtState   = multiAmounts[member.userId] ?: return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarSm)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(name, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("$", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(end = 4.dp))
                        BasicTextField(
                            value         = amtState.value,
                            onValueChange = { new ->
                                if (new.isEmpty() || new.matches(Regex("^[0-9]*(\\.[0-9]{0,2})?$"))) amtState.value = new
                            },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = TextPrimary, textAlign = TextAlign.Start),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction    = ImeAction.Next,
                            ),
                            singleLine = true,
                            modifier   = Modifier.width(90.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (amtState.value.isEmpty()) {
                                        Text("0.00", fontSize = 15.sp, color = TextTertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                }

                Spacer(modifier = Modifier.height(Spacing.md))
                FsPrimaryButton(
                    text = if (multiValid) "Confirm" else "Amounts must sum to ${MoneyUtils.format(total, "USD")}",
                    enabled = multiValid,
                    onClick = {
                        members.forEach { m -> onChanged(m.userId, 0.0) }
                        multiAmounts.forEach { (userId, state) ->
                            val amt = state.value.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onChanged(userId, amt)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                )
            }
        }
    }
}

// ── Split Bottom Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSplitBottomSheet(
    splitType    : SplitType,
    members      : List<GroupMember>,
    splitData    : Map<String, Double>,
    total        : Double,
    currentUserId: String?,
    onConfirm    : (Map<String, Double>) -> Unit,
    onDismiss    : () -> Unit,
) {
    val localValues = remember(splitType, members) {
        members.associate { member ->
            val existing = splitData[member.userId]
            member.userId to mutableStateOf(
                when (splitType) {
                    SplitType.SHARES     -> (existing ?: 1.0).toInt().toString()
                    SplitType.PERCENTAGE -> if (existing != null && existing > 0)
                        existing.toBigDecimal().stripTrailingZeros().toPlainString() else ""
                    else                 -> if (existing != null && existing > 0)
                        existing.toBigDecimal().stripTrailingZeros().toPlainString() else ""
                }
            )
        }
    }

    val localIncluded = remember(members) {
        members.associate { member ->
            member.userId to mutableStateOf(
                splitData.isEmpty() || splitData.containsKey(member.userId)
            )
        }
    }

    val includedMembers = members.filter { localIncluded[it.userId]?.value == true }

    val title = when (splitType) {
        SplitType.UNEQUAL    -> "Split exact amounts"
        SplitType.PERCENTAGE -> "Split by percentage"
        SplitType.SHARES     -> "Split by shares"
        else -> ""
    }
    val hint = when (splitType) {
        SplitType.UNEQUAL    -> "Must sum to ${MoneyUtils.format(total, "USD")}"
        SplitType.PERCENTAGE -> "Must sum to 100%"
        SplitType.SHARES     -> "Any whole numbers (e.g. 2, 1, 1)"
        else -> ""
    }

    val currentSum = includedMembers.sumOf { localValues[it.userId]?.value?.toDoubleOrNull() ?: 0.0 }
    val isValid = includedMembers.isNotEmpty() && when (splitType) {
        SplitType.UNEQUAL    -> Math.abs(currentSum - total) < 0.01
        SplitType.PERCENTAGE -> Math.abs(currentSum - 100.0) < 0.01
        SplitType.SHARES     -> includedMembers.all {
            val v = localValues[it.userId]?.value?.toDoubleOrNull() ?: 0.0; v > 0 && v == Math.floor(v)
        }
        else -> true
    }
    val sumColor = if (isValid) Green400 else Negative

    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.xxxl)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(hint, fontSize = 12.sp, color = TextSecondary)
                }
                if (splitType != SplitType.SHARES) {
                    val sumLabel = when (splitType) {
                        SplitType.UNEQUAL    -> MoneyUtils.format(currentSum, "USD")
                        SplitType.PERCENTAGE -> "${currentSum.toBigDecimal().stripTrailingZeros().toPlainString()}%"
                        else -> ""
                    }
                    Text(sumLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = sumColor)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)

            members.forEach { member ->
                val name       = if (member.userId == currentUserId) "You" else member.fullName
                val valueState = localValues[member.userId] ?: return@forEach
                val includedState = localIncluded[member.userId] ?: return@forEach
                val isIncluded = includedState.value
                val suffix     = when (splitType) {
                    SplitType.PERCENTAGE -> "%"; SplitType.SHARES -> "shares"; else -> "$"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIncluded) Green400 else Surface4)
                            .clickable { includedState.value = !isIncluded },
                    ) {
                        if (isIncluded) Text("✓", fontSize = 12.sp, color = Surface0, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarSm)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(name, fontSize = 14.sp, color = if (isIncluded) TextPrimary else TextSecondary,
                        modifier = Modifier.weight(1f))

                    if (isIncluded) {
                        if (splitType == SplitType.UNEQUAL) {
                            Text("$", fontSize = 14.sp, color = TextSecondary,
                                modifier = Modifier.padding(end = 4.dp))
                        }
                        BasicTextField(
                            value         = valueState.value,
                            onValueChange = { new ->
                                val regex = if (splitType == SplitType.SHARES) Regex("^\\d*$")
                                else Regex("^\\d*(\\.\\d{0,2})?$")
                                if (new.isEmpty() || regex.matches(new)) valueState.value = new
                            },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = TextPrimary, textAlign = TextAlign.Start),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (splitType == SplitType.SHARES) KeyboardType.Number
                                else KeyboardType.Decimal,
                                imeAction    = ImeAction.Next,
                            ),
                            singleLine = true,
                            modifier   = Modifier.width(90.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (valueState.value.isEmpty()) {
                                        Text("0", fontSize = 15.sp, color = TextTertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                        if (splitType != SplitType.UNEQUAL) {
                            Text(suffix, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            FsPrimaryButton(
                text = if (isValid) "Confirm" else when (splitType) {
                    SplitType.UNEQUAL    -> "Amounts must sum to ${MoneyUtils.format(total, "USD")}"
                    SplitType.PERCENTAGE -> "Percentages must sum to 100%"
                    SplitType.SHARES     -> "Enter whole numbers for each person"
                    else -> "Confirm"
                },
                onClick = {
                    if (isValid) {
                        onConfirm(includedMembers.associate { member ->
                            member.userId to (localValues[member.userId]?.value?.toDoubleOrNull() ?: 0.0)
                        }.filter { it.value > 0 })
                    }
                },
                enabled  = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Category Bottom Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryBottomSheet(
    selected : ExpenseCategory?,
    onSelect : (ExpenseCategory?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text("Category", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
            HorizontalDivider(color = Surface4)
            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }
                .background(if (selected == null) Surface4 else Surface2)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-detect", color = if (selected == null) Green400 else TextSecondary,
                    fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal)
                if (selected == null) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            ExpenseCategory.entries.forEach { cat ->
                val isSel = cat == selected
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(cat) }
                    .background(if (isSel) Surface4 else Surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                        color = if (isSel) Green400 else TextPrimary,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                    if (isSel) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            }
        }
    }
}

// ── Split Type Sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSplitTypeSheet(
    selected : SplitType,
    onSelect : (SplitType) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        SplitType.EQUAL      to "Equally"           to "Split the total evenly between everyone",
        SplitType.UNEQUAL    to "Exact amounts"     to "Enter how much each person owes",
        SplitType.PERCENTAGE to "By percentage"     to "Enter a % for each person (must sum to 100)",
        SplitType.SHARES     to "By shares"         to "Enter a ratio, e.g. 2:1:1",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface2,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text("Split method", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
            HorizontalDivider(color = Surface4)
            options.forEach { (labelPair, description) ->
                val (type, label) = labelPair
                val isSel = type == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                        .background(if (isSel) Surface4 else Surface2)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label,
                            fontSize   = 15.sp,
                            color      = if (isSel) Green400 else TextPrimary,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(description, fontSize = 12.sp, color = TextSecondary)
                    }
                    if (isSel) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun editCurrencySymbol(code: String): String = when (code) {
    "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"; "JPY" -> "¥"
    "CNY" -> "¥"; "KRW" -> "₩"; "RUB" -> "₽"; "BRL" -> "R$"; "CAD" -> "CA$"
    "AUD" -> "A$"; "SGD" -> "S$"; "HKD" -> "HK$"; "MXN" -> "MX$"; "TRY" -> "₺"
    "THB" -> "฿"; "IDR" -> "Rp"; "MYR" -> "RM"; "PHP" -> "₱"; "VND" -> "₫"
    "PKR" -> "₨"; "BDT" -> "৳"; "NGN" -> "₦"; "ZAR" -> "R"; "AED" -> "د.إ"
    "SAR" -> "﷼"; "PLN" -> "zł"; "SEK" -> "kr"; "NOK" -> "kr"; "DKK" -> "kr"
    else  -> code
}

private fun editFormatDisplayDate(isoDate: String): String {
    return try {
        val dateTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val date     = dateTime.toLocalDate()
        val today    = LocalDate.now()
        when {
            date == today              -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) { "Today" }
}