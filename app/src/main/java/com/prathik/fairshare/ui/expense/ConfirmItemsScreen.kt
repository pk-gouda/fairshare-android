package com.prathik.fairshare.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.*
import com.prathik.fairshare.util.MoneyUtils
import kotlin.math.abs

/**
 * ConfirmItemsScreen — Step 1 of 3 in the itemised receipt flow.
 *
 * Read-only receipt review. The user confirms what was detected before
 * proceeding to manual item assignment.
 *
 * Flow: AddExpense → ConfirmItems → ItemAssignment → ReviewSubmit
 *
 * Add/edit/delete item support is deferred to a later release.
 */
@Composable
fun ConfirmItemsScreen(
    receipt   : Receipt,
    items     : List<ExpenseItem>,
    currency  : String,
    isLoading : Boolean,
    onBack    : () -> Unit,
    onNext    : () -> Unit,
) {
    val realItems    = items.filter { !isReceiptCharge(it.name) }
    val charges      = items.filter {  isReceiptCharge(it.name) }
    val itemsTotal   = realItems.sumOf { it.totalPrice }
    val grandTotal   = receipt.totalAmount
    val autoSplitTotal = grandTotal - itemsTotal   // fees + tax + any adjustment
    val adjustment   = autoSplitTotal - charges.sumOf { it.totalPrice }

    Scaffold(
        containerColor = Surface0,
        topBar = { FsTopBar(title = "Confirm Items", onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Surface0)
                    .padding(horizontal = Spacing.lg)
                    .padding(top = Spacing.sm, bottom = Spacing.xxl),
            ) {
                Button(
                    onClick  = onNext,
                    enabled  = !isLoading && realItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.buttonHeight),
                    shape  = RoundedCornerShape(Radius.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = Green400,
                        disabledContainerColor = Surface4,
                    ),
                ) {
                    Text(
                        text       = if (realItems.isEmpty()) "No items to assign" else "Next",
                        color      = if (!isLoading && realItems.isNotEmpty()) Color.Black else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp,
                    )
                }
            }
        },
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green400, strokeWidth = 2.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {

            // ── Heading ───────────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text       = "Review detected items",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SyneFontFamily,
                        color      = TextPrimary,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text     = "Fees and taxes will be split proportionally after you assign the items.",
                        fontSize = 13.sp,
                        color    = TextSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }

            // ── Receipt summary: thumbnail + breakdown ────────────────────────
            item {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment   = Alignment.Top,
                ) {
                    // Thumbnail / placeholder
                    ReceiptThumbnail(
                        imageUrl = receipt.imageUrl,
                        modifier = Modifier
                            .width(80.dp)
                            .height(100.dp),
                    )

                    // Breakdown
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        receipt.merchantName?.let { merchant ->
                            Text(
                                text       = merchant,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis,
                            )
                        }
                        BreakdownRow(
                            label  = "Items subtotal",
                            amount = MoneyUtils.format(itemsTotal, currency),
                            color  = TextSecondary,
                        )
                        if (autoSplitTotal > 0.001) {
                            BreakdownRow(
                                label  = "Auto-split charges",
                                amount = MoneyUtils.format(autoSplitTotal, currency),
                                color  = TextSecondary,
                            )
                        } else if (autoSplitTotal < -0.001) {
                            BreakdownRow(
                                label  = "Discount / adjustment",
                                amount = "\u2212 ${MoneyUtils.format(abs(autoSplitTotal), currency)}",
                                color  = Green400,
                            )
                        }
                        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
                        BreakdownRow(
                            label    = "Grand total",
                            amount   = MoneyUtils.format(grandTotal, currency),
                            color    = TextPrimary,
                            bold     = true,
                        )
                    }
                }
            }

            // ── Items section ─────────────────────────────────────────────────
            if (realItems.isNotEmpty()) {
                item {
                    // Section header
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = "${realItems.size} ${if (realItems.size == 1) "item" else "items"}",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                        )
                        Text(
                            text       = MoneyUtils.format(itemsTotal, currency),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                        )
                    }
                }

                items(realItems, key = { it.id }) { item ->
                    ItemRow(item = item, currency = currency)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text("No items detected", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text(
                                "Add the expense manually instead.",
                                fontSize = 12.sp,
                                color    = TextTertiary,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}

// ── Receipt thumbnail ─────────────────────────────────────────────────────────

@Composable
private fun ReceiptThumbnail(imageUrl: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.lg)
    var showFull by remember { mutableStateOf(false) }

    if (imageUrl != null) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = "Receipt — tap to enlarge",
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .clip(shape)
                .clickable { showFull = true },
        )
        if (showFull) {
            Dialog(onDismissRequest = { showFull = false }) {
                AsyncImage(
                    model              = imageUrl,
                    contentDescription = "Receipt full size",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.lg))
                        .clickable { showFull = false },
                )
            }
        }
    } else {
        // Placeholder — not tappable when no image available
        Box(
            modifier         = modifier
                .clip(shape)
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Receipt,
                contentDescription = null,
                tint               = TextTertiary,
                modifier           = Modifier.size(28.dp),
            )
        }
    }
}

// ── Breakdown row (receipt summary) ──────────────────────────────────────────

@Composable
private fun BreakdownRow(
    label  : String,
    amount : String,
    color  : Color,
    bold   : Boolean = false,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = if (bold) TextPrimary else TextSecondary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier   = Modifier.weight(1f).padding(end = Spacing.sm),
        )
        Text(
            text       = amount,
            fontSize   = 12.sp,
            color      = color,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Real item row ─────────────────────────────────────────────────────────────

@Composable
private fun ItemRow(item: ExpenseItem, currency: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(
                text       = item.name,
                fontSize   = 13.sp,
                color      = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            if ((item.quantity ?: 1) > 1) {
                Text(
                    text     = "${item.quantity} \u00d7 ${MoneyUtils.format(item.price, currency)} each",
                    fontSize = 11.sp,
                    color    = TextTertiary,
                )
            }
        }
        Text(
            text       = MoneyUtils.format(item.totalPrice, currency),
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
        )
    }
}

// ── Charge row (visually quiet) ───────────────────────────────────────────────

@Composable
private fun ChargeRow(
    label    : String,
    amount   : Double,
    currency : String,
    hint     : String? = null,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(
                text     = label,
                fontSize = 12.sp,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hint != null) {
                Text(text = hint, fontSize = 10.sp, color = TextTertiary)
            }
        }
        val displayAmount = if (amount < 0)
            "\u2212 ${MoneyUtils.format(abs(amount), currency)}"
        else
            MoneyUtils.format(amount, currency)
        Text(
            text       = displayAmount,
            fontSize   = 12.sp,
            color      = if (amount < 0) Green400 else TextTertiary,
            fontWeight = FontWeight.Medium,
        )
    }
}