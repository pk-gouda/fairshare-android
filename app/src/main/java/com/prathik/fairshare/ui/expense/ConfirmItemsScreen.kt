package com.prathik.fairshare.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.*
import com.prathik.fairshare.util.MoneyUtils

/**
 * ConfirmItemsScreen — shown after receipt scan, before item assignment.
 *
 * Shows OCR-extracted items before assignment. Read-only in v1 — add/delete/edit
 * items is deferred to a later release. Separates real items from receipt charges.
 *
 * Flow: AddExpense → [Itemize] → ConfirmItems → ItemAssignment → ReviewSubmit
 */
@Composable
fun ConfirmItemsScreen(
    receipt   : Receipt,
    items     : List<ExpenseItem>,
    currency  : String,
    isLoading : Boolean,
    onBack    : () -> Unit,
    onNext    : () -> Unit,   // navigates to ItemAssignment
) {
    val realItems = items.filter { !isReceiptCharge(it.name) }
    val charges   = items.filter {  isReceiptCharge(it.name) }
    val chargesTotal = charges.sumOf { it.totalPrice }
    val itemsTotal   = realItems.sumOf { it.totalPrice }
    val grandTotal   = receipt.totalAmount

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(title = "Review items", onBack = onBack)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Surface0)
                    .padding(Spacing.lg)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (realItems.isEmpty()) {
                    Text(
                        "No assignable items found. You can still save the expense manually.",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                    )
                }
                Button(
                    onClick  = onNext,
                    enabled  = !isLoading && realItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(Radius.lg),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Green400,
                        disabledContainerColor = Surface3,
                    ),
                ) {
                    Text(
                        "Next: Assign items",
                        color      = if (!isLoading && realItems.isNotEmpty()) androidx.compose.ui.graphics.Color.Black else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp,
                    )
                }
            }
        },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green400)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // ── Receipt summary card ──────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(Surface2)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    receipt.merchantName?.let {
                        Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    SummaryRow("Items subtotal", MoneyUtils.format(itemsTotal, currency))
                    if (chargesTotal > 0) {
                        SummaryRow(
                            "Fees / tax / tip",
                            MoneyUtils.format(chargesTotal, currency),
                            tint = TextSecondary,
                        )
                    }
                    val uncategorized = grandTotal - itemsTotal - chargesTotal
                    if (kotlin.math.abs(uncategorized) > 0.01) {
                        SummaryRow(
                            "Receipt adjustment",
                            (if (uncategorized > 0) "+ " else "− ") +
                                    MoneyUtils.format(kotlin.math.abs(uncategorized), currency),
                            tint = TextSecondary,
                        )
                        Text(
                            "Difference from receipt total, split proportionally.",
                            fontSize = 10.sp,
                            color    = TextTertiary,
                        )
                    }
                    HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                    SummaryRow(
                        "Grand total",
                        MoneyUtils.format(grandTotal, currency),
                        bold = true,
                        tint = TextPrimary,
                    )
                }
            }

            // ── Real items header ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Items to assign  (${realItems.size})",
                    fontSize   = 13.sp,
                    color      = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // ── Real items list ───────────────────────────────────────────────
            if (realItems.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Surface2)
                            .padding(Spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No items found", fontSize = 13.sp, color = TextTertiary)
                    }
                }
            } else {
                items(realItems, key = { it.id }) { item ->
                    ConfirmItemRow(item = item, currency = currency)
                }
            }

            // ── Auto-allocated charges ────────────────────────────────────────
            if (charges.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Auto-allocated charges  (${charges.size})",
                        fontSize   = 13.sp,
                        color      = TextTertiary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "These are split proportionally based on each person\u2019s item total.",
                        fontSize = 11.sp,
                        color    = TextTertiary,
                    )
                }
                items(charges, key = { "charge-${it.id}" }) { charge ->
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Surface2)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(charge.name, fontSize = 13.sp, color = TextSecondary)
                        Text(
                            MoneyUtils.format(charge.totalPrice, currency),
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ConfirmItemRow(
    item     : ExpenseItem,
    currency : String,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            if ((item.quantity ?: 1) > 1) {
                Text(
                    "${item.quantity}x \u00d7 ${MoneyUtils.format(item.price, currency)}",
                    fontSize = 12.sp,
                    color    = TextTertiary,
                )
            }
        }
        Text(
            MoneyUtils.format(item.totalPrice, currency),
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
        )
    }
}

@Composable
private fun SummaryRow(
    label : String,
    value : String,
    bold  : Boolean = false,
    tint  : androidx.compose.ui.graphics.Color = TextSecondary,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize   = 14.sp,
            color      = if (bold) TextPrimary else TextSecondary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            value,
            fontSize   = 14.sp,
            color      = tint,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
