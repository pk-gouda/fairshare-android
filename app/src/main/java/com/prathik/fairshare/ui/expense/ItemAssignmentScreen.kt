package com.prathik.fairshare.ui.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.AvatarColors
import com.prathik.fairshare.ui.theme.Orange400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import kotlin.math.absoluteValue

// ── Design tokens ─────────────────────────────────────────────────────────────
private val Accent     = Color(0xFF00D9A3)
private val AccentBg   = Color(0xFF00D9A3).copy(alpha = 0.08f)
private val CardBg     = Color(0xFF121212)
private val CardBorder = Color(0xFF2A2A2A)
private val ErrorColor = Color(0xFFFF5A5F)
private val PillBg     = Color(0xFFFFFFFF).copy(alpha = 0.08f)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ItemAssignmentScreen(
    receiptId          : String,
    members            : List<GroupMember>,
    currency           : String,
    onBack             : () -> Unit,
    onDone             : (Map<String, List<String>>) -> Unit,
    onNavigateToReview : () -> Unit,
    viewModel          : ItemAssignmentViewModel = hiltViewModel(),
) {
    val items         by viewModel.items.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val shareStates   by viewModel.shareStates.collectAsState()
    val separateItems by viewModel.separateItems.collectAsState()
    val expandedKey   by viewModel.expandedKey.collectAsState()
    val receiptTotal  by viewModel.receiptTotal.collectAsState()
    val totalAssigned by viewModel.totalAssigned.collectAsState()
    val myTotal       by viewModel.myTotal.collectAsState()
    val allDone        = receiptTotal > 0 && (totalAssigned - receiptTotal).absoluteValue < 0.01

    val snackbar = remember { SnackbarHostState() }
    var pendingMergeItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(receiptId) { viewModel.loadItems(receiptId) }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar         = { FsTopBar(title = "Assign items", onBack = onBack) },
        bottomBar      = {
            ItemizeFooter(
                myTotal  = myTotal,
                currency = currency,
                allDone  = allDone,
                onDone   = { onDone(viewModel.buildAssignmentsMap()); onNavigateToReview() },
            )
        },
    ) { pad ->

        if (isLoading) { ItemAssignmentSkeleton(Modifier.padding(pad)); return@Scaffold }

        if (items.isEmpty()) {
            EmptyState(onSkip = { onDone(emptyMap()); onNavigateToReview() })
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            ItemizeHeader(
                receiptTotal  = receiptTotal,
                totalAssigned = totalAssigned,
                currency      = currency,
                allDone       = allDone,
                onAssignToMe  = { viewModel.assignRemainingToMe() },
                onSplitAll    = { viewModel.splitRemainingEqually(members) },
            )

            LazyColumn(
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(
                    start  = Spacing.lg,
                    end    = Spacing.lg,
                    top    = Spacing.md,
                    bottom = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(items, key = { it.id }) { item ->
                    val isSeparate = item.id in separateItems
                    val qty        = item.quantity ?: 1

                    // Everything lives in ONE card per item
                    ItemCard(
                        item          = item,
                        isSeparate    = isSeparate,
                        qty           = qty,
                        shareStates   = shareStates,
                        members       = members,
                        currency      = currency,
                        expandedKey   = expandedKey,
                        onToggleExpand    = { key -> viewModel.toggleExpand(key) },
                        onTogglePerson    = { key, uid -> viewModel.togglePerson(key, uid) },
                        onSelectAll       = { key -> viewModel.selectAll(key, members) },
                        onClear           = { key -> viewModel.clearShare(key) },
                        onSetMode         = { key, mode -> viewModel.setSplitMode(key, mode) },
                        onShowAdvanced    = { key -> viewModel.showAdvanced(key) },
                        onShowNormal      = { key -> viewModel.showNormal(key) },
                        onSetAmount       = { key, uid, v -> viewModel.setAmount(key, uid, v) },
                        onSetPercent      = { key, uid, v -> viewModel.setPercent(key, uid, v) },
                        onSetShares       = { key, uid, v -> viewModel.setShares(key, uid, v) },
                        onSeparateToggle  = { checked ->
                            if (!checked && viewModel.share1HasAssignments(item.id)) {
                                pendingMergeItemId = item.id
                            } else if (checked) {
                                viewModel.enableSeparate(item.id)
                            } else {
                                viewModel.disableSeparate(item.id)
                            }
                        },
                    )
                }
            }
        }
    }

    // Merge confirm dialog
    if (pendingMergeItemId != null) {
        AlertDialog(
            onDismissRequest = { pendingMergeItemId = null },
            containerColor   = Color(0xFF1C1C1C),
            title  = { Text("Merge shares?", color = TextPrimary) },
            text   = { Text("Assignments on individual shares will be lost.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    pendingMergeItemId?.let { viewModel.disableSeparate(it) }
                    pendingMergeItemId = null
                }) { Text("Merge", color = ErrorColor, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingMergeItemId = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ItemizeHeader(
    receiptTotal  : Double,
    totalAssigned : Double,
    currency      : String,
    allDone       : Boolean,
    onAssignToMe  : () -> Unit,
    onSplitAll    : () -> Unit,
) {
    val pct by animateFloatAsState(
        if (receiptTotal > 0) (totalAssigned / receiptTotal).coerceIn(0.0, 1.0).toFloat() else 0f,
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface0)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            Column {
                Text("Receipt total", fontSize = 11.sp, color = TextTertiary)
                Text(
                    MoneyUtils.format(receiptTotal, currency),
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = TextPrimary,
                    letterSpacing = (-0.5).sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Assigned", fontSize = 11.sp, color = TextTertiary)
                Text(
                    MoneyUtils.format(totalAssigned, currency),
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Accent,
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        LinearProgressIndicator(
            progress   = { pct },
            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)),
            color      = Accent,
            trackColor = Surface3,
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "${MoneyUtils.format(totalAssigned, currency)} of ${MoneyUtils.format(receiptTotal, currency)} assigned",
            fontSize = 11.sp,
            color    = TextTertiary,
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BulkBtn("Assign remaining to me",  !allDone, Modifier.weight(1f), onAssignToMe)
            BulkBtn("Split remaining equally", !allDone, Modifier.weight(1f), onSplitAll)
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
    }
}

@Composable
private fun BulkBtn(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(if (enabled) Surface3 else CardBg)
            .border(0.5.dp, CardBorder, RoundedCornerShape(Radius.md))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = if (enabled) TextSecondary else TextTertiary,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
        )
    }
}

// ── Unified item card — one card per receipt item ─────────────────────────────
// When qty > 1, the checkbox sits right below the header row.
// When "assign separately" is checked, all shares render inside the same card
// separated by dividers — matching the reference design.

@Composable
private fun ItemCard(
    item             : ExpenseItem,
    isSeparate       : Boolean,
    qty              : Int,
    shareStates      : Map<String, ShareUiState>,
    members          : List<GroupMember>,
    currency         : String,
    expandedKey      : String?,
    onToggleExpand   : (String) -> Unit,
    onTogglePerson   : (String, String) -> Unit,
    onSelectAll      : (String) -> Unit,
    onClear          : (String) -> Unit,
    onSetMode        : (String, SplitMode) -> Unit,
    onShowAdvanced   : (String) -> Unit,
    onShowNormal     : (String) -> Unit,
    onSetAmount      : (String, String, String) -> Unit,
    onSetPercent     : (String, String, String) -> Unit,
    onSetShares      : (String, String, String) -> Unit,
    onSeparateToggle : (Boolean) -> Unit,
) {
    // Border: green when fully assigned, orange when partial, subtle when untouched
    val shareCount0  = if (isSeparate && qty > 1) qty else 1
    val allComplete  = (0 until shareCount0).all { i ->
        shareStates["${item.id}-$i"]?.isComplete() == true
    }
    val anyAssigned  = (0 until shareCount0).any { i ->
        (shareStates["${item.id}-$i"]?.assignedAmount() ?: 0.0) > 0.0
    }
    val borderColor  = when {
        allComplete -> Accent
        anyAssigned -> Orange400
        else        -> CardBorder
    }
    val borderWidth  = if (anyAssigned || allComplete) 1.5.dp else 0.5.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(CardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(Radius.xl)),
    ) {
        // ── Item header: name + qty badge + total price ───────────────────
        // Tapping the header area opens/closes Share 0 (the first/only share section)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand("${item.id}-0") }
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.md, bottom = Spacing.sm),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = item.name,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary,
                        // Full name wraps — no ellipsis
                    )
                    // Sub line: "5 × $9.45" for multi-qty items
                    val sub = when {
                        qty > 1 -> "$qty × ${MoneyUtils.format(item.price, currency)}"
                        else    -> ""
                    }
                    if (sub.isNotEmpty()) {
                        Text(sub, fontSize = 13.sp, color = TextTertiary)
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = MoneyUtils.format(item.totalPrice, currency),
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    if (qty > 1) {
                        // "N items" badge
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A2A))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text     = "$qty items",
                                fontSize = 11.sp,
                                color    = TextTertiary,
                            )
                        }
                    }
                }
            }

            // ── Checkbox: "Assign each X separately" — right below header ──
            if (qty > 1) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onSeparateToggle(!isSeparate) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked         = isSeparate,
                        onCheckedChange = onSeparateToggle,
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = Accent,
                            uncheckedColor = TextTertiary,
                            checkmarkColor = Color.Black,
                        ),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text     = "Assign each item separately",
                        fontSize = 14.sp,
                        color    = if (isSeparate) TextPrimary else TextTertiary,
                    )
                }
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

        // ── Share sections ────────────────────────────────────────────────
        val shareCount = if (isSeparate && qty > 1) qty else 1

        for (shareIdx in 0 until shareCount) {
            val key        = "${item.id}-$shareIdx"
            val shareState = shareStates[key] ?: continue
            val isExpanded = expandedKey == key
            val shareLabel = if (isSeparate && qty > 1) "Share ${shareIdx + 1}" else null

            if (shareIdx > 0) {
                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            }

            ShareSection(
                shareState     = shareState,
                shareLabel     = shareLabel,
                sharePrice     = shareState.amount,
                currency       = currency,
                members        = members,
                isExpanded     = isExpanded,
                onToggleExpand = { onToggleExpand(key) },
                onTogglePerson = { uid -> onTogglePerson(key, uid) },
                onSelectAll    = { onSelectAll(key) },
                onClear        = { onClear(key) },
                onSetMode      = { mode -> onSetMode(key, mode) },
                onShowAdvanced = { onShowAdvanced(key) },
                onShowNormal   = { onShowNormal(key) },
                onSetAmount    = { uid, v -> onSetAmount(key, uid, v) },
                onSetPercent   = { uid, v -> onSetPercent(key, uid, v) },
                onSetShares    = { uid, v -> onSetShares(key, uid, v) },
            )
        }
    }
}

// ── Share section — one per share inside ItemCard ─────────────────────────────

@Composable
private fun ShareSection(
    shareState     : ShareUiState,
    shareLabel     : String?,      // "Share 1", "Share 2", or null for unsplit items
    sharePrice     : Double,
    currency       : String,
    members        : List<GroupMember>,
    isExpanded     : Boolean,
    onToggleExpand : () -> Unit,
    onTogglePerson : (String) -> Unit,
    onSelectAll    : () -> Unit,
    onClear        : () -> Unit,
    onSetMode      : (SplitMode) -> Unit,
    onShowAdvanced : () -> Unit,
    onShowNormal   : () -> Unit,
    onSetAmount    : (String, String) -> Unit,
    onSetPercent   : (String, String) -> Unit,
    onSetShares    : (String, String) -> Unit,
) {
    val chevronAngle by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chev")
    val summaryText   = shareState.summaryText(members, currency)
    val isAssigned    = shareState.assignedAmount() > 0
    val isComplete    = shareState.isComplete()

    // ── Collapsed row ─────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        // Share label + price on same row (only when separate mode)
        if (shareLabel != null) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1C1C1C))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text       = shareLabel,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextSecondary,
                    )
                }
                Text(
                    text       = MoneyUtils.format(sharePrice, currency),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        // Summary + chevron
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = if (!isAssigned) "+ Add people" else summaryText,
                fontSize = 13.sp,
                fontWeight = if (!isAssigned) FontWeight.Medium else FontWeight.Normal,
                color    = when {
                    !isAssigned -> Accent.copy(alpha = 0.7f)
                    isComplete  -> Accent
                    else        -> Orange400
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector        = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint               = TextTertiary,
                modifier           = Modifier.size(18.dp).rotate(chevronAngle),
            )
        }
    }

    // ── Expanded content ──────────────────────────────────────────────────
    AnimatedVisibility(
        visible = isExpanded,
        enter   = expandVertically(),
        exit    = shrinkVertically(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
        ) {
            if (!shareState.isAdvanced) {
                NormalModeContent(
                    shareState     = shareState,
                    members        = members,
                    currency       = currency,
                    onTogglePerson = onTogglePerson,
                    onSelectAll    = onSelectAll,
                    onClear        = onClear,
                    onShowAdvanced = onShowAdvanced,
                )
            } else {
                AdvancedModeContent(
                    shareState     = shareState,
                    members        = members,
                    currency       = currency,
                    onSetMode      = onSetMode,
                    onTogglePerson = onTogglePerson,
                    onSelectAll    = onSelectAll,
                    onClear        = onClear,
                    onSetAmount    = onSetAmount,
                    onSetPercent   = onSetPercent,
                    onSetShares    = onSetShares,
                    onShowNormal   = onShowNormal,
                )
            }
        }
    }
}

// ── Normal mode ───────────────────────────────────────────────────────────────

@Composable
private fun NormalModeContent(
    shareState     : ShareUiState,
    members        : List<GroupMember>,
    currency       : String,
    onTogglePerson : (String) -> Unit,
    onSelectAll    : () -> Unit,
    onClear        : () -> Unit,
    onShowAdvanced : () -> Unit,
) {
    // "Tap people to split equally:"  with  All / Clear  text links on the right
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = "Tap people to split equally:",
            fontSize = 12.sp,
            color    = TextTertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text       = "All",
            fontSize   = 12.sp,
            color      = Accent,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier
                .clickable { onSelectAll() }
                .padding(horizontal = Spacing.sm, vertical = 4.dp),
        )
        if (shareState.selected.isNotEmpty()) {
            Text(
                text     = "Clear",
                fontSize = 12.sp,
                color    = TextTertiary,
                modifier = Modifier
                    .clickable { onClear() }
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }

    Spacer(Modifier.height(Spacing.sm))

    // Horizontal scrolling circles with first-name labels underneath
    AvatarRow(
        members        = members,
        selected       = shareState.selected,
        onTogglePerson = onTogglePerson,
    )

    Spacer(Modifier.height(Spacing.sm))

    // "Advanced splitting →" — no CalcBar here, summary line already shows the split
    Row(
        modifier          = Modifier
            .clickable { onShowAdvanced() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Advanced splitting", fontSize = 12.sp, color = Accent)
        Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Accent, modifier = Modifier.size(14.dp))
    }
}

// ── Advanced mode ─────────────────────────────────────────────────────────────

@Composable
private fun AdvancedModeContent(
    shareState     : ShareUiState,
    members        : List<GroupMember>,
    currency       : String,
    onSetMode      : (SplitMode) -> Unit,
    onTogglePerson : (String) -> Unit,
    onSelectAll    : () -> Unit,
    onClear        : () -> Unit,
    onSetAmount    : (String, String) -> Unit,
    onSetPercent   : (String, String) -> Unit,
    onSetShares    : (String, String) -> Unit,
    onShowNormal   : () -> Unit,
) {
    // 4 pills
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            SplitMode.EQUAL   to "Equally",
            SplitMode.AMOUNT  to "Amount",
            SplitMode.PERCENT to "Percent",
            SplitMode.SHARES  to "Shares",
        ).forEach { (mode, label) ->
            val active = shareState.splitMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(if (active) Accent else PillBg)
                    .clickable { onSetMode(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize   = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color      = if (active) Color.Black else TextTertiary,
                )
            }
        }
    }

    Spacer(Modifier.height(Spacing.sm))

    when (shareState.splitMode) {
        SplitMode.EQUAL -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tap people to split equally:", fontSize = 12.sp, color = TextTertiary, modifier = Modifier.weight(1f))
                Text(
                    "All",
                    fontSize   = 12.sp,
                    color      = Accent,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.clickable { onSelectAll() }.padding(horizontal = Spacing.sm, vertical = 4.dp),
                )
                if (shareState.selected.isNotEmpty()) {
                    Text(
                        "Clear",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                        modifier = Modifier.clickable { onClear() }.padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            AvatarRow(members = members, selected = shareState.selected, onTogglePerson = onTogglePerson)
        }
        SplitMode.AMOUNT  -> PersonInputRows(members, shareState.amounts,  "₹", "",  KeyboardType.Decimal, onSetAmount)
        SplitMode.PERCENT -> PersonInputRows(members, shareState.percents, "",  "%", KeyboardType.Decimal, onSetPercent)
        SplitMode.SHARES  -> PersonInputRows(members, shareState.shares,   "",  "x", KeyboardType.Number,  onSetShares)
    }

    // Validation status bar — only shown for Amount/Percent/Shares
    if (shareState.splitMode != SplitMode.EQUAL) {
        Spacer(Modifier.height(Spacing.sm))
        AdvStatusBar(
            text    = shareState.advStatusText(currency),
            isWarn  = shareState.advStatusIsWarn(),
            isError = shareState.advStatusIsError(),
        )
    }

    Spacer(Modifier.height(Spacing.sm))

    // ← Back to simple
    Row(
        modifier          = Modifier.clickable { onShowNormal() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.KeyboardArrowRight, null, tint = TextTertiary, modifier = Modifier.size(14.dp).rotate(180f))
        Text("Back to simple", fontSize = 12.sp, color = TextTertiary)
    }
}

// ── Avatar horizontal row with first-name labels ──────────────────────────────
// Replaces the old 3-col square grid.
// Each avatar is a 44dp circle with a 6-char truncated first name underneath.
// Row scrolls horizontally if members overflow (e.g. 8+ people).

@Composable
private fun AvatarRow(
    members        : List<GroupMember>,
    selected       : List<String>,
    onTogglePerson : (String) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        members.forEach { member ->
            val isSel     = member.userId in selected
            val initials  = member.fullName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
            val firstName = member.fullName.split(" ").firstOrNull()?.take(9) ?: initials
            val avatarCol = AvatarColors[member.userId.hashCode().and(0x7FFFFFFF) % AvatarColors.size]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .clickable { onTogglePerson(member.userId) }
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSel) avatarCol else Color(0xFF2A2A2A))
                        .then(
                            if (isSel) Modifier.border(2.dp, Accent, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = initials,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isSel) Color.White else TextTertiary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = firstName,
                    fontSize  = 10.sp,
                    color     = if (isSel) Accent else TextTertiary,
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    modifier  = Modifier.width(44.dp),
                )
            }
        }
    }
}

// ── Advanced status bar (Amount / Percent / Shares validation) ────────────────

@Composable
private fun AdvStatusBar(text: String, isWarn: Boolean, isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(
                when {
                    isError -> ErrorColor.copy(alpha = 0.08f)
                    isWarn  -> Orange400.copy(alpha = 0.08f)
                    else    -> AccentBg
                }
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text       = text,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = when {
                isError -> ErrorColor
                isWarn  -> Orange400
                else    -> Accent
            },
        )
    }
}

// ── Per-person input rows (Amount / Percent / Shares) ─────────────────────────

@Composable
private fun PersonInputRows(
    members      : List<GroupMember>,
    values       : Map<String, String>,
    prefix       : String,
    suffix       : String,
    keyboardType : KeyboardType,
    onValueChange: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        members.forEach { member ->
            val initials  = member.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
            val avatarCol = AvatarColors[member.userId.hashCode().and(0x7FFFFFFF) % AvatarColors.size]
            val firstName = member.fullName.split(" ").firstOrNull() ?: initials

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Surface3)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier         = Modifier.size(32.dp).clip(CircleShape).background(avatarCol),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    firstName,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextSecondary,
                    modifier   = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Color(0xFF1C1C1C))
                        .border(0.5.dp, CardBorder, RoundedCornerShape(Radius.sm))
                        .padding(horizontal = Spacing.sm, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix.isNotEmpty()) Text(prefix, fontSize = 12.sp, color = TextTertiary)
                    BasicTextField(
                        value           = values[member.userId] ?: "",
                        onValueChange   = { onValueChange(member.userId, it) },
                        textStyle       = TextStyle(fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.Center),
                        singleLine      = true,
                        cursorBrush     = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier        = Modifier.width(56.dp),
                    ) { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if ((values[member.userId] ?: "").isEmpty())
                                Text("0", fontSize = 13.sp, color = TextTertiary, textAlign = TextAlign.Center)
                            inner()
                        }
                    }
                    if (suffix.isNotEmpty()) Text(suffix, fontSize = 12.sp, color = TextTertiary)
                }
            }
        }
    }
}



// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun ItemizeFooter(myTotal: Double, currency: String, allDone: Boolean, onDone: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Surface0.copy(alpha = 0.95f))
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text("Your items", fontSize = 12.sp, color = TextTertiary)
            Text(MoneyUtils.format(myTotal, currency), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Accent)
        }
        Button(
            onClick        = onDone,
            enabled        = allDone,
            shape          = RoundedCornerShape(Radius.lg),
            colors         = ButtonDefaults.buttonColors(
                containerColor         = Accent,
                disabledContainerColor = Surface3,
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 13.dp),
        ) {
            Text(
                "Done",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = if (allDone) Color.Black else TextTertiary,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onSkip: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧾", fontSize = 48.sp)
            Spacer(Modifier.height(Spacing.md))
            Text("No items detected", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(Spacing.sm))
            Text("Gemini couldn't extract items from this receipt.", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = onSkip, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Continue without itemizing", color = Color.Black)
            }
        }
    }
}

// ── ItemAssignment skeleton ───────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun ItemAssignmentSkeleton(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        com.prathik.fairshare.ui.components.FsSkeletonBlock(height = 48.dp, widthFraction = 1f, cornerRadius = 10.dp)
        repeat(5) { com.prathik.fairshare.ui.components.FsSkeletonTimelineRow() }
        com.prathik.fairshare.ui.components.FsSkeletonBlock(height = 44.dp, widthFraction = 1f, cornerRadius = 8.dp)
    }
}