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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.data.model.response.SettlementChangeLogResponse
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettlementStatus
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
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementDetailScreen(
    onBack          : () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleted       : () -> Unit,
    viewModel       : SettlementDetailViewModel = hiltViewModel(),
) {
    val state        by viewModel.state.collectAsState()
    val actionState  by viewModel.actionState.collectAsState()
    val changeLog    by viewModel.changeLog.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showCancelDialog  by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load()
        }
    }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is SettlementDetailActionState.Cancelled -> {
                snackbarHost.showSnackbar("Settlement cancelled")
                viewModel.load()
                viewModel.resetActionState()
            }
            is SettlementDetailActionState.Restored -> {
                snackbarHost.showSnackbar("Settlement restored")
                viewModel.load()
                viewModel.resetActionState()
            }
            is SettlementDetailActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    val settlement     = (state as? SettlementDetailUiState.Success)?.settlement
    val isFullySettled = settlement?.isFullSettle == true
    val isCompleted    = settlement?.status == SettlementStatus.COMPLETED
    val isCancelled    = settlement?.status == SettlementStatus.CANCELLED
    val canAct         = settlement != null && viewModel.isParticipant(settlement)

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            FsTopBar(
                title  = when {
                    isCancelled    -> "Payment cancelled"
                    isFullySettled -> "Settled up"
                    else           -> "Payment"
                },
                onBack  = onBack,
                actions = {
                    // Only COMPLETED settlements show Edit button
                    if (isCompleted && canAct) {
                        FsIconButton(
                            icon               = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            onClick            = { onNavigateToEdit(viewModel.settlementId) },
                        )
                    }
                    // COMPLETED → Cancel button; CANCELLED → no destructive action in topbar
                    if (isCompleted && canAct) {
                        FsIconButton(
                            icon               = Icons.Filled.Close,
                            contentDescription = "Cancel settlement",
                            onClick            = { showCancelDialog = true },
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
                is SettlementDetailUiState.NotFound,
                is SettlementDetailUiState.Deleted ->
                    FsErrorScreen(message = "This settlement no longer exists.")
                is SettlementDetailUiState.Success ->
                    if (s.settlement.isFullSettle && s.settlement.settleType == "ALL" && isCompleted) {
                        FullySettledContent(settlement = s.settlement)
                    } else {
                        SettlementDetailContent(
                            settlement  = s.settlement,
                            changeLog   = changeLog,
                            isCancelled = isCancelled,
                            canRestore  = isCancelled && canAct,
                            onRestore   = { showRestoreDialog = true },
                        )
                    }
            }
        }
    }

    // ── Cancel confirmation dialog ─────────────────────────────────────────
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title   = { Text("Cancel this settlement?") },
            text    = { Text("This will reverse the settlement and restore the previous balances.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelSettlement()
                }) { Text("Cancel settlement", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep") }
            },
        )
    }

    // ── Restore confirmation dialog ────────────────────────────────────────
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title   = { Text("Restore this settlement?") },
            text    = { Text("This will apply the settlement again and update balances.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    viewModel.restoreSettlement()
                }) { Text("Restore", color = Green400) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── "Fully settled up in all groups" detail screen ────────────────────────────

@Composable
private fun FullySettledContent(settlement: Settlement) {
    val displayDate = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } catch (e: Exception) { settlement.settlementDate }
    }

    // Parse group balance snapshot JSON
    // Format: [{"groupId":"...","groupName":"...","amount":4.06,"direction":"owed_you","currency":"USD"},...]
    data class GroupSnapshotEntry(
        val groupName: String,
        val amount: Double,
        val direction: String, // "owed_you" or "you_owed"
        val currency: String,
    )

    val groupEntries = remember(settlement.groupBalanceSnapshot) {
        val list = mutableListOf<GroupSnapshotEntry>()
        try {
            val arr = JSONArray(settlement.groupBalanceSnapshot ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(GroupSnapshotEntry(
                    groupName = obj.optString("groupName", "Group"),
                    amount    = obj.optDouble("amount", 0.0),
                    direction = obj.optString("direction", "you_owed"),
                    currency  = obj.optString("currency", "USD"),
                ))
            }
        } catch (e: Exception) { /* ignore parse errors */ }
        list
    }

    // Net direction summary
    val otherPersonName = settlement.payerName.takeIf {
        it != settlement.recordedByName
    } ?: settlement.receiverName

    // Total across all groups for the summary line
    val totalOwedToRecorder = groupEntries.filter { it.direction == "owed_you" }.sumOf { it.amount }
    val totalOwedByRecorder = groupEntries.filter { it.direction == "you_owed" }.sumOf { it.amount }
    val net = totalOwedToRecorder - totalOwedByRecorder
    val summaryLine = when {
        net > 0 -> "${settlement.receiverName} owed ${settlement.payerName} ${MoneyUtils.format(net, settlement.currency)} in shared groups"
        net < 0 -> "${settlement.payerName} owed ${settlement.receiverName} ${MoneyUtils.format(-net, settlement.currency)} in shared groups"
        else    -> "Balances were equal across shared groups"
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        // Scales icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Green400.copy(alpha = 0.12f)),
        ) { Text("⚖️", fontSize = 32.sp) }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text       = "You fully settled up in all groups",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = Spacing.lg),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // "with [avatar] [name]" pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(Surface2)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        ) {
            Text("with", fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.width(Spacing.sm))
            FsAvatar(
                name   = otherPersonName,
                userId = if (settlement.payerName == settlement.recordedByName)
                    settlement.receiverId else settlement.payerId,
                size   = 24.dp,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(otherPersonName, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text     = "Added on $displayDate",
            fontSize = 13.sp,
            color    = TextTertiary,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Info box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2)
                .padding(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Green400.copy(alpha = 0.15f)),
            ) {
                Text("ⓘ", fontSize = 14.sp, color = Green400)
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text     = "When you settled up on $displayDate, FairShare automatically cleared your group balances, as shown below.",
                fontSize = 13.sp,
                color    = TextSecondary,
            )
        }

        if (groupEntries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text     = summaryLine,
                fontSize = 13.sp,
                color    = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2),
            ) {
                groupEntries.forEachIndexed { index, entry ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Group initial box
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(groupColor(entry.groupName)),
                        ) {
                            Text(
                                text       = entry.groupName.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.width(Spacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.groupName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            val subtitle = if (entry.direction == "owed_you")
                                "${otherPersonName} owed You ${MoneyUtils.format(entry.amount, entry.currency)}"
                            else
                                "You owed ${otherPersonName} ${MoneyUtils.format(entry.amount, entry.currency)}"
                            Text(subtitle, fontSize = 12.sp,
                                color = if (entry.direction == "owed_you") Green400 else Negative)
                        }
                    }
                    if (index < groupEntries.lastIndex) {
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = Spacing.lg))
                    }
                }
            }
        }
    }
}

private fun groupColor(name: String): Color {
    val colors = listOf(
        Color(0xFF1A2A3A), Color(0xFF1A3A1A), Color(0xFF2A1A0A),
        Color(0xFF1A1A3A), Color(0xFF2A1A2A),
    )
    return colors[(name.hashCode() and 0x7fffffff) % colors.size]
}

// ── Standard payment detail screen ───────────────────────────────────────────

@Composable
private fun SettlementDetailContent(
    settlement  : Settlement,
    changeLog   : List<SettlementChangeLogResponse>,
    isCancelled : Boolean = false,
    canRestore  : Boolean = false,
    onRestore   : () -> Unit = {},
) {
    val displayDate = remember(settlement.settlementDate) {
        try {
            val dt = LocalDateTime.parse(settlement.settlementDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) { settlement.settlementDate }
    }

    val cancelledDate = remember(settlement.cancelledAt) {
        try {
            if (settlement.cancelledAt == null) null
            else {
                val dt = LocalDateTime.parse(settlement.cancelledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        } catch (e: Exception) { settlement.cancelledAt }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── CANCELLED badge ───────────────────────────────────────────────
        if (isCancelled) {
            SuggestionChip(
                onClick = {},
                label   = { Text("CANCELLED", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp) },
                colors  = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Negative.copy(alpha = 0.15f),
                    labelColor     = Negative,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        // Money icon — dimmed when cancelled
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background((if (isCancelled) TextTertiary else Green400).copy(alpha = 0.12f)),
        ) { Text("💸", fontSize = 32.sp) }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text       = "${settlement.payerName} paid ${settlement.receiverName}",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isCancelled) TextSecondary else TextPrimary,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = Spacing.lg),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Amount — strikethrough when cancelled
        Text(
            text           = MoneyUtils.format(settlement.amount, settlement.currency),
            fontSize       = 38.sp,
            fontWeight     = FontWeight.ExtraBold,
            color          = if (isCancelled) TextTertiary else Green400,
            textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FsAvatar(name = settlement.payerName, userId = settlement.payerId, size = ComponentSize.avatarLg)
            Spacer(modifier = Modifier.width(Spacing.lg))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Surface2),
            ) { Text("→", fontSize = 14.sp, color = if (isCancelled) TextTertiary else Green400, fontWeight = FontWeight.Bold) }
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2),
        ) {
            MetaRow(label = "Date",           value = displayDate)
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

            // Show who cancelled and when
            if (isCancelled && !settlement.cancelledByName.isNullOrBlank()) {
                HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = Spacing.lg))
                MetaRow(
                    label = "Cancelled by",
                    value = if (cancelledDate != null) "${settlement.cancelledByName} on $cancelledDate"
                    else settlement.cancelledByName,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        if (isCancelled) {
            Text(
                text      = "This settlement was cancelled. Balances have been restored.",
                fontSize  = 12.sp,
                color     = Negative.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = Spacing.xl),
            )
        } else {
            Text(
                text      = "This payment was added using the \"record a payment\" feature. No money has been moved.",
                fontSize  = 12.sp,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = Spacing.xl),
            )
        }

        // ── Restore button (CANCELLED only) ───────────────────────────────
        if (canRestore) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            Button(
                onClick  = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                colors   = ButtonDefaults.buttonColors(containerColor = Green400),
            ) {
                Text("Restore settlement", fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Edit history ─────────────────────────────────────────────────────
        if (changeLog.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xl))
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = "EDIT HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    changeLog.forEachIndexed { entryIdx: Int, entry: SettlementChangeLogResponse ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    entry.changedByName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    entry.fieldName.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            if (entry.oldValue != null) {
                                Text("Was: ${entry.oldValue}", fontSize = 12.sp, color = TextSecondary)
                            }
                            val displayNew = entry.newValue ?: "—"
                            Text("Now: $displayNew", fontSize = 12.sp, color = TextPrimary)
                        }
                        if (entryIdx < changeLog.lastIndex)
                            HorizontalDivider(
                                color = Surface3,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = Spacing.md),
                            )
                    }
                }
            }
        }
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