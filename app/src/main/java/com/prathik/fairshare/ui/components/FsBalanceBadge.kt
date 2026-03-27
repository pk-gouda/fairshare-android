package com.prathik.fairshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Green900
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.NegativeBg
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.util.MoneyUtils
import java.util.Locale

/**
 * Inline balance text — shows "+$12.50" in green or "-$8.00" in orange.
 * Used inside expense rows, friend rows, group tiles.
 *
 * [amount]   — positive = you lent (green), negative = you owe (orange)
 * [currency] — ISO currency code e.g. "USD", "EUR"
 */
@Composable
fun FsBalanceText(
    amount: Double,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    showSign: Boolean = true,
) {
    val color = remember(amount) {
        when {
            amount > 0 -> Green400
            amount < 0 -> Negative
            else       -> TextSecondary
        }
    }

    val text = remember(amount, currency, showSign) {
        val formatted = MoneyUtils.format(Math.abs(amount), currency)
        when {
            !showSign  -> formatted
            amount > 0 -> "+$formatted"
            amount < 0 -> "-$formatted"
            else       -> formatted
        }
    }

    Text(
        text       = text,
        color      = color,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        modifier   = modifier,
    )
}

/**
 * Rounded chip badge showing a balance amount.
 * Tinted background matches the sign — green bg for positive, orange bg for negative.
 * Used on group tiles, settle up screen.
 */
@Composable
fun FsBalanceBadge(
    amount: Double,
    modifier: Modifier = Modifier,
    currency: String = "USD",
) {
    val (textColor, bgColor) = remember(amount) {
        when {
            amount > 0 -> Pair(Green400, Green900)
            amount < 0 -> Pair(Negative, NegativeBg)
            else       -> Pair(TextSecondary, androidx.compose.ui.graphics.Color(0xFF1A1A1C))
        }
    }

    val text = remember(amount, currency) {
        val formatted = MoneyUtils.format(Math.abs(amount), currency)
        when {
            amount > 0 -> "+$formatted"
            amount < 0 -> "-$formatted"
            else       -> formatted
        }
    }

    Box(
        modifier = modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(Radius.full),
            )
            .padding(horizontal = Spacing.md, vertical = 4.dp),
    ) {
        Text(
            text       = text,
            color      = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 12.sp,
        )
    }
}

/**
 * Uppercase letter-spaced section label.
 * Used as section headers in lists — "THIS MONTH", "LAST MONTH", "PENDING".
 */
@Composable
fun FsSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text       = text.uppercase(Locale.getDefault()),
        color      = TextSecondary,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 1.sp,
        modifier   = modifier,
    )
}