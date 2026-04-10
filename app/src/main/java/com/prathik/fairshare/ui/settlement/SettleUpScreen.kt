package com.prathik.fairshare.ui.settlement

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.prathik.fairshare.ui.auth.BiometricResult
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
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
    val balanceCurrency by viewModel.balanceCurrency.collectAsState()
    val snackbarHost    = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current
    val activity        = context as? FragmentActivity
    val isLoading       = uiState is SettleUpUiState.Loading
    val biometricHelper = remember { BiometricHelper(context) }

    // Direction: overridePayerId set → payer chosen manually ("More options")
    // otherwise → derive from balance sign (positive = they owe you → they pay)
    val payerIsOther = (viewModel.overridePayerId != null &&
            viewModel.overridePayerId != viewModel.currentUserId) ||
            (viewModel.overridePayerId == null && balanceAmount > 0)

    val fromName   = if (payerIsOther) otherUserName.ifBlank { "Friend" } else "You"
    val fromUserId = if (payerIsOther) viewModel.otherUserId else ""
    val toName     = if (payerIsOther) "You" else otherUserName.ifBlank { "Friend" }
    val toUserId   = if (payerIsOther) "" else viewModel.otherUserId

    // Amount to show as placeholder / default
    val defaultAmount = when {
        balanceAmount > 0 -> balanceAmount
        balanceAmount < 0 -> -balanceAmount
        else              -> 0.0
    }

    fun confirmAndSettle(settleAction: () -> Unit) {
        val amt = amount.toDoubleOrNull() ?: 0.0
        // When amount is blank the user is settling the full balance (defaultAmount).
        // Use the actual balance for the threshold — otherwise every settle-all
        // triggers biometric even for a $0.50 balance.
        val effectiveAmount = if (amount.isBlank()) defaultAmount else amt
        val needsBiometric = effectiveAmount >= 50.0
        if (needsBiometric && activity != null && biometricHelper.canAuthenticate()) {
            scope.launch {
                val label = if (amount.isBlank()) "the full balance" else "$${"%.2f".format(amt)}"
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

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Record a payment", onBack = onBack) },
        snackbarHost   = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Avatars + arrow ───────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                // From
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(name = fromName, userId = fromUserId, size = 72.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fromName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }

                Spacer(modifier = Modifier.width(32.dp))

                Icon(
                    imageVector        = Icons.Filled.ArrowForward,
                    contentDescription = "pays",
                    tint               = TextTertiary,
                    modifier           = Modifier.size(22.dp),
                )

                Spacer(modifier = Modifier.width(32.dp))

                // To
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(name = toName, userId = toUserId, size = 72.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(toName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Editable amount ───────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Currency symbol
                val symbol = remember(balanceCurrency) {
                    try {
                        java.util.Currency.getInstance(balanceCurrency).symbol
                    } catch (e: Exception) { "$" }
                }
                Text(
                    text       = symbol,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextSecondary,
                    modifier   = Modifier.padding(end = 4.dp),
                )
                // Inline editable amount — grows with text
                BasicTextField(
                    value         = amount,
                    onValueChange = { new ->
                        if (new.isEmpty() || new.matches(Regex("^\\d*(\\.\\d{0,2})?$")))
                            viewModel.onAmountChanged(new)
                    },
                    textStyle     = TextStyle(
                        fontSize   = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Start,
                    ),
                    cursorBrush   = SolidColor(Green400),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction    = ImeAction.Done,
                    ),
                    singleLine    = true,
                    decorationBox = { inner ->
                        if (amount.isEmpty()) {
                            Text(
                                text       = if (defaultAmount > 0)
                                    "%.2f".format(defaultAmount)
                                else "0.00",
                                fontSize   = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextTertiary,
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = if (amount.isEmpty() && defaultAmount > 0)
                    "Tap to edit · leave blank to settle all"
                else
                    "Leave blank to settle everything",
                fontSize = 12.sp,
                color    = TextTertiary,
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Payment method ────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf("Cash", "Bank transfer", "UPI", "Other").forEach { method ->
                    val selected = paymentMethod == method
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.full))
                            .background(if (selected) Green400 else Surface2)
                            .clickable { viewModel.onPaymentMethodChanged(method) }
                            .padding(vertical = Spacing.sm),
                    ) {
                        Text(
                            text       = method,
                            fontSize   = 12.sp,
                            color      = if (selected) Surface0 else TextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Notes ─────────────────────────────────────────────────────────
            BasicTextField(
                value         = notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                textStyle     = TextStyle(
                    fontSize  = 14.sp,
                    color     = TextPrimary,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush   = SolidColor(Green400),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine    = true,
                decorationBox = { inner ->
                    if (notes.isEmpty()) {
                        Text(
                            text      = "Add a comment",
                            fontSize  = 14.sp,
                            color     = TextTertiary,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            // Underline for the notes field
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .height(0.5.dp)
                    .background(Surface3),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Info note ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ⓘ", fontSize = 16.sp, color = TextTertiary)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text     = "You are recording a payment that happened outside FairShare. No money will be moved.",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Record button ─────────────────────────────────────────────────
            // Disabled when balance is already 0 and the user hasn't typed a custom
            // amount — settling a zero balance would create a phantom ₹0 record.
            val enteredAmount = amount.toDoubleOrNull() ?: 0.0
            val canSettle = defaultAmount > 0 || enteredAmount > 0
            FsPrimaryButton(
                text      = "Record a payment",
                onClick   = {
                    confirmAndSettle {
                        if (amount.isBlank()) viewModel.settleAll()
                        else viewModel.settlePartial()
                    }
                },
                enabled   = canSettle,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                isLoading = isLoading,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun PersonAvatar(name: String, userId: String, size: androidx.compose.ui.unit.Dp) {
    if (userId.isNotBlank()) {
        FsAvatar(name = name, userId = userId, size = size)
    } else {
        // "You" — no userId, show initial circle
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