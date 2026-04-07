package com.prathik.fairshare.ui.settlement

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementDetailScreen(
    onBack        : () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleted     : () -> Unit,
    viewModel     : SettlementDetailViewModel = hiltViewModel(),
) {
    val state        by viewModel.state.collectAsState()
    val actionState  by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is SettlementDetailActionState.Deleted -> { onDeleted(); viewModel.resetActionState() }
            is SettlementDetailActionState.Error   -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    val settlement = (state as? SettlementDetailUiState.Success)?.settlement
    val canEdit = settlement != null && viewModel.isRecordedByMe(settlement)

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            FsTopBar(
                title   = "Payment",
                onBack  = onBack,
                actions = {
                    if (canEdit) {
                        FsIconButton(
                            icon               = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            onClick            = { onNavigateToEdit(viewModel.settlementId) },
                        )
                        FsIconButton(
                            icon               = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            onClick            = { showDeleteDialog = true },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is SettlementDetailUiState.Loading ->
                    FsLoadingScreen()
                is SettlementDetailUiState.Error ->
                    FsErrorScreen(message = s.message, onRetry = { viewModel.load() })
                is SettlementDetailUiState.Deleted ->
                    FsErrorScreen(message = "This settlement no longer exists.")
                is SettlementDetailUiState.Success ->
                    SettlementDetailContent(settlement = s.settlement)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete payment?") },
            text    = { Text("This will reverse the balance changes. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSettlement()
                }) { Text("Delete", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettlementDetailContent(settlement: Settlement) {
    val displayDate = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) { settlement.settlementDate }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        // Money icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Green400.copy(alpha = 0.12f)),
        ) { Text("💸", fontSize = 32.sp) }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Payer → Receiver
        Text(
            text       = "${settlement.payerName} paid ${settlement.receiverName}",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = Spacing.lg),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Amount
        Text(
            text       = MoneyUtils.format(settlement.amount, settlement.currency),
            fontSize   = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Green400,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Avatars row
        Row(verticalAlignment = Alignment.CenterVertically) {
            FsAvatar(name = settlement.payerName, userId = settlement.payerId, size = ComponentSize.avatarLg)
            Spacer(modifier = Modifier.width(Spacing.lg))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Surface2),
            ) { Text("→", fontSize = 14.sp, color = Green400, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(Spacing.lg))
            FsAvatar(name = settlement.receiverName, userId = settlement.receiverId, size = ComponentSize.avatarLg)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(settlement.payerName, fontSize = 12.sp, color = TextSecondary,
                modifier = Modifier.width(ComponentSize.avatarLg))
            Spacer(modifier = Modifier.width(Spacing.lg + 28.dp + Spacing.lg))
            Text(settlement.receiverName, fontSize = 12.sp, color = TextSecondary,
                modifier = Modifier.width(ComponentSize.avatarLg),
                textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        HorizontalDivider(color = Surface3)
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Meta info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2),
        ) {
            MetaRow(label = "Date",          value = displayDate)
            HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg))
            MetaRow(label = "Payment method", value = settlement.paymentMethod ?: "Not specified")
            if (!settlement.notes.isNullOrBlank()) {
                HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                MetaRow(label = "Notes", value = settlement.notes)
            }
            if (!settlement.groupName.isNullOrBlank()) {
                HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                MetaRow(label = "Group", value = settlement.groupName)
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg))
            MetaRow(label = "Recorded by", value = settlement.recordedByName)
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Disclaimer
        Text(
            text      = "This payment was recorded using the \"record a payment\" feature. No money has been moved.",
            fontSize  = 12.sp,
            color     = TextTertiary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = Spacing.xl),
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End)
    }
}