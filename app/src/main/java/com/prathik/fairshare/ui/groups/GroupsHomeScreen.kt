package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
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

private val Gold = Color(0xFFF59E0B)

/**
 * Groups Home Screen — redesigned to match mockups.
 *
 * Five states:
 *  1. Empty — no groups yet (centered CTA)
 *  2. Add Group sheet — 3 paths: create / join / import
 *  3. All settled — gold ring on cards, green "All settled 🎉" net bar
 *  4. Owed to you — green rings, green net bar
 *  5. You owe — red rings, red net bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val groupsState     by viewModel.groupsState.collectAsState()
    val balanceSummary  by viewModel.balanceSummary.collectAsState()
    val groupBalanceMap by viewModel.groupBalanceMap.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val isLoading = groupsState is GroupsUiState.Loading

    var showAddGroupSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    // ── Add Group sheet ───────────────────────────────────────────────────────
    if (showAddGroupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddGroupSheet = false },
            sheetState = sheetState,
            containerColor = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxxl),
            ) {
                Text("Add a group", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Choose how you want to connect", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(Spacing.lg))

                GroupSheetOption(
                    icon = Icons.Outlined.GroupWork,
                    iconBg = Green400,
                    title = "Create a new group",
                    subtitle = "Start fresh with a name and members",
                    onClick = { showAddGroupSheet = false; onNavigateToCreateGroup() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                GroupSheetOption(
                    icon = Icons.Outlined.Link,
                    iconBg = Color(0xFF4A6FE8),
                    title = "Join with invite code",
                    subtitle = "Enter a code shared by a friend",
                    onClick = { showAddGroupSheet = false; onNavigateToJoinGroup() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                GroupSheetOption(
                    icon = Icons.Outlined.FileUpload,
                    iconBg = Color(0xFFE8A84F),
                    title = "Import from Splitwise",
                    subtitle = "Upload a CSV to migrate a group",
                    onClick = { showAddGroupSheet = false; onNavigateToImport() },
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                TextButton(
                    onClick = { showAddGroupSheet = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Cancel", fontSize = 15.sp, color = TextSecondary)
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            Surface(color = Surface0, shadowElevation = 0.dp) {
                Column {
                    // ── Search bar ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Surface2)
                                .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                                .padding(horizontal = Spacing.md, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchChanged(it) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                                cursorBrush = SolidColor(Green400),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search groups...", fontSize = 14.sp, color = TextSecondary)
                                    }
                                    inner()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Add-group icon button (hamburger/group-add style)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Surface2)
                                .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                                .clickable { showAddGroupSheet = true },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GroupAdd,
                                contentDescription = "Add group",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // ── Net balance sticky bar ────────────────────────────────
                    balanceSummary?.let { summary ->
                        val hasGroups = groupsState is GroupsUiState.Success &&
                                (groupsState as GroupsUiState.Success).groups.isNotEmpty()
                        if (hasGroups) {
                            NetBalanceBar(summary = summary)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = Green400,
                contentColor = Surface0,
                shape = RoundedCornerShape(Radius.xl),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = groupsState) {
                is GroupsUiState.Loading -> FsLoadingScreen()

                is GroupsUiState.Error -> FsErrorScreen(
                    message = state.message,
                    isNetwork = state.isNetwork,
                    onRetry = { viewModel.loadData() },
                )

                is GroupsUiState.Success -> {
                    val allGroups    = state.groups
                    val activeGroups = allGroups.filter { !it.isArchived }.let { list ->
                        if (searchQuery.isBlank()) list
                        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }
                    val archivedGroups = allGroups.filter { it.isArchived }.let { list ->
                        if (searchQuery.isBlank()) list
                        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }
                    val displayGroups = if (searchQuery.isBlank()) activeGroups
                    else activeGroups + archivedGroups

                    if (activeGroups.isEmpty() && archivedGroups.isEmpty() && searchQuery.isBlank()) {
                        // ── State 1: Empty ────────────────────────────────────
                        EmptyGroupsState(onAddGroup = { showAddGroupSheet = true })
                    } else {
                        // Compute total owed / owed-to-me for ring fill fractions
                        val totalOwed    = balanceSummary?.youOwe?.takeIf { it > 0 } ?: 1.0
                        val totalOwedMe  = balanceSummary?.owedToMe?.takeIf { it > 0 } ?: 1.0

                        var showArchived by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = Spacing.lg,
                                end = Spacing.lg,
                                top = Spacing.md,
                                bottom = 100.dp,
                            ),
                        ) {
                            // ── Active groups ─────────────────────────────────
                            items(items = activeGroups, key = { it.id }) { group ->
                                val entries  = groupBalanceMap[group.id]
                                val netAmt   = entries?.sumOf { it.first }
                                val dominant = entries?.maxByOrNull { Math.abs(it.first) }
                                val fraction: Float = when {
                                    netAmt == null || netAmt == 0.0 -> 0f
                                    netAmt > 0  -> (netAmt / totalOwedMe).toFloat().coerceIn(0f, 1f)
                                    else        -> (-netAmt / totalOwed).toFloat().coerceIn(0f, 1f)
                                }
                                GroupCard(
                                    group    = group,
                                    entries  = entries,
                                    fraction = fraction,
                                    onClick  = { onNavigateToGroup(group.id) },
                                )
                            }

                            // ── Archived section header ───────────────────────
                            if (archivedGroups.isNotEmpty()) {
                                item {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showArchived = !showArchived }
                                            .padding(vertical = Spacing.sm),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = if (showArchived) "▾" else "▸",
                                            fontSize = 12.sp,
                                            color = TextTertiary,
                                        )
                                        androidx.compose.material3.Text(
                                            text = "Archived (${archivedGroups.size})",
                                            fontSize = 12.sp,
                                            color = TextTertiary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                        )
                                    }
                                }

                                // ── Archived group cards ──────────────────────
                                if (showArchived) {
                                    items(items = archivedGroups, key = { "archived_${it.id}" }) { group ->
                                        GroupCard(
                                            group    = group,
                                            entries  = null,
                                            fraction = 0f,
                                            onClick  = { onNavigateToGroup(group.id) },
                                        )
                                    }
                                }
                            }

                            // ── Add a group dashed card ───────────────────────
                            item {
                                AddGroupDashedCard(onClick = { showAddGroupSheet = true })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Net Balance Bar ───────────────────────────────────────────────────────────

@Composable
private fun NetBalanceBar(summary: BalanceSummary) {
    // Compute per-direction entries first
    val owedEntries = summary.entries.filter { it.net > 0.0 }
    val oweEntries  = summary.entries.filter { it.net < 0.0 }
    val hasBothDirections = owedEntries.isNotEmpty() && oweEntries.isNotEmpty()

    // Mixed: pill shows "Owed to you" for dominant direction
    val dominantIsOwed = summary.owedToMe >= summary.youOwe
    val netColor = if (dominantIsOwed) Green400 else Negative
    val pillLabel = if (dominantIsOwed) "Owed to you" else "You owe"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface0)
            .border(width = 0.5.dp, color = Surface4, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = Spacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            when {
                summary.entries.isEmpty() -> Text(
                    "All settled up",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextTertiary,
                )
                hasBothDirections -> {
                    // Two lines — Splitwise style, no "Mixed" label
                    val oweText  = oweEntries.joinToString(" + ") { MoneyUtils.format(Math.abs(it.net), it.currency) }
                    val owedText = owedEntries.joinToString(" + ") { MoneyUtils.format(it.net, it.currency) }
                    Text(
                        text = "You owe $oweText",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Negative,
                    )
                    Text(
                        text = "Owed to you $owedText",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Green400,
                    )
                }
                oweEntries.isNotEmpty() -> Text(
                    text = oweEntries.joinToString(" + ") { MoneyUtils.format(Math.abs(it.net), it.currency) },
                    fontSize = if (oweEntries.size > 1) 18.sp else 24.sp,
                    fontWeight = FontWeight.Bold, color = Negative,
                )
                else -> Text(
                    text = owedEntries.joinToString(" + ") { MoneyUtils.format(it.net, it.currency) },
                    fontSize = if (owedEntries.size > 1) 18.sp else 24.sp,
                    fontWeight = FontWeight.Bold, color = Green400,
                )
            }
        }

        // Pill badge — shows dominant direction with currency, never "Mixed"
        if (summary.entries.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(netColor.copy(alpha = 0.15f))
                    .border(1.dp, netColor.copy(alpha = 0.3f), RoundedCornerShape(Radius.full))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(pillLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = netColor)
            }
        }
    }
}

// ── Group Card ────────────────────────────────────────────────────────────────

@Composable
private fun GroupCard(
    group    : Group,
    entries  : List<Pair<Double, String>>?,  // null=no expenses; one entry per currency
    fraction : Float,     // 0..1 arc fill on the ring
    onClick  : () -> Unit,
) {
    val positives = entries?.filter { it.first > 0 } ?: emptyList()
    val negatives = entries?.filter { it.first < 0 } ?: emptyList()
    val isMixed   = positives.isNotEmpty() && negatives.isNotEmpty()
    // Dominant = larger absolute value side
    val posTotal  = positives.sumOf { it.first }
    val negTotal  = negatives.sumOf { -it.first }
    val isSettled = entries != null && entries.isNotEmpty() && positives.isEmpty() && negatives.isEmpty()
    val arcColor  = when {
        isSettled       -> Gold
        isMixed         -> if (negTotal > posTotal) Negative else Green400
        positives.isNotEmpty() -> Green400
        negatives.isNotEmpty() -> Negative
        else            -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Surface4, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ring + emoji
        GroupProgressRing(
            emoji     = groupTypeEmoji(group.type),
            fraction  = fraction,
            arcColor  = arcColor,
            isSettled = isSettled,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = group.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${group.memberCount} members",
                    fontSize = 12.sp,
                    color = TextTertiary,
                )
            }
        }

        // Balance label — Splitwise pattern
        when {
            entries == null -> Text("no expenses", fontSize = 12.sp, color = TextTertiary)
            isSettled -> Text("settled up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green400)
            else -> Column(horizontalAlignment = Alignment.End) {
                when {
                    // All owed to you
                    negatives.isEmpty() -> {
                        Text("owed to you", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = positives.joinToString(" + ") { (a,c) -> MoneyUtils.format(a,c) },
                            fontSize = if (positives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Green400, maxLines = 1,
                        )
                    }
                    // All you owe
                    positives.isEmpty() -> {
                        Text("you owe", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = negatives.joinToString(" + ") { (a,c) -> MoneyUtils.format(-a,c) },
                            fontSize = if (negatives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Negative, maxLines = 1,
                        )
                    }
                    // Mixed — show dominant direction + asterisk
                    negTotal >= posTotal -> {
                        Text("you owe", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = negatives.joinToString(" + ") { (a,c) -> MoneyUtils.format(-a,c) } + "*",
                            fontSize = if (negatives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Negative, maxLines = 1,
                        )
                    }
                    else -> {
                        Text("owed to you", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = positives.joinToString(" + ") { (a,c) -> MoneyUtils.format(a,c) } + "*",
                            fontSize = if (positives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Green400, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ── Progress Ring ─────────────────────────────────────────────────────────────

@Composable
private fun GroupProgressRing(
    emoji    : String,
    fraction : Float,     // 0..1; 0 = no arc drawn (no expenses)
    arcColor : Color,     // ring color (green/red/gold)
    isSettled: Boolean,   // true → full gold ring, no % badge
) {
    Box(modifier = Modifier.size(52.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val inset = strokeWidth / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Gray track
            drawArc(
                color      = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = style,
            )

            // Colored arc
            val sweep = if (isSettled) 360f else (fraction * 360f).coerceAtLeast(0f)
            if (sweep > 0f) {
                drawArc(
                    color      = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = if (isSettled) Stroke(width = strokeWidth)
                    else style,
                )
            }
        }

        // Center emoji tile
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface3),
        ) {
            Text(emoji, fontSize = 18.sp)
        }

        // % badge (bottom-right, only when there's an active balance)
        if (!isSettled && fraction > 0f) {
            val pct = (fraction * 100).toInt().coerceAtLeast(1)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 3.dp, vertical = 1.5.dp),
            ) {
                Text(
                    text = "${pct}%",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyGroupsState(onAddGroup: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xxl),
        ) {
            // Icon box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface2)
                    .border(1.dp, Surface4, RoundedCornerShape(24.dp)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.GroupAdd,
                    contentDescription = null,
                    tint = Green400,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No groups yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a group to start splitting\nexpenses with friends",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary CTA
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Green400)
                    .clickable(onClick = onAddGroup)
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Create your first group",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Surface0,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary CTA
            Text(
                text = "Join with invite code",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .clickable(onClick = onAddGroup)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

// ── Add Group Dashed Card ─────────────────────────────────────────────────────

@Composable
private fun AddGroupDashedCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Surface4, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "Add a group",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
    }
}

// ── Add Group Sheet Option ────────────────────────────────────────────────────

@Composable
private fun GroupSheetOption(
    icon    : ImageVector,
    iconBg  : Color,
    title   : String,
    subtitle: String,
    onClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface0)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg.copy(alpha = 0.15f)),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text("›", fontSize = 20.sp, color = TextTertiary)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun groupTypeEmoji(type: GroupType): String = when (type) {
    GroupType.HOME      -> "🏠"
    GroupType.TRIP      -> "✈️"
    GroupType.COUPLE    -> "💑"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👫"
    GroupType.EVENT     -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER     -> "💰"
}