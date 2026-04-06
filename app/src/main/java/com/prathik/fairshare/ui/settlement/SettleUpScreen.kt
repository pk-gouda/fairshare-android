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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.prathik.fairshare.ui.auth.BiometricHelper
import com.prathik.fairshare.ui.auth.BiometricResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSectionLabel
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
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
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils

/**
 * Settle Up Screen.
 *
 * Shows who is paying whom with a direction header, then lets the user
 * record a full or partial settlement.
 *
 * Payment direction:
 *   balance > 0 → they owe you → they pay you (Friend → You)
 *   balance < 0 → you owe them → you pay them (You → Friend)
 *   balance = 0 → all settled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    onBack     : () -> Unit,
    onSuccess  : () -> Unit,
    viewModel  : SettleUpViewModel = hiltViewModel(),
) {
    val uiState        by viewModel.uiState.collectAsState()
    val notes          by viewModel.notes.collectAsState()
    val amount         by viewModel.amount.collectAsState()
    val paymentMethod  by viewModel.paymentMethod.collectAsState()
    val otherUserName  by viewModel.otherUserName.collectAsState()
    val balanceAmount  by viewModel.balanceAmount.collectAsState()
    val balanceCurrency by viewModel.balanceCurrency.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()
    val context        = LocalContext.current
    val activity       = context as? FragmentActivity
    val isLoading      = uiState is SettleUpUiState.Loading

    val biometricHelper = remember { BiometricHelper(context) }

    fun confirmAndSettle(settleAction: () -> Unit) {
        val amt = amount.toDoubleOrNull() ?: 0.0
        val needsBiometric = amt >= 50.0 || amount.isBlank()
        if (needsBiometric && activity != null && biometricHelper.canAuthenticate()) {
            scope.launch {
                val amountLabel = if (amount.isBlank()) "the full balance" else "$${"%,.2f".format(amt)}"
                when (biometricHelper.authenticate(
                    activity = activity,
                    title    = "Confirm settlement",
                    subtitle = "Settling $amountLabel",
                )) {
                    is BiometricResult.Success   -> settleAction()
                    is BiometricResult.Cancelled -> { /* do nothing */ }
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
        when (val state = uiState) {
            is SettleUpUiState.Success -> { onSuccess(); viewModel.resetUiState() }
            is SettleUpUiState.Error   -> { snackbarHost.showSnackbar(state.message); viewModel.resetUiState() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = { FsTopBar(title = "Settle up", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Direction header ───────────────────────────────────────────────
            DirectionHeader(
                otherUserName   = otherUserName,
                otherUserId     = viewModel.otherUserId,
                balanceAmount   = balanceAmount,
                balanceCurrency = balanceCurrency,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
            HorizontalDivider(color = Surface4, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Partial amount ────────────────────────────────────────────────
            FsSectionLabel(
                text     = "Amount (optional)",
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text     = "Leave blank to settle everything",
                fontSize = 12.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            FsTextField(
                value         = amount,
                onValueChange = { viewModel.onAmountChanged(it) },
                label         = "Enter amount",
                imeAction     = ImeAction.Next,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Payment method ────────────────────────────────────────────────
            FsSectionLabel(
                text     = "Payment method",
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            val methods = listOf("Cash", "Bank transfer", "UPI", "Other")
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                methods.forEach { method ->
                    val isSelected = paymentMethod == method
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.full))
                            .background(if (isSelected) Green400 else Surface2)
                            .clickable { viewModel.onPaymentMethodChanged(method) }
                            .padding(vertical = Spacing.sm),
                    ) {
                        Text(
                            text       = method,
                            fontSize   = 12.sp,
                            color      = if (isSelected) Surface0 else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Notes ─────────────────────────────────────────────────────────
            FsSectionLabel(
                text     = "Notes",
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            FsTextField(
                value         = notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label         = "Add a note (optional)",
                imeAction     = ImeAction.Done,
                singleLine    = false,
                maxLines      = 3,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Actions ───────────────────────────────────────────────────────
            val buttonLabel = when {
                amount.isNotBlank() -> "Record payment of $amount"
                balanceAmount > 0   -> "Record payment of ${MoneyUtils.format(balanceAmount, balanceCurrency)}"
                balanceAmount < 0   -> "Record payment of ${MoneyUtils.format(-balanceAmount, balanceCurrency)}"
                else                -> "Record payment"
            }
            FsPrimaryButton(
                text      = buttonLabel,
                onClick   = {
                    confirmAndSettle {
                        if (amount.isBlank()) viewModel.settleAll()
                        else viewModel.settlePartial()
                    }
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                isLoading = isLoading,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

// ── Direction Header ──────────────────────────────────────────────────────────

/**
 * Shows who is paying whom:
 *   balance > 0 → Friend → You   (they owe you, they pay you)
 *   balance < 0 → You → Friend   (you owe them, you pay them)
 *   balance = 0 → All settled up
 */
@Composable
private fun DirectionHeader(
    otherUserName   : String,
    otherUserId     : String,
    balanceAmount   : Double,
    balanceCurrency : String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    ) {
        when {
            balanceAmount > 0 -> {
                // They owe you → they pay you: Friend ──▶ You
                PaymentDirectionRow(
                    fromName   = otherUserName.ifBlank { "Friend" },
                    fromUserId = otherUserId,
                    toName     = "You",
                    toUserId   = "",
                    amount     = balanceAmount,
                    currency   = balanceCurrency,
                    color      = Green400,
                    subtitle   = "${otherUserName.ifBlank { "They" }} owe you this amount",
                )
            }
            balanceAmount < 0 -> {
                // You owe them → you pay them: You ──▶ Friend
                PaymentDirectionRow(
                    fromName   = "You",
                    fromUserId = "",
                    toName     = otherUserName.ifBlank { "Friend" },
                    toUserId   = otherUserId,
                    amount     = -balanceAmount,
                    currency   = balanceCurrency,
                    color      = Negative,
                    subtitle   = "You owe this amount",
                )
            }
            otherUserName.isNotBlank() -> {
                // All settled up — no payment needed
                Text(text = "💸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text       = "All settled up!",
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = Green400,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "You and $otherUserName have no outstanding balance",
                    fontSize = 14.sp,
                    color    = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                // Loading / unknown
                Text(text = "💸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text       = "Record a payment",
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun PaymentDirectionRow(
    fromName   : String,
    fromUserId : String,
    toName     : String,
    toUserId   : String,
    amount     : Double,
    currency   : String,
    color      : androidx.compose.ui.graphics.Color,
    subtitle   : String,
) {
    // Amount chip
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text       = MoneyUtils.format(amount, currency),
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = color,
        )
    }

    Spacer(modifier = Modifier.height(Spacing.lg))

    // From → To row
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        // From
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (fromUserId.isNotBlank()) {
                FsAvatar(name = fromName, userId = fromUserId, size = ComponentSize.avatarMd)
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(ComponentSize.avatarMd)
                        .clip(CircleShape)
                        .background(Surface2),
                ) {
                    Text("You", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = fromName, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.width(Spacing.lg))

        // Arrow
        Icon(
            imageVector        = Icons.Filled.ArrowForward,
            contentDescription = "pays",
            tint               = color,
            modifier           = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(Spacing.lg))

        // To
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (toUserId.isNotBlank()) {
                FsAvatar(name = toName, userId = toUserId, size = ComponentSize.avatarMd)
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(ComponentSize.avatarMd)
                        .clip(CircleShape)
                        .background(Surface2),
                ) {
                    Text("You", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = toName, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
    }

    Spacer(modifier = Modifier.height(Spacing.md))
    Text(text = subtitle, fontSize = 13.sp, color = TextTertiary, textAlign = TextAlign.Center)
}
