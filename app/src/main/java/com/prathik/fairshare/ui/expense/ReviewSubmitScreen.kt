package com.prathik.fairshare.ui.expense

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.ui.components.FsTopBar
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

/**
 * ReviewSubmitScreen — shown after item assignment, before expense is saved.
 *
 * Shows:
 * - Receipt breakdown (subtotal, tax, fees, discounts, grand total)
 * - Per-person item totals based on assignments
 * - Submit expense button
 */
@Composable
fun ReviewSubmitScreen(
    receipt      : Receipt,
    items        : List<ExpenseItem>,
    assignments  : Map<String, List<String>>,   // itemId → userIds
    members      : List<GroupMember>,
    currency     : String,
    isLoading    : Boolean,
    onBack       : () -> Unit,
    onSubmit     : () -> Unit,
) {
    // ── Per-person totals ─────────────────────────────────────────────────────
    // For each member: sum items assigned to them.
    // Items with no assignment → split equally among all members.
    val perPersonTotals: Map<GroupMember, Double> = buildPerPersonTotals(
        items, assignments, members
    )

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(title = "Review & Submit", onBack = onBack)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Surface0)
                    .padding(Spacing.lg)
                    .padding(bottom = Spacing.lg),
            ) {
                Button(
                    onClick  = onSubmit,
                    enabled  = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(Radius.lg),
                    colors = ButtonDefaults.buttonColors(containerColor = Green400),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Submit expense", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // ── Receipt breakdown card ────────────────────────────────────────
            item {
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(Surface2)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    receipt.subtotal?.let { subtotal ->
                        ReceiptRow("Subtotal", MoneyUtils.format(subtotal, currency))
                    }
                    receipt.taxAmount?.let { tax ->
                        if (tax > 0) ReceiptRow("Sales Tax", "+ ${MoneyUtils.format(tax, currency)}", tint = TextSecondary)
                    }
                    receipt.tipAmount?.let { tip ->
                        if (tip > 0) ReceiptRow("Service Fee / Tip", "+ ${MoneyUtils.format(tip, currency)}", tint = TextSecondary)
                    }
                    // Check if total > subtotal + tax + tip → there may be fees embedded
                    val knownSum = (receipt.subtotal ?: 0.0) + (receipt.taxAmount ?: 0.0) + (receipt.tipAmount ?: 0.0)
                    val diff = receipt.totalAmount - knownSum
                    if (diff > 0.01) {
                        ReceiptRow("Other fees", "+ ${MoneyUtils.format(diff, currency)}", tint = TextSecondary)
                    } else if (diff < -0.01) {
                        ReceiptRow("Discount", "- ${MoneyUtils.format(-diff, currency)}", tint = Green400)
                    }

                    HorizontalDivider(color = Surface4, thickness = 0.5.dp)

                    ReceiptRow(
                        label  = "Grand total",
                        value  = MoneyUtils.format(receipt.totalAmount, currency),
                        bold   = true,
                        tint   = TextPrimary,
                    )
                }
            }

            // ── Per person totals ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text       = "Per person totals",
                    fontSize   = 13.sp,
                    color      = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            items(members) { member ->
                val amount    = perPersonTotals[member] ?: 0.0
                val itemCount = countItemsForMember(member.userId, items, assignments, members.size)
                val pct       = if (receipt.totalAmount > 0) (amount / receipt.totalAmount * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Surface2)
                        .padding(Spacing.md),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarColor(member.fullName)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = member.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                            )
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text(member.fullName, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("$itemCount items · $pct% of total", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                    Text(
                        text       = MoneyUtils.format(amount, currency),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (amount > 0) TextPrimary else TextTertiary,
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ReceiptRow(
    label : String,
    value : String,
    bold  : Boolean = false,
    tint  : Color = TextSecondary,
) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = if (bold) TextPrimary else TextSecondary, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, color = tint, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun buildPerPersonTotals(
    items      : List<ExpenseItem>,
    assignments: Map<String, List<String>>,
    members    : List<GroupMember>,
): Map<GroupMember, Double> {
    val totals = members.associateWith { 0.0 }.toMutableMap()
    val memberIds = members.map { it.userId }

    for (item in items) {
        val assigned = assignments[item.id]?.filter { it in memberIds } ?: emptyList()
        val recipients = assigned.ifEmpty { memberIds }
        val share = item.totalPrice / recipients.size
        for (memberId in recipients) {
            val member = members.find { it.userId == memberId } ?: continue
            totals[member] = (totals[member] ?: 0.0) + share
        }
    }
    return totals
}

private fun countItemsForMember(
    userId     : String,
    items      : List<ExpenseItem>,
    assignments: Map<String, List<String>>,
    memberCount: Int,
): Int {
    var count = 0
    for (item in items) {
        val assigned = assignments[item.id] ?: emptyList()
        if (assigned.isEmpty() || userId in assigned) count++
    }
    return count
}

private val avatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043),
    Color(0xFF66BB6A), Color(0xFFEC407A),
)

private fun avatarColor(name: String): Color =
    avatarColors[Math.abs(name.hashCode()) % avatarColors.size]