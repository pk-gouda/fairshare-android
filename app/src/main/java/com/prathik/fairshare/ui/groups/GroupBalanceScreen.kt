package com.prathik.fairshare.ui.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsErrorScreen
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
fun GroupBalancesScreen(
    onBack       : () -> Unit,
    onSettleWith : (String) -> Unit,
    viewModel    : GroupBalancesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Balances", onBack = onBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is GroupBalancesUiState.Loading ->
                    FsLoadingScreen()

                is GroupBalancesUiState.Error ->
                    FsErrorScreen(message = s.message, onRetry = { viewModel.load() })

                is GroupBalancesUiState.Success -> {
                    if (s.memberNets.isEmpty()) {
                        FsErrorScreen(message = "No balances yet — add some expenses first.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }

                            items(s.memberNets, key = { it.userId }) { member ->
                                MemberBalanceRow(
                                    member       = member,
                                    currentUserId = viewModel.currentUserId,
                                    onSettleWith = onSettleWith,
                                )
                                HorizontalDivider(
                                    color     = Surface3,
                                    thickness = 0.5.dp,
                                    modifier  = Modifier.padding(start = Spacing.lg + ComponentSize.avatarLg + Spacing.md),
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberBalanceRow(
    member        : MemberNet,
    currentUserId : String?,
    onSettleWith  : (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val isMe     = member.userId == currentUserId
    val name     = if (isMe) "You" else member.name
    val netColor = when {
        member.net > 0  -> Green400
        member.net < 0  -> Negative
        else            -> TextTertiary
    }
    // Build per-currency label — Splitwise style
    val positiveNets = member.netByCurrency.filter { it.value > 0 }
    val negativeNets = member.netByCurrency.filter { it.value < 0 }
    val netLabel = when {
        member.netByCurrency.isEmpty() -> "is settled up"
        negativeNets.isEmpty() -> {
            val txt = positiveNets.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(a,c) }
            "gets back $txt in total"
        }
        positiveNets.isEmpty() -> {
            val txt = negativeNets.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(-a,c) }
            "owes $txt in total"
        }
        else -> {
            val oweText  = negativeNets.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(-a,c) }
            val owedText = positiveNets.entries.toList().joinToString(" + ") { (c,a) -> MoneyUtils.format(a,c) }
            "owes $oweText · gets back $owedText"
        }
    }

    Column {
        // ── Main row ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FsAvatar(name = member.name, userId = member.userId, size = ComponentSize.avatarLg)

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = name,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary,
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.full))
                                .background(Green400.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("you", fontSize = 10.sp, color = Green400, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    text     = netLabel,
                    fontSize = 13.sp,
                    color    = netColor,
                )
            }

            Icon(
                imageVector        = if (expanded) Icons.Outlined.KeyboardArrowUp
                else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint               = TextTertiary,
                modifier           = Modifier.size(20.dp),
            )
        }

        // ── Expanded per-person breakdown ─────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(),
            exit    = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg + ComponentSize.avatarLg + Spacing.md, end = Spacing.lg)
                    .padding(bottom = Spacing.sm),
            ) {
                if (member.details.isEmpty()) {
                    Text(
                        text     = "No individual balances",
                        fontSize = 13.sp,
                        color    = TextTertiary,
                        modifier = Modifier.padding(vertical = Spacing.sm),
                    )
                } else {
                    member.details.forEach { balance ->
                        val isOwed   = balance.amount > 0
                        val label    = if (isOwed)
                            "${balance.otherUserName} owes ${if (isMe) "you" else member.name}"
                        else
                            "${if (isMe) "You" else member.name} owe${if (isMe) "" else "s"} ${balance.otherUserName}"
                        val color    = if (isOwed) Green400 else Negative

                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .then(
                                    // Only show settle button on your own rows for people who owe you / you owe
                                    if (isMe) Modifier.clickable { onSettleWith(balance.otherUserId) }
                                    else Modifier
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FsAvatar(
                                name   = balance.otherUserName,
                                userId = balance.otherUserId,
                                size   = 28.dp,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 13.sp, color = TextSecondary)
                                Text(
                                    text       = MoneyUtils.format(kotlin.math.abs(balance.amount), balance.currency),
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = color,
                                )
                            }
                            if (isMe) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.full))
                                        .background(Surface2)
                                        .padding(horizontal = Spacing.sm, vertical = 4.dp),
                                ) {
                                    Text(
                                        text      = "Settle up",
                                        fontSize  = 11.sp,
                                        color     = Green400,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}