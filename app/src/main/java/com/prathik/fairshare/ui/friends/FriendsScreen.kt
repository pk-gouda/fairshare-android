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
import androidx.compose.material3.TextButton
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
import com.prathik.fairshare.ui.components.FsSkeletonBlock
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
import com.prathik.fairshare.domain.model.BalanceCurrencyEntry

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
    val manualRefreshing by viewModel.manualRefreshing.collectAsState()
    val friendsLoaded    by viewModel.friendsLoaded.collectAsState()
    val friendsLoadFailed by viewModel.friendsLoadFailed.collectAsState()
    val owedToYou        by viewModel.owedToYou.collectAsState()
    val youOwe           by viewModel.youOwe.collectAsState()
    val balanceMap               by viewModel.balanceMap.collectAsState()
    val optimisticFriendBalanceMap by viewModel.optimisticFriendBalanceMap.collectAsState()
    val friendsWithPendingSync   by viewModel.friendsWithPendingSync.collectAsState()
    val effectiveSummary         by viewModel.effectiveSummary.collectAsState()
    val balanceEntries           by viewModel.balanceEntries.collectAsState()
    val actionState      by viewModel.actionState.collectAsState()
    val searchQuery      by viewModel.searchQuery.collectAsState()
    val friends          by viewModel.friends.collectAsState()   // collect directly — triggers recomposition instantly
    val snackbarHost     = remember { SnackbarHostState() }
    val sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet        by remember { mutableStateOf(false) }

    // Filter inline so Compose recomposes whenever friends OR searchQuery changes
    val q = searchQuery.trim().lowercase()
    val filteredFriends = friends.filter { it.isActive }.let { list ->
        if (q.isBlank()) list
        else list.filter { it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q) }
    }
    val nonActiveFriends = friends.filter { it.isPlaceholder || it.isInvited }.let { list ->
        if (q.isBlank()) list
        else list.filter { it.fullName.lowercase().contains(q) }
    }

    val effectiveOwedToYou = effectiveSummary?.owedToMe ?: owedToYou
    val effectiveYouOwe    = effectiveSummary?.youOwe   ?: youOwe
    val effectiveEntries   = effectiveSummary?.entries   ?: balanceEntries
    val netBalance         = effectiveOwedToYou - effectiveYouOwe
    // hasAnyBalance considers both confirmed and effective pending data.
    val hasAnyBalance =
        effectiveEntries.isNotEmpty() ||
                balanceMap.isNotEmpty() ||
                optimisticFriendBalanceMap.isNotEmpty()
    val hasMultiCurrency = effectiveEntries.size > 1

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()  // silent background refresh; manual=false
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
                            entries       = effectiveEntries,
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
            isRefreshing = manualRefreshing,
            onRefresh    = { viewModel.refresh(manual = true) },
            modifier     = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isLoading && filteredFriends.isEmpty() && nonActiveFriends.isEmpty()) {
                FriendsHomeSkeleton()
                return@PullToRefreshBox
            }

            val allEmpty = filteredFriends.isEmpty() && nonActiveFriends.isEmpty()

            if (allEmpty && searchQuery.isBlank()) {
                when {
                    // Network failed and no cache — show non-blank error, not empty state
                    friendsLoadFailed -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Couldn't load friends",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    "Check your connection and try again",
                                    fontSize = 14.sp,
                                    color = TextTertiary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { viewModel.loadData() }) {
                                    Text("Retry", color = Green400, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    // Confirmed empty from cache or network
                    friendsLoaded -> EmptyFriendsState(onAddFriend = { showSheet = true })
                    // Still loading — skeleton visible above; do nothing here
                    else -> Unit
                }
            } else {
                // Compute total owed / owed-to-me for ring fractions
                val totalOwedMe = effectiveOwedToYou.takeIf { it > 0 } ?: 1.0
                val totalOwed   = effectiveYouOwe.takeIf { it > 0 } ?: 1.0

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
                        val isPendingFriend = friend.id in friendsWithPendingSync
                        val entries = if (isPendingFriend)
                            optimisticFriendBalanceMap[friend.id] ?: balanceMap[friend.id] ?: emptyList()
                        else
                            balanceMap[friend.id] ?: emptyList()
                        val netAmt = entries.sumOf { it.first }
                        val fraction: Float = when {
                            entries.isEmpty() || netAmt == 0.0 -> 0f
                            netAmt > 0 -> (netAmt / totalOwedMe).toFloat().coerceIn(0f, 1f)
                            else       -> (-netAmt / totalOwed).toFloat().coerceIn(0f, 1f)
                        }
                        FriendCard(
                            friend    = friend,
                            entries   = entries,
                            fraction  = fraction,
                            isPending = isPendingFriend,
                            onClick   = { onNavigateToFriend(friend.id) },
                        )
                    }

                    // ── Pending / Placeholder friends ─────────────────────────
                    items(nonActiveFriends, key = { "pending_${it.id}" }) { friend ->
                        // Use optimistic entries first so pending Placeholder balances
                        // show immediately without requiring a refresh.
                        // optimisticFriendBalanceMap now includes both DIRECT and GROUP
                        // pending expense impacts (group UPDATE excluded until old/new
                        // payer/split context is stored — see Wave2F TODO in FriendsViewModel).
                        val isPendingFriend = friend.id in friendsWithPendingSync
                        val pendingEntries =
                            optimisticFriendBalanceMap[friend.id] ?: balanceMap[friend.id] ?: emptyList()
                        val pendingAmt = pendingEntries.sumOf { it.first }
                        val pendingCur = pendingEntries.maxByOrNull { Math.abs(it.first) }?.second ?: "USD"
                        PendingFriendCard(
                            friend    = friend,
                            balance   = if (pendingEntries.isEmpty()) null else pendingAmt,
                            currency  = pendingCur,
                            isPending = isPendingFriend,
                            onClick   = { onNavigateToFriend(friend.id) },
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
    entries      : List<BalanceCurrencyEntry>,
    hasAnyBalance: Boolean,
) {
    val owedToMe  = entries.sumOf { it.owedToMe }
    val youOwe    = entries.sumOf { it.youOwe }
    val showPill  = hasAnyBalance

    val owedEntries2 = entries.filter { it.net > 0.0 }
    val oweEntries2  = entries.filter { it.net < 0.0 }
    val hasBoth      = owedEntries2.isNotEmpty() && oweEntries2.isNotEmpty()

    val dominantIsOwed = owedToMe >= youOwe
    val netColor  = if (dominantIsOwed) Green400 else Negative
    val pillLabel = if (dominantIsOwed) "Owed to you" else "You owe"

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
            when {
                !showPill -> {
                    Text(MoneyUtils.format(0.0, "USD"), fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("No expenses yet", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
                hasBoth -> {
                    val oweText  = oweEntries2.joinToString(" + ") { MoneyUtils.format(Math.abs(it.net), it.currency) }
                    val owedText = owedEntries2.joinToString(" + ") { MoneyUtils.format(it.net, it.currency) }
                    Text("You owe $oweText", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Negative)
                    Text("Owed to you $owedText", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Green400)
                }
                oweEntries2.isNotEmpty() -> Text(
                    text = oweEntries2.joinToString(" + ") { MoneyUtils.format(Math.abs(it.net), it.currency) },
                    fontSize = if (oweEntries2.size > 1) 18.sp else 24.sp,
                    fontWeight = FontWeight.Bold, color = Negative,
                )
                else -> Text(
                    text = owedEntries2.joinToString(" + ") { MoneyUtils.format(it.net, it.currency) },
                    fontSize = if (owedEntries2.size > 1) 18.sp else 24.sp,
                    fontWeight = FontWeight.Bold, color = Green400,
                )
            }
        }

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
    friend    : Friend,
    entries   : List<Pair<Double, String>>,
    fraction  : Float,
    isPending : Boolean = false,
    onClick   : () -> Unit,
) {
    val positives = entries.filter { it.first > 0 }
    val negatives = entries.filter { it.first < 0 }
    val posTotal  = positives.sumOf { it.first }
    val negTotal  = negatives.sumOf { -it.first }
    val isMixed   = positives.isNotEmpty() && negatives.isNotEmpty()
    val hasEntries = entries.isNotEmpty()
    val isSettled  = hasEntries && positives.isEmpty() && negatives.isEmpty()
    val arcColor   = when {
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
        FriendProgressRing(
            name      = friend.fullName,
            userId    = friend.id,
            fraction  = fraction,
            arcColor  = arcColor,
            isSettled = isSettled,
            isPending = isPending,
            imageUrl  = friend.profilePictureUrl,
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

        // Balance label — Splitwise pattern
        when {
            !hasEntries -> Text("no expenses", fontSize = 12.sp, color = TextTertiary)
            isSettled && isPending -> Column(horizontalAlignment = Alignment.End) {
                Text("settled up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green400)
                Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
            }
            isSettled   -> Text("settled up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green400)
            else -> Column(horizontalAlignment = Alignment.End) {
                when {
                    negatives.isEmpty() -> {
                        Text("owes you", fontSize = 10.sp, color = TextTertiary)
                        Text(positives.joinToString(" + ") { (a,c) -> MoneyUtils.format(a,c) },
                            fontSize = if (positives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Green400, maxLines = 1)
                    }
                    positives.isEmpty() -> {
                        Text("you owe", fontSize = 10.sp, color = TextTertiary)
                        Text(negatives.joinToString(" + ") { (a,c) -> MoneyUtils.format(-a,c) },
                            fontSize = if (negatives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Negative, maxLines = 1)
                    }
                    // Mixed — dominant + asterisk (sub-detail visible in friend detail screen)
                    negTotal >= posTotal -> {
                        Text("you owe", fontSize = 10.sp, color = TextTertiary)
                        Text(negatives.joinToString(" + ") { (a,c) -> MoneyUtils.format(-a,c) } + "*",
                            fontSize = if (negatives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Negative, maxLines = 1)
                    }
                    else -> {
                        Text("owes you", fontSize = 10.sp, color = TextTertiary)
                        Text(positives.joinToString(" + ") { (a,c) -> MoneyUtils.format(a,c) } + "*",
                            fontSize = if (positives.size > 1) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold, color = Green400, maxLines = 1)
                    }
                }
                if (isPending) {
                    Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
                }
            }
        }
    }
}

// ── Pending / Placeholder Card ────────────────────────────────────────────────

@Composable
private fun PendingFriendCard(friend: Friend, balance: Double?, currency: String = "USD", isPending: Boolean = false, onClick: () -> Unit) {
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
            isPending = isPending || friend.isInvited,
            imageUrl  = friend.profilePictureUrl,
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
            isPending && (balance == null || balance == 0.0) -> Column(horizontalAlignment = Alignment.End) {
                Text("no expenses", fontSize = 12.sp, color = TextTertiary)
                Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
            }
            balance == null || balance == 0.0 ->
                Text("no expenses", fontSize = 12.sp, color = TextTertiary)
            balance > 0 -> Column(horizontalAlignment = Alignment.End) {
                Text("owes you",                         fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(balance, currency),  fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green400)
                if (isPending) Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
            }
            else -> Column(horizontalAlignment = Alignment.End) {
                Text("you owe",                          fontSize = 10.sp, color = TextTertiary)
                Text(MoneyUtils.format(-balance, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Negative)
                if (isPending) Text("Pending sync", fontSize = 9.sp, color = Color(0xFF9AA3AF))
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
    imageUrl : String? = null,
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

        // Center avatar — show photo if available, otherwise initials
        if (!imageUrl.isNullOrBlank()) {
            var imageLoadFailed by remember(imageUrl) { mutableStateOf(false) }
            if (!imageLoadFailed) {
                coil.compose.AsyncImage(
                    model              = imageUrl,
                    contentDescription = name,
                    contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                    onError            = { imageLoadFailed = true },
                    modifier           = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(avatarBg),
                ) {
                    Text(initial, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Surface0)
                }
            }
        } else {
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
// ── FriendsHome skeleton placeholder ─────────────────────────────────────────

@Composable
private fun FriendsHomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Net balance bar placeholder
        FsSkeletonBlock(
            height = 52.dp,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 10.dp,
        )
        // Friend card placeholders
        repeat(5) { FriendCardSkeleton() }
    }
}

@Composable
private fun FriendCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color(0xFF1A1A1C))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsSkeletonBlock(height = 40.dp, widthFraction = 0.12f, cornerRadius = 20.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FsSkeletonBlock(height = 14.dp, widthFraction = 0.5f)
            FsSkeletonBlock(height = 11.dp, widthFraction = 0.3f)
        }
        FsSkeletonBlock(height = 13.dp, widthFraction = 0.2f)
    }
}