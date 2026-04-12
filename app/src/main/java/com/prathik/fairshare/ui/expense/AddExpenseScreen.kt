package com.prathik.fairshare.ui.expense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.ui.components.FsAvatar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack              : () -> Unit,
    onSuccess           : () -> Unit,
    onNavigateToCurrency: () -> Unit,
    viewModel           : AddExpenseViewModel = hiltViewModel(),
) {
    val uiState           by viewModel.uiState.collectAsState()
    val description       by viewModel.description.collectAsState()
    val amount            by viewModel.amount.collectAsState()
    val currency          by viewModel.currency.collectAsState()
    val selectedGroupId   by viewModel.selectedGroupId.collectAsState()
    val splitType         by viewModel.splitType.collectAsState()
    val category          by viewModel.category.collectAsState()
    val notes             by viewModel.notes.collectAsState()
    val expenseDate       by viewModel.expenseDate.collectAsState()
    val groups            by viewModel.groups.collectAsState()
    val members           by viewModel.members.collectAsState()
    val payerData         by viewModel.payerData.collectAsState()
    val splitData         by viewModel.splitData.collectAsState()
    val equalExcluded     by viewModel.equalExcluded.collectAsState()
    val receiptState      by viewModel.receiptState.collectAsState()
    val preselectedFriend by viewModel.preselectedFriend.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    var showGroupSheet    by remember { mutableStateOf(false) }
    var showPayerSheet    by remember { mutableStateOf(false) }
    var showSplitTypeSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var showNoteField     by remember { mutableStateOf(notes.isNotBlank()) }

    val receiptScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { viewModel.scanReceipt(it) } }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AddExpenseUiState.Success -> { onSuccess(); viewModel.resetUiState() }
            is AddExpenseUiState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetUiState() }
            else -> Unit
        }
    }

    val isLoading        = uiState is AddExpenseUiState.Loading
    val amountDouble     = amount.toDoubleOrNull() ?: 0.0
    val displayDate      = remember(expenseDate) { formatDisplayDate(expenseDate) }
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
        // ── Fixed bottom bar — Date | Category | Note ─────────────────────────
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface0)
                    .border(width = 0.5.dp, color = Surface4,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Date chip
                BottomChip(
                    icon  = Icons.Outlined.CalendarMonth,
                    label = displayDate,
                    onClick = { showDatePicker = true },
                )
                // Category chip
                BottomChip(
                    icon  = Icons.Outlined.Category,
                    label = categoryText,
                    onClick = { showCategorySheet = true },
                )
                // Note chip
                BottomChip(
                    icon  = Icons.Outlined.Message,
                    label = if (notes.isBlank()) "Add note" else notes,
                    onClick = { showNoteField = !showNoteField },
                )
            }
        },
    ) { innerPadding ->

        if (isLoading) { FsLoadingScreen(); return@Scaffold }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {

            // ── Custom top bar ────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // X close button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                ) {
                    Text("✕", fontSize = 18.sp, color = TextSecondary)
                }

                // Group pill — center
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (preselectedFriend != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.full))
                                .background(Surface2)
                                .padding(horizontal = Spacing.md, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FsAvatar(name = preselectedFriend!!.fullName,
                                userId = preselectedFriend!!.id, size = 20.dp)
                            Text(preselectedFriend!!.fullName, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.full))
                                .background(Surface2)
                                .clickable { showGroupSheet = true }
                                .padding(horizontal = Spacing.md, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.Group, null, tint = TextSecondary,
                                modifier = Modifier.size(14.dp))
                            Text(
                                text       = groups.find { it.id == selectedGroupId }?.name ?: "No group",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (selectedGroupId == null) TextSecondary else TextPrimary,
                            )
                            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = TextSecondary,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Save button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(Green400)
                        .clickable { viewModel.submit() }
                        .padding(horizontal = Spacing.md, vertical = 8.dp),
                ) {
                    Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Surface0)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Scan receipt button ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface0)
                    .border(1.dp, Green400.copy(alpha = 0.5f), RoundedCornerShape(Radius.xl))
                    .clickable { receiptScanLauncher.launch(null) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint               = Green400,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when (receiptState) {
                        is ReceiptScanState.Success  -> "Receipt scanned ✓"
                        is ReceiptScanState.Scanning -> "Scanning…"
                        else                         -> "Scan receipt to auto-fill"
                    },
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Green400,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Amount ────────────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Currency symbol — tappable, top-aligned, lighter
                    Text(
                        text       = currencySymbol(currency),
                        fontSize   = 28.sp,
                        color      = TextSecondary,
                        fontWeight = FontWeight.Light,
                        modifier   = Modifier
                            .padding(top = 10.dp, end = 4.dp)
                            .clickable { onNavigateToCurrency() },
                    )
                    BasicTextField(
                        value         = amount,
                        onValueChange = { new ->
                            val regex = Regex("^\\d*(\\.\\d{0,2})?$")
                            if (new.isEmpty() || regex.matches(new)) {
                                // Cap at 100,000,000 (100M)
                                val parsed = new.toDoubleOrNull() ?: 0.0
                                if (parsed <= 100_000_000.0) viewModel.onAmountChanged(new)
                            }
                        },
                        textStyle     = TextStyle(
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next),
                        singleLine    = true,
                        modifier      = Modifier.widthIn(min = 60.dp, max = 280.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (amount.isBlank()) {
                                    Text("0", fontSize = 48.sp, fontWeight = FontWeight.Bold,
                                        color = Surface4, textAlign = TextAlign.Center)
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            // ── Description — boxed input ─────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .border(1.dp, Surface4, RoundedCornerShape(Radius.xl))
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Edit, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.sm))
                BasicTextField(
                    value         = description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    textStyle     = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (description.isBlank()) {
                                Text("What for? e.g. Dinner, Uber, Hotel",
                                    fontSize = 15.sp, color = TextTertiary)
                            }
                            inner()
                        }
                    }
                )
            }

            // ── Itemize button — only shown after successful receipt scan ──────
            if (receiptState is ReceiptScanState.Success) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface0)
                        .border(1.dp, Green400.copy(alpha = 0.5f), RoundedCornerShape(Radius.xl))
                        .clickable { /* TODO: navigate to itemize screen */ }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Category, null, tint = Green400, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Itemize receipt", fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = Green400)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Section separator ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Surface0)
                .border(0.5.dp, Surface3, RoundedCornerShape(0.dp)))

            // ── Paid by — horizontal chips ────────────────────────────────────
            if (members.isNotEmpty()) {
                Text(
                    text          = "PAID BY",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = TextTertiary,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
                )
                LazyRow(
                    modifier            = Modifier.fillMaxWidth(),
                    contentPadding      = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(members) { member ->
                        val isSelected = payerData.isEmpty() && member.userId == viewModel.currentUserId
                                || payerData.containsKey(member.userId)
                        val name = if (member.userId == viewModel.currentUserId) "You"
                        else member.fullName.split(" ").firstOrNull() ?: member.fullName
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.full))
                                .background(if (isSelected) Green400.copy(alpha = 0.15f) else Surface2)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Green400 else Surface4,
                                    shape = RoundedCornerShape(Radius.full),
                                )
                                .clickable {
                                    // Set as sole payer. Use max(amount,1.0) so
                                    // the tap registers even before amount is entered
                                    // (ViewModel updates amount to actual total later)
                                    val payAmt = if (amountDouble > 0) amountDouble else 1.0
                                    members.forEach { m ->
                                        viewModel.onPayerChanged(m.userId,
                                            if (m.userId == member.userId) payAmt else 0.0)
                                    }
                                }
                                .padding(horizontal = Spacing.md, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FsAvatar(name = member.fullName, userId = member.userId, size = 22.dp)
                            Text(
                                text       = name,
                                fontSize   = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isSelected) Green400 else TextPrimary,
                            )
                        }
                    }
                    // Multiple payers option
                    item {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.full))
                                .background(if (payerData.size > 1) Green400.copy(alpha = 0.15f) else Surface2)
                                .border(1.dp,
                                    if (payerData.size > 1) Green400 else Surface4,
                                    RoundedCornerShape(Radius.full))
                                .clickable { showPayerSheet = true }
                                .padding(horizontal = Spacing.md, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = "+ Multiple",
                                fontSize = 13.sp,
                                color = if (payerData.size > 1) Green400 else TextSecondary,
                            )
                        }
                    }
                }

                HorizontalDivider(color = Surface4, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))

                // ── Split [tabs] Between ──────────────────────────────────────
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text       = "Split",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextTertiary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    // Segmented control
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Surface2)
                            .padding(2.dp),
                    ) {
                        listOf(
                            SplitType.EQUAL      to "Equally",
                            SplitType.UNEQUAL    to "$",
                            SplitType.PERCENTAGE to "%",
                            SplitType.SHARES     to "Ratio",
                        ).forEach { (type, label) ->
                            val selected = splitType == type
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .clip(RoundedCornerShape(Radius.sm))
                                    .background(if (selected) Green400 else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { viewModel.onSplitTypeChanged(type) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text       = label,
                                    fontSize   = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (selected) Surface0 else TextSecondary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text       = "Between",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextTertiary,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Member split rows
                val totalDouble = amountDouble
                val currentSum = when (splitType) {
                    SplitType.EQUAL -> null
                    else -> members
                        .filter { !equalExcluded.contains(it.userId) }
                        .sumOf { splitData[it.userId] ?: 0.0 }
                }
                val sumIsValid = when (splitType) {
                    SplitType.EQUAL      -> null
                    SplitType.UNEQUAL    -> currentSum != null && Math.abs(currentSum - totalDouble) < 0.01
                    SplitType.PERCENTAGE -> currentSum != null && Math.abs(currentSum - 100.0) < 0.01
                    SplitType.SHARES     -> members
                        .filter { !equalExcluded.contains(it.userId) }
                        .all { (splitData[it.userId] ?: 0.0) > 0 }
                }

                members.forEach { member ->
                    val isIncluded = !equalExcluded.contains(member.userId)
                    val name = if (member.userId == viewModel.currentUserId) "You" else member.fullName

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onToggleEqualMember(member.userId) }
                            .padding(horizontal = Spacing.lg, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Checkbox
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isIncluded) Green400 else Surface2)
                                .border(1.dp,
                                    if (isIncluded) Green400 else Surface4,
                                    RoundedCornerShape(6.dp)),
                        ) {
                            if (isIncluded) {
                                Text("✓", fontSize = 11.sp, color = Surface0, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        FsAvatar(name = member.fullName, userId = member.userId, size = 32.dp)
                        Spacer(modifier = Modifier.width(Spacing.md))

                        // Name + input — name left, underline input right
                        when {
                            !isIncluded -> {
                                Text(name, fontSize = 14.sp, color = TextTertiary,
                                    modifier = Modifier.weight(1f))
                            }
                            splitType == SplitType.EQUAL -> {
                                Text(name, fontSize = 14.sp, color = TextPrimary,
                                    modifier = Modifier.weight(1f))
                                Text(
                                    text       = MoneyUtils.format(splitData[member.userId] ?: 0.0, currency),
                                    fontSize   = 14.sp,
                                    color      = Green400,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            else -> {
                                Text(name, fontSize = 14.sp, color = TextPrimary,
                                    modifier = Modifier.weight(1f))

                                // Suffix label
                                val suffix = when (splitType) {
                                    SplitType.PERCENTAGE -> "%"
                                    SplitType.SHARES     -> "shares"
                                    else                 -> ""
                                }
                                val prefix = when (splitType) {
                                    SplitType.UNEQUAL -> currencySymbol(currency)
                                    else              -> ""
                                }

                                // Always show "0" when empty — makes field obviously tappable
                                val raw = splitData[member.userId] ?: 0.0
                                val display = when (splitType) {
                                    SplitType.SHARES -> if (raw > 0) raw.toInt().toString() else "0"
                                    else -> if (raw > 0) raw.toBigDecimal().stripTrailingZeros().toPlainString() else "0"
                                }

                                if (prefix.isNotEmpty()) {
                                    Text(prefix, fontSize = 13.sp, color = TextSecondary,
                                        modifier = Modifier.padding(end = 2.dp))
                                }

                                BasicTextField(
                                    value         = if (display == "0") "" else display,
                                    onValueChange = { new ->
                                        val regex = if (splitType == SplitType.SHARES)
                                            Regex("^\\d*$") else Regex("^\\d*(\\.\\d{0,2})?$")
                                        if (new.isEmpty() || regex.matches(new))
                                            viewModel.onSplitChanged(member.userId, new.toDoubleOrNull() ?: 0.0)
                                    },
                                    textStyle = TextStyle(
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = TextPrimary,
                                        textAlign  = TextAlign.End,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (splitType == SplitType.SHARES)
                                            KeyboardType.Number else KeyboardType.Decimal,
                                        imeAction = ImeAction.Next,
                                    ),
                                    singleLine = true,
                                    modifier   = Modifier
                                        .width(72.dp)
                                        .drawBehind {
                                            val strokeWidth = 1.dp.toPx()
                                            drawLine(
                                                color       = Color(0xFF2C2C2C),
                                                start       = androidx.compose.ui.geometry.Offset(0f, size.height),
                                                end         = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                                strokeWidth = strokeWidth,
                                            )
                                        },
                                    decorationBox = { inner ->
                                        Box(contentAlignment = Alignment.CenterEnd,
                                            modifier = Modifier.padding(bottom = 2.dp)) {
                                            // Always show "0" as placeholder
                                            if (raw == 0.0) {
                                                Text(display, fontSize = 14.sp, color = TextTertiary,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth())
                                            }
                                            inner()
                                        }
                                    }
                                )

                                if (suffix.isNotEmpty()) {
                                    Text(" $suffix", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + 22.dp + Spacing.md + 32.dp + Spacing.md))
                }

                // Sum indicator
                if (splitType != SplitType.EQUAL && currentSum != null) {
                    val sumLabel = when (splitType) {
                        SplitType.UNEQUAL    ->
                            "✓ ${MoneyUtils.format(currentSum, currency)} of ${MoneyUtils.format(totalDouble, currency)} split"
                        SplitType.PERCENTAGE ->
                            "✓ ${currentSum.toBigDecimal().stripTrailingZeros().toPlainString()}% of 100%"
                        SplitType.SHARES     -> "Tap members to include/exclude"
                        else -> ""
                    }
                    Text(
                        text     = sumLabel,
                        fontSize = 12.sp,
                        color    = if (sumIsValid == true) Green400 else TextTertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    )
                }
            }

            // ── Note field (shown when tapped from bottom bar) ────────────────
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
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
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

    if (showGroupSheet) {
        GroupBottomSheet(groups = groups, selected = selectedGroupId,
            onSelect = { viewModel.onGroupSelected(it); showGroupSheet = false },
            onDismiss = { showGroupSheet = false })
    }

    if (showPayerSheet) {
        PayerBottomSheet(members = members, payerData = payerData, total = amountDouble,
            currentUserId = viewModel.currentUserId,
            onChanged = { userId, amt -> viewModel.onPayerChanged(userId, amt) },
            onDismiss = { showPayerSheet = false })
    }

    if (showSplitTypeSheet) {
        SplitTypeSheet(
            selected  = splitType,
            onSelect  = { type ->
                viewModel.onSplitTypeChanged(type)
                showSplitTypeSheet = false
            },
            onDismiss = { showSplitTypeSheet = false },
        )
    }

    if (showCategorySheet) {
        CategoryBottomSheet(selected = categoryResolved,
            onSelect = { viewModel.onCategoryChanged(it); showCategorySheet = false },
            onDismiss = { showCategorySheet = false })
    }
}

// ── Bottom chip ───────────────────────────────────────────────────────────────

@Composable
private fun BottomChip(
    icon   : ImageVector,
    label  : String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
    }
}

// ── Compact Detail Row ────────────────────────────────────────────────────────

@Composable
private fun CompactDetailRow(
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

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupBottomSheet(
    groups   : List<Group>,
    selected : String?,
    onSelect : (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text("Select group", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
            HorizontalDivider(color = Surface4)
            groups.forEach { group ->
                val isSel = group.id == selected
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(group.id) }
                    .background(if (isSel) Surface4 else Surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(group.name, color = if (isSel) Green400 else TextPrimary,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                    if (isSel) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerBottomSheet(
    members      : List<GroupMember>,
    payerData    : Map<String, Double>,
    total        : Double,
    currentUserId: String?,
    onChanged    : (String, Double) -> Unit,
    onDismiss    : () -> Unit,
) {
    // Determine initial mode: multiple if more than one payer already selected
    var multipleMode by remember { mutableStateOf(payerData.size > 1) }

    // Single payer: who is currently selected
    val initialSingle = payerData.keys.firstOrNull() ?: currentUserId
    var singleSelected by remember { mutableStateOf(initialSingle) }

    // Multiple payers: editable amounts per person
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

            // Header + toggle
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Who paid?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                // Multiple payers toggle
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
                // ── Single payer — radio style ────────────────────────────────
                members.forEach { member ->
                    val name     = if (member.userId == currentUserId) "You" else member.fullName
                    val selected = member.userId == singleSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                singleSelected = member.userId
                                // Clear all others, set this one to full total
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
                        // Radio circle
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
                // ── Multiple payers — amount per person ───────────────────────
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
                        // Clear all then set each
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitBottomSheet(
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

    // Track which members are included in the split (default: all included)
    val localIncluded = remember(members) {
        members.associate { member ->
            member.userId to mutableStateOf(
                splitData.isEmpty() || splitData.containsKey(member.userId)
            )
        }
    }

    // Only validate sum for included members
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
                    // Checkbox
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
                        // Prefix
                        if (splitType == SplitType.UNEQUAL) {
                            Text("$", fontSize = 14.sp, color = TextSecondary,
                                modifier = Modifier.padding(end = 4.dp))
                        }
                        // Amount field — full width feel, left aligned
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitTypeSheet(
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

private fun currencySymbol(code: String): String = when (code) {
    "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"; "JPY" -> "¥"
    "CNY" -> "¥"; "KRW" -> "₩"; "RUB" -> "₽"; "BRL" -> "R$"; "CAD" -> "CA$"
    "AUD" -> "A$"; "SGD" -> "S$"; "HKD" -> "HK$"; "MXN" -> "MX$"; "TRY" -> "₺"
    "THB" -> "฿"; "IDR" -> "Rp"; "MYR" -> "RM"; "PHP" -> "₱"; "VND" -> "₫"
    "PKR" -> "₨"; "BDT" -> "৳"; "NGN" -> "₦"; "ZAR" -> "R"; "AED" -> "د.إ"
    "SAR" -> "﷼"; "PLN" -> "zł"; "SEK" -> "kr"; "NOK" -> "kr"; "DKK" -> "kr"
    else  -> code
}

private fun formatDisplayDate(isoDate: String): String {
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