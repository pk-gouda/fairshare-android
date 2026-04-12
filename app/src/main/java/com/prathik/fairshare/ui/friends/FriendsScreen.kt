package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.theme.AvatarColors
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils

private val Gold = Color(0xFFF59E0B)

/**
 * Friends Home Screen — redesigned to match mockups.
 *
 * Five states:
 *  1. Empty — no friends at all (centered CTA)
 *  2. Pending — friend invited but not yet active (dashed card)
 *  3. You Owe — active friend, red ring, red net bar
 *  4. Owed To You — active friend, green ring, green net bar
 *  5. Settled — full gold ring, gold checkmark, "All settled 🎉" net bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateToAddFriendByEmail: () -> Unit,
    onNavigateToScanQr          : () -> Unit,
    onNavigateToImport          : () -> Unit,
    onNavigateToFriend          : (String) -> Unit,
    viewModel                   : FriendsViewModel = hiltViewModel(),
) {
    val isLoading        by viewModel.isLoading.collectAsState()
    val owedToYou        by viewModel.owedToYou.collectAsState()
    val youOwe           by viewModel.youOwe.collectAsState()
    val balanceMap       by viewModel.balanceMap.collectAsState()
    val actionState      by viewModel.actionState.collectAsState()
    val searchQuery      by viewModel.searchQuery.collectAsState()
    val snackbarHost     = remember { SnackbarHostState() }
    val sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet        by remember { mutableStateOf(false) }

    val filteredFriends    by remember { derivedStateOf { viewModel.filteredActiveFriends() } }
    val nonActiveFriends   by remember { derivedStateOf { viewModel.filteredNonActiveFriends() } }

    val netBalance = owedToYou - youOwe
    val hasAnyBalance = balanceMap.isNotEmpty()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FriendsActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is FriendsActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    // ── Add Friend bottom sheet ───────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            containerColor   = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxxl),
            ) {
                Text("Add a friend", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Choose how you want to connect", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(Spacing.lg))

                FriendSheetOption(
                    icon     = Icons.Outlined.PersonAdd,
                    iconBg   = Green400,
                    title    = "Add / invite a friend",
                    subtitle = "By email or phone number",
                    onClick  = { showSheet = false; onNavigateToAddFriendByEmail() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                FriendSheetOption(
                    icon     = Icons.Outlined.QrCodeScanner,
                    iconBg   = Color(0xFF4A6FE8),
                    title    = "Scan QR code",
                    subtitle = "Scan or share your code",
                    onClick  = { showSheet = false; onNavigateToScanQr() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                FriendSheetOption(
                    icon     = Icons.Outlined.Upload,
                    iconBg   = Color(0xFFE8A84F),
                    title    = "Import from Splitwise",
                    subtitle = "Upload CSV to find friends",
                    onClick  = { showSheet = false; onNavigateToImport() },
                )
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
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
                                value         = searchQuery,
                                onValueChange = { viewModel.onSearchChanged(it) },
                                singleLine    = true,
                                textStyle     = TextStyle(fontSize = 14.sp, color = TextPrimary),
                                cursorBrush   = SolidColor(Green400),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search friends...", fontSize = 14.sp, color = TextSecondary)
                                    }
                                    inner()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Add friend icon button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Surface2)
                                .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                                .clickable { showSheet = true },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add friend",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // ── Net balance bar (only when friends exist) ─────────────
                    val hasFriends = filteredFriends.isNotEmpty() || nonActiveFriends.isNotEmpty()
                    if (hasFriends) {
                        FriendsNetBalanceBar(
                            netBalance    = netBalance,
                            hasAnyBalance = hasAnyBalance,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
                containerColor = Green400,
                contentColor   = Surface0,
                shape          = RoundedCornerShape(Radius.xl),
                modifier       = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add friend", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh    = { viewModel.loadData() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isLoading && filteredFriends.isEmpty() && nonActiveFriends.isEmpty()) {
                FsLoadingScreen()
                return@PullToRefreshBox
            }

            val allEmpty = filteredFriends.isEmpty() && nonActiveFriends.isEmpty()

            if (allEmpty && searchQuery.isBlank()) {
                // ── State 1: Empty ────────────────────────────────────────────
                EmptyFriendsState(onAddFriend = { showSheet = true })
            } else {
                // Compute total owed / owed-to-me for ring fractions
                val totalOwedMe = owedToYou.takeIf { it > 0 } ?: 1.0
                val totalOwed   = youOwe.takeIf { it > 0 } ?: 1.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start  = Spacing.lg,
                        end    = Spacing.lg,
                        top    = Spacing.md,
                        bottom = 100.dp,
                    ),
                ) {
                    // ── Active friends ────────────────────────────────────────
                    items(filteredFriends, key = { it.id }) { friend ->
                        val balance  = balanceMap[friend.id]
                        val fraction: Float = when {
                            balance == null || balance == 0.0 -> 0f
                            balance > 0 -> (balance / totalOwedMe).toFloat().coerceIn(0f, 1f)
                            else        -> (-balance / totalOwed).toFloat().coerceIn(0f, 1f)
                        }
                        FriendCard(
                            friend   = friend,
                            balance  = balance,
                            fraction = fraction,
                            onClick  = { onNavigateToFriend(friend.id) },
                        )
                    }

                    // ── Pending / Placeholder friends ─────────────────────────
                    items(nonActiveFriends, key = { "pending_${it.id}" }) { friend ->
                        PendingFriendCard(
                            friend  = friend,
                            balance = balanceMap[friend.id],
                            onClick = { onNavigateToFriend(friend.id) },
                        )
                    }

                    // ── Add another friend dashed card ────────────────────────
                    item {
                        AddFriendDashedCard(onClick = { showSheet = true })
                    }
                }
            }
        }
    }
}

// ── Net Balance Bar ───────────────────────────────────────────────────────────

@Composable
private fun FriendsNetBalanceBar(
    netBalance   : Double,
    hasAnyBalance: Boolean,   // true if any UserBalance entries exist (i.e. expenses happened)
) {
    val showPill = netBalance != 0.0 || hasAnyBalance

    val netColor = when {
        netBalance > 0 -> Green400
        netBalance < 0 -> Negative
        else           -> Green400   // settled → still show green
    }
    val pillLabel = when {
        netBalance > 0 -> "Owed to you"
        netBalance < 0 -> "You owe"
        else           -> "All settled 🎉"
    }
    val displayAmount = MoneyUtils.format(Math.abs(netBalance), "USD")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface0)
            .border(0.5.dp, Surface4, RoundedCornerShape(0.dp))
            .padding(horizontal = Spacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text          = "NET BALANCE",
                fontSize      = 10.sp,
                color         = TextTertiary,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
            if (showPill) {
                // Active or all-settled: show colored amount
                Text(
                    text       = displayAmount,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = netColor,
                    lineHeight = 28.sp,
                )
            } else {
                // No expenses at all: gray $0.00 + "No expenses yet"
                Text(
                    text       = MoneyUtils.format(0.0, "USD"),
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextSecondary,
                    lineHeight = 28.sp,
                )
                Text(
                    text       = "No expenses yet",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextSecondary,
                )
            }
        }

        // Pill badge — only when active/settled
        if (showPill) {
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

// ── Friend Card (active friends) ──────────────────────────────────────────────

@Composable
private fun FriendCard(
    friend  : Friend,
    balance : Double?,   // null=no expenses, 0.0=settled, +/-=active
    fraction: Float,     // 0..1 ring fill fraction
    onClick : () -> Unit,
) {
    val isSettled = balance != null && balance == 0.0
    val arcColor = when {
        isSettled           -> Gold
        balance != null && balance > 0 -> Green400
        balance != null && balance < 0 -> Negative
        else                -> Color.Transparent
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
        FriendProgressRing(
            name      = friend.fullName,
            userId    = friend.id,
            fraction  = fraction,
            arcColor  = arcColor,
            isSettled = isSettled,
            isPending = false,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = friend.fullName,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = friend.email,
                fontSize = 12.sp,
                color    = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Balance label
        when {
            balance == null -> Text("no expenses", fontSize = 12.sp, color = TextTertiary)
            isSettled       -> Text(
                text       = "settled up",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Green400,
            )
            balance > 0 -> Column(horizontalAlignment = Alignment.End) {
                Text("owes you",                           fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(balance, "USD"),   fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green400)
            }
            else -> Column(horizontalAlignment = Alignment.End) {
                Text("you owe",                           fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(-balance, "USD"),  fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Negative)
            }
        }
    }
}

// ── Pending / Placeholder Card ────────────────────────────────────────────────

@Composable
private fun PendingFriendCard(friend: Friend, balance: Double?, onClick: () -> Unit) {
    val statusText = when {
        friend.isPlaceholder -> "Placeholder • Claim to link"
        friend.isInvited     -> "Invited • Waiting to accept"
        else                 -> "Pending"
    }
    // Placeholders can have balances (e.g. imported Splitwise expenses)
    val arcColor = when {
        balance != null && balance > 0 -> Green400
        balance != null && balance < 0 -> Negative
        else                           -> Color.Transparent
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
        FriendProgressRing(
            name      = friend.fullName,
            userId    = friend.id,
            fraction  = 0f,       // no arc fill — status badge is the signal
            arcColor  = arcColor,
            isSettled = false,
            isPending = friend.isInvited,   // placeholders don't get the PENDING badge
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = friend.fullName,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = statusText,
                fontSize = 12.sp,
                color    = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            balance == null || balance == 0.0 ->
                Text("no expenses", fontSize = 12.sp, color = TextTertiary)
            balance > 0 -> Column(horizontalAlignment = Alignment.End) {
                Text("owes you",                         fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(balance, "USD"),  fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green400)
            }
            else -> Column(horizontalAlignment = Alignment.End) {
                Text("you owe",                          fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(-balance, "USD"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Negative)
            }
        }
    }
}

// ── Progress Ring ─────────────────────────────────────────────────────────────

@Composable
private fun FriendProgressRing(
    name     : String,
    userId   : String,
    fraction : Float,
    arcColor : Color,
    isSettled: Boolean,
    isPending: Boolean,
) {
    // Avatar background color from userId hash — same treatment for all states
    val avatarBg = if (isSettled) {
        Gold
    } else {
        arcColor.takeIf { it != Color.Transparent }
            ?: AvatarColors[Math.abs(userId.hashCode()) % AvatarColors.size]
    }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(modifier = Modifier.size(52.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val inset   = strokeWidth / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val style   = Stroke(width = strokeWidth, cap = StrokeCap.Round)

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
            val sweep = when {
                isSettled -> 360f
                else      -> (fraction * 360f).coerceAtLeast(0f)
            }
            if (sweep > 0f) {
                drawArc(
                    color      = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = if (isSettled) Stroke(width = strokeWidth) else style,
                )
            }
        }

        // Center avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .background(avatarBg),
        ) {
            if (isSettled) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Surface0,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text       = initial,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Surface0,
                )
            }
        }

        // Badge at bottom-right
        val showPctBadge    = !isSettled && !isPending && fraction > 0f
        val showPendingBadge = isPending

        if (showPctBadge) {
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
                Text("${pct}%", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (showPendingBadge) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Surface4)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text("PENDING", fontSize = 5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyFriendsState(onAddFriend: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xxl),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface2)
                    .border(1.dp, Surface4, RoundedCornerShape(24.dp)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    tint = Green400,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("No friends yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "Add friends to start splitting\nexpenses 1-on-1",
                fontSize  = 14.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Green400)
                    .clickable(onClick = onAddFriend)
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text(
                    text       = "Add your first friend",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Surface0,
                )
            }
        }
    }
}

// ── Add Friend Dashed Card ────────────────────────────────────────────────────

@Composable
private fun AddFriendDashedCard(onClick: () -> Unit) {
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
        Icon(Icons.Filled.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text("Add another friend", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
    }
}

// ── Sheet Option ──────────────────────────────────────────────────────────────

@Composable
private fun FriendSheetOption(
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
            Text(text = title,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text("›", fontSize = 20.sp, color = TextTertiary)
    }
}