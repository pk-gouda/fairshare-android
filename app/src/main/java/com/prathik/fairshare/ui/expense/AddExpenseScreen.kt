package com.prathik.fairshare.ui.expense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsIconButton
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
import com.prathik.fairshare.util.MoneyUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Add Expense / Transfer Screen.
 *
 * Top bar:
 *   - Back arrow
 *   - "New expense" title
 *   - AddPhotoAlternate icon → attach photo to expense (gallery/camera)
 *
 * Layout:
 *   - Tab toggle: [ Expense ] / [ Transfer ]
 *   - Currency pill + amount on same row (tappable → AmountInputDialog)
 *   - "tap to enter amount" hint below
 *   - "What's this for?" description field
 *   - Scan receipt button (CameraAlt → AI receipt scan)
 *   - Split chips: Equal / Exact / % / Shares (equally spaced pills)
 *   - Details card: Group, Paid by, Date, Category
 *   - Split preview card
 *   - "+ Add a note" expandable
 *   - Save expense / Save transfer button (always green)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val description by viewModel.description.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val splitType by viewModel.splitType.collectAsState()
    val category by viewModel.category.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val expenseDate by viewModel.expenseDate.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val members by viewModel.members.collectAsState()
    val payerData by viewModel.payerData.collectAsState()
    val splitData by viewModel.splitData.collectAsState()
    val receiptState by viewModel.receiptState.collectAsState()
    val transferFromId by viewModel.transferFromId.collectAsState()
    val transferToId by viewModel.transferToId.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    var showGroupSheet by remember { mutableStateOf(false) }
    var showPayerSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showNotes by remember { mutableStateOf(false) }
    var showTransferFrom by remember { mutableStateOf(false) }
    var showTransferTo by remember { mutableStateOf(false) }

    // Photo attachment launcher (gallery)
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> /* TODO Day 15 — attach uri to expense */ }

    // Receipt scan launcher (camera → AI extraction)
    val receiptScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { viewModel.scanReceipt(it) } }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddExpenseUiState.Success -> {
                onSuccess(); viewModel.resetUiState()
            }

            is AddExpenseUiState.Error -> {
                snackbarHost.showSnackbar(state.message); viewModel.resetUiState()
            }

            else -> Unit
        }
    }

    LaunchedEffect(receiptState) {
        if (receiptState is ReceiptScanState.Error) {
            snackbarHost.showSnackbar((receiptState as ReceiptScanState.Error).message)
        }
    }

    val isLoading = uiState is AddExpenseUiState.Loading
    val displayDate = remember(expenseDate) { formatDisplayDate(expenseDate) }
    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val hasAmount = amountDouble > 0
    val amountFontSize = when {
        amount.length <= 4 -> 56.sp
        amount.length <= 6 -> 44.sp
        amount.length <= 8 -> 36.sp
        else -> 28.sp
    }

    // Paid by
    val paidByMember =
        if (payerData.size == 1) members.find { it.userId == payerData.keys.first() } else null
    val paidByText = when {
        payerData.isEmpty() -> "You"
        payerData.size == 1 -> if (payerData.keys.first() == viewModel.currentUserId) "You"
        else members.find { it.userId == payerData.keys.first() }?.fullName ?: "1 person"

        else -> "${payerData.size} people"
    }

    // Category — use local val to allow smart cast
    val categoryResolved: ExpenseCategory? = category
    val categoryText = if (categoryResolved == null) "Auto-detect"
    else categoryResolved.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
    val categoryColor = if (categoryResolved == null) TextSecondary else TextPrimary

    // Transfer names
    val transferFromName = members.find { it.userId == transferFromId }?.let {
        if (it.userId == viewModel.currentUserId) "You" else it.fullName
    } ?: "You"
    val transferToName = members.find { it.userId == transferToId }?.fullName ?: "Select person"

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title = "New expense",
                onBack = onBack,
                actions = {
                    // Gallery/camera — attach a photo to this expense
                    FsIconButton(
                        icon = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Add photo",
                        onClick = { photoLauncher.launch("image/*") },
                        tint = TextSecondary,
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->

        if (isLoading) {
            FsLoadingScreen(); return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Tab toggle ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(Surface2),
            ) {
                listOf(ExpenseTab.EXPENSE to "Expense", ExpenseTab.TRANSFER to "Transfer")
                    .forEach { (tab, label) ->
                        val isActive = activeTab == tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(if (isActive) Green400 else Color.Transparent)
                                .clickable { viewModel.onTabChanged(tab) }
                                .padding(vertical = Spacing.md),
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                color = if (isActive) Surface0 else TextSecondary,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Currency pill + inline amount input ───────────────────────────
            val amountFocusRequester = remember { FocusRequester() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                ) {
                    // Currency pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.full))
                            .background(Surface3)
                            .clickable { onNavigateToCurrency() }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currency,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "▾", fontSize = 11.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    // Currency symbol
                    Text(
                        text = currencySymbol(currency),
                        fontSize = 20.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    // Inline amount input — tapping opens keyboard directly
                    androidx.compose.foundation.text.BasicTextField(
                        value = amount,
                        onValueChange = { new ->
                            val regex = Regex("^\\d*(\\.\\d{0,2})?$")
                            if (new.isEmpty() || regex.matches(new)) viewModel.onAmountChanged(new)
                        },
                        textStyle = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = amountFontSize,
                            color = if (hasAmount) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Start,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier.focusRequester(amountFocusRequester),
                        decorationBox = { innerTextField ->
                            Box {
                                if (amount.isBlank()) {
                                    Text(
                                        text = "0.00",
                                        fontSize = amountFontSize,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Green underline accent
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(if (hasAmount) 80.dp else 40.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (hasAmount) Green400 else Surface4),
                )

                if (!hasAmount) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "tap to enter amount",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Description ───────────────────────────────────────────────────
            FsTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = "What's this for?",
                imeAction = ImeAction.Next,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Scan receipt button ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Surface2)
                    .clickable { receiptScanLauncher.launch(null) }
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Scan receipt",
                    tint = if (receiptState is ReceiptScanState.Success) Green400 else TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when (receiptState) {
                        is ReceiptScanState.Success -> "Receipt scanned ✓"
                        is ReceiptScanState.Scanning -> "Scanning..."
                        else -> "Scan receipt"
                    },
                    fontSize = 13.sp,
                    color = if (receiptState is ReceiptScanState.Success) Green400 else TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            if (activeTab == ExpenseTab.EXPENSE) {

                // ── Split chips — equally spaced ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    SplitType.entries.forEach { type ->
                        val isSelected = splitType == type
                        val label = when (type) {
                            SplitType.EQUAL -> "Equal"
                            SplitType.UNEQUAL -> "Exact"
                            SplitType.PERCENTAGE -> "%"
                            SplitType.SHARES -> "Shares"
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.full))
                                .background(if (isSelected) Green400 else Surface2)
                                .clickable { viewModel.onSplitTypeChanged(type) }
                                .padding(vertical = Spacing.sm),
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (isSelected) Surface0 else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // ── Details card ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    // Group — green when unselected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGroupSheet = true }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Group, null,
                                tint = TextSecondary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text("Group", fontSize = 15.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = groups.find { it.id == selectedGroupId }?.name
                                    ?: "Select group",
                                fontSize = 15.sp,
                                color = if (selectedGroupId == null) Green400 else TextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos, null,
                                tint = TextSecondary, modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )

                    // Paid by — with avatar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPayerSheet = true }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Person, null,
                                tint = TextSecondary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text("Paid by", fontSize = 15.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (paidByMember != null) {
                                FsAvatar(
                                    name = paidByMember.fullName, userId = paidByMember.userId,
                                    size = ComponentSize.avatarSm
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                            }
                            Text(
                                paidByText,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos, null,
                                tint = TextSecondary, modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )

                    DetailRow(
                        icon = Icons.Outlined.CalendarMonth, label = "Date",
                        value = displayDate, onClick = { showDatePicker = true })

                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )

                    // Category — gray when auto-detect
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategorySheet = true }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Category, null,
                                tint = TextSecondary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text("Category", fontSize = 15.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                categoryText,
                                fontSize = 15.sp,
                                color = categoryColor,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos, null,
                                tint = TextSecondary, modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Split preview
                if (members.isNotEmpty() && hasAmount) {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    SplitPreviewCard(
                        members = members, splitData = splitData,
                        payerData = payerData, currency = currency,
                        currentUserId = viewModel.currentUserId,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                }

            } else {

                // ── Transfer tab ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGroupSheet = true }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Group, null,
                                tint = TextSecondary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text("Group", fontSize = 15.sp, color = TextSecondary)
                        }
                        Text(
                            text = groups.find { it.id == selectedGroupId }?.name ?: "Select group",
                            fontSize = 15.sp,
                            color = if (selectedGroupId == null) Green400 else TextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                    DetailRow(
                        icon = Icons.Outlined.Person, label = "From",
                        value = transferFromName, onClick = { showTransferFrom = true })
                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                    DetailRow(
                        icon = Icons.Outlined.Person, label = "To",
                        value = transferToName, onClick = { showTransferTo = true })
                    HorizontalDivider(
                        color = Surface4, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                    DetailRow(
                        icon = Icons.Outlined.CalendarMonth, label = "Date",
                        value = displayDate, onClick = { showDatePicker = true })
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Notes ─────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !showNotes,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FsTextButton(
                    text = "+ Add a note", onClick = { showNotes = true },
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }
            AnimatedVisibility(
                visible = showNotes,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FsTextField(
                    value = notes, onValueChange = { viewModel.onNotesChanged(it) },
                    label = "Add a note", imeAction = ImeAction.Done,
                    singleLine = false, maxLines = 3,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Save button — always green ─────────────────────────────────────
            FsPrimaryButton(
                text = if (activeTab == ExpenseTab.TRANSFER) "Save transfer" else "Save expense",
                onClick = { viewModel.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                isLoading = isLoading,
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
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.onDateChanged(date.atStartOfDay().toString())
                    }
                    showDatePicker = false
                })
            },
            dismissButton = { FsTextButton(text = "Cancel", onClick = { showDatePicker = false }) },
        ) { DatePicker(state = datePickerState) }
    }

    if (showGroupSheet) {
        GroupBottomSheet(
            groups = groups, selected = selectedGroupId,
            onSelect = { viewModel.onGroupSelected(it); showGroupSheet = false },
            onDismiss = { showGroupSheet = false })
    }

    if (showPayerSheet) {
        PayerBottomSheet(
            members = members, payerData = payerData, total = amountDouble,
            currentUserId = viewModel.currentUserId,
            onChanged = { userId, amt -> viewModel.onPayerChanged(userId, amt) },
            onDismiss = { showPayerSheet = false })
    }

    if (showCategorySheet) {
        CategoryBottomSheet(
            selected = categoryResolved,
            onSelect = { viewModel.onCategoryChanged(it); showCategorySheet = false },
            onDismiss = { showCategorySheet = false })
    }

    if (showTransferFrom) {
        MemberSelectSheet(
            title = "From", members = members, selected = transferFromId,
            currentUserId = viewModel.currentUserId,
            onSelect = { viewModel.onTransferFromChanged(it); showTransferFrom = false },
            onDismiss = { showTransferFrom = false })
    }

    if (showTransferTo) {
        MemberSelectSheet(
            title = "To", members = members, selected = transferToId,
            currentUserId = viewModel.currentUserId,
            onSelect = { viewModel.onTransferToChanged(it); showTransferTo = false },
            onDismiss = { showTransferTo = false })
    }
}

// ── Split Preview Card ────────────────────────────────────────────────────────

@Composable
private fun SplitPreviewCard(
    members: List<GroupMember>,
    splitData: Map<String, Double>,
    payerData: Map<String, Double>,
    currency: String,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(vertical = Spacing.sm)
    ) {
        Text(
            "SPLIT PREVIEW", fontSize = 11.sp, color = TextSecondary,
            fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        )
        members.forEach { member ->
            val share = splitData[member.userId] ?: 0.0
            val isPayer = payerData.containsKey(member.userId)
            val name = if (member.userId == currentUserId) "You" else member.fullName
            val shareColor = if (isPayer) Green400 else Negative
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FsAvatar(
                        name = member.fullName,
                        userId = member.userId,
                        size = ComponentSize.avatarMd
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            name,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (isPayer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Radius.xs))
                                    .background(Green400.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "paid",
                                    fontSize = 10.sp,
                                    color = Green400,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Text(
                    MoneyUtils.format(share, currency), fontSize = 15.sp,
                    color = shareColor, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Detail Row ────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                icon,
                null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(label, fontSize = 15.sp, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos, null,
                tint = TextSecondary, modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupBottomSheet(
    groups: List<Group>, selected: String?,
    onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text(
                "Select group",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
            HorizontalDivider(color = Surface4)
            groups.forEach { group ->
                val isSel = group.id == selected
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(group.id) }
                    .background(if (isSel) Surface4 else Surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        group.name, color = if (isSel) Green400 else TextPrimary,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                    )
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
    members: List<GroupMember>,
    payerData: Map<String, Double>,
    total: Double,
    currentUserId: String?,
    onChanged: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text(
                "Who paid?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
            HorizontalDivider(color = Surface4)
            members.forEach { member ->
                val isPayer = payerData.containsKey(member.userId)
                val name = if (member.userId == currentUserId) "You" else member.fullName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPayer) onChanged(member.userId, 0.0) else onChanged(
                                member.userId,
                                total
                            )
                        }
                        .background(if (isPayer) Surface4 else Surface2)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FsAvatar(
                            name = member.fullName,
                            userId = member.userId,
                            size = ComponentSize.avatarSm
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            name, color = if (isPayer) Green400 else TextPrimary,
                            fontWeight = if (isPayer) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isPayer) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSelectSheet(
    title: String, members: List<GroupMember>, selected: String?,
    currentUserId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text(
                title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
            HorizontalDivider(color = Surface4)
            members.forEach { member ->
                val isSel = member.userId == selected
                val name = if (member.userId == currentUserId) "You" else member.fullName
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(member.userId) }
                    .background(if (isSel) Surface4 else Surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FsAvatar(
                            name = member.fullName,
                            userId = member.userId,
                            size = ComponentSize.avatarSm
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            name, color = if (isSel) Green400 else TextPrimary,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSel) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    selected: ExpenseCategory?, onSelect: (ExpenseCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface2, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(bottom = Spacing.xxxl)) {
            Text(
                "Category", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
            HorizontalDivider(color = Surface4)
            // Auto-detect option at top
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(null) }
                .background(if (selected == null) Surface4 else Surface2)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Auto-detect", color = if (selected == null) Green400 else TextSecondary,
                    fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal
                )
                if (selected == null) Text("✓", color = Green400, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            ExpenseCategory.entries.forEach { cat ->
                val isSel = cat == selected
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(cat) }
                    .background(if (isSel) Surface4 else Surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        cat.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                        color = if (isSel) Green400 else TextPrimary,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                    )
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
    else -> code
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        val dateTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val date = dateTime.toLocalDate()
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) {
        "Today"
    }
}