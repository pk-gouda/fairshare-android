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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSectionLabel
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSettlementScreen(
    onBack : () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditSettlementViewModel = hiltViewModel(),
) {
    val loadState    by viewModel.loadState.collectAsState()
    val saveState    by viewModel.saveState.collectAsState()
    val amount       by viewModel.amount.collectAsState()
    val notes        by viewModel.notes.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is EditSettlementSaveState.Saved -> { onSaved(); viewModel.resetSaveState() }
            is EditSettlementSaveState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Edit payment", onBack = onBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val ls = loadState) {
                is EditSettlementLoadState.Loading -> FsLoadingScreen()
                is EditSettlementLoadState.Error   -> FsErrorScreen(message = ls.message)
                is EditSettlementLoadState.Success -> {
                    val settlement = ls.settlement
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(bottom = Spacing.xxxl),
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.lg))

                        // Amount
                        FsSectionLabel(
                            text     = "AMOUNT",
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        FsTextField(
                            value         = amount,
                            onValueChange = { new ->
                                if (new.isEmpty() || new.matches(Regex("^\\d*(\\.\\d{0,2})?$")))
                                    viewModel.onAmountChanged(new)
                            },
                            label         = settlement.currency,
                            imeAction     = ImeAction.Next,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                        )

                        Spacer(modifier = Modifier.height(Spacing.lg))

                        // Payment method
                        FsSectionLabel(
                            text     = "PAYMENT METHOD",
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
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
                                        .clip(RoundedCornerShape(Radius.full))
                                        .background(if (selected) Green400 else Surface2)
                                        .clickable { viewModel.onPaymentMethodChanged(method) }
                                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                ) {
                                    Text(
                                        text       = method,
                                        fontSize   = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (selected) Surface0 else TextSecondary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.lg))

                        // Notes
                        FsSectionLabel(
                            text     = "NOTES",
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        FsTextField(
                            value         = notes,
                            onValueChange = { viewModel.onNotesChanged(it) },
                            label         = "Add a note (optional)",
                            singleLine    = false,
                            maxLines      = 3,
                            imeAction     = ImeAction.Done,
                            modifier      = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xl))

                        FsPrimaryButton(
                            text      = "Save changes",
                            onClick   = { viewModel.save() },
                            isLoading = saveState is EditSettlementSaveState.Loading,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}