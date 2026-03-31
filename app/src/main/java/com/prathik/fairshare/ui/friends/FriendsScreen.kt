package com.prathik.fairshare.ui.friends

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateToAddFriendByEmail: () -> Unit,
    onNavigateToScanQr          : () -> Unit,
    onNavigateToImport          : () -> Unit,
    onNavigateToRequests        : () -> Unit,
    onNavigateToFriend          : (String) -> Unit,
    viewModel                   : FriendsViewModel = hiltViewModel(),
) {
    val isLoading       by viewModel.isLoading.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val owedToYou       by viewModel.owedToYou.collectAsState()
    val youOwe          by viewModel.youOwe.collectAsState()
    val balanceMap      by viewModel.balanceMap.collectAsState()
    val actionState     by viewModel.actionState.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val snackbarHost    = remember { SnackbarHostState() }
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet       by remember { mutableStateOf(false) }

    val netBalance = owedToYou - youOwe
    val netColor   = when {
        netBalance > 0 -> Green400
        netBalance < 0 -> Negative
        else           -> TextSecondary
    }
    val netText = when {
        netBalance > 0 -> "+${MoneyUtils.format(netBalance, "USD")}"
        netBalance < 0 -> "-${MoneyUtils.format(-netBalance, "USD")}"
        else           -> MoneyUtils.format(0.0, "USD")
    }

    val filteredFriends by remember { derivedStateOf { viewModel.filteredFriends() } }
    val hasPending = pendingRequests.isNotEmpty()

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is FriendsActionState.Success -> { snackbarHost.showSnackbar(state.message); viewModel.resetActionState() }
            is FriendsActionState.Error   -> { snackbarHost.showSnackbar(state.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            Surface(color = Surface0, shadowElevation = 0.dp) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Row(
                        modifier          = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .padding(horizontal = Spacing.md, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Search,
                            contentDescription = null,
                            tint               = TextSecondary,
                            modifier           = Modifier.size(16.dp),
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
                    FsIconButton(
                        icon               = Icons.Outlined.PersonAdd,
                        contentDescription = "Add friend",
                        onClick            = { showSheet = true },
                    )
                }
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
            if (isLoading && filteredFriends.isEmpty()) {
                FsLoadingScreen()
                return@PullToRefreshBox
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Net balance hero — same position/style as GroupsHomeScreen ─
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                    ) {
                        Text(
                            text          = "NET BALANCE",
                            fontSize      = 11.sp,
                            color         = TextSecondary,
                            fontWeight    = FontWeight.Medium,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text       = netText,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 42.sp,
                            color      = netColor,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            StatPill(label = "Owed to you", amount = MoneyUtils.format(owedToYou, "USD"), color = Green400)
                            StatPill(label = "You owe",     amount = MoneyUtils.format(youOwe, "USD"),    color = Negative)
                        }
                    }
                }

                // ── Pending requests banner — only if requests exist ───────────
                if (hasPending) {
                    item {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg)
                                .padding(bottom = Spacing.md)
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Color(0xFF0D2A0D))
                                .clickable { onNavigateToRequests() }
                                .padding(horizontal = Spacing.md, vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A3A1A)),
                            ) {
                                Text(text = "👤", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = "${pendingRequests.size} pending request${if (pendingRequests.size > 1) "s" else ""}",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Green400,
                                )
                                Text(text = "Tap to review", fontSize = 11.sp, color = TextTertiary)
                            }
                            Text(text = "›", fontSize = 18.sp, color = TextTertiary)
                        }
                    }
                }

                // ── Friends list ──────────────────────────────────────────────
                if (filteredFriends.isEmpty()) {
                    item {
                        FsEmptyState(
                            title    = "No friends yet",
                            subtitle = "Add friends to split expenses together",
                            modifier = Modifier.height(200.dp),
                        )
                    }
                } else {
                    items(
                        items = filteredFriends,
                        key   = { it.id },
                    ) { friend ->
                        FriendRow(
                            friend  = friend,
                            balance = balanceMap[friend.id] ?: 0.0,
                            onClick = { onNavigateToFriend(friend.id) },
                        )
                        HorizontalDivider(
                            color     = Surface3,
                            thickness = 0.5.dp,
                            modifier  = Modifier.padding(start = Spacing.lg + 56.dp),
                        )
                    }
                }

                // ── Add a new friend button — bottom of list ──────────────────
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .fillMaxWidth()
                            .clickable { showSheet = true }
                            .padding(vertical = Spacing.lg),
                    ) {
                        Text(
                            text       = "+ Add a new friend",
                            fontSize   = 14.sp,
                            color      = Green400,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ── Add Friend Bottom Sheet ───────────────────────────────────────────────
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
                Text(
                    text       = "Add a friend",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(bottom = Spacing.lg),
                )
                SheetOption(
                    icon     = Icons.Outlined.PersonAdd,
                    iconBg   = Color(0xFF1A3A1A),
                    iconTint = Green400,
                    title    = "Add / invite a friend",
                    subtitle = "By email or phone number",
                    onClick  = { showSheet = false; onNavigateToAddFriendByEmail() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                SheetOption(
                    icon     = Icons.Outlined.QrCodeScanner,
                    iconBg   = Color(0xFF1A1A3A),
                    iconTint = Color(0xFF7F77DD),
                    title    = "Scan QR code",
                    subtitle = "Scan or share your code",
                    onClick  = { showSheet = false; onNavigateToScanQr() },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                SheetOption(
                    icon     = Icons.Outlined.Upload,
                    iconBg   = Color(0xFF2A1A0A),
                    iconTint = Color(0xFFF0A500),
                    title    = "Import from Splitwise",
                    subtitle = "Upload CSV to find friends",
                    onClick  = { showSheet = false; onNavigateToImport() },
                )
            }
        }
    }
}

// ── Friend Row ────────────────────────────────────────────────────────────────

@Composable
private fun FriendRow(
    friend : Friend,
    balance: Double,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsAvatar(
            name   = friend.fullName,
            userId = friend.id,
            size   = ComponentSize.avatarLg,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = friend.fullName,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
            )
            Text(
                text     = friend.email,
                fontSize = 12.sp,
                color    = TextTertiary,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        when {
            balance > 0 -> Column(horizontalAlignment = Alignment.End) {
                Text(text = "owes you",                         fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Green400)
                Text(text = MoneyUtils.format(balance, "USD"),  fontSize = 14.sp, fontWeight = FontWeight.Bold,   color = Green400)
            }
            balance < 0 -> Column(horizontalAlignment = Alignment.End) {
                Text(text = "you owe",                          fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Negative)
                Text(text = MoneyUtils.format(-balance, "USD"), fontSize = 14.sp, fontWeight = FontWeight.Bold,   color = Negative)
            }
            else -> Text(text = "settled", fontSize = 13.sp, color = TextTertiary)
        }
    }
}

// ── Stat Pill — identical to GroupsHomeScreen ─────────────────────────────────

@Composable
private fun StatPill(
    label : String,
    amount: String,
    color : Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(text = label,  fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = amount, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
// ── Sheet Option ──────────────────────────────────────────────────────────────

@Composable
private fun SheetOption(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    iconBg  : Color,
    iconTint: Color,
    title   : String,
    subtitle: String,
    onClick : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface0)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(iconBg),
        ) {
            androidx.compose.material3.Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title,    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}