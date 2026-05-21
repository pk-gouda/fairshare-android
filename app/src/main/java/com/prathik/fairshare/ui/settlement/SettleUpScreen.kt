package com.prathik.fairshare.ui.settlement

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.auth.BiometricHelper
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.auth.BiometricResult
import com.prathik.fairshare.ui.components.FsAvatar
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    onBack    : () -> Unit,
    onSuccess : () -> Unit,
    viewModel : SettleUpViewModel = hiltViewModel(),
) {
    val uiState         by viewModel.uiState.collectAsState()
    val notes           by viewModel.notes.collectAsState()
    val amount          by viewModel.amount.collectAsState()
    val paymentMethod   by viewModel.paymentMethod.collectAsState()
    val otherUserName   by viewModel.otherUserName.collectAsState()
    val balanceAmount   by viewModel.balanceAmount.collectAsState()
    val balanceCurrency    by viewModel.balanceCurrency.collectAsState()
    val payerName          by viewModel.payerName.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val activeCurrency      by viewModel.activeCurrency.collectAsState()

    val previewState    by viewModel.previewState.collectAsState()
    val snackbarHost    = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current
    val activity        = context as? FragmentActivity
    val isLoading       = uiState is SettleUpUiState.Loading
    val biometricHelper = remember { BiometricHelper(context) }
    var showNoteField   by remember { mutableStateOf(false) }

    // Direction logic
    val overridePayer = viewModel.overridePayerId
    val currentUser   = viewModel.currentUserId

    val fromName = when {
        overridePayer == null && balanceAmount > 0  -> otherUserName.ifBlank { "Friend" }
        overridePayer == null                       -> "You"
        overridePayer == currentUser                -> "You"
        else                                        -> payerName.ifBlank { "..." }
    }
    val fromUserId = when {
        overridePayer == null && balanceAmount > 0  -> viewModel.otherUserId
        overridePayer == null                       -> ""
        overridePayer == currentUser                -> ""
        else                                        -> overridePayer
    }
    val toName = when {
        overridePayer == null && balanceAmount > 0  -> "You"
        overridePayer == null                       -> otherUserName.ifBlank { "Friend" }
        overridePayer == currentUser                -> otherUserName.ifBlank { "Friend" }
        else                                        -> otherUserName.ifBlank { "Friend" }
    }
    val toUserId = when {
        overridePayer == null && balanceAmount > 0  -> ""
        else                                        -> viewModel.otherUserId
    }

    val defaultAmount = when {
        balanceAmount > 0 -> balanceAmount
        balanceAmount < 0 -> -balanceAmount
        else              -> 0.0
    }

    val symbol = remember(balanceCurrency) {
        MoneyUtils.getSymbol(balanceCurrency)
    }

    val enteredAmount = amount.toDoubleOrNull() ?: 0.0
    val canSettle     = defaultAmount > 0 || enteredAmount > 0

    fun confirmAndSettle(settleAction: () -> Unit) {
        val amt = amount.toDoubleOrNull() ?: 0.0
        val effectiveAmount = if (amount.isBlank()) defaultAmount else amt
        val needsBiometric  = effectiveAmount >= 50.0
        if (needsBiometric && activity != null && biometricHelper.canAuthenticate()) {
            scope.launch {
                val label = if (amount.isBlank()) "the full balance" else "${"%.2f".format(amt)}"
                when (biometricHelper.authenticate(activity, "Confirm settlement", "Settling $label")) {
                    is BiometricResult.Success   -> settleAction()
                    is BiometricResult.Cancelled -> Unit
                    is BiometricResult.Error     -> scope.launch {
                        snackbarHost.showSnackbar("Biometric auth failed. Try again.")
                    }
                }
            }
        } else {
            settleAction()
        }
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is SettleUpUiState.Success -> { onSuccess(); viewModel.resetUiState() }
            is SettleUpUiState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetUiState() }
            else -> Unit
        }
    }

    // ── Overpayment confirmation dialog ───────────────────────────────────────
    // Shown when preview reveals the payment exceeds the balance.
    // User must explicitly confirm before settlement is submitted.
    val awaitingState = uiState as? SettleUpUiState.AwaitingConfirmation
    val preview       = previewState
    val hasOverpayment = preview?.overpaymentAmount != null && (preview.overpaymentAmount ?: 0.0) > 0.0

    if (awaitingState != null && preview != null && hasOverpayment) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPreview() },
            containerColor   = com.prathik.fairshare.ui.theme.Surface2,
            title = {
                Text(
                    text       = "Overpayment detected",
                    color      = com.prathik.fairshare.ui.theme.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column {
                    Text(
                        text     = preview.overpaymentMessage
                            ?: "You are paying more than owed. A reverse credit will be created.",
                        color    = com.prathik.fairshare.ui.theme.TextSecondary,
                        fontSize = 14.sp,
                    )
                    if (preview.allocations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = com.prathik.fairshare.ui.theme.Surface3)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text     = "Breakdown",
                            color    = com.prathik.fairshare.ui.theme.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        preview.allocations.forEach { alloc ->
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text     = if (alloc.contextType == "GROUP" && alloc.groupName != null)
                                        alloc.groupName else "Direct",
                                    color    = if (alloc.isOverpaymentCredit)
                                        com.prathik.fairshare.ui.theme.TextTertiary
                                    else com.prathik.fairshare.ui.theme.TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text     = MoneyUtils.format(alloc.amount, alloc.currency),
                                    color    = if (alloc.isOverpaymentCredit)
                                        com.prathik.fairshare.ui.theme.TextTertiary
                                    else com.prathik.fairshare.ui.theme.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmSettle(awaitingState.type, awaitingState.amount)
                }) {
                    Text("Confirm", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPreview() }) {
                    Text("Cancel", color = com.prathik.fairshare.ui.theme.TextSecondary)
                }
            },
        )
    } else if (awaitingState != null && preview != null && !hasOverpayment) {
        // No overpayment — auto-confirm immediately
        LaunchedEffect(awaitingState) {
            viewModel.confirmSettle(awaitingState.type, awaitingState.amount)
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Record a payment", onBack = onBack) },
        bottomBar = {
            // ── Sticky record button ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .background(Surface0)
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.lg, top = Spacing.md)
                    .imePadding(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(if (canSettle && !isLoading) Green400 else Surface2)
                        .then(if (canSettle && !isLoading) Modifier.clickable {
                            confirmAndSettle {
                                if (amount.isBlank()) viewModel.settleAll()
                                else viewModel.settlePartial()
                            }
                        } else Modifier)
                        .padding(vertical = 16.dp),
                ) {
                    Text(
                        text       = if (isLoading) "Recording…" else "Record payment",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (canSettle && !isLoading) Color.Black else TextTertiary,
                    )
                }
            }
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // top bar rendered via Scaffold topBar param below

            // ── Avatars + arrow ───────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(name = fromName, userId = fromUserId, size = 72.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text       = fromName,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.width(90.dp),
                        maxLines   = 1,
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector        = Icons.Filled.ArrowForward,
                    contentDescription = "pays",
                    tint               = TextTertiary,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(name = toName, userId = toUserId, size = 72.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text       = toName,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.width(90.dp),
                        maxLines   = 1,
                    )
                }
            }

            // ── Balance context card ──────────────────────────────────────────
            if (defaultAmount > 0) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(Surface2)
                        .border(0.5.dp, Surface4, RoundedCornerShape(Radius.lg))
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = "$fromName owes $toName",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                    )
                    Text(
                        text       = MoneyUtils.format(defaultAmount, balanceCurrency),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Negative,
                    )
                }
            }

            // ── Currency selector (shown only when multiple currencies) ─────────
            if (availableCurrencies.size > 1) {
                Spacer(modifier = Modifier.height(Spacing.md))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    availableCurrencies.forEach { currency ->
                        val isSelected = currency == activeCurrency
                        Box(
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(com.prathik.fairshare.ui.theme.Radius.full))
                                .background(if (isSelected) Green400 else Surface2)
                                .border(1.dp, if (isSelected) Green400 else Surface4,
                                    androidx.compose.foundation.shape.RoundedCornerShape(com.prathik.fairshare.ui.theme.Radius.full))
                                .clickable { viewModel.setActiveCurrency(currency) }
                                .padding(horizontal = Spacing.md, vertical = 6.dp),
                        ) {
                            Text(
                                text = currency,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Surface0 else TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Amount input — centered ───────────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text     = symbol,
                        fontSize = 32.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(top = 12.dp, end = 4.dp),
                    )
                    BasicTextField(
                        value         = amount,
                        onValueChange = { new ->
                            if (new.isEmpty() || new.matches(Regex("^\\d*(\\.\\d{0,2})?$")))
                                viewModel.onAmountChanged(new)
                        },
                        textStyle = TextStyle(
                            fontSize   = 64.sp,
                            fontWeight = FontWeight.Light,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Start,
                        ),
                        cursorBrush     = SolidColor(Green400),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction    = ImeAction.Done,
                        ),
                        singleLine    = true,
                        modifier      = Modifier.widthIn(min = 60.dp, max = 240.dp),
                        decorationBox = { inner ->
                            if (amount.isEmpty()) {
                                Text(
                                    text       = "0",
                                    fontSize   = 64.sp,
                                    fontWeight = FontWeight.Light,
                                    color      = Surface4,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            // ── Settle full balance chip ──────────────────────────────────────
            if (defaultAmount > 0 && amount.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Green400.copy(alpha = 0.12f))
                        .border(1.dp, Green400.copy(alpha = 0.4f), RoundedCornerShape(Radius.full))
                        .clickable { viewModel.onAmountChanged("%.2f".format(defaultAmount)) }
                        .padding(horizontal = Spacing.md, vertical = 8.dp),
                ) {
                    Text(
                        text       = "Settle full balance: ${MoneyUtils.format(defaultAmount, balanceCurrency)}",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Green400,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Section divider ───────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Surface3))
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Payment method — 2×2 grid with icons ──────────────────────────
            Text(
                text          = "PAYMENT METHOD",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = TextTertiary,
                letterSpacing = 1.sp,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Payment methods vary by currency/region
                val paymentMethods = remember(balanceCurrency) {
                    when (balanceCurrency) {
                        "INR" -> listOf(
                            Triple("Cash",          Icons.Outlined.Payments,             "Cash"),
                            Triple("UPI",           Icons.Outlined.AccountBalanceWallet,  "UPI"),
                            Triple("Bank transfer", Icons.Outlined.AccountBalance,        "Bank"),
                            Triple("Other",         Icons.Outlined.MoreHoriz,            "Other"),
                        )
                        "GBP" -> listOf(
                            Triple("Cash",          Icons.Outlined.Payments,             "Cash"),
                            Triple("Bank transfer", Icons.Outlined.AccountBalance,        "Bank"),
                            Triple("Other",         Icons.Outlined.MoreHoriz,            "Other"),
                            Triple("PayPal",        Icons.Outlined.CreditCard,           "PayPal"),
                        )
                        "EUR" -> listOf(
                            Triple("Cash",          Icons.Outlined.Payments,             "Cash"),
                            Triple("Bank transfer", Icons.Outlined.AccountBalance,        "SEPA"),
                            Triple("PayPal",        Icons.Outlined.CreditCard,           "PayPal"),
                            Triple("Other",         Icons.Outlined.MoreHoriz,            "Other"),
                        )
                        "AUD", "CAD", "NZD" -> listOf(
                            Triple("Cash",          Icons.Outlined.Payments,             "Cash"),
                            Triple("Bank transfer", Icons.Outlined.AccountBalance,        "Bank"),
                            Triple("PayPal",        Icons.Outlined.CreditCard,           "PayPal"),
                            Triple("Other",         Icons.Outlined.MoreHoriz,            "Other"),
                        )
                        else -> listOf( // USD and all others
                            Triple("Cash",          Icons.Outlined.Payments,             "Cash"),
                            Triple("Bank transfer", Icons.Outlined.AccountBalance,        "Bank"),
                            Triple("Venmo/PayPal",  Icons.Outlined.CreditCard,           "Venmo"),
                            Triple("Other",         Icons.Outlined.MoreHoriz,            "Other"),
                        )
                    }
                }
                paymentMethods.forEach { (method, icon, label) ->
                    val selected = paymentMethod == method
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(if (selected) Green400 else Surface2)
                            .border(
                                width = if (selected) 0.dp else 0.5.dp,
                                color = if (selected) Color.Transparent else Surface4,
                                shape = RoundedCornerShape(Radius.lg),
                            )
                            .clickable { viewModel.onPaymentMethodChanged(method) }
                            .padding(vertical = Spacing.md),
                    ) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = label,
                            tint               = if (selected) Color.Black else TextSecondary,
                            modifier           = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text       = label,
                            fontSize   = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selected) Color.Black else TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Surface3))

            // ── Add comment ───────────────────────────────────────────────────
            if (!showNoteField) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNoteField = true }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint               = TextTertiary,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text     = if (notes.isBlank()) "Add a comment (optional)" else notes,
                        fontSize = 14.sp,
                        color    = if (notes.isBlank()) TextTertiary else TextPrimary,
                    )
                }
            } else {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint               = Green400,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    BasicTextField(
                        value         = notes,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        textStyle     = TextStyle(fontSize = 14.sp, color = TextPrimary),
                        cursorBrush   = SolidColor(Green400),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (notes.isEmpty()) {
                                Text("Add a comment…", fontSize = 14.sp, color = TextTertiary)
                            }
                            inner()
                        },
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).height(0.5.dp).background(Green400.copy(alpha = 0.5f)))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Surface3))
            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Info note ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                verticalAlignment = Alignment.Top,
            ) {
                Text("ⓘ", fontSize = 12.sp, color = TextTertiary)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text     = "This records a payment made outside FairShare. No money will be moved.",
                    fontSize = 12.sp,
                    color    = TextTertiary,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun PersonAvatar(name: String, userId: String, size: androidx.compose.ui.unit.Dp) {
    if (userId.isNotBlank()) {
        FsAvatar(name = name, userId = userId, size = size)
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Surface2),
        ) {
            Text(
                text       = name.firstOrNull()?.uppercase() ?: "Y",
                fontSize   = (size.value * 0.38f).sp,
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}