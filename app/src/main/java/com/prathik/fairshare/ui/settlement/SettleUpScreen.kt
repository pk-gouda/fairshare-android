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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSectionLabel
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Settle Up Screen.
 *
 * Lets the current user record a settlement with another user.
 * Two modes:
 *   - Settle all: records full balance as settled
 *   - Partial: user enters a specific amount
 *
 * Payment method selector: Cash / Bank Transfer / UPI / Other
 * Optional notes field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    onBack     : () -> Unit,
    onSuccess  : () -> Unit,
    viewModel  : SettleUpViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsState()
    val notes         by viewModel.notes.collectAsState()
    val amount        by viewModel.amount.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current
    val activity      = context as? FragmentActivity

    val isLoading = uiState is SettleUpUiState.Loading

    // Biometric gate — required for settlements above $50
    val biometricHelper = remember { BiometricHelper(context) }

    fun confirmAndSettle(settleAction: () -> Unit) {
        val amt = amount.toDoubleOrNull() ?: 0.0
        val needsBiometric = amt >= 50.0 || amount.isBlank() // settle all always requires
        if (needsBiometric && activity != null && biometricHelper.canAuthenticate()) {
            scope.launch {
                val amountLabel = if (amount.isBlank()) "the full balance" else "$${"%.2f".format(amt)}"
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
        topBar = {
            FsTopBar(title = "Settle up", onBack = onBack)
        },
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

            // ── Header ────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxWidth(),
            ) {
                Text(text = "💸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text       = "Record a payment",
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "Mark your debts as settled",
                    fontSize = 14.sp,
                    color    = TextSecondary,
                )
            }

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
            FsPrimaryButton(
                text      = if (amount.isBlank()) "Settle everything" else "Settle ${amount}",
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