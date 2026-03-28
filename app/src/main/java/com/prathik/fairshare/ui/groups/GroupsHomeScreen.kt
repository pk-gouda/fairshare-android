package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsSecondaryButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TileCoupleEnd
import com.prathik.fairshare.ui.theme.TileCoupleStart
import com.prathik.fairshare.ui.theme.TileEventEnd
import com.prathik.fairshare.ui.theme.TileEventStart
import com.prathik.fairshare.ui.theme.TileFriendsEnd
import com.prathik.fairshare.ui.theme.TileFriendsStart
import com.prathik.fairshare.ui.theme.TileHomeEnd
import com.prathik.fairshare.ui.theme.TileHomeStart
import com.prathik.fairshare.ui.theme.TileOfficeEnd
import com.prathik.fairshare.ui.theme.TileOfficeStart
import com.prathik.fairshare.ui.theme.TileOtherEnd
import com.prathik.fairshare.ui.theme.TileOtherStart
import com.prathik.fairshare.ui.theme.TileTripEnd
import com.prathik.fairshare.ui.theme.TileTripStart
import com.prathik.fairshare.util.MoneyUtils

/**
 * Groups Home Screen — the main screen after login.
 *
 * Shows:
 * - Net balance hero section (how much you're owed overall)
 * - LazyVerticalGrid of group tiles with gradients by GroupType
 * - "Create a group" button at bottom of grid (always visible)
 * - FAB → Add Expense (quick add)
 * - Top bar GroupAdd icon → Create Group
 * - Empty state CTA → Create Group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    onNavigateToGroup       : (String) -> Unit,
    onNavigateToSearch      : () -> Unit,
    onNavigateToCreateGroup : () -> Unit,
    onNavigateToAddExpense  : () -> Unit,
    viewModel               : GroupsViewModel = hiltViewModel(),
) {
    val groupsState    by viewModel.groupsState.collectAsState()
    val balanceSummary by viewModel.balanceSummary.collectAsState()
    val isLoading = groupsState is GroupsUiState.Loading

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title   = "", // No title on Groups Home — just icons
                actions = {
                    FsIconButton(
                        icon               = Icons.Outlined.Search,
                        contentDescription = "Search",
                        onClick            = onNavigateToSearch,
                    )
                    FsIconButton(
                        icon               = Icons.Outlined.GroupAdd,
                        contentDescription = "Create group",
                        onClick            = onNavigateToCreateGroup,
                    )
                }
            )
        },
        floatingActionButton = {
            // FAB → Add Expense (quick action)
            FloatingActionButton(
                onClick        = onNavigateToAddExpense,
                containerColor = Green400,
                contentColor   = Surface0,
                shape          = RoundedCornerShape(Radius.full),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    modifier           = Modifier.size(24.dp),
                )
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
            when (val state = groupsState) {
                is GroupsUiState.Loading -> FsLoadingScreen()

                is GroupsUiState.Error -> FsErrorScreen(
                    message   = state.message,
                    isNetwork = state.isNetwork,
                    onRetry   = { viewModel.loadData() },
                )

                is GroupsUiState.Success -> {
                    if (state.groups.isEmpty()) {
                        FsEmptyState(
                            title    = "No groups yet",
                            subtitle = "Create a group to start splitting expenses with friends",
                            ctaText  = "Create a group",
                            onCta    = onNavigateToCreateGroup,
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {

                            // ── Balance hero ──────────────────────────────────
                            balanceSummary?.let { summary ->
                                BalanceHeroSection(
                                    summary  = summary,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical   = Spacing.lg,
                                    ),
                                )
                            }

                            // ── Group tiles grid ──────────────────────────────
                            LazyVerticalGrid(
                                columns               = GridCells.Fixed(2),
                                contentPadding        = PaddingValues(Spacing.lg),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalArrangement   = Arrangement.spacedBy(Spacing.md),
                            ) {
                                items(
                                    items = state.groups,
                                    key   = { it.id },
                                ) { group ->
                                    GroupTile(
                                        group   = group,
                                        onClick = { onNavigateToGroup(group.id) },
                                    )
                                }

                                // "Create a group" button — spans both columns
                                item(span = { GridItemSpan(2) }) {
                                    FsSecondaryButton(
                                        text     = "+ Create a group",
                                        onClick  = onNavigateToCreateGroup,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.sm),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Balance Hero Section ──────────────────────────────────────────────────────

@Composable
private fun BalanceHeroSection(
    summary : BalanceSummary,
    modifier: Modifier = Modifier,
) {
    val netColor = when {
        summary.netBalance > 0 -> Green400
        summary.netBalance < 0 -> Negative
        else                   -> TextSecondary
    }

    val netText = when {
        summary.netBalance > 0 -> "+${MoneyUtils.format(summary.netBalance, summary.currency)}"
        summary.netBalance < 0 -> "-${MoneyUtils.format(Math.abs(summary.netBalance), summary.currency)}"
        else                   -> MoneyUtils.format(0.0, summary.currency)
    }

    Column(modifier = modifier.fillMaxWidth()) {
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

        // Stats pills
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatPill(
                label  = "Owed to you",
                amount = MoneyUtils.format(summary.owedToMe, summary.currency),
                color  = Green400,
            )
            StatPill(
                label  = "You owe",
                amount = MoneyUtils.format(summary.youOwe, summary.currency),
                color  = Negative,
            )
        }
    }
}

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
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = amount, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Group Tile ────────────────────────────────────────────────────────────────

@Composable
private fun GroupTile(
    group  : Group,
    onClick: () -> Unit,
) {
    val gradient = groupTypeGradient(group.type)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(Radius.xl))
            .background(Brush.verticalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text     = groupTypeEmoji(group.type),
                fontSize = 24.sp,
            )

            Column {
                Text(
                    text       = group.name,
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = "${group.memberCount} members",
                    fontSize = 11.sp,
                    color    = TextSecondary,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun groupTypeGradient(type: GroupType): List<Color> = when (type) {
    GroupType.HOME      -> listOf(TileHomeStart, TileHomeEnd)
    GroupType.TRIP      -> listOf(TileTripStart, TileTripEnd)
    GroupType.COUPLE    -> listOf(TileCoupleStart, TileCoupleEnd)
    GroupType.OFFICE    -> listOf(TileOfficeStart, TileOfficeEnd)
    GroupType.FRIENDS   -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.EVENT     -> listOf(TileEventStart, TileEventEnd)
    GroupType.APARTMENT -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.OTHER     -> listOf(TileOtherStart, TileOtherEnd)
}

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